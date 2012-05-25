package drugmodel;

import repast.simphony.space.graph.EdgeCreator;

public class SNEdgeCreator<T> implements EdgeCreator<SNEdge<T>, T> {
	public SNEdge<T> createEdge(T source, T target, boolean isDirected, double weight) {
		return new SNEdge<T>(source, target, isDirected, weight);
	}
	public SNEdge<T> createEdge(T source, T target, boolean isDirected) {
		return new SNEdge<T>(source, target, isDirected);
	}
	
	@SuppressWarnings("rawtypes")
	public Class<SNEdge> getEdgeType() {
		return SNEdge.class;
	}

}

