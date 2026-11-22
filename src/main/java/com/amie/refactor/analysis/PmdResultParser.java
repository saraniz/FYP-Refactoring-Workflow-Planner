package com.amie.refactor.analysis;

import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.SmellType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PmdResultParser {

    public List<CodeSmell> parse(Path pmdXmlPath)
            throws Exception {

        List<CodeSmell> codeSmells = new ArrayList<>();

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder =
                factory.newDocumentBuilder();

        Document document =
                builder.parse(pmdXmlPath.toFile());

        NodeList fileNodes =
                document.getElementsByTagName("file");

        for (int i = 0; i < fileNodes.getLength(); i++) {

            Element fileElement =
                    (Element) fileNodes.item(i);

            String filePath =
                    fileElement.getAttribute("name");

            NodeList violationNodes =
                    fileElement.getElementsByTagName("violation");

            for (int j = 0; j < violationNodes.getLength(); j++) {

                Element violationElement =
                        (Element) violationNodes.item(j);

                String ruleName =
                        violationElement.getAttribute("rule");

                SmellType smellType =
                        mapRuleToSmellType(ruleName);

                if (smellType == null) {
                    continue;
                }

                String className =
                        violationElement.getAttribute("class");

                // PMD reports the package and the class
                // separately. The fully qualified name is
                // needed to match findings with CK Tool
                // metrics, which also uses fully qualified
                // class names.
                String packageName =
                        violationElement.hasAttribute("package")
                                ? violationElement.getAttribute("package")
                                : "";

                String fullyQualifiedClassName =
                        packageName.isBlank()
                                ? className
                                : packageName + "." + className;

                String methodName =
                        violationElement.hasAttribute("method")
                                ? violationElement.getAttribute("method")
                                : null;

                int startLine =
                        Integer.parseInt(
                                violationElement.getAttribute("beginline")
                        );

                int endLine =
                        Integer.parseInt(
                                violationElement.getAttribute("endline")
                        );

                String severity =
                        violationElement.getAttribute("priority");

                String message =
                        violationElement.getTextContent().trim();

                CodeSmell codeSmell =
                        new CodeSmell(
                                smellType,
                                className,
                                fullyQualifiedClassName,
                                methodName,
                                filePath,
                                startLine,
                                endLine,
                                severity,
                                ruleName,
                                message
                        );

                codeSmells.add(codeSmell);
            }
        }

        return codeSmells;
    }

    private SmellType mapRuleToSmellType(String ruleName) {
        if (ruleName == null) {
            return null;
        }

        return switch (ruleName) {
            case "TooManyMethods", "GodClass", "ExcessivePublicCount" ->
                    SmellType.GOD_CLASS;

            case "NcssCount", "ExcessiveMethodLength", "CyclomaticComplexity", "CognitiveComplexity" ->
                    SmellType.LONG_METHOD;

            case "ExcessiveParameterList" ->
                    SmellType.DATA_CLUMPS;

            case "CouplingBetweenObjects", "LawOfDemeter" ->
                    SmellType.FEATURE_ENVY;

            default ->
                    null;
        };
    }
}
