package individual;

import cern.jet.random.Uniform;
import drugmodel.Settings;

public class Test2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int totalDays = 30;
		double[] numSalesAmount = new double[totalDays];
		double[] numSalesUnits = new double[totalDays];
		double totalSalesAmount = 0;
		double totalSalesUnits = 0;
		
		double meanSalesAmount = 0;
		double meanSalesUnits = 0;
		
		double[] variance = new double[3];
		double[] sumSq = new double[3];
		
		System.out.println("Printing data: ");
		
		double unitspg = Settings.unitsPerGram;
		for (int i=0; i<totalDays; i++) {			
			
			numSalesUnits[i] = (double) Uniform.staticNextIntFromTo(0, 2) * unitspg;
			totalSalesUnits += numSalesUnits[i];
			sumSq[1] += numSalesUnits[i]*numSalesUnits[i];
			
			numSalesAmount[i] = (120* numSalesUnits[i]) / unitspg;
			totalSalesAmount += numSalesAmount[i];
			sumSq[0] += numSalesAmount[i]*numSalesAmount[i];
			
			System.out.println("i: " + i + " amount: " + numSalesAmount[i] + " units: " + numSalesUnits[i]);
		}
		
		meanSalesAmount = totalSalesAmount/totalDays;
		meanSalesUnits = totalSalesUnits/totalDays;
		
		variance[0] = (sumSq[0] - (meanSalesAmount*totalSalesAmount))/(totalDays-1);
		variance[1] = (sumSq[1] - (meanSalesUnits*totalSalesUnits))/(totalDays-1);
	

		System.out.println("Printing summary: ");
		System.out.println("Total amount: " + totalSalesAmount + " - total units: " + totalSalesUnits);
		System.out.println("SumSq. amount: " + sumSq[0] + " units: " + sumSq[1]);
		System.out.println("Average. amount: " + meanSalesAmount + " units: " + meanSalesUnits);
		System.out.println("Variance. amount: " + variance[0] + " units: " + variance[1]);
		System.out.println("StdDev. amount: " + Math.sqrt(variance[0]) + " units: " + Math.sqrt(variance[1]));
		
	}

}
