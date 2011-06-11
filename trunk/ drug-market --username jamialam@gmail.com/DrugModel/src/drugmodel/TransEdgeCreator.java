package drugmodel;

import repast.simphony.space.graph.EdgeCreator;

public class TransEdgeCreator<T> implements EdgeCreator<TransEdge<T>, T> {
	public TransEdge<T> createEdge(T source, T target, boolean isDirected, double weight) {
		return new TransEdge<T>(source, target, isDirected, weight);
		}
		public TransEdge<T> createEdge(T source, T target, boolean isDirected) {
			return new TransEdge<T>(source, target, isDirected);
		}	
	
	@SuppressWarnings("rawtypes")
	public Class<TransEdge> getEdgeType() {
		return TransEdge.class;
	}

}

