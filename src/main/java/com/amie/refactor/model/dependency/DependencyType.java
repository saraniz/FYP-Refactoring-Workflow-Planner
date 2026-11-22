package com.amie.refactor.model.dependency;

/**
 * Types of structural and behavioural dependencies between Java artifacts.
 * Based on Zhang et al. (2025) and standard object-oriented dependency taxonomies.
 */
public enum DependencyType {

    /** Method invocation from caller to callee. */
    CALLS,

    /** Class inheritance (extends). */
    EXTENDS,

    /** Interface implementation (implements). */
    IMPLEMENTS,

    /** Class contains a field of target type. */
    CONTAINS,

    /** Field access / read / write on target object. */
    USES_FIELD,

    /** Target type appears as a method parameter. */
    PARAMETER,

    /** Target type appears as a method return type. */
    RETURNS,

    /** Object instantiation (new TargetClass()). */
    CREATES,

    /** Source file imports target class. */
    IMPORTS
}
