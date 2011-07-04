package drugmodel;

import java.util.ArrayList;
import java.util.EnumMap;

import drugmodel.Settings.Endorsement;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SNEdge<T> extends RepastEdge {	
	private ArrayList<Transaction> transactionList;
	private EnumMap<Endorsement, ArrayList<Double>> endorsements;
	
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
		endorsements = new EnumMap<Settings.Endorsement, ArrayList<Double>>(Endorsement.class);
		for (Endorsement endorsement : Endorsement.values()) {
			endorsements.put(endorsement, new ArrayList<Double>());
		}
	}
	
	public void addTransaction(Transaction transaction) {
		if (transactionList.contains(transaction) == false) {
			transactionList.add(transaction);
		}
	}
	
	public int returnLastTransactionIndex() {
		if (transactionList.isEmpty()) {
			return -1;
		}
		else {
			return (transactionList.size()-1);
		}
	}

	public Transaction returnLastTransaction() {
		return transactionList.get(transactionList.size()-1);
	}
	
	public int returnTotalTransactions() {
		return transactionList.size();
	}
	 
	public int returnNumGoodEndorsements() {
		return endorsements.get(Endorsement.Good).size();
	}
	
	public int returnNumBadEndorsements() {
		return endorsements.get(Endorsement.Bad).size();
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
}