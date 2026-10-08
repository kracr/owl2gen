package com.owl2gendl.api.dto;

import java.util.List;

/** {@code truncated}: true when the ontology had more edges than {@code OntologyGraphBuilder.MAX_EDGES} and
 *  the excess was cut off, so the frontend can warn that the rendered graph is a partial view. */
public record OntologyGraphDto(
		List<GraphNodeDto> nodes,
		List<GraphEdgeDto> edges,
		boolean truncated) {
}
