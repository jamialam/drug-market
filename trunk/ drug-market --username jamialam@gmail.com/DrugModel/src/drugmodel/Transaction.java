package drugmodel;

import individual.Dealer;
import drugmodel.Settings.Endorsement;

public class Transaction {
	private static int lastID = -1;
	private int ID;
	private Dealer dealer;
	private int customerID;
	private Integer time;
	private Double costPerUnit;
	private Double drugQtyInUnits;
	private Endorsement endorsement;
	private double taxAmount; 

	public Transaction() {
		ID = ++lastID;
		time = -1;
		costPerUnit = 0d;
		drugQtyInUnits = 0d;
		endorsement = Endorsement.None;
		taxAmount = 0.0d;
	}
	
	public Transaction(Dealer _dealer, int _customerID, Integer _time, double _drugCost, double _drugQty, Endorsement _endorsement) {
		ID = ++lastID;
		//dealerID = _dealerID;
		dealer = _dealer;
		customerID = _customerID;
		time = _time;
		costPerUnit = _drugCost;
		drugQtyInUnits = _drugQty;
		endorsement = _endorsement;
	}
	
	public Transaction(Transaction transaction) {
		ID = ++lastID;
		dealer = transaction.getDealer();
		customerID = transaction.getCustomerID();
		time = transaction.getTime();
		costPerUnit = transaction.getCostPerUnit();
		drugQtyInUnits = transaction.getDrugQtyInUnits();
		endorsement = transaction.getEndorsement();
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
}
