package drugmodel;

import java.util.ArrayList;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SNEdge<T> extends RepastEdge {
	//private EdgeDataMap<Integer, Transaction> TransactionData;	
	private ArrayList<Transaction> transactionList;
	
	public SNEdge(Object source, Object target, boolean directed, double weight) {
		super(source, target, directed, weight);
		initialize();
	}

	public SNEdge(Object source, Object target,	boolean directed) {
		super(source, target, directed);
		initialize();
	}	
	
	private void initialize() {
		transactionList = new ArrayList<Transaction>();
		//TransactionData = new EdgeDataMap<Integer, Transaction>();
	}
	
	public void addTransaction(Transaction transaction) {
		if (transactionList.contains(transaction) == false) {
			transactionList.add(transaction);
		}
	}
	
	public int getLastTransactionIndex() {
		if (transactionList.isEmpty()) {
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
		
	public int getLastDealIndex() {
		if (this.transactionList.isEmpty()) {
			return -1;
		}
		else {
			return (this.transactionList.size()-1);
		}
	}
	
	public ArrayList<Transaction> getTransactionList() {
		return transactionList;
	}

	public void setTransactionList(ArrayList<Transaction> transactionData) {
		this.transactionList = transactionData;
	}
	
/*	public EdgeDataMap<Integer, Transaction> getTransactionData() {
		return TransactionData;
	}

	public void setTransactionData(EdgeDataMap<Integer, Transaction> TransactionData) {
		this.TransactionData = TransactionData;
	}*/
}