package drugmodel;

import java.util.ArrayList;
import java.util.EnumMap;

import drugmodel.Settings.Endorsement;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SNEdge<T> extends RepastEdge {	
	//Added by SA
	private class TransactionEndorsement {
		public Transaction transaction;
		public Endorsement endorsement;
		public double endorsementTime;
	}
	//hold endorsed deals only
	private ArrayList<TransactionEndorsement> etransactionList;
	//by SJA	
	private ArrayList<Transaction> transactionList;
	//map imp
//	private EnumMap<Endorsement, ArrayList<Double>> endorsements;
	
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
		//map imp
		/*		endorsements = new EnumMap<Settings.Endorsement, ArrayList<Double>>(Endorsement.class);
		for (Endorsement endorsement : Endorsement.values()) {
			endorsements.put(endorsement, new ArrayList<Double>());
		}
*/	//Added by SA
		etransactionList = new ArrayList<SNEdge<T>.TransactionEndorsement>();
		
	}
	
	public void addEndorsement(Transaction deal , Endorsement endorsement, double time) {
	//map imp	
//		endorsements.get(endorsement).add(time);

		//Added by SA
		TransactionEndorsement etransaction = new TransactionEndorsement();
		etransaction.transaction = deal;
		etransaction.endorsement = endorsement;
		etransaction.endorsementTime = time;
		etransactionList.add(etransaction);
//		System.out.println("DEal endorsed and Added");
/*		if(etransactionList.isEmpty() ){
			System.err.println("DEal to be endorsed not found. list is empty.");
		}
		for (TransactionEndorsement etransaction : etransactionList){
			if(etransaction.transaction.getID() == deal.getID() ){
				etransaction.endorsement = endorsement;
				etransaction.endorsementTime = time;
				System.out.println("DEal found.");
				break;
			}
		}
*/			
	}

	//public void addEndorsement()
	public void addTransaction(Transaction transaction) {
		if (transactionList.contains(transaction) == false) {
			transactionList.add(transaction);
		}
		//Added by SA
/*		TransactionEndorsement etransaction = new TransactionEndorsement();
		etransaction.transaction = transaction;
		etransaction.endorsement = Endorsement.None;
		etransaction.endorsementTime = -1;
		if (etransactionList.contains(etransaction) == false) {
			etransactionList.add(etransaction);
			System.out.println("DEal Added");
		}
*/		
	}
	public int returnNumBadEndorsement(double time_frame){
		int bad_deal=0;
		double currentTick = ContextCreator.getTickCount();
		if(etransactionList.isEmpty())
			return 0;
		for(TransactionEndorsement etransaction : etransactionList){
			if(currentTick - etransaction.endorsementTime < time_frame 
					&& etransaction.endorsement == Endorsement.Bad ){
				++bad_deal;
	//			System.out.println("time frame:" + time_frame + " current time:" + currentTick + "  endorsement time:"+ etransaction.endorsementTime  +  "CT - ET: " + (currentTick -etransaction.endorsementTime ));
			}
		}
	//	System.out.println("num of bad deals in window:" + bad_deal);
		return bad_deal;
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
	//map imp	 
	/*public int returnNumGoodEndorsements() {
		return endorsements.get(Endorsement.Good).size();
	}
	
	public int returnNumBadEndorsements() {
		return endorsements.get(Endorsement.Bad).size();
	}
	*/	
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