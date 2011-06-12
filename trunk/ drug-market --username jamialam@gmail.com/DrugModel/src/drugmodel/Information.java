package drugmodel;

import drugmodel.Settings.Endorsement;

/* We don't need to store the drug dealer's reference on the TransactionEdge ... only the ID.
 *  We can then iterate over the alter customer's dealers' list and fetch the dealer.
 *  Of course, only programmatically; otherwise this information isn't global from a
 *  fellow customer's perspective. 
 *   
 *  This may be slow but it might be useful when removing/adding agents. 
*/

public class Information {
	private Integer time;
	private Double drugCost;
	private Double drugQty;
	private Endorsement endorsement;	
	private int dealerID;

	public Information() {
		time = -1;
		drugCost = 0d;
		drugQty = 0d;
		endorsement = Endorsement.None;
		dealerID = -1;
	}
		
	public Information(Integer _time, double _drugCost, double _drugQty, Endorsement _endorsement, int _dealerID) {
		time = _time;
		drugCost = _drugCost;
		drugQty = _drugQty;
		endorsement = _endorsement;
		dealerID = _dealerID;
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

	public int getDealerID() {
		return dealerID;
	}

	public void setDealerID(int dealerID) {
		this.dealerID = dealerID;
	}
}