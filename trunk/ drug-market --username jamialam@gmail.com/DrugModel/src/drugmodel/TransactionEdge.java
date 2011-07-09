package drugmodel;

import java.util.ArrayList;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings("rawtypes")
public class TransactionEdge<T> extends RepastEdge{
	
	//private EdgeDataMap<Integer, Transaction> transactionData;
	private ArrayList<Transaction> transactionList; 
	
	@SuppressWarnings("unchecked")
	public TransactionEdge(Object source, Object target, boolean directed, double weight) {
		super(source, target, directed, weight);
		initialize();
	}
	
	@SuppressWarnings("unchecked")
	public TransactionEdge(Object source, Object target,	boolean directed) {
		super(source, target, directed);
		initialize();
	}
	
	@SuppressWarnings("unchecked")
	/** Called when the edge is undirected. */	
	public TransactionEdge(Object source, Object target) {
		super(source, target, false);
		initialize();
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
		return transactionList.get(transactionList.size()-1);
	}
	
	public int getTotalTransactions() {
		return transactionList.size();
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