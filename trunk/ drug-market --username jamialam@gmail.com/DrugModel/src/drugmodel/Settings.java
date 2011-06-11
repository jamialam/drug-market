package drugmodel;

import cern.jet.random.Normal;

public class Settings {
	public static final boolean errorLog = true;
	public static final String transactionnetwork = "transactionnetwork";
	public static final String socialnetwork = "socialnetwork";

	public static int initCustomers = 100;
	public static int initDealers = 10;
	public static int maxCoordinate = 1000;
	public static double endTime = 1000.0;
	public static int minDealerCustomerlinks = 1;
	public static int maxDealerCustomerlinks = 3;
	
	//12 Units = 1 gram
	public static final double unitsPerGram = 12;	
	//In Minutes - Each time step is 10 minutes
	public static final double step = 10;
	//8 hours = 1 Day
	public static final double hoursInDay = 8;
	//Number of steps in One day
	public static final double stepsInDay = (hoursInDay*60)/step;
//	//in minutes
//	public static final double consumptionTimePerGram = 8*60;
	public static final double consumptionStepsPerUnit = stepsInDay/unitsPerGram;
	/** Initial period. In time steps (days) */	
	public static double initialPhase = 30*stepsInDay;
	/** We assume this to remain constant for now. */
	public static double price_per_grams = 120;
	/** Initially, the units sold per grams is kept same and later varied depending upon the sales. */
	public static double units_per_grams = 12; 
	
	public static class DealersParams {
		public static final double TimeToLeaveMarketInDays = 7d;
		//This is the one read by the Dealer
		public static final double TimeToLeaveMarket = TimeToLeaveMarketInDays * stepsInDay;
	}

	/** Endorsements */
	public static enum Endorsement {None, Bad, Good};

	public static enum SupplyOption {Automatic, RegularConstant, RegularSurplus};
	public static class Resupply {
		private static SupplyOption supplyOption = SupplyOption.RegularConstant;
		/** Resupply interval*/
		public static final int resupplyInterval = (int)(21 * stepsInDay);
		/** 12 grams */		
		public static double constantDrugsGrams = 12;
		public static double constantDrugsUnits = constantDrugsGrams * 12;

		public void setSupplyOption(SupplyOption _supplyOption) {
			supplyOption = _supplyOption;
		}
		public static SupplyOption getSupplyOption() {
			return supplyOption;
		}
		public static double resupplyDrugs(double currentDrugs) {
			double amountInUnits = 0;
			switch(supplyOption) {
			case Automatic: 
				amountInUnits = constantDrugsUnits;
				break;
			case RegularConstant:
				amountInUnits = constantDrugsUnits - currentDrugs;
				break;
			case RegularSurplus:
				amountInUnits = constantDrugsUnits;
				break;
			default: break;
			}
			return amountInUnits;			
		}
	}

	public static enum GeneratorType{Unconnected, BarabasiAlbert, KleinbergSmallWorld, 
		WattsSmallWorld, ErdosRenyiRandom, EppsteinPowerLaw};
		public static GeneratorType generator_type = GeneratorType.WattsSmallWorld;
		public static class SocaialNetworkParam {
			public static boolean DIRECTED = true;
			public static String NAME = "socialnetwork";			
			/** Average social networks degree. */
			public static int DEGREE = 8;
			/** Parameter for the Clustering coefficient in the Kleinberg network generator. */
			public static double CLUSTERING_COEFFICIENT = 0.4;
			/** Parameter for the WattsStrogatz Small world network generator. */
			public static final double REWIRE_PROB = 0.15;
			/** Parameter for the Barabasi-Albert network Jung generator. */
			public static final int EDGES_TO_ATTACH = 10;
			public static GeneratorType convert(String genType) {
				return GeneratorType.valueOf(GeneratorType.class, genType);
			}
		}

		/** Setup Budget. We need to figure out what would be the distribution of initial budget for the customer agents. 
		 * Right now, we just assume them to be normally distributed*/
		public static class Budget {
			public static double meanBudget = 200;
			public static double stdDevBudget = 50;

			public static double returnInitialBudget() {
				return Normal.staticNextDouble(meanBudget, stdDevBudget);
			}
		}
}