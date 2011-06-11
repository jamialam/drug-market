package drugmodel;

import drugmodel.Settings.Endorsement;

public class Transaction {
	private Integer time;
	private Double drugCost;
	private Double drugQty;
	private Endorsement endorsement;	

	public Transaction() {
		time = -1;
		drugCost = 0d;
		drugQty = 0d;
		endorsement = Endorsement.None;
	}
	
	public Transaction(Integer _time, double _drugCost, double _drugQty, Endorsement _endorsement) {
		time = _time;
		drugCost = _drugCost;
		drugQty = _drugQty;
		endorsement = _endorsement;
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

	public Double getDrugQty() {
		return drugQty;
	}

	public void setDrugQty(Double drugQty) {
		this.drugQty = drugQty;
	}

	public Endorsement getEndorsement() {
		return endorsement;
	}

	public void setEndorsement(Endorsement endorsement) {
		this.endorsement = endorsement;
	}
}
