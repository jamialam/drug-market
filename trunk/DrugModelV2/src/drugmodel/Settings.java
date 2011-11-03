package drugmodel;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;

public class Settings {
	public static boolean errorLog = false;
	public static boolean outputLog = false;
	public static final int GarbageTime = 25000;
	
	public static final String transactionnetwork = "transactionnetwork";
	public static final String socialnetwork = "socialnetwork";

	public static int InitCustomers = 500;
	public static int InitDealers = 100;
	public static double endTime = 50000.0;

	/** Minimum number of dealers for a customer agent */
	public static int MinDealerCustomerlinks = 1;
	/** Maximum number of dealers for a customer agent */
	public static int MaxDealerCustomerlinks = 3;
	/** In Minutes - Each time step is 10 minutes */
	public static final double step = 10;
	/** 8 hours = 1 Day */
	private static final double hoursInDay = 8;
	/** Number of steps in one day of simulation*/
	public static final double StepsInDay = (hoursInDay*60)/step;
	/** Initial phase should be > 1 && should be one less than the actual initial days. */
	public static final int NumDaysInitialPhase = 15;
	/** Initial period. In time steps (days) */
	public static final double initialPhase = NumDaysInitialPhase*StepsInDay;
	/** We assume that the price per gram to remain constant for now. */
	public static final double PricePerGram = 120.0;
	/** Initially, the units sold per grams is kept same and later varied depending upon the sales. */
	public static final double UnitsPerGram = 12.0;
	/** Amount of drugs consumed by a customer per time step in the model. */
	public static final double consumptionUnitsPerStep = UnitsPerGram/StepsInDay;

	public static enum SupplyOption {Automatic, RegularConstant, RegularSurplus};
	public static enum DealerSelection {Random, MyBest, NetworkBest};
	public static enum DealerType {Greedy, Planner, Old};
	
	public static class DealersParams {  
		private static final int DealerRessuplyIntervalInDays = 21;
		private static final double TimeToLeaveMarketInDays = 14d;

		/** Re-supply interval in time steps*/
		public static final int ResupplyInterval = (int)(DealersParams.DealerRessuplyIntervalInDays * StepsInDay);
		//This is the one read by the Dealer
		public static final double TimeToLeaveMarket = TimeToLeaveMarketInDays * StepsInDay;
		public static double SurplusLimit =  14.0 * UnitsPerGram;
		public static final double NewDealerInterval = 1.0 * StepsInDay;
		public static double MaxDealsLimit = 30.0;
		public static boolean NewDealerType = true;
		public static double GreedynewDealerProb = 0.5;
		public static final double MinUnitsToSell = 9.0;
		public static final double MaxUnitsToSell = 15.0;
	}

	public static enum ShareDealMode {OnDemand, Proactive};
	public static enum ShareDealWay {X_Percent_of_Netowrk, X_Percent_of_Deals};
	/** Types of endorsements */
	public static enum Endorsement {None, Bad, Good};
	/** Types of tax */
	public static enum TaxType {FlatFee, AmountDrug};

	public static class CustomerParams {
		//public static DealerSelection dealerSelection = DealerSelection.NetworkBest;
		public static final double DealerSelectionProb = 0.8;

		public static ShareDealMode shareDealMode = ShareDealMode.OnDemand;
		public static ShareDealWay shareDealWay = ShareDealWay.X_Percent_of_Deals;

		private static final int CustomerConnectionEvaluationIntervalInDays = 7;	
		private static final double TimeFrameToDropLinkInDays = 30d;
		private static final double TimeFrameToForgetDropLinkInDays = 90d;
		private static int CustomerIncomeIntervalInDays = 21;
		private static final int DefaultBadAllowed = 3;

		public static final int CustomerConnectionEvaluationInterval = (int) (CustomerConnectionEvaluationIntervalInDays * StepsInDay);		
		public static final double DefaultTimeFrameToDropLink = TimeFrameToDropLinkInDays * StepsInDay;
		public static final double TimeFrameToForgetDropLink = TimeFrameToForgetDropLinkInDays * StepsInDay;

