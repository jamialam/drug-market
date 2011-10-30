package individual;

/**
 * @author shah
 */

import java.
util.ArrayList;
import java.util.Iterator;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;

import drugmodel.ContextCreator;
import drugmodel.Settings;
import drugmodel.Settings.DealerType;
import drugmodel.Settings.SupplyOption;
import drugmodel.TransactionEdge;
import drugmodel.Transaction;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })

public class Dealer extends Person {
	private double unitsToSell;
	private double timeToLeaveMarket;
	private double lastTimeZeroDrug;
	private double timeLastTransaction; 
	private ArrayList<Summary> summaries;
	private ArrayList<Transaction> lastDayTransactions;
	private int evaluationInterval;
	private double surplus; 
	private double dealsPerDay;

	//new dealers params
	private double entryTick;
	private int parentID;
	private DealerType type;
	private double dealsYesterday;
	private double dealsToday;

	// for display
	private double salesPerDay;
	private double salesYesterday;
	private double salesToday;
	private double totalSales;

	public Dealer(double... params) {
		setDrugs(Settings.Resupply.constantDrugsUnits);
		unitsToSell = Settings.unitsPerGram;
		timeToLeaveMarket = Settings.DealersParams.TimeToLeaveMarket;
		lastTimeZeroDrug = -1;
		timeLastTransaction = -1;
		summaries = new ArrayList<Summary>();
		lastDayTransactions = new ArrayList<Transaction>();
		// added by sa on 10/9/2011
		this.drugs = Settings.Resupply.constantDrugsUnits;
		this.evaluationInterval = Uniform.staticNextIntFromTo(1, (int)Settings.DealersParams.TimeToLeaveMarket);
		this.surplus = 0.0;
		dealsPerDay = 0.0;

		if(ContextCreator.getTickCount() == -1)
			this.entryTick = 0.0;//ContextCreator.getTickCount();
		else
			this.entryTick = ContextCreator.getTickCount();

		this.salesPerDay = 0.0;
		this.salesToday = 0.0;
		this.salesYesterday = -1.0;
		this.dealsToday = 0.0;
		this.dealsYesterday = -1.0;
		this.totalSales = 0.0 ;

		if(params.length == 0 ){
			parentID = -1;
			type = DealerType.old;
		}
		else{
			parentID = (int)params[0];
			unitsToSell = params[1];
			if (Settings.DealersParams.newDealerType == true)
			{	
				if(Math.random() <= Settings.DealersParams.GreedynewDealerProb)
					type = DealerType.Greedy;
				else
					type = DealerType.Planner;
			}
			else
				type = DealerType.old;
		}
		System.out.println("new Dealer : " + this.personID + " entry tick: " + entryTick +" entry/48 : " +entryTick/48);
		
	}

	@ScheduledMethod(start = Settings.initialPhase, interval = Settings.stepsInDay, priority = 5)
	public void updatePrice() {
		Context context = ContextUtils.getContext(this);
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getEdges(this).iterator();
		double currentTick = ContextCreator.getTickCount();		
		
		System.out.println("current tick: " +currentTick  +" day : " + currentTick/48);
		
		if(currentTick <  Settings.initialPhase + this.entryTick){
			System.out.println("D: " + this.personID  +" entry: " + this.entryTick  + "LESS current tick: " +currentTick  +" day : " + currentTick/48);
			if(this.type == DealerType.Greedy){
				if( (this.salesYesterday == -1 || this.salesToday > salesYesterday) && unitsToSell > 9 )
					unitsToSell--;
			}
			else if(this.type == DealerType.Planner){
				if( ( this.dealsYesterday == -1 || this.dealsToday > this.dealsYesterday) && unitsToSell < 15)
					unitsToSell++;
			}
			return;
		}
		//to call it at the start of the day
		else if(currentTick == (Settings.initialPhase + entryTick ) ){
//		else if(currentTick == (Settings.initialPhase + entryTick )  +( Settings.stepsInDay - (Settings.initialPhase + this.entryTick) % Settings.stepsInDay )){

			System.out.println("D: " + this.personID  +" entry: " + this.entryTick + " EQUAL current tick: " +currentTick  +" day : " + currentTick/48);
			System.out.println("(Settings.initialPhase + entryTick ): " + (Settings.initialPhase + entryTick ));
			double val = (Settings.initialPhase + entryTick )+( Settings.stepsInDay - (Settings.initialPhase + this.entryTick) % Settings.stepsInDay );
			System.out.println("Jamal: " + val);
			Summary summary = initializeMeanAndVarianceSA(itr);
			summaries.add(summary);
			return;
		}
		
		else if(currentTick > Settings.initialPhase + entryTick ){
			System.out.println("D: " + this.personID  +" entry: " + this.entryTick + " GREATER current tick: " +currentTick  +" day : " + currentTick/48);

			double numSales = 0;
			double salesInUnits = 0;
			for (Transaction transaction : lastDayTransactions) {
				numSales++;
				salesInUnits += transaction.getDrugQtyInUnits();
			}			

			int lastIndex = summaries.size()-1;
			double diff_new = salesInUnits - summaries.get(lastIndex).meanSalesUnits;

			//1st impl
			meanNumSalesFn(numSales );
			//2nd impl
//			meanSaleUnitsFn(salesInUnits);
			// 3rd impl
//			diffSalesInUnits(salesInUnits);
			 //calculate mean and variance for the next day based on today's sales.
			 summaries.add(progressiveSummarySA((int)currentTick, numSales, salesInUnits));
		}						
		lastDayTransactions.clear();			
	}

