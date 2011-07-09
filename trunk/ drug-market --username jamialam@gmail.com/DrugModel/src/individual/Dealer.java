package individual;

/**
 * @author shah
 */

import java.util.ArrayList;
import java.util.Iterator;

import cern.jet.random.Uniform;

import drugmodel.ContextCreator;
import drugmodel.Settings;
import drugmodel.Settings.SupplyOption;
import drugmodel.TransactionEdge;
import drugmodel.Transaction;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

public class Dealer extends Person {
	private double timeToLeaveMarket;
	private double lastTimeZeroDrug;
	private double unitsToSell;
	private ArrayList<Summary> summaries;
	private ArrayList<Transaction> lastDayTransactions;

	public Dealer() {
		setDrugs(Settings.Resupply.constantDrugsUnits);
		unitsToSell = Settings.unitsPerGram;
		timeToLeaveMarket = Settings.DealersParams.TimeToLeaveMarket;
		lastTimeZeroDrug = -1;
		summaries = new ArrayList<Summary>();
		lastDayTransactions = new ArrayList<Transaction>();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@ScheduledMethod(start = Settings.initialPhase, interval = Settings.stepsInDay, priority = 6)
	public void updatePrice() {
		Context context = ContextUtils.getContext(this);
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getEdges(this).iterator();
		double currentTick = ContextCreator.getTickCount();		

		unitsToSell = Settings.unitsPerGram;
		
		if ((int)currentTick == Settings.initialPhase) {
			Summary summary = initializeMeanAndVariance(itr);
			summaries.add(summary);
		}
		else {
			double numSales = 0;
			double salesInUnits = 0;
			for (Transaction transaction : lastDayTransactions) {
				numSales++;
				salesInUnits += transaction.getDrugQtyInUnits();
			}			
			int lastIndex = summaries.size()-1;
			double diff = salesInUnits - summaries.get(lastIndex).meanSalesUnits;
			
			double unitsToSell = Settings.unitsPerGram;
			
			if (diff < 0) {
				unitsToSell = unitsToSell + diff;
				
			}		 
			else if (diff > 0) {
				unitsToSell = unitsToSell - diff;				
			}				
			//Skipping the case when the difference is zero.					
			
			//calculate mean and variance for the next day based on today's sales.
			summaries.add(progressiveSummary((int)currentTick, numSales, salesInUnits));
		}						
		lastDayTransactions.clear();			
	}
	
	/** Calculate the online variance (REF: http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#On-line_algorithm)*/
	private Summary progressiveSummary(int currentTick, double numSales, double salesInUnits) {
		Summary summary = new Summary();
		int n = currentTick - 1;

		int lastIndex = summaries.size()-1;
		summary.meanNumSales = summaries.get(lastIndex).meanNumSales;
		summary.meanSalesUnits = summaries.get(lastIndex).meanSalesUnits;

		double deltaNumSales = numSales - summary.meanNumSales;
		double deltaSalesUnits = salesInUnits - summary.meanSalesUnits;
		
		summary.meanNumSales += deltaNumSales/n;
		summary.meanSalesUnits += deltaSalesUnits/n;

		double m2Num = deltaNumSales*(numSales - summary.meanNumSales);
		double m2Units = deltaSalesUnits*(salesInUnits - summary.meanSalesUnits);
		
		summary.varianceNumSales = m2Num/(n-1);
		summary.varianceSalesUnits = m2Units/(n-1);
		
		return summary;
		
		/*def online_variance(data):
		    n = 0
		    mean = 0
		    M2 = 0
		 
		    for x in data:
		        n = n + 1
		        delta = x - mean
		        mean = mean + delta/n
		        M2 = M2 + delta*(x - mean)  # This expression uses the new value of mean
		 
		    variance_n = M2/n
		    variance = M2/(n - 1)
		    return variance
		 */			
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Summary initializeMeanAndVariance(Iterator itr) {
		int totalDays = (int) Settings.initialPhase;
		double totalNumSales = 0;
		double totalUnitsSold = 0;
		double meanNumSales = 0;
		double meanSalesUnits = 0;
		double sumSqUnits = 0;
		double sumSqNumSales = 0;
		//now calculate summaries
		while (itr.hasNext()) {
			TransactionEdge edge = (TransactionEdge) itr.next();
			for (Transaction transaction : (ArrayList<Transaction>) edge.getTransactionList()) {
				totalNumSales++;
				totalUnitsSold += transaction.getDrugQtyInUnits();
				sumSqUnits += totalUnitsSold*totalUnitsSold;
				sumSqNumSales += totalNumSales*totalNumSales;
			}
		}		
		Summary summary = new Summary();
		summary.meanNumSales = totalNumSales/totalDays;
		summary.meanSalesUnits = totalUnitsSold/totalDays;
		summary.varianceNumSales = (sumSqUnits - (meanSalesUnits*totalUnitsSold))/(totalDays-1);
		summary.varianceSalesUnits = (sumSqNumSales - (meanNumSales*totalNumSales))/(totalDays-1);
		
		return summary;
	}

	public double returnUnitsToSell() {
		//return Uniform.staticNextDoubleFromTo(9,12);
		return unitsToSell;
	}
	
	public double returnCostPerUnit() {
		return (unitsToSell/Settings.pricePerGram);
	}

	public void sellDrug(Transaction transaction) {
		if(this.drugs < unitsToSell && Settings.Resupply.getSupplyOption().equals(SupplyOption.Automatic)){
			supplyAutomatic();
		}
			
		deductDrug(transaction.getDrugQtyInUnits());
		addMoney(Settings.pricePerGram);
		lastDayTransactions.add(transaction);	
	}

	/** 
	 * 	Dealers are supplied an inventory of drugs on a schedule, based on the cycles of the model, with a standard amount of drug (we could start with 12 grams supplied every three weeks). They all start with the same amount. If dealers run out of drug supplies to sell 
	 * before their scheduled re-supply they are automatically re-supplied. If at the re-supply time dealer still has drug to sell there can be two options (to play with in experiments). 
	 * The first option, dealers are re-supplied with the difference between what is remaining and their original supply amount. The second option, dealers could be resupplied with the standard amount and will have to deal with the “surplus.” 
	 * Dealer agents could grow as there business increases, shrink as their business decreases, and change colors if in the “black” at there last resupply deadline. Alternatively, will only a few dealers it might be nice to see the supplies and surpluses displayed in graphs.  
	 * If a dealer runs out of customers or drug supply, after X number of cycles of the simulation, they are eliminated. 
	 */
	@ScheduledMethod(start = 1, interval = Settings.DealersParams.resupplyInterval, priority = 5)
	public void supplyRegular(){
		SupplyOption supplyOption = Settings.Resupply.getSupplyOption();;
		if ( supplyOption.equals(SupplyOption.Automatic) == true) {
			return;
		}
		this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));						
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 5)
	public void supplyAutomatic() {
		if (this.drugs < unitsToSell ) {
			this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));	
		}
	}

	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 5)
	public void supply() {
		double currentTick = ContextCreator.getTickCount();
		if (this.drugs <= 0 && lastTimeZeroDrug == -1)  {
			lastTimeZeroDrug = currentTick;
		}
		if (Settings.Resupply.getSupplyOption().equals(SupplyOption.Automatic)) {
			if (this.drugs <= 0.0) {
				this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));	
			}	
		}
		else {
			if (currentTick % Settings.DealersParams.resupplyInterval == 0) {
				this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));		
			}
		}
		if (this.drugs > 0) {
			lastTimeZeroDrug = -1;
		}
	}

	@SuppressWarnings("rawtypes")
	@ScheduledMethod(start = Settings.initialPhase, interval = Settings.DealersParams.TimeToLeaveMarket, priority = 1)
	public void dropOut() {		
		double currentTick = ContextCreator.getTickCount();
		Context context = ContextUtils.getContext(this);
		if (	(this.lastTimeZeroDrug != -1
				&& currentTick - lastTimeZeroDrug > Settings.DealersParams.TimeToLeaveMarket)
				|| isLastTransaction(context, currentTick)
		) {
			System.out.println("Dealer dropping out. :  " + personID);
			context.remove(this);	
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean isLastTransaction(Context context, double currentTick) {
		Network transactionNetwork = (Network)context.getProjection(Settings.transactionnetwork);
		Iterator itr = transactionNetwork.getEdges(this).iterator();
		//flag to check if there is a previous transaction within the period.
		boolean flag = false;
		while (itr.hasNext()) {
			TransactionEdge edge = (TransactionEdge) itr.next();
			if (edge.getTransactionList().isEmpty() == false) {
				int size = edge.getTransactionList().size();				
				Transaction transaction = (Transaction) edge.getTransactionList().get(size-1);
				if (currentTick - transaction.getTime() >= Settings.DealersParams.TimeToLeaveMarket) {
					flag = true;
					break;
				}
			}
		}
		return flag;
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
	
	protected class Summary {
		public double meanSalesUnits;
		public double meanNumSales;
		public double varianceSalesUnits;
		public double varianceNumSales;
		
		public Summary() {
			meanSalesUnits = 0;
			meanNumSales = 0;
			varianceSalesUnits = 0;
			varianceNumSales = 0;			
		}
	}

	public double getUnitsToSell() {
		return unitsToSell;
	}

	public void setUnitsToSell(double unitsToSell) {
		this.unitsToSell = unitsToSell;
	}
}