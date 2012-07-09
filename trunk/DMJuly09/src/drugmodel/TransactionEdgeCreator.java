package drugmodel;

import repast.simphony.space.graph.EdgeCreator;

public class TransactionEdgeCreator<T> implements EdgeCreator<TransactionEdge<T>, T> {
	public TransactionEdge<T> createEdge(T source, T target, boolean isDirected, double weight) {
		return new TransactionEdge<T>(source, target, isDirected, weight);
	}
	public TransactionEdge<T> createEdge(T source, T target, boolean isDirected) {
		return new TransactionEdge<T>(source, target, isDirected);
	}	
	public TransactionEdge<T> createEdge(T source, T target, double _timeCreated) {
		return new TransactionEdge<T>(source, target, _timeCreated);
	}	
	@SuppressWarnings("rawtypes")
	public Class<TransactionEdge> getEdgeType() {
		return TransactionEdge.class;
	}

}

