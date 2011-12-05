package drugmodel;

import java.util.ArrayList;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class TransactionEdge<T> extends RepastEdge{
	
	private ArrayList<Transaction> transactionList; 

	/** This is the actual constructor used in the model. */
	public TransactionEdge(Object source, Object target, boolean directed) {
		super(source, target, directed);
		initialize();
	}

	public TransactionEdge(Object source, Object target, boolean directed, double weight) {
		super(source, target, directed, weight);
		initialize();
	}
		
	/** Called when the edge is undirected. */	
	public TransactionEdge(Object source, Object target) {
		super(source, target, false);
		initialize();
	}
	
	private void initialize() {
		transactionList = new ArrayList<Transaction>();
	}
	
	public void addTransaction(Transaction transaction) {
		if (transactionList.contains(transaction) == false) {
			transactionList.add(transaction);
		}
	}
	
	public int getLastTransactionIndex() {
		if (transactionList.isEmpty() ) {
			return -1;
		}
		else {
			return (transactionList.size()-1);
		}
	}

	public Transaction getLastTransaction() {
		if (transactionList.isEmpty() ) 
			return null;
		return transactionList.get(transactionList.size()-1);
	}
	
	public ArrayList<Transaction> getTransactionList() {
		return transactionList;
	}

	public void setTransactionList(ArrayList<Transaction> transactionList) {
		this.transactionList = transactionList;
	}
}