		public static double MinshareDealProb = 0.75;
		public static double MaxshareDealProb = 0.75;

		public static final double MakeLinkProb = 0.01;		
		public static final int incomeInterval = (int)(CustomerIncomeIntervalInDays * StepsInDay);

		private static double meanBudget = 200.0;
		private static double stdDevBudget = 50.0;

		public static int returnInitKnownDealers() {			
			return Uniform.staticNextIntFromTo(Settings.MinDealerCustomerlinks, Settings.MaxDealerCustomerlinks);
		}
		public static int returnBadAllowed() {
			return DefaultBadAllowed;
		}
		public static double returnShareDealProb(){
			return Uniform.staticNextDoubleFromTo(MinshareDealProb, MaxshareDealProb);		
		}
		/** Setup Budget. We need to figure out what would be the distribution of initial budget for the customer agents. 
		 * Right now, we just assume them to be normally distributed*/
		public static double returnInitialBudget() {
			return Normal.staticNextDouble(meanBudget, stdDevBudget);
		}
		/** Returns the initial drug quantity in units assigned to a Customer. */
		public static double returnInitialDrugs() {
			return Uniform.staticNextDoubleFromTo(0,12);
		}
	}

	public static class Tax {
		public static TaxType taxType = TaxType.FlatFee;

		public static double Min_FlatFee_in_dollar = 5.0;
		public static double Max_FlatFee_in_dollar = 10.0;

		public static double setInitialFlatFee(){
			return Uniform.staticNextDoubleFromTo(Min_FlatFee_in_dollar, Max_FlatFee_in_dollar);
		}

		public static double Min_Drug_Percentage = 5.0;
		public static double Max_Drug_Percentage = 10.0;

		public static double setInitialDrugPercent(){
			return Uniform.staticNextDoubleFromTo(Min_Drug_Percentage, Max_Drug_Percentage);
		}

		/** Used in Customer.income() - to check */
		public static double returnMaxTax() {
			return Max_FlatFee_in_dollar ;
		}
	}

	/** Class for re-supply for dealers and customers both */
	public static class Resupply {
		/** Re-supply setting for Dealer agent */
		private static SupplyOption supplyOptionForDealer = SupplyOption.RegularSurplus;//RegularConstant;
		/** Income supply setting for Customer agent */
		private static SupplyOption incomeOptionForCustomer = SupplyOption.Automatic;//RegularSurplus;//RegularConstant;
		/** How many grams of drugs to re-supply to a dealer agent - default: 12 grams */				
		private static double constantDrugsGrams = 12.0;
		/** How many units of drugs to re-supply to a dealer agent -  constantDrugsGrams * units in grams*/
		public static double constantDrugsUnits = constantDrugsGrams * UnitsPerGram;
		/** Income supply for a customer agent In Dollars - */
		public static double constantMoneyForCustomer = 120.0;

		public void setSupplyOptionForDealer(SupplyOption _supplyOption) {
			supplyOptionForDealer = _supplyOption;
		}
		public static SupplyOption getSupplyOptionForDealer() {
			return supplyOptionForDealer;
		}
		public void setIncomeOptionForCustomer(SupplyOption _incomeOption) {
			incomeOptionForCustomer = _incomeOption;
		}
		public static SupplyOption getIncomeOptionForCustomer() {
			return incomeOptionForCustomer;
		}

		/** For customer agent */
		public static double incomeForCustomer(double currentMoney) {
			double amountInDollars = 0;
			switch(incomeOptionForCustomer) {
			case Automatic: 
				amountInDollars = constantMoneyForCustomer;
				break;
			case RegularConstant:
				amountInDollars = constantMoneyForCustomer - currentMoney;
				break;
			case RegularSurplus:
				amountInDollars = constantMoneyForCustomer;
				break;
			default: break;
			}
			return amountInDollars;			
		}

		/** For dealer agents */
		public static double resupplyDrugs(double currentDrugs) {
			double amountInUnits = 0;
			switch(supplyOptionForDealer) {
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
}