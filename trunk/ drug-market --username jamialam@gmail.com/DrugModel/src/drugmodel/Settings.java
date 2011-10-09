package drugmodel;

/**
 * @author shah
 */

import cern.jet.random.Normal;
import cern.jet.random.Uniform;

public class Settings {
	public static final boolean errorLog = true;
	public static final String transactionnetwork = "transactionnetwork";
	public static final String socialnetwork = "socialnetwork";

	public static int initCustomers = 25;
	public static int initDealers = 5;
//	public static int initCustomers = 30;
//	public static int initDealers = 4;
	//public static int maxCoordinate = 1000;
	public static double endTime = 1000.0;
	public static int minDealerCustomerlinks = 1;
	public static int maxDealerCustomerlinks = 3;
	
	//In Minutes - Each time step is 10 minutes
	public static final double step = 10;
	//8 hours = 1 Day
	public static final double hoursInDay = 8;
	//Number of steps in One day
	public static final double stepsInDay = (hoursInDay*60)/step;
	//	//in minutes
	//	public static final double consumptionTimePerGram = 8*60;
	public static final double numDaysInitialPhase = 5;
	/** Initial period. In time steps (days) */
	public static final double initialPhase = numDaysInitialPhase*stepsInDay;
	/** We assume this to remain constant for now. */
	public static final double pricePerGram = 120;
	/** Initially, the units sold per grams is kept same and later varied depending upon the sales. */
	public static final double unitsPerGram = 12;
	public static final double consumptionUnitsPerStep = unitsPerGram/stepsInDay;

	public static enum SupplyOption {Automatic, RegularConstant, RegularSurplus};
	public static enum DealerSelection {Random, MyBest, NetworkBest};
	public static enum DealerType { Greedy, Planner, old};
	public static class DealersParams {
		public static DealerSelection dealerSelection = DealerSelection.NetworkBest;  
		public static final int DealerRessuplyIntervalInDays = 21;
		/** Resupply interval*/
		public static final int resupplyInterval = (int)(DealersParams.DealerRessuplyIntervalInDays * stepsInDay);
		public static final double TimeToLeaveMarketInDays = 14d;
		//This is the one read by the Dealer
		public static final double TimeToLeaveMarket = TimeToLeaveMarketInDays * stepsInDay;
		public static final double surplusLimit = 14 * unitsPerGram;
		public static final double newDealerInterval = 1 * stepsInDay;
		public static final double maxDealsLimit = 10;
		public static final double dealerSelectionProb = 0.8;
		
	}
	public static enum ShareDealMode {ShareWhenAsked, ShareWithoutAsking};
	public static enum ShareDealWay {X_percent_of_netowek, X_percent_of_deals};
	
	public static class CustomerParams {
		public static ShareDealMode shareDealMode = ShareDealMode.ShareWhenAsked;
		public static ShareDealWay shareDealWay = ShareDealWay.X_percent_of_deals;
		
		public static final int CustomerConnectionEvaluationIntervalInDays = 7;
		public static final int CustomerConnectionEvaluationInterval = (int) (CustomerConnectionEvaluationIntervalInDays * stepsInDay);
		
		public static int CustomerIncomeIntervalInDays = 21;
		public static final int incomeInterval = (int)(CustomerIncomeIntervalInDays * stepsInDay);
		//magic number
//		public static final double shareDealProb = 0.75;
		public static double minshareDealProb = 0.75;
		public static double maxshareDealProb = 0.75;
		
		public static final double makeLinkProb = 0.01;
		
		public static final int defaultBadAllowed = 3;
		public static final double TimeFrameToDropLinkInDays = 30d;
		public static final double TimeFrameToForgetDropLinkInDays = 90d;
		
		public static final double defaultTimeFrameToDropLink = TimeFrameToDropLinkInDays * stepsInDay;
		public static final double TimeFrameToForgetDropLink = TimeFrameToForgetDropLinkInDays * stepsInDay;
		
		public static int returnBadAllowed() {
			return defaultBadAllowed;
		}
		public static double returnShareDealProb(){
			return Uniform.staticNextDoubleFromTo(minshareDealProb, maxshareDealProb);
		}
	}

	/** Endorsements */
	public static enum Endorsement {None, Bad, Good};
	public static enum TaxType {FlatFee, AmountDrug};
	
	public static class Tax {
		public static TaxType taxType = TaxType.FlatFee;
/*		public static double max_tax = 10;
		public static double min_tax = 5;
*/		
		public static double max_FlatFee_in_doller = 10;
		public static double min_FlatFee_in_doller = 5;
		
		public static double setInitialFlatFee(){
			return Uniform.staticNextDoubleFromTo(min_FlatFee_in_doller, max_FlatFee_in_doller);
		}

		public static double max_drug_percentage = 10;
		public static double min_drug_percentage = 5;
		
		public static double setInitialDrugPercent(){
			return Uniform.staticNextDoubleFromTo(min_drug_percentage, max_drug_percentage);
		}
		/*
		//1/2 unit price
		public static double flatFee = (double) 0.5 * (pricePerGram/unitsPerGram);
		//1/2 unit
		public static double percentageInUnits = 1.0; 		
		public static double returnTaxCost() {
			if (taxType.equals(TaxType.FlatFee)) {
				return flatFee;
			}
			else {
				return ((double) percentageInUnits * (pricePerGram/unitsPerGram));
			}
		}
		
		public static double returnPercentageInUnits() {
			return percentageInUnits;
		}*/
		public static double returnMaxTax() {
			return max_FlatFee_in_doller ;
		}
	}

	public static class Resupply {
		private static SupplyOption supplyOption = SupplyOption.RegularSurplus;//RegularConstant;
		private static SupplyOption incomeOption = SupplyOption.Automatic;//RegularSurplus;//RegularConstant;
		/** 12 grams */		
		public static double constantDrugsGrams = 12;
		public static double constantDrugsUnits = constantDrugsGrams * 12;
		/** In Dollars */
		public static double constantMoney = 120;

		public void setSupplyOption(SupplyOption _supplyOption) {
			supplyOption = _supplyOption;
		}
		public static SupplyOption getSupplyOption() {
			return supplyOption;
		}
		public void setIncomeOption(SupplyOption _incomeOption) {
			incomeOption = _incomeOption;
		}
		public static SupplyOption getIncomeOption() {
			return incomeOption;
		}

		public static double income(double currentMoney) {
			double amountInDollars = 0;
			switch(incomeOption) {
			case Automatic: 
				amountInDollars = constantMoney;
				break;
			case RegularConstant:
				amountInDollars = constantMoney - currentMoney;
				break;
			case RegularSurplus:
				amountInDollars = constantMoney;
				break;
			default: break;
			}
			return amountInDollars;			
		}
/*		public static double income_and_tax(double currentMoney) {
			double amountInDollars = 0;
			switch(incomeOption) {
			case Automatic: 
				amountInDollars = constantMoney;
				break;
			case RegularConstant:
				amountInDollars = constantMoney - currentMoney;
				break;
			case RegularSurplus:
				amountInDollars = constantMoney;
				break;
			default: break;
			}
			return amountInDollars + Tax.max_FlatFee_in_doller;			
		}
*/
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
		public static GeneratorType generator_type = GeneratorType.ErdosRenyiRandom;
		public static class SocialNetworkParam {
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