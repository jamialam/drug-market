package individual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;

import drugmodel.ContextCreator;
import drugmodel.Settings;
import drugmodel.Settings.DealerType;
import drugmodel.Settings.DropOutReason;
import drugmodel.Settings.SupplyOption;
import drugmodel.TransactionEdge;
import drugmodel.Transaction;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Dealer extends Person {
	/** Type of the dealer old, greedy , planner*/
	private DealerType dealerType;
	/** No. of units that this dealer is selling currently. */
	private double unitsToSell;
	/** Last time (in steps) when this dealer has no drugs.*/
	private double lastTimeZeroDrug;
	/** Last time when this dealer sold drugs to a customer agent.*/
	private double timeLastTransaction;
	/** Amount of drug that was surplus for this dealer agent. */
	private double surplus;
	
	/** Interval for this dealer when evaluation was made for No customer or transaction. */
	private int noCustomerEvaluationInterval;
	/** Interval for this dealer when evaluation was made for surplus amount. */
	private int surplusEvaluationInterval;
	
	/**ID of the parent dealer from this (new dealer) is created. */
	private int parentID;
	/** */	
	private ArrayList<Summary> summaries;
	/** */
	private ArrayList<Transaction> currentDayTransactions;
	/** No. of deals made yesterday. */
	private double dealsYesterday;
	/** No. of deals made today; calculated at the end of the day. */
	private double dealsToday;
	private double dealsPerDay;
	private DropOutReason resaonOfDropOut;

	// For display
	private double unitsSoldPerDay;
	private double salesYesterday;
	private double salesToday;
	private double totalUnitsSold;
	private double totalDeals;
	private double[] numOfDealsInLastDays;
	private int currentDay;
	
	public Dealer(double... params) {
		this.unitsToSell = Settings.UnitsPerGram;
		this.lastTimeZeroDrug = -1;
		this.timeLastTransaction = -1;
		this.summaries = new ArrayList<Summary>();
		this.currentDayTransactions = new ArrayList<Transaction>();
		this.drugs = Settings.Resupply.constantDrugsUnits;
		this.noCustomerEvaluationInterval = Uniform.staticNextIntFromTo(1, (int)Settings.DealersParams.NoCustomerInterval);
		this.surplusEvaluationInterval = Uniform.staticNextIntFromTo(1, (int)Settings.DealersParams.SurplusInterval);
		this.surplus = 0.0;
		this.dealsPerDay = 0.0;
		this.resaonOfDropOut = DropOutReason.NotDroppingOut;
		
		this.numOfDealsInLastDays = new double[Settings.DealersParams.NewDealerIntervalInDays];
		for(int i= 0 ; i < numOfDealsInLastDays.length ; ++i  ){
			numOfDealsInLastDays[i] = 0.0;
		}
		this.currentDay = 0;	
		
		if(ContextCreator.getTickCount() == -1) {
			this.entryTick = 0.0;
		}
		else {
			this.entryTick = ContextCreator.getTickCount();
		}
		this.unitsSoldPerDay = 0.0;
		this.salesToday = 0.0;
		this.salesYesterday = -1.0;
		this.dealsToday = 0.0;
		this.totalUnitsSold = 0.0;
		this.totalDeals = 0.0;

		if(params.length == 0) {
			parentID = -1;
			dealerType = DealerType.Old;
		}
		else{
			parentID = (int)params[0];
			unitsToSell = params[1];
			// Check if we want to assign new types to this new dealer. 
			if (Settings.DealersParams.NewDealerType == true)	{	
				if(Math.random() <= Settings.DealersParams.GreedynewDealerProb) {
					dealerType = DealerType.Greedy;
				}
				else {
					dealerType = DealerType.Planner;
				}
			}
			else {
				dealerType = DealerType.Old;
			}
		}

		if(Settings.outputLog) {
			System.out.println("new Dealer : " + this.personID + " entry tick: " + entryTick +" entry/48 : " +entryTick/48);
		}
	}

	/**
	 * This is the first implementation
	 * @param numSales is today's total number of sales.
	 */
	public void meanNumSalesFn(double numSales){
		int lastIndex = summaries.size()-1;
		double stdDevNumSales = Math.sqrt(summaries.get(lastIndex).varianceNumSales);
		double meanNumSales = summaries.get(lastIndex).meanNumSales;

		/* If my sales are Less than (mean-k*stdDev) then increase units; i.e. reduce price per units, k being NumStandardDeviations  */
		if (numSales < (meanNumSales - Settings.DealersParams.NumStandardDeviations * stdDevNumSales)) {
			if (unitsToSell < Settings.DealersParams.MaxUnitsToSell) {
				unitsToSell = unitsToSell + 1;
			} 
			else {
				unitsToSell = Uniform.staticNextIntFromTo((int)Settings.DealersParams.MinUnitsToSell, (int)Settings.DealersParams.MaxUnitsToSell);
			}
		}
		/* If my sales are Greater than (mean+k*stdDev) then decrease units; i.e. increase price per units, k being NumStandardDeviations  */
		else if(numSales > (meanNumSales + Settings.DealersParams.NumStandardDeviations * stdDevNumSales) ){ 
			if(unitsToSell > Settings.DealersParams.MinUnitsToSell)
				unitsToSell = unitsToSell - 1;
			else {
				unitsToSell = Uniform.staticNextIntFromTo((int)Settings.DealersParams.MinUnitsToSell, (int)Settings.DealersParams.MaxUnitsToSell);
			}
		}
	}

	/**
	 * This is the second implementation. It uses mean sales units.
	 * @param salesInUnits is today's total units of drugs sold.
	 */
	public void meanSaleUnitsFn(double salesInUnits){
		int lastIndex = summaries.size()-1;		
		double stdDevSaleUnits = Math.sqrt(summaries.get(lastIndex).varianceSalesUnits);
		double meanSaleUnits = summaries.get(lastIndex).meanSalesUnits;

		if (salesInUnits < (meanSaleUnits - Settings.DealersParams.NumStandardDeviations * stdDevSaleUnits)) {
			if (unitsToSell < Settings.DealersParams.MaxUnitsToSell) {
				unitsToSell = unitsToSell + 1;
			}
			else {
				unitsToSell = Uniform.staticNextIntFromTo((int)Settings.DealersParams.MinUnitsToSell, (int)Settings.DealersParams.MaxUnitsToSell);
			}
		}
		else if(salesInUnits > (meanSaleUnits + Settings.DealersParams.NumStandardDeviations * stdDevSaleUnits) ){ 
			if(unitsToSell > Settings.DealersParams.MinUnitsToSell) {
				unitsToSell = unitsToSell - 1;
			}
			else {
				unitsToSell = Uniform.staticNextIntFromTo((int)Settings.DealersParams.MinUnitsToSell, (int)Settings.DealersParams.MaxUnitsToSell);
			}
		}
	}

	/**
	 * This is the third implementation.
	 * @param salesInUnits is today's total units of drugs sold.
	 */
	public void diffSalesInUnits(double salesInUnits){ 
		int lastIndex = summaries.size()-1;
		double newDifference = salesInUnits - summaries.get(lastIndex).meanSalesUnits;

		if (newDifference < -1) {
			if(unitsToSell < Settings.DealersParams.MaxUnitsToSell) {
				unitsToSell = unitsToSell + 1;
			}
			else {
				unitsToSell = Uniform.staticNextIntFromTo((int)Settings.DealersParams.MinUnitsToSell, (int)Settings.DealersParams.MaxUnitsToSell);
			}
		}		 
		else if (newDifference > 1) {
			if(unitsToSell > Settings.DealersParams.MinUnitsToSell) {
				unitsToSell = unitsToSell - 1;
			}
			else {
				unitsToSell = Uniform.staticNextIntFromTo((int)Settings.DealersParams.MinUnitsToSell, (int)Settings.DealersParams.MaxUnitsToSell);
			}
		}				
	}

	/**
	 * Supply method to replenish drug supply to this dealer. It is called at every time step in the simulation.
	 * Also, if a dealer has a surplus amount left and the supply option is 'Regular-Surplus' then
	 * the dealer agent is dropped from the context. 
	 * 	Currently, the income interval is Homogeneous. 
	 */
	//TODO:make everyone interval tick different
	@ScheduledMethod(start = 1, interval = 1, priority = 6)
	public void supply() {
		double currentTick = ContextCreator.getTickCount();
		/* If my drugs are zero or less then store current tick as the time where I had zero drugs.*/
		if (this.drugs <= 0 && lastTimeZeroDrug == -1)  {
			lastTimeZeroDrug = currentTick;
		}
		/* I am in the re-supply interval */
		if ((currentTick - entryTick) % Settings.DealersParams.ResupplyInterval == 0) {
				/* This dealer did not drop-out. So re-supply drug. */
				this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));
		}

		/* Re-supply was successful. Last time zero drug is reset. */
		if (this.drugs > 0) {
			lastTimeZeroDrug = -1;
		}
	}

	/** 
	 * Dealers are supplied an inventory of drugs on a schedule, based on the cycles of the model, with a standard amount of drug (we could start with 12 grams supplied every three weeks). They all start with the same amount. If dealers run out of drug supplies to sell 
	 * before their scheduled re-supply they are automatically re-supplied. If at the re-supply time dealer still has drug to sell there can be two options (to play with in experiments). 
	 * The first option, dealers are re-supplied with the difference between what is remaining and their original supply amount. The second option, dealers could be resupplied with the standard amount and will have to deal with the “surplus.” 
	 * Dealer agents could grow as there business increases, shrink as their business decreases, and change colors if in the “black” at there last resupply deadline. Alternatively, will only a few dealers it might be nice to see the supplies and surpluses displayed in graphs.  
	 * If a dealer runs out of customers or drug supply, after X number of cycles of the simulation, they are eliminated. 
	 */
	@ScheduledMethod(start = 1, interval = Settings.StepsInDay, priority = 5)

	public void policeArrestDropOut(){
		Context context = ContextUtils.getContext(this);
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		int degree =  transactionNetwork.getDegree(this);
		double chance = degree * Settings.DealersParams.PoliceArrestProb;
		if(Math.random() < chance){
			this.resaonOfDropOut = DropOutReason.PoliceArrest;
			dropFromContext();
		}
	}
	
	@ScheduledMethod(start = Settings.DealersParams.NoCustomerInterval, interval = 1, priority = 5)
	public void NoCustomerDropOut() {		
		double currentTick = ContextCreator.getTickCount();
		/* At least spend minimum required time before drop out. */
		if(currentTick - entryTick < Settings.DealersParams.NoCustomerInterval ) {
			return;
		}

		if(currentTick == this.noCustomerEvaluationInterval 
				||	currentTick % Settings.DealersParams.NoCustomerInterval == this.noCustomerEvaluationInterval) {
			if (this.timeLastTransaction == -1 /*  No customer came at all */
					//&& currentTick - lastTimeZeroDrug >= Settings.DealersParams.TimeToLeaveMarket)
					/*  Enough time has passed without a customer. */
					|| currentTick - timeLastTransaction > Settings.DealersParams.NoCustomerInterval) {
				
				this.resaonOfDropOut = DropOutReason.NoCustomer;
				
				dropFromContext();
				System.out.println("Dealer: " + personID + " dropping out due to no customer");

			}
		}
	}
	@ScheduledMethod(start = Settings.DealersParams.SurplusInterval, interval = 1, priority = 5)
	public void SurplusDropOut() {
		double currentTick = ContextCreator.getTickCount();
		/* At least spend minimum required time before drop out. */
		if(currentTick - entryTick < Settings.DealersParams.SurplusInterval ) {
			return;
		}

		if(currentTick == this.surplusEvaluationInterval 
				||	currentTick % Settings.DealersParams.SurplusInterval == this.surplusEvaluationInterval) {

			this.surplus = this.drugs;  
			if (this.surplus > Settings.DealersParams.SurplusLimit) {
				this.resaonOfDropOut = DropOutReason.Surplus;
				/* Remove the dealer from the context if it should be dropped. */
				dropFromContext();
				System.out.println("Dealer: " + personID + " dropping out due to surplus");
			}
		}
	}

	public DropOutReason getResaonOfDropOut() {
		return resaonOfDropOut;
	}

	/** Remove this dealer agent from the context. */
	private void dropFromContext() {
		Context context = ContextUtils.getContext(this);
		context.remove(this);	
	}
	
	private void initialPhaseUpdetePriceFixed(){
		if(this.dealerType == DealerType.Greedy) {
			/* A 'greedy' dealer agent wants to sell as many drugs as possible in order to make the most profit.
			 * The underlying assumption is that there is an unlimited supply available to the dealers so a 
			 * 'greedy' dealer agent would want to sell its drugs quickly so that it gets further supply. 
			 */
			if ( this.salesToday > salesYesterday
					&& unitsToSell > Settings.DealersParams.MinUnitsToSell ) {
				unitsToSell--;
			}
		}			
		else if (this.dealerType == DealerType.Planner) {
			/* A 'planner' dealer agent is keen in making most connections during its initial phase. 
			 * It is more interested in the number of deals instead of the amount of drugs sold 
			 * (as opposed to the 'greedy' agent). 				  
			 */
			if (  this.dealsToday < this.dealsYesterday 
					&& unitsToSell < Settings.DealersParams.MaxUnitsToSell ) {
				unitsToSell++;
			}
		
		}
		else if(this.dealerType == DealerType.Old){
/*			do {
				unitsToSell = (int) Normal.staticNextDouble(Settings.UnitsPerGram, NumStandardDeviations);
			} while (unitsToSell < Settings.DealersParams.MinUnitsToSell || unitsToSell > Settings.DealersParams.MaxUnitsToSell);
*/		}
		
	}
	/** 
	 * 	This method is called at the end of the day. Priority 2.0
	 * 	Sets salesYesterday, salesToday, dealsYesterday, dealsToday variables.  
	 * 	It updates the @param unitsToSell for each dealer agent based on the updated selected mechanism.  
	 */
	@ScheduledMethod(start = Settings.StepsInDay, interval = Settings.StepsInDay, priority = 3)
	public void updatePrice() {
		Context context = ContextUtils.getContext(this);
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getEdges(this).iterator();
		double currentTick = ContextCreator.getTickCount();
		
		 /*Set sales and deals variables here.*/ 
		this.salesYesterday = this.salesToday;
		this.salesToday = this.unitsSoldPerDay;
		this.dealsYesterday = this.dealsToday;
		this.dealsToday = this.dealsPerDay;
/*		if(true)
			return;
*/		
		if (Settings.outputLog) {
			System.out.println("D: " + this.personID  + " current tick: " +currentTick  +" day : " + currentTick/48);
		}
		/* Decide units to sell, while still in the initial phase.  
		 * Updates only if the dealer is new, i.e. Greedy or Planner.
		 * If the dealer is old, it will sell at the uniform standard price.
		 */
		if (currentTick < Settings.initialPhase + this.entryTick) {

			if (Settings.outputLog) {
				System.out.println("D: " + this.personID  +" entry: " + this.entryTick  + "LESS current tick: " +currentTick  +" day : " + currentTick/48);
			}
			initialPhaseUpdetePriceFixed();
		}

		/* If it is at the end of the initial phase. Calculate summaries of the transactions made during the initial phase.*/
		else if(currentTick == (Settings.initialPhase + entryTick ) ){
			if (Settings.outputLog) {
				System.out.println("D: " + this.personID  +" entry: " + this.entryTick + " EQUAL current tick: " +currentTick  +" day : " + currentTick/48);
				System.out.println("(Settings.initialPhase + entryTick ): " + (Settings.initialPhase + entryTick ));
				double val = (Settings.initialPhase + entryTick )+( Settings.StepsInDay - (Settings.initialPhase + this.entryTick) % Settings.StepsInDay );
				System.out.println("Jamal: " + val);
			}
			Summary summary = initializeMeanAndVarianceSA(itr);
			summaries.add(summary);
		}

		/* In post-initial phase period. */
		else if(currentTick > Settings.initialPhase + entryTick ){
			double numSales = 0;
			double salesInUnits = 0;
			/* Fetch all transactions made this day. lastDay is this day; we are at the end of this day. */
			for (Transaction transaction : currentDayTransactions) {
				numSales++;
				salesInUnits += transaction.getDrugQtyInUnits();
			}	
			if (Settings.outputLog) {
				System.out.println("D: " + this.personID  +" entry: " + this.entryTick + " GREATER current tick: " +currentTick  +" day : " + currentTick/48);
				System.out.println("today Numsales: " +numSales +  " todays salesInUnits: " + salesInUnits );
			}

			switch(Settings.DealersParams.updatePriceMode) {
			case MeanNumSalesFn: //1st implementation
				meanNumSalesFn(numSales );
				break;
			case MeanSaleUnitsFn: //2nd implementation
				meanSaleUnitsFn(salesInUnits);
				break;
			case DiffSalesInUnitsFn: //3rd implementation
				diffSalesInUnits(salesInUnits);
				break;
			default: break;
			}			
			/* Calculate mean and variance for the next day based on today's sales. */
			summaries.add(progressiveSummarySA((int)currentTick, numSales, salesInUnits));
		}

		/* Clear last day transactions.*/
		currentDayTransactions.clear();

		if (Settings.outputLog) {
			System.out.println("cleared last day transaction list........");		
		}			
	}


	/**
	 * In this method, a new dealer is created if an old dealer exceeds its load of deals in the day.
	 * This method is called with the lowest priority.
	 * Set to 1.5 so that it is called just before the setTransactionVariables() method
	 */
	@ScheduledMethod(start = Settings.StepsInDay, interval = Settings.StepsInDay, priority = 2)
	public void newDealer() {
		if(this.currentDay < Settings.DealersParams.NewDealerIntervalInDays){
			numOfDealsInLastDays[this.currentDay] = this.dealsToday;
			++this.currentDay;
		}
		else if(this.currentDay == Settings.DealersParams.NewDealerIntervalInDays ){	
			// min of last 7 days transactions.
			boolean franchise = true;
			//String deals = "";
			
			for(double dealsPerDay : numOfDealsInLastDays){
				//deals += dealsPerDay + ",";  
				if (dealsPerDay < Settings.DealersParams.MinDealsLimit) {
					franchise = false;
					break;
				}
			}
			//System.out.println("tick:" + ContextCreator.getTickCount() + " array: " + deals + " " + franchise);
			if (franchise) {
				//System.out.println("ND tick:" + ContextCreator.getTickCount() + " array: " + deals + " " + franchise);

				Dealer newDealer = new Dealer(this.personID, this.unitsToSell);
				Context context = ContextUtils.getContext(this);
				context.add(newDealer);
				Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));

				int numCustomers = transactionNetwork.getDegree(this);
				Iterator itrCustomers = transactionNetwork.getEdges(this).iterator();

				if(transactionNetwork.getDegree(this) != transactionNetwork.getOutDegree(this)){
					System.err.println(" TransactionNetwork.getDegree(this) != transactionNetwork.getOutDegree(this)");
				}
				System.out.println("ND tick: " + ContextCreator.getTickCount() + " ME: " +this.personID + " creates new dealer:" + newDealer.personID + " ME num of customers: " + numCustomers);

				/* Split the parent dealer's number of customers in half. */
				numCustomers /= 2;

				int i=0;
				Customer customer = null;
				ArrayList<TransactionEdge> list = new ArrayList<TransactionEdge>(); 

				while (itrCustomers.hasNext() && i<numCustomers) {				
					TransactionEdge edge = (TransactionEdge) itrCustomers.next();
					Object objSource = edge.getSource();
					Object objTarget = edge.getTarget();
					if(objSource instanceof Customer){
						customer  = (Customer) objSource;
					}
					else if(objTarget instanceof Customer){
						customer = (Customer) objTarget;
					}
					else{
						System.err.println("FATAL ERR: newDealer neither source nor target in transaction edge are customer");
						return;
					}
					list.add(edge);
					/* Create a new edge between the customer and the new dealer. */
					TransactionEdge newEdge = new TransactionEdge(newDealer, customer, ContextCreator.getTickCount());
					/* Fetch the transactions list from the edge. */
					ArrayList<Transaction> transactionlist = edge.getTransactionList();
					/* Set the transaction's dealer to be new dealer. We are assuming that customers know that this new dealer is
					 * an off shoot of the previous dealer. */
					for (Transaction transaction : transactionlist) {
						transaction.setDealer(newDealer);
					}				
					/* Assign the old edge's transaction list to this new edge. */
					newEdge.setTransactionList(transactionlist);
					/* Add the new edge in the transaction networks. */
					transactionNetwork.addEdge(newEdge);					
					i++;
				}
				System.out.println("ND tick: " + ContextCreator.getTickCount() + " new dealer:" + newDealer.personID + " num of customers: " + transactionNetwork.getDegree(newDealer));

				for(TransactionEdge e : list){
					transactionNetwork.removeEdge(e);
				}
			}
			
			this.currentDay = 0;
			numOfDealsInLastDays[this.currentDay] = this.dealsToday;
			++this.currentDay;
		}
		else{
			System.err.println("NewDealer function current day is greater than interval");
		}
	}


	/**
	 * Sets salesPerDay, dealsPerDay variables to zero. 
	 * We want this method to execute in the last.
	 */
	@ScheduledMethod(start = Settings.StepsInDay, interval = Settings.StepsInDay, priority = 1)
	public void setTransactionVariables() {
		unitsSoldPerDay = 0.0;
		dealsPerDay = 0;
	}

	/** Return unitsToSell*/
	public double returnUnitsToSell() {
		if(this.drugs < unitsToSell){ 
			return this.drugs;
		}
		else {
			return unitsToSell;
		}
	}

	/** Returns current cost per unit. */
	public double returnCostPerUnit() {
		if(this.drugs < unitsToSell){ 
			return (Settings.PricePerGram/this.drugs);
		}
		else {
			return (Settings.PricePerGram/this.unitsToSell);
		}
	}

	/** Returns true if this dealer can sell drugs at the moment; otherwise, false. */
	public boolean canSellDrugs() {
		if (this.drugs <= unitsToSell) { 
			if	(Settings.Resupply.getSupplyOptionForDealer().equals(Settings.SupplyOption.Automatic)){
//				this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));
				this.addDrug(2.0*12.0);	
				return true;
			}
			else{
				return false;
			}
		}
		return true;
	}

	/** 
	 * @param transaction
	 */
	public boolean sellDrug(Transaction transaction) {
		if (this.drugs <= unitsToSell) {
			if (Settings.errorLog) {
				System.err.println("Dealer "+this.personID+ "  cant sell. drug amount is less than unitsToSell. " + ContextCreator.getTickCount());				
			}
			return false;
		}
		
		deductDrug(transaction.getDrugQtyInUnits());
		addMoney(Settings.PricePerGram);
		currentDayTransactions.add(transaction);
		timeLastTransaction = transaction.getTime();
		dealsPerDay++;
		unitsSoldPerDay += transaction.getDrugQtyInUnits();
		totalUnitsSold += transaction.getDrugQtyInUnits();
		totalDeals++;
		if(Settings.outputLog){
			double time = ((double)(transaction.getTime())) / (double ) Settings.StepsInDay;
			int	day = (int) Math.ceil(time); 
			System.out.println("Dealer "+this.personID+ " sell drug add Transaction time: " + transaction.getTime() + "Transaction day: " + day );
		}
		return true;
	}

	public double getSalesYesterday() {
		return salesYesterday;
	}
	public double getSalesToday() {
		return salesToday;
	}
	public double getUnitsToSell() {
		return unitsToSell;
	}
	public void setUnitsToSell(double unitsToSell) {
		this.unitsToSell = unitsToSell;
	}
	public int getNoCustomerEvaluationInterval() {
		return noCustomerEvaluationInterval;
	}
	public double getDealsPerDay() {
		return dealsPerDay;
	}
	public int getParentID() {
		return parentID;
	}
	public DealerType getDealerType() {
		return dealerType;
	}
	public double getDealsYesterday() {
		return dealsYesterday;
	}
	public double getDealsToday() {
		return dealsToday;
	}
	public double getSalesPerDay() {
		return unitsSoldPerDay;
	}
	public double getTotalSales() {
		return totalUnitsSold;
	}
	public double getTotalDeals() {
		return totalDeals;
	}

	public int getOldDealer(){
		if(this.dealerType == DealerType.Old)
			return 1;
		else
			return 0;
	}
	public int getGreedyDealer(){
		if(this.dealerType == DealerType.Greedy)
			return 1;
		else
			return 0;
	}
	public int getPlannerDealer(){
		if(this.dealerType == DealerType.Planner)
			return 1;
		else
			return 0;
	}

	public int getDegree(){
		Context context = ContextUtils.getContext(this);
		Network network = (Network)(context.getProjection(Settings.transactionnetwork));
		return network.getDegree(this);
	}

	protected class Summary {
		public double meanSalesUnits;
		public double meanNumSales;
		public double varianceSalesUnits;
		public double varianceNumSales;

		public double n ;
		public double meanNumSalesProgressive, meanSalesUnitProgressive;
		public double m2NumSalesProgressive, m2SalesUnitsProgressive;

		public Summary() {
			meanSalesUnits = 0;
			meanNumSales = 0;
			varianceSalesUnits = 0;
			varianceNumSales = 0;
			n = 0;
			m2NumSalesProgressive = 0;
			m2SalesUnitsProgressive = 0;
			meanNumSalesProgressive = 0;
			meanSalesUnitProgressive = 0;
		}
	}

	/** 
	 * Calculates 'online' variance (REF: http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#On-line_algorithm)
	 */
	private Summary progressiveSummarySA(int currentTick, double numSales, double salesInUnits) {
		Summary summary = new Summary();
		int lastIndex = summaries.size()-1;
		summary.n = (int) summaries.get(lastIndex).n + 1;

		summary.meanNumSalesProgressive = summaries.get(lastIndex).meanNumSalesProgressive;
		summary.meanSalesUnitProgressive = summaries.get(lastIndex).meanSalesUnitProgressive;
		summary.m2NumSalesProgressive = summaries.get(lastIndex).m2NumSalesProgressive;
		summary.m2SalesUnitsProgressive = summaries.get(lastIndex).m2SalesUnitsProgressive;

		double deltaNumSales = numSales - summary.meanNumSalesProgressive;
		double deltaSalesUnits = salesInUnits - summary.meanSalesUnitProgressive;

		summary.meanNumSalesProgressive += deltaNumSales/summary.n;
		summary.meanSalesUnitProgressive += deltaSalesUnits/summary.n;

		summary.m2NumSalesProgressive += deltaNumSales * (numSales - summary.meanNumSalesProgressive);
		summary.m2SalesUnitsProgressive += deltaSalesUnits*(salesInUnits - summary.meanSalesUnitProgressive);

		summary.meanNumSales = summary.meanNumSalesProgressive;
		summary.meanSalesUnits = summary.meanSalesUnitProgressive;

		summary.varianceNumSales = summary.m2NumSalesProgressive/(summary.n-1);
		summary.varianceSalesUnits = summary.m2SalesUnitsProgressive/(summary.n-1);

		return summary;
	}

	/**
	 * 
	 * @param itr
	 * @return
	 */
	private Summary initializeMeanAndVarianceSA(Iterator itr) {
		int totalDays = (int) (Settings.NumDaysInitialPhase) ;
		if(totalDays < 2 ){
			System.err.println("Initial Phase is less than 2.");
			return new Summary();
		}
		double totalNumSales = 0;
		double totalUnitsSold = 0;
		double sumSqUnits = 0;
		double sumSqNumSales = 0;
		Summary summary = new Summary();

		String strSales = "", strSalesUnits = ""; 

		int num_sales[] = new int[totalDays];
		double sales_units[]=  new double[totalDays];
		
		/* Initialize arrays*/
		for(int i=0; i<totalDays; i++){
			num_sales[i] = 0;
			sales_units[i] = 0.0;
		}
		
		/* Now calculate summaries */
		while (itr.hasNext()) {
			TransactionEdge edge = (TransactionEdge) itr.next();
			for (Transaction transaction : (ArrayList<Transaction>) edge.getTransactionList()) {
				if (transaction.getDrugQtyInUnits() > 0.0 
					//IMP CHECK 
					&& transaction.getTime() > this.entryTick) {
					double time = ((double)(transaction.getTime() - this.entryTick)) / ((double) Settings.StepsInDay);
					int	day = (int) Math.ceil(time);
					/* Why day-1? */
					num_sales[day-1]++;
					totalNumSales++;
					totalUnitsSold += transaction.getDrugQtyInUnits();
					sales_units[day-1] += transaction.getDrugQtyInUnits();
				}
			}
		}
		
		for (int i=0; i<totalDays; i++) {
			sumSqNumSales += num_sales[i] * num_sales[i];
			sumSqUnits +=  sales_units[i] * sales_units[i]; // * ( num_sales[i] * sales_units[i] );
			
			strSales += (num_sales[i] + " , ");
			strSalesUnits += (sales_units[i] + " , ");
			
			summary.n++;

			double delta = num_sales[i] - summary.meanNumSalesProgressive;
			summary.meanNumSalesProgressive += delta/summary.n;
			summary.m2NumSalesProgressive += delta * (num_sales[i] - summary.meanNumSalesProgressive);

			double deltaV = ( sales_units[i] )  - summary.meanSalesUnitProgressive;
			summary.meanSalesUnitProgressive += deltaV/summary.n;
			summary.m2SalesUnitsProgressive += deltaV * ( sales_units[i]  - summary.meanSalesUnitProgressive);

		}

		double varianceNUMS = summary.m2NumSalesProgressive / (summary.n-1);
		double varianceSU = summary.m2SalesUnitsProgressive / (summary.n-1);

		summary.meanNumSales = totalNumSales/totalDays;
		summary.meanSalesUnits = totalUnitsSold/totalDays;

		summary.varianceNumSales = (sumSqNumSales - (summary.meanNumSales*totalNumSales))/(totalDays-1);
		summary.varianceSalesUnits = (sumSqUnits - (summary.meanSalesUnits*totalUnitsSold))/(totalDays-1);

		if(Math.abs(summary.meanNumSales - summary.meanNumSalesProgressive) > 0.1)
			System.err.println("MEAN numsales IS NOT CALCULATED CORRECTLY. " + summary.meanNumSales + " , " + summary.meanNumSalesProgressive);

		if(Math.abs(summary.varianceNumSales - varianceNUMS) > 0.1 )
			System.err.println("VARIANCE numsales IS NOT CALCULATED CORRECTLY. " +  summary.varianceNumSales+ " , " +varianceNUMS);

		if(Math.abs(summary.meanSalesUnits - summary.meanSalesUnitProgressive) > 0.1)
			System.err.println("MEAN salesunits IS NOT CALCULATED CORRECTLY. " + summary.meanSalesUnits + " , " + summary.meanSalesUnitProgressive);

		if(Math.abs(summary.varianceSalesUnits - varianceSU) > 0.1)
			System.err.println("VARIANCE salesunits IS NOT CALCULATED CORRECTLY. " +  summary.varianceSalesUnits + " , " +varianceSU);

		if(Settings.errorLog){
			System.out.println("Dealer: "+ this.personID + " sales:" + strSales + "  sum sq = " + sumSqNumSales);
			System.out.println("Dealer: "+ this.personID +  " meanNumSales: " + summary.meanNumSales + " meanSalesUnits: " + summary.meanSalesUnits +
					" NumSaleVar: " + summary.varianceNumSales + " SalesUnitVar: " + summary.varianceSalesUnits);
		}
		return summary;
	}
}