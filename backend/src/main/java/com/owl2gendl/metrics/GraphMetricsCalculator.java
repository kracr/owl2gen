package com.owl2gendl.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLEntity;
import org.springframework.stereotype.Component;

@Component
public class GraphMetricsCalculator {

	public GraphMetrics analyze(Map<OWLEntity, Set<OWLEntity>> adjacency) {
		int nodeCount = adjacency.size();
		if (nodeCount == 0) {
			return new GraphMetrics(0, 0, 0.0, 0.0);
		}
		int edgeCount = adjacency.values().stream().mapToInt(Set::size).sum() / 2;
		double avgDegree = (2.0 * edgeCount) / nodeCount;
		double clusteringCoefficient = averageClusteringCoefficient(adjacency);
		return new GraphMetrics(nodeCount, edgeCount, avgDegree, clusteringCoefficient);
	}

	/**
	 * Standard (Watts-Strogatz) definition: average over every node of (closed triangles among its
	 * neighbors) / (possible triangles among its neighbors), with degree-&lt;2 nodes contributing 0.
	 */
	private double averageClusteringCoefficient(Map<OWLEntity, Set<OWLEntity>> adjacency) {
		double sum = 0.0;
		for (Set<OWLEntity> neighbors : adjacency.values()) {
			int degree = neighbors.size();
			if (degree < 2) {
				continue;
			}
			List<OWLEntity> neighborList = new ArrayList<>(neighbors);
			int linksAmongNeighbors = 0;
			for (int i = 0; i < neighborList.size(); i++) {
				Set<OWLEntity> neighborsOfNeighbor = adjacency.get(neighborList.get(i));
				for (int j = i + 1; j < neighborList.size(); j++) {
					if (neighborsOfNeighbor.contains(neighborList.get(j))) {
						linksAmongNeighbors++;
					}
				}
			}
			double possibleLinks = degree * (degree - 1) / 2.0;
			sum += linksAmongNeighbors / possibleLinks;
		}
		return sum / adjacency.size();
	}
}
