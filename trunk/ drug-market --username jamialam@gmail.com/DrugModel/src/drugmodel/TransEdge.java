package drugmodel;

import java.util.ArrayList;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings("rawtypes")
public class TransEdge<T> extends RepastEdge{
	
	//private EdgeDataMap<Integer, Transaction> transactionData;
	private ArrayList<Transaction> transactionList; 
	
	@SuppressWarnings("unchecked")
	public TransEdge(Object source, Object target, boolean directed, double weight) {
		super(source, target, directed, weight);
		initialize();
	}
	
	@SuppressWarnings("unchecked")
	public TransEdge(Object source, Object target,	boolean directed) {
		super(source, target, directed);
		initialize();
	}
	
	@SuppressWarnings("unchecked")
	/** Called when the edge is undirected. */	
	public TransEdge(Object source, Object target) {
		super(source, target, false);
		initialize();
	}
	
	private void initialize() {
		transactionList = new ArrayList<Transaction>();
		//transactionData = new EdgeDataMap<Integer, Transaction>();
	}

	public ArrayList<Transaction> getTransactionList() {
		return transactionList;
	}

	public void setTransactionList(ArrayList<Transaction> transactionList) {
		this.transactionList = transactionList;
	}
	
/*	public EdgeDataMap<Integer, Transaction> getTransactionData() {
		return transactionData;
	}

	public void setTransactionData(EdgeDataMap<Integer, Transaction> transactionData) {
		this.transactionData = transactionData;
	}*/
}