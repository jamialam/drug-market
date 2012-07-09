package drugmodel;

import individual.Dealer;
import drugmodel.Settings.Endorsement;
import drugmodel.Settings.TransactionType;

public class Transaction {
	public static int lastID = -1;
	/** Transaction ID*/
	private int transactionID;
	/** Dealer of this transaction */
	private Dealer dealer;
	/** Customer ID who bought the deal */
	private int customerID;
	/** Time of transaction in time steps */
	private Double time;
	/** Cost per unit charged by the dealer */
	private Double costPerUnit;
	/** drug quantity in units sold to the customer */
	private Double drugQtyInUnits;
	/** Endorsement of the deal by the customer */
	private Endorsement endorsement;
	/** Amount of tax paid, if the customer used a shared deal */
	private double taxAmount; 
	/** ID of the deal that initiated by deal */
	private int dealUsed;
	/** ID of the customer whose deal the buying customer used. If it is own deal then it will be the buying customer's ID */
	private int whoseDealUsed;
	
	private TransactionType type; 
	
//	private int drugBoughtFor;
	

	/**
	 * 
	 * @param _dealer
	 * @param _customerID If the customerID is the same as whoseDealUsed then one's own deal was used.  
	 * @param _time
	 * @param _costPerUnit
	 * @param _drugQtyInUnits
	 * @param _endorsement
	 * @param _dealUsed
	 * @param _whoseDealUsed
	 */
	public Transaction (Dealer _dealer, int _customerID, double _time, double _costPerUnit, double _drugQtyInUnits, Endorsement _endorsement, int _dealUsed, int _whoseDealUsed,
			TransactionType _type){
		transactionID = ++lastID;
		dealer = _dealer;
		customerID = _customerID;
		time = _time;
		costPerUnit = _costPerUnit;
		drugQtyInUnits = _drugQtyInUnits;
		endorsement = _endorsement;
		taxAmount = 0.0d;
		dealUsed = _dealUsed;
		whoseDealUsed = _whoseDealUsed;
		type = _type;
		if( Settings.outputLog && Settings.errorLog && _time == -1) {
			System.out.println("A. TIME is -1. Actual time from context creator: " + ContextCreator.getTickCount());
		}
	}
	
	/**
	 * 
	 * @param transaction
	 */
/*	public Transaction (Transaction transaction) {
		transactionID = ++lastID;
		dealer = transaction.getDealer();
		customerID = transaction.getCustomerID();
		time = transaction.getTime();
		costPerUnit = transaction.getCostPerUnit();
		drugQtyInUnits = transaction.getDrugQtyInUnits();
		endorsement = transaction.getEndorsement();
		taxAmount = transaction.getTaxAmount();
		dealUsed = transaction.getDealUsed();
		whoseDealUsed = transaction.getWhoseDealUsed();
		
		if( Settings.errorLog && time == -1) {
			System.out.println("B. TIME is -1. Actual time from context creator: " + ContextCreator.getTickCount());
		}
	}
*/	
	public void print() {
		System.out.println("Trans: " + transactionID + " dealerID: " + dealer.getPersonID()
				+ "customer id: " + customerID + " whose deal used: " + whoseDealUsed
				+ " deal type: " + type
				+ " time: " + time + " cost: " + costPerUnit + " qty: " + drugQtyInUnits + " endorsement: " + endorsement);
	}

	public Double getCostPerUnit() {
		return costPerUnit;
	}
	public Double getDrugQtyInUnits() {
		return drugQtyInUnits;
	}
	public Endorsement getEndorsement() {
		return endorsement;
	}
	/**
	 * customerid is the id of the customr who is paying - the actual buyer and not the broker.
	 * @return
	 */
	public int getCustomerID() {
		return customerID;
	}	
	public double getTaxAmount() {
		return taxAmount;
	}
	public void setTaxAmount(double taxAmount) {
		this.taxAmount = taxAmount;
	}	
	public Dealer getDealer() {
		return dealer;
	}
	public void setDealer(Dealer dealer) {
		this.dealer = dealer;
	}
	public int getDealUsed() {
		return dealUsed;
	}
	public int getWhoseDealUsed() {
		return whoseDealUsed;
	}
	public TransactionType getTransactionType() {
		return type;
	}
	public static int getLastID() {
		return lastID;
	}
	public int getTransactionID() {
		return transactionID;
	}
	public Double getTime() {
		return time;
	}
}