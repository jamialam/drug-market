package drugmodel;

import java.util.ArrayList;

import org.aspectj.weaver.patterns.ArgsAnnotationPointcut;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class TransactionEdge<T> extends RepastEdge{
	/** Tick when this edge was created. 0 for those created at the setup. */
	private double timeCreated;
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
	public TransactionEdge(Object source, Object target, double _timeCreated) {
		super(source, target, false);
		initialize(_timeCreated);
	}
	
	private void initialize(double... args) {
		if (args.length == 0) {
			timeCreated = 0;
		}
		else {
			this.timeCreated = args[0];
		}
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

	public double getTimeCreated() {
		return timeCreated;
	}
}