	public void meanNumSalesFn(double numSales){
		int lastIndex = summaries.size()-1;
		double stdDivNumSales = Math.sqrt(summaries.get(lastIndex).varianceNumSales);
		double meanNumSales = summaries.get(lastIndex).meanNumSales;

		if(numSales < (meanNumSales - 3*stdDivNumSales) ){
			if (unitsToSell < 15)
				unitsToSell = unitsToSell + 1;
			else
				unitsToSell = Uniform.staticNextIntFromTo(9, 15);
		}
		else if(numSales > (meanNumSales + 3*stdDivNumSales) ){ 
			if(unitsToSell > 9)
				unitsToSell = unitsToSell - 1;
			else
				unitsToSell = Uniform.staticNextIntFromTo(9, 15);
		}
	}
	public void meanSaleUnitsFn(double salesInUnits){
		int lastIndex = summaries.size()-1;		
		double stdDivSaleUnits = Math.sqrt(summaries.get(lastIndex).varianceSalesUnits);
		double meanSaleUnits = summaries.get(lastIndex).meanSalesUnits;
		
		 if(salesInUnits < (meanSaleUnits - 3*stdDivSaleUnits) ){
			 if (unitsToSell < 15)
				 unitsToSell = unitsToSell + 1;
			 else
				 unitsToSell = Uniform.staticNextIntFromTo(9, 15);
		 }
		 else if(salesInUnits > (meanSaleUnits + 3*stdDivSaleUnits) ){ 
			 if(unitsToSell > 9)
				 unitsToSell = unitsToSell - 1;
			 else
				 unitsToSell = Uniform.staticNextIntFromTo(9, 15);
		 }
	}
	public void diffSalesInUnits(double salesInUnits){ 
		int lastIndex = summaries.size()-1;
		double diff_new = salesInUnits - summaries.get(lastIndex).meanSalesUnits;

		if (diff_new < -1 ) {
			if(unitsToSell < 15)
				unitsToSell = unitsToSell + 1;
			else
				unitsToSell = Uniform.staticNextIntFromTo(9, 15);
		}		 
		else if (diff_new > 1 ) {
			if(unitsToSell > 9)
				unitsToSell = unitsToSell - 1;
			else
				unitsToSell = Uniform.staticNextIntFromTo(9, 15);;
		}				
	}

