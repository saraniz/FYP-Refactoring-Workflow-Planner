package com.amie.refactor.planning;

import java.util.*;

/**
 * Directed acyclic graph (DAG) representing precedence and ordering constraints
 * between refactoring candidates.
 *
 * <p>An edge A -> B indicates that candidate A is a prerequisite for candidate B
 * (A must be refactored before B). Ordering uses Kahn's algorithm augmented with a
 * Max-Priority Queue so that unconstrained steps are chosen by descending priority score.</p>
 */
public class RefactoringDependencyGraph {

    private final Map<String, RefactoringCandidate> candidateMap;
    private final Map<String, Set<String>> outgoingEdges; // u -> {v} (u must precede v)
    private final Map<String, Set<String>> incomingEdges; // v -> {u} (u is prerequisite of v)
    private final List<String> warnings;

    public RefactoringDependencyGraph(List<RefactoringCandidate> candidates) {
        this.candidateMap = new LinkedHashMap<>();
        this.outgoingEdges = new LinkedHashMap<>();
        this.incomingEdges = new LinkedHashMap<>();
        this.warnings = new ArrayList<>();

        for (RefactoringCandidate c : candidates) {
            candidateMap.put(c.getCandidateId(), c);
            outgoingEdges.put(c.getCandidateId(), new LinkedHashSet<>());
            incomingEdges.put(c.getCandidateId(), new LinkedHashSet<>());
        }
    }

    /**
     * Adds a precedence constraint: prerequisiteId MUST be executed before dependentId.
     */
    public boolean addPrecedenceConstraint(String prerequisiteId, String dependentId, String reason) {
        if (prerequisiteId.equals(dependentId)) {
            return false;
        }
        if (!candidateMap.containsKey(prerequisiteId) || !candidateMap.containsKey(dependentId)) {
            return false;
        }

        // Add edge prerequisiteId -> dependentId
        outgoingEdges.get(prerequisiteId).add(dependentId);
        incomingEdges.get(dependentId).add(prerequisiteId);

        // Check for cycle introduced by this edge
        if (hasCycle()) {
            // Remove edge to maintain DAG property
            outgoingEdges.get(prerequisiteId).remove(dependentId);
            incomingEdges.get(dependentId).remove(prerequisiteId);
            warnings.add("Circular constraint avoided between " + prerequisiteId + " and " + dependentId + " (" + reason + ")");
            return false;
        }

        // Record in candidate model for traceability
        candidateMap.get(dependentId).addPrerequisite(prerequisiteId);
        return true;
    }

    private boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (String node : candidateMap.keySet()) {
            if (isCyclicUtil(node, visited, recStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCyclicUtil(String node, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        recStack.add(node);

        for (String neighbor : outgoingEdges.getOrDefault(node, Collections.emptySet())) {
            if (isCyclicUtil(neighbor, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(node);
        return false;
    }

    /**
     * Executes Priority-Topological Sort (Kahn's Algorithm with Max-Priority Queue).
     * Candidates with no unsatisfied prerequisites are scheduled first, ordered by
     * their calculated priority score (highest score first).
     */
    public List<RefactoringCandidate> getTopologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String id : candidateMap.keySet()) {
            inDegree.put(id, incomingEdges.get(id).size());
        }

        // Max-priority queue ordered by candidate priority score (descending)
        PriorityQueue<RefactoringCandidate> readyQueue = new PriorityQueue<>();

        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                readyQueue.add(candidateMap.get(entry.getKey()));
            }
        }

        List<RefactoringCandidate> ordered = new ArrayList<>();
        while (!readyQueue.isEmpty()) {
            RefactoringCandidate current = readyQueue.poll();
            ordered.add(current);

            for (String neighborId : outgoingEdges.getOrDefault(current.getCandidateId(), Collections.emptySet())) {
                int updatedInDegree = inDegree.get(neighborId) - 1;
                inDegree.put(neighborId, updatedInDegree);
                if (updatedInDegree == 0) {
                    readyQueue.add(candidateMap.get(neighborId));
                }
            }
        }

        // If any remaining candidates were not processed (due to any edge issue), append by priority
        if (ordered.size() < candidateMap.size()) {
            List<RefactoringCandidate> remaining = new ArrayList<>();
            for (RefactoringCandidate c : candidateMap.values()) {
                if (!ordered.contains(c)) {
                    remaining.add(c);
                }
            }
            remaining.sort(Comparator.naturalOrder());
            ordered.addAll(remaining);
            warnings.add("Topological sort resolved " + remaining.size() + " unlinked candidates via greedy priority ordering.");
        }

        return ordered;
    }

    public int getEdgeCount() {
        int count = 0;
        for (Set<String> targets : outgoingEdges.values()) {
            count += targets.size();
        }
        return count;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
