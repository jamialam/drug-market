package drugmodel;

import java.util.ArrayList;

import cern.jet.random.Uniform;

import drugmodel.Settings.Endorsement;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SNEdge<T> extends RepastEdge {
	/** Holds endorsed deals that are endorsed by the source to the target. These deals were shared by the target and used by the source.*/
	private ArrayList<TransactionEndorsement> endorsedTransactionList;
	/** Holds transactions shared by the source to the target */	
	private ArrayList<Transaction> transactionList;
	
	/** holds number of times, tax is paid by the source to the target */
	private int numTimesTaxPaid ;	
	/** Number of times, source should pay tax before getting direct access to dealer */
	private int maxTimesPayTax ; 

	private class TransactionEndorsement {
		private Transaction transaction;
		private Endorsement endorsement;
		private double endorsementTime;
		public TransactionEndorsement(Transaction _transaction, Endorsement _endorsement, double _endorsementTime) {
			transaction = _transaction;
			endorsement = _endorsement;
			endorsementTime = _endorsementTime;
		}
	}

	/** This is the actual constructor used in the model. */
	public SNEdge(Object source, Object target,	boolean directed) {
		super(source, target, directed);
		initialize();
	}	

	public SNEdge(Object source, Object target, boolean directed, double weight) {
		super(source, target, directed, weight);
		initialize();
	}

	private void initialize() {
		transactionList = new ArrayList<Transaction>();
		endorsedTransactionList = new ArrayList<SNEdge<T>.TransactionEndorsement>();
		numTimesTaxPaid = 0;
		maxTimesPayTax = Uniform.staticNextIntFromTo(Settings.CustomerParams.MinTimesPayTax,Settings.CustomerParams.MaxTimesPayTax );
	}
	public int getNumTimesTaxPaid(){
		return numTimesTaxPaid;
	}
	/**
	 * We reset the number of times a tax is paid to ZERO.
	 * Then redraw the maxTimesPayTax so that it is different for each round of tax payments 
	 * between the two agents.
	 */
	public void resetNumTimesTaxPaid(){
		numTimesTaxPaid = 0;
		maxTimesPayTax = Uniform.staticNextIntFromTo(Settings.CustomerParams.MinTimesPayTax,Settings.CustomerParams.MaxTimesPayTax );
	}
	public void incNumTimesTaxPaid(){
		++numTimesTaxPaid;
	}
	public int getMaxTimesPayTax(){
		return maxTimesPayTax;
	}
	
	/** Called in the Customer class by the updateEndorsement() method to update endorsement of a shared deal. */
	public void addEndorsement(Transaction deal, Endorsement endorsement, double time) {
		TransactionEndorsement etransaction = new TransactionEndorsement(deal, endorsement, time);
		endorsedTransactionList.add(etransaction);
	}

	public void addTransaction(Transaction transaction) {
		if (transactionList.contains(transaction) == false) {
			transactionList.add(transaction);
		}
	}

	/**	Returns the number of bad endorsements on this directed edge within @param timeWindow. 
	 * 	Called by source to get its endorsements for the target agent. 
	 */
	public int returnNumBadEndorsement (double timeWindow){
		if(endorsedTransactionList.isEmpty()) {
			return 0;
		}
		int numBadDeals = 0;
		double currentTick = ContextCreator.getTickCount();
		for(TransactionEndorsement endorsedTransaction : endorsedTransactionList) {
			if(currentTick - endorsedTransaction.endorsementTime < timeWindow 
					&& ( endorsedTransaction.endorsement == Endorsement.DealerNotFoundBad
						|| endorsedTransaction.endorsement == Endorsement.SoldBad
						|| endorsedTransaction.endorsement == Endorsement.UnSoldBad ) ) {
				++numBadDeals;
			}
		}
		return numBadDeals;
	}
	/**	Returns the number of good endorsements on this directed edge within @param timeWindow. 
	 * 	Called by source to get its endorsements for the target agent. use to sort customer's preference for other customers
	 */
	public int returnNumGoodEndorsement (double timeWindow){
		if(endorsedTransactionList.isEmpty()) {
			return 0;
		}
		int numGoodDeals = 0;
		double currentTick = ContextCreator.getTickCount();
		for(TransactionEndorsement endorsedTransaction : endorsedTransactionList) {
			if(currentTick - endorsedTransaction.endorsementTime < timeWindow 
					&& endorsedTransaction.endorsement == Endorsement.Good) {
				++numGoodDeals;
			}
		}
		return numGoodDeals;
	}

	/**	Returns index of the last transaction that is shared by the source with the target. */
	public int returnLastTransactionIndex() {
		if (transactionList.isEmpty()) {
			return -1;
		}
		else {
			return (transactionList.size()-1);
		}
	}

	/**	Returns the last transaction that is shared by the source with the target. */
	public Transaction returnLastTransaction() {
		return transactionList.get(transactionList.size()-1);
	}
}