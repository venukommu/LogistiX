package org.logistix.model.visualizer;

import org.logistix.model.edge.DecisionEdge;
import org.logistix.model.model.DecisionModel;
import org.logistix.model.node.DecisionNode;

/**
 * Visualizer contract for rendering DecisionModels into diagrams and topology schemas.
 */
public interface DecisionVisualizer {

    String toMermaid(DecisionModel model);

    String toJson(DecisionModel model);

    String toPlantUml(DecisionModel model);

    String toGraphViz(DecisionModel model);

    /**
     * Default visualizer rendering a standard Mermaid flowchart.
     */
    static DecisionVisualizer defaultVisualizer() {
        return new DecisionVisualizer() {
            @Override
            public String toMermaid(DecisionModel model) {
                StringBuilder sb = new StringBuilder("flowchart TD\n");
                for (DecisionNode node : model.getNodes()) {
                    sb.append("    ")
                            .append(node.getNodeId())
                            .append("[\"<b>")
                            .append(node.getName())
                            .append("</b><br/><i>(")
                            .append(node.getNodeType())
                            .append(")</i>\"]\n");
                }
                for (DecisionEdge edge : model.getEdges()) {
                    sb.append("    ")
                            .append(edge.sourceNodeId())
                            .append(" -->");
                    if (!edge.conditionExpression().isBlank()) {
                        sb.append("|").append(edge.conditionExpression()).append("|");
                    }
                    sb.append(" ")
                            .append(edge.targetNodeId())
                            .append("\n");
                }
                return sb.toString();
            }

            @Override
            public String toJson(DecisionModel model) {
                return String.format("{\"modelId\":\"%s\",\"name\":\"%s\",\"nodeCount\":%d,\"edgeCount\":%d}",
                        model.getModelId(), model.getName(), model.getNodes().size(), model.getEdges().size());
            }

            @Override
            public String toPlantUml(DecisionModel model) {
                StringBuilder sb = new StringBuilder("@startuml\n");
                for (DecisionEdge edge : model.getEdges()) {
                    sb.append("(")
                            .append(edge.sourceNodeId())
                            .append(") --> (")
                            .append(edge.targetNodeId())
                            .append(")\n");
                }
                sb.append("@enduml\n");
                return sb.toString();
            }

            @Override
            public String toGraphViz(DecisionModel model) {
                StringBuilder sb = new StringBuilder("digraph G {\n");
                for (DecisionEdge edge : model.getEdges()) {
                    sb.append("    \"")
                            .append(edge.sourceNodeId())
                            .append("\" -> \"")
                            .append(edge.targetNodeId())
                            .append("\";\n");
                }
                sb.append("}\n");
                return sb.toString();
            }
        };
    }
}
