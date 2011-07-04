package drugmodel;

import drugmodel.Settings.Endorsement;

public class Transaction {
	private static int lastID = -1;
	private int ID;
	private int dealerID;
	private int customerID;
	private Integer time;
	private Double drugCost;
	private Double drugQtyInUnits;
	private Endorsement endorsement;
	private double taxAmount; 

	public Transaction() {
		ID = ++lastID;
		time = -1;
		drugCost = 0d;
		drugQtyInUnits = 0d;
		endorsement = Endorsement.None;
		taxAmount = 0.0d;
	}
	
	public Transaction(int _dealerID, int _customerID, Integer _time, double _drugCost, double _drugQty, Endorsement _endorsement) {
		ID = ++lastID;
		dealerID = _dealerID;
		customerID = _customerID;
		time = _time;
		drugCost = _drugCost;
		drugQtyInUnits = _drugQty;
		endorsement = _endorsement;
	}
	
	public Transaction(Transaction transaction) {
		ID = ++lastID;
		dealerID = transaction.getDealerID();
		customerID = transaction.getCustomerID();
		time = transaction.getTime();
		drugCost = transaction.getDrugCost();
		drugQtyInUnits = transaction.getDrugQtyInUnits();
		endorsement = transaction.getEndorsement();
	}

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
		this.time = time;
	}

	public Double getDrugCost() {
		return drugCost;
	}

	public void setDrugCost(Double drugCost) {
		this.drugCost = drugCost;
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

	public int getDealerID() {
		return dealerID;
	}

	public void setDealerID(int dealerID) {
		this.dealerID = dealerID;
	}

	public int getCustomerID() {
		return customerID;
	}

	public void setCustomerID(int customerID) {
		this.customerID = customerID;
	}
	
	public void print() {
		System.out.println("Trans: " + ID + " dealerID: " + dealerID + " time: " + time + " cost: " + drugCost + " qty: " + drugQtyInUnits + " endorsement: " + endorsement);
	}

	public double getTaxAmount() {
		return taxAmount;
	}

	public void setTaxAmount(double taxAmount) {
		this.taxAmount = taxAmount;
	}
}
