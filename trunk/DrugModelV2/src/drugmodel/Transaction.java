package drugmodel;

import individual.Dealer;
import drugmodel.Settings.Endorsement;

public class Transaction {
	public static int lastID = -1;
	private int ID;
	private Dealer dealer;
	private int customerID;
	private Integer time;
	private Double costPerUnit;
	private Double drugQtyInUnits;
	private Endorsement endorsement;
	private double taxAmount; 
	private int dealUsed;
	private int whoseDealUsed;
	
	public Transaction() {
		ID = ++lastID;
		time = -1;
		costPerUnit = 0d;
		drugQtyInUnits = 0d;
		endorsement = Endorsement.None;
		taxAmount = 0.0d;
		dealUsed = -1;
		whoseDealUsed = -1;
//		System.out.println("new transcation: " + ID);

	}
	
	public int getID() {
		return ID;
	}

	public Transaction(Dealer _dealer, int _customerID, Integer _time, double _drugCost, double _drugQty, Endorsement _endorsement, int deal_used, int whose_deal_used) {
		ID = ++lastID;
		dealer = _dealer;
		customerID = _customerID;
		if( Settings.errorLog && _time == -1)
			System.out.println("TIME -1");
		time = _time;
		costPerUnit = _drugCost;
		drugQtyInUnits = _drugQty;
		endorsement = _endorsement;
		taxAmount = 0.0d;
		dealUsed = deal_used;
		whoseDealUsed = whose_deal_used;
	//	System.out.println("new transcation: " + ID);

	}
	
	public Transaction(Transaction transaction) {
		ID = ++lastID;
		dealer = transaction.getDealer();
		customerID = transaction.getCustomerID();
		time = transaction.getTime();
		costPerUnit = transaction.getCostPerUnit();
		drugQtyInUnits = transaction.getDrugQtyInUnits();
		endorsement = transaction.getEndorsement();
		taxAmount = transaction.getTaxAmount();
		dealUsed = transaction.getDealUsed();
		whoseDealUsed = transaction.getWhoseDealUsed();
		if(time == -1)
			System.out.println("TIME2 -1");

//		System.out.println("new transcation: " + ID);

	}

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
		this.time = time;
	}

	public Double getCostPerUnit() {
		return costPerUnit;
	}

	public void setCostPerUnit(Double drugCost) {
		this.costPerUnit = drugCost;
	}

	public Double getDrugQtyInUnits() {
		return drugQtyInUnits;
	}

	public void setDrugQtyInUnits(Double drugQty) {
		this.drugQtyInUnits = drugQty;
	}

	public Endorsement getEndorsement() {
		return endorsement;
	}

	public void setEndorsement(Endorsement endorsement) {
		this.endorsement = endorsement;
	}

	public int getCustomerID() {
		return customerID;
	}

	public void setCustomerID(int customerID) {
		this.customerID = customerID;
	}
	
	public void print() {
		System.out.println("Trans: " + ID + " dealerID: " + dealer.getPersonID() + " time: " + time + " cost: " + costPerUnit + " qty: " + drugQtyInUnits + " endorsement: " + endorsement);
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

	public static int getLastID() {
		return lastID;
	}
}