	/** Calculate the online variance (REF: http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#On-line_algorithm)*/
	private Summary progressiveSummarySA(int currentTick, double numSales, double salesInUnits) {
		Summary summary = new Summary();
		int lastIndex = summaries.size()-1;
		summary.n = (int) summaries.get(lastIndex).n + 1;

		summary.meanNumSalesProgressive = summaries.get(lastIndex).meanNumSalesProgressive;
		summary.meanSalesUnitProgressive = summaries.get(lastIndex).meanSalesUnitProgressive;
		summary.M2NumSalesProgressive = summaries.get(lastIndex).M2NumSalesProgressive;
		summary.M2SalesUnitsProgressive = summaries.get(lastIndex).M2SalesUnitsProgressive;

		double deltaNumSales = numSales - summary.meanNumSalesProgressive;
		double deltaSalesUnits = salesInUnits - summary.meanSalesUnitProgressive;

		summary.meanNumSalesProgressive += deltaNumSales/summary.n;
		summary.meanSalesUnitProgressive += deltaSalesUnits/summary.n;

		summary.M2NumSalesProgressive += deltaNumSales * (numSales - summary.meanNumSalesProgressive);
		summary.M2SalesUnitsProgressive += deltaSalesUnits*(salesInUnits - summary.meanSalesUnitProgressive);


		summary.meanNumSales = summary.meanNumSalesProgressive;
		summary.meanSalesUnits = summary.meanSalesUnitProgressive;

		summary.varianceNumSales = summary.M2NumSalesProgressive/(summary.n-1);
		summary.varianceSalesUnits = summary.M2SalesUnitsProgressive/(summary.n-1);

		return summary;

	}
	private Summary initializeMeanAndVarianceSA(Iterator itr) {
		int totalDays = (int) (Settings.numDaysInitialPhase);
		double totalNumSales = 0;
		double totalUnitsSold = 0;
		double sumSqUnits = 0;
		double sumSqNumSales = 0;
		Summary summary = new Summary();

		String strSales = "", strSalesUnits = ""; 

		int num_sales[] = new int[ totalDays ];
		double sales_units[]=  new double[totalDays]; 
		for(int i =0; i < totalDays ; i++){
			num_sales[i] = 0;
			sales_units[i] = 0.0;
		}
		//now calculate summaries
		while (itr.hasNext()) {
			TransactionEdge edge = (TransactionEdge) itr.next();
			for (Transaction transaction : (ArrayList<Transaction>) edge.getTransactionList()) {
				if(transaction.getDrugQtyInUnits() > 0.0 && transaction.getTime() > this.entryTick ){
					double time = ((double)(transaction.getTime() - this.entryTick)) / (double ) Settings.stepsInDay;
					int	day = (int) Math.ceil(time); 
					num_sales[day-1]++;
					totalNumSales++;
					totalUnitsSold += transaction.getDrugQtyInUnits();
					sales_units[day-1] += transaction.getDrugQtyInUnits();
				}
			}
		}
		for(int i = 0; i < totalDays; i++  ){
			sumSqNumSales += num_sales[i] * num_sales[i];
			sumSqUnits += ( sales_units[i] * sales_units[i] );// * ( num_sales[i] * sales_units[i] );
			strSales += (num_sales[i] + " , ");
			strSalesUnits += (sales_units[i] + " , ");
			summary.n++;
			double delta = num_sales[i] - summary.meanNumSalesProgressive;
			summary.meanNumSalesProgressive += delta/summary.n;
			summary.M2NumSalesProgressive += delta * (num_sales[i] - summary.meanNumSalesProgressive);

			double deltaV = ( sales_units[i] )  - summary.meanSalesUnitProgressive;
			summary.meanSalesUnitProgressive += deltaV/summary.n;
			summary.M2SalesUnitsProgressive += deltaV * ( sales_units[i]  - summary.meanSalesUnitProgressive);

		}

		double varianceNUMS = summary.M2NumSalesProgressive / (summary.n-1);
		double varianceSU = summary.M2SalesUnitsProgressive / (summary.n-1);

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


		System.out.println("Dealer: "+ this.personID + " sales:" + strSales + "  sum sq = " + sumSqNumSales);
		System.out.println("Dealer: "+ this.personID +  " meanNumSales: " + summary.meanNumSales + " meanSalesUnits: " + summary.meanSalesUnits +
				" NumSaleVar: " + summary.varianceNumSales + " SalesUnitVar: " + summary.varianceSalesUnits);

		return summary;
	}

	//TODO
	/*
	 * make everyone interval tick different
	 */

