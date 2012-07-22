package test;

import individual.Customer;
import individual.Dealer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cern.jet.random.Uniform;

import drugmodel.Settings;
import drugmodel.Settings.TransactionType;
import drugmodel.Transaction;
import drugmodel.Settings.Endorsement;
import drugmodel.Settings.TaxType;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<Transaction> deals = new ArrayList<Transaction>();
		int time = 0;
		double cost = 0;
		Endorsement endorsment = Endorsement.None;
		for (int i=0; i<10; i++) {
			time = Uniform.staticNextIntFromTo(1, 10);
			//			cost = Uniform.staticNextDoubleFromTo(50, 100);
			cost = 120;
			//Dealer dealer = new Dealer();
			//public Transaction (Dealer _dealer, int _customerID, double _time, double _costPerUnit, double _drugQtyInUnits, Endorsement _endorsement, int _dealUsed, int _whoseDealUsed) {
			Transaction deal = new Transaction(null, Uniform.staticNextIntFromTo(1, 10), time, cost, Uniform.staticNextIntFromTo(9, 15), endorsment, -1, -1, TransactionType.directDeal);
			deal.setTaxAmount(Uniform.staticNextDoubleFromTo(5, 10));
			/*			if (Math.random() <= 0.5) {
				if (Math.random() <= 0.1)
					deal.setCostPerUnit(0.0);
			}*/			
			deals.add(deal);
			deal.print();						
		}
	
		
		Collections.sort(deals, new Comparator<Transaction>() {
			public int compare(Transaction t1, Transaction t2) {
				int personID = 4;
				double tax1 = personID != t1.getCustomerID() ?  t1.getTaxAmount() : 0d;
				double tax2 = personID != t2.getCustomerID() ? t2.getTaxAmount() : 0d;				
				double cost1 = 0;				
				double cost2 = 0;
				if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {					
					cost1 = (Settings.PricePerGram + tax1)/t1.getDrugQtyInUnits();
					cost2 = (Settings.PricePerGram + tax2)/t2.getDrugQtyInUnits();
				}
				else if (Settings.Tax.taxType.equals((TaxType.AmountDrug))) {
					cost1 = Settings.PricePerGram / ( t1.getDrugQtyInUnits() - (t1.getDrugQtyInUnits() * (tax1/100) ) ); 
					cost2 = Settings.PricePerGram / ( t2.getDrugQtyInUnits() - (t2.getDrugQtyInUnits() * (tax2/100) ) );										
				}
				return (int)cost1 > (int) cost2 ? +1 : (int) cost1 == (int) cost2 
							? (t1.getTime().doubleValue() < t2.getTime().doubleValue() ? +1 : (t1.getTime().doubleValue() == t2.getTime().doubleValue()) ? 0 : -1)
						: -1;
			}
		});		


/*		*//** My Sorted Best*//*
		if (deals.size() > 0) {			
			Collections.shuffle(deals);
			Collections.sort(deals, new Comparator<Transaction>() {
				public int compare(Transaction t1, Transaction t2) {
					return t1.getDrugQtyInUnits().doubleValue() < t2.getDrugQtyInUnits().doubleValue() ? +1 
							: (t1.getDrugQtyInUnits().doubleValue() == t2.getDrugQtyInUnits().doubleValue()) 
								? (t1.getTime().doubleValue() < t2.getTime().doubleValue() ? +1 : (t1.getTime().doubleValue() == t2.getTime().doubleValue()) ? 0 : -1)
							: -1;
				}
			});	
			
*/	
/*			Collections.sort(deals, new Comparator<Transaction>() {
				public int compare(Transaction t1, Transaction t2) {
					int result = -1 * t1.getDrugQtyInUnits().compareTo(t2.getDrugQtyInUnits());
					if (result == 0) {
						result =  -1 * t1.getTime().compareTo(t2.getTime());
					}
					return result;
				}
			});*/
			
/*			Collections.sort(deals, new Comparator<Transaction>() {
				public int compare(Transaction trans1, Transaction trans2) {
					double cost1 = 0;
					double cost2 = 0;
					if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {					
						cost1 = (Settings.PricePerGram + trans1.getTaxAmount())/trans1.getDrugQtyInUnits();
						cost2 = (Settings.PricePerGram + trans2.getTaxAmount())/trans2.getDrugQtyInUnits();
					}
					else if (Settings.Tax.taxType.equals((TaxType.AmountDrug))) {
						cost1 = Settings.PricePerGram / ( trans1.getDrugQtyInUnits() - (trans1.getDrugQtyInUnits() * (trans1.getTaxAmount()/100) ) ); 
						cost2 = Settings.PricePerGram / ( trans2.getDrugQtyInUnits() - (trans2.getDrugQtyInUnits() * (trans2.getTaxAmount()/100) ) );										
					}
					if (trans1.getDealer().getPersonID() != trans2.getDealer().getPersonID()) {					
						return cost1 > cost2 ? +1 : cost1 == cost2 ? 0 : -1;				
					}
					else {
						return trans1.getTime() < trans2.getTime() ? +1 : (trans1.getTime() == trans2.getTime()) ? 0 : -1;
					}
				}
			});
		}		
*/
		

		System.out.println("----------after sorting --------------");
		for (Transaction deal: deals) {
			deal.print();
		}
	}

}
