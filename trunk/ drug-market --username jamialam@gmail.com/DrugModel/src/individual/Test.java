package individual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cern.jet.random.Uniform;

import drugmodel.Transaction;
import drugmodel.Settings.Endorsement;

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
			time = Uniform.staticNextIntFromTo(1, 4);
			cost = Uniform.staticNextDoubleFromTo(50, 100);
			Transaction deal = new Transaction(i, -1, time, cost, 0.0, endorsment);
			if (Math.random() <= 0.5) {
				deal.setDealerID(3);
				if (Math.random() <= 0.1)
					deal.setDrugCost(0.0);
			}			
			deals.add(deal);
			deal.print();						
		}

		Collections.sort(deals, new Comparator<Transaction>() {
			public int compare(Transaction trans1, Transaction trans2) {
				if (trans1.getDealerID() != trans2.getDealerID()) {
					return trans1.getDrugCost() > trans2.getDrugCost() ? +1 
							: (trans1.getDrugCost() == trans2.getDrugCost()) ? 0 : -1;				
				}
				else {
					return trans1.getTime() < trans2.getTime() ? +1 
							: (trans1.getTime() == trans2.getTime()) ? 0 : -1;
				}
			}
		});		

		System.out.println("----------after sorting --------------");
		for (Transaction deal: deals) {
			deal.print();
		}
	}

}