	@ScheduledMethod(start = 1, interval = 1, priority = 4)
	public void supply() {
		double currentTick = ContextCreator.getTickCount();
		if (this.drugs <= 0 && lastTimeZeroDrug == -1)  {
			lastTimeZeroDrug = currentTick;
		}
		if (Settings.Resupply.getSupplyOption().equals(SupplyOption.Automatic)) {
			if (this.drugs <  unitsToSell ) {
				this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));	
			}	
		}
		else {
			if ( ( (currentTick - entryTick) % Settings.DealersParams.resupplyInterval) == 0) {
				if(Settings.Resupply.getSupplyOption().equals(SupplyOption.RegularSurplus)){
					this.surplus = this.drugs;  
					if(this.surplus > Settings.DealersParams.surplusLimit ){
						System.out.println("tick:"+ ContextCreator.getTickCount()+ " Dealer dropping out due to surplus :  " + personID);
						Context context = ContextUtils.getContext(this);
						context.remove(this);	
					}
				}
				this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));		
			}
		}
		if (this.drugs > 0) {
			lastTimeZeroDrug = -1;
		}
	}

	/** 
	 * 	Dealers are supplied an inventory of drugs on a schedule, based on the cycles of the model, with a standard amount of drug (we could start with 12 grams supplied every three weeks). They all start with the same amount. If dealers run out of drug supplies to sell 
	 * before their scheduled re-supply they are automatically re-supplied. If at the re-supply time dealer still has drug to sell there can be two options (to play with in experiments). 
	 * The first option, dealers are re-supplied with the difference between what is remaining and their original supply amount. The second option, dealers could be resupplied with the standard amount and will have to deal with the “surplus.” 
	 * Dealer agents could grow as there business increases, shrink as their business decreases, and change colors if in the “black” at there last resupply deadline. Alternatively, will only a few dealers it might be nice to see the supplies and surpluses displayed in graphs.  
	 * If a dealer runs out of customers or drug supply, after X number of cycles of the simulation, they are eliminated. 
	 */

	@ScheduledMethod(start = Settings.DealersParams.TimeToLeaveMarket, interval = 1, priority = 4)
	public void dropOut() {		
		double currentTick = ContextCreator.getTickCount();

		if(currentTick - entryTick < Settings.DealersParams.TimeToLeaveMarket )
			return;

		if(ContextCreator.getTickCount() == this.evaluationInterval 
				||	(ContextCreator.getTickCount() % (Settings.DealersParams.TimeToLeaveMarket) ) == this.evaluationInterval ) {
			Context context = ContextUtils.getContext(this);
			if (	(this.timeLastTransaction == -1)
					/*	&& currentTick - lastTimeZeroDrug >= Settings.DealersParams.TimeToLeaveMarket)
					//no customer
					 */	||

					 currentTick - timeLastTransaction > Settings.DealersParams.TimeToLeaveMarket		) 
			{
	//			System.out.println("tick:"+ ContextCreator.getTickCount()+" Dealer dropping out due to NO customer. :  " + personID);
				context.remove(this);	
			}
		}
	}

	@ScheduledMethod(start = Settings.stepsInDay, interval = Settings.DealersParams.newDealerInterval, priority = 2)
	public void newDealer() {		

		if (dealsPerDay > Settings.DealersParams.maxDealsLimit ) 
		{
			Dealer new_dealer = new Dealer(this.personID, this.unitsToSell);
			Context context = ContextUtils.getContext(this);
			context.add(new_dealer);
			Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
			//check it out.............................
			int numOfCustomers = transactionNetwork.getDegree(this);
//			System.out.println("tick: "+ ContextCreator.getTickCount()+" Dealer: " + personID + " spliting due to more deals: " + dealsPerDay + "  num of customer: " +numOfCustomers+ "  new Dealer:  " + new_dealer.getPersonID());

			Iterator itr_customers = transactionNetwork.getEdges(this).iterator();

			numOfCustomers /= 2;
			int i=0;
			Customer customer = null;
			ArrayList<TransactionEdge> list = new ArrayList<TransactionEdge>(); 
			ArrayList<Transaction> transactionlist = new ArrayList<Transaction>();

			while(itr_customers.hasNext()	&&	i < numOfCustomers){
				TransactionEdge edge = (TransactionEdge) itr_customers.next();

				Object objS = edge.getSource();
				Object objT = edge.getTarget();
				if(objS instanceof Customer){
					customer  = (Customer) objS;
				}
				else if(objT instanceof Customer){
					customer = (Customer) objT;
				}
				else{
					System.err.println("FATAL ERR: newDealer neither source nor target in transaction edge are customer");
					return;
				}
				list.add(edge);
				TransactionEdge newEdge = new TransactionEdge(new_dealer, customer);
				//9/28/2011
				transactionlist = edge.getTransactionList();
				for(Transaction transaction : transactionlist ){
					transaction.setDealer(new_dealer);
				}
				newEdge.setTransactionList(transactionlist);
				/////////////
				transactionNetwork.addEdge(newEdge);
				i++;
			}
			for(TransactionEdge e : list){
				transactionNetwork.removeEdge(e);
			}
		}
		this.salesYesterday = this.salesToday;
		this.salesToday = salesPerDay;
		this.dealsYesterday = this.dealsToday;
		this.dealsToday = this.dealsPerDay;
		salesPerDay = 0.0;
		dealsPerDay = 0;

	}



	/**
	 * TODO: Replace the Normal function, currently, only for testing purposes 
	 * and link it  the updatePrice method. 
	 * @return
	 */

	public double returnUnitsToSell() {
		/*		do {
			unitsToSell = (int) Normal.staticNextDouble(12, 3);
		} while (unitsToSell <= 0);
		 */		
		if(this.drugs < unitsToSell ){
			supplyAutomatic();
		}
		return unitsToSell;
	}

	public double returnCostPerUnit() {
		return (Settings.pricePerGram/this.unitsToSell);
	}

	public boolean sellDrug(Transaction transaction) {
		if(this.drugs <= 0.0 && Settings.errorLog){
			System.err.println("Dealer "+this.personID+ "  cant sell. drug amount is zero. " +ContextCreator.getTickCount());
			return false;
		}
		deductDrug(transaction.getDrugQtyInUnits());
		addMoney(Settings.pricePerGram);
		lastDayTransactions.add(transaction);
		timeLastTransaction = transaction.getTime();
		dealsPerDay++;
		salesPerDay += transaction.getDrugQtyInUnits();
		totalSales += salesPerDay;
		return true;
	}
	public void supplyAutomatic() {
		if (this.drugs < unitsToSell ) {
			this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));	
		}
	}

	public double getSalesYesterday() {
		return salesYesterday;
	}
	public double getSalesToday() {
		return salesToday;
	}
	public double getTimeToLeaveMarket() {
		return timeToLeaveMarket;
	}
	public void setTimeToLeaveMarket(double timeToLeaveMarket) {
		this.timeToLeaveMarket = timeToLeaveMarket;
	}
	public double getLastTimeZeroDrug() {
		return lastTimeZeroDrug;
	}
	public void setLastTimeZeroDrug(double lastTimeZeroDrug) {
		this.lastTimeZeroDrug = lastTimeZeroDrug;
	}
	public double getUnitsToSell() {
		return unitsToSell;
	}
	public void setUnitsToSell(double unitsToSell) {
		this.unitsToSell = unitsToSell;
	}
	public double getTimeLastTransaction() {
		return timeLastTransaction;
	}
	public void setTimeLastTransaction(double timeLastTransaction) {
		this.timeLastTransaction = timeLastTransaction;
	}
	public int getEvaluationInterval() {
		return evaluationInterval;
	}
	public float getSale() {
		return (float) salesPerDay;
	}
	public double getDealsPerDay() {
		return dealsPerDay;
	}
	public int getParentID() {
		return parentID;
	}
	public DealerType getType() {
		return type;
	}
	public double getDealsYesterday() {
		return dealsYesterday;
	}
	public double getDealsToday() {
		return dealsToday;
	}
	public double getSalesPerDay() {
		return salesPerDay;
	}
	protected class Summary {
		public double meanSalesUnits;
		public double meanNumSales;
		public double varianceSalesUnits;
		public double varianceNumSales;

		public double n ;
		public double meanNumSalesProgressive, meanSalesUnitProgressive;
		public double M2NumSalesProgressive, M2SalesUnitsProgressive;

		public Summary() {
			meanSalesUnits = 0;
			meanNumSales = 0;
			varianceSalesUnits = 0;
			varianceNumSales = 0;

			n = 0;
			M2NumSalesProgressive = 0;
			M2SalesUnitsProgressive = 0;
			meanNumSalesProgressive = 0;
			meanSalesUnitProgressive = 0;
		}
	}
	public double getTotalSales() {
		return totalSales;
	}
}