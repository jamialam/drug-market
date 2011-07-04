package individual;

/**
 * @author shah
 */
import java.util.Iterator;

import drugmodel.ContextCreator;
import drugmodel.Settings;
import drugmodel.Settings.SupplyOption;
import drugmodel.TransEdge;
import drugmodel.Transaction;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

public class Dealer extends Person {
	private double priceInUnits;
	private double timeToLeaveMarket;
	private double lastTimeZeroDrug;
	
	public Dealer() {
		setDrugs(Settings.Resupply.constantDrugsUnits);
		priceInUnits = (Settings.price_per_gram/Settings.units_per_gram);
		timeToLeaveMarket = Settings.DealersParams.TimeToLeaveMarket;
		lastTimeZeroDrug = -1;
	}
	
	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 3)
	public void updatePrice(){
		
	}
	
	public double returnDrugInUnits(){
		if (ContextCreator.getTickCount() <= Settings.initialPhase) {
			return Settings.units_per_gram;
		}
		else {
			double units = (Settings.price_per_gram/priceInUnits);
			return units;
		}
	}
	
	public void sellDrug(double quantity) {
		deductDrug(quantity);
		addMoney(Settings.price_per_gram);
	}

	/** 
	 * 	Dealers are supplied an inventory of drugs on a schedule, based on the cycles of the model, with a standard amount of drug (we could start with 12 grams supplied every three weeks). They all start with the same amount. If dealers run out of drug supplies to sell 
	 * before their scheduled resupply they are automatically resupplied. If at the resupply time dealer still has drug to sell there can be two options (to play with in experiments). 
	 * The first option, dealers are resupplied with the difference between what is remaining and their original supply amount. The second option, dealers could be resupplied with the standard amount and will have to deal with the �surplus.� 
	 * Dealer agents could grow as there business increases, shrink as their business decreases, and change colors if in the �black� at there last resupply deadline. Alternatively, will only a few dealers it might be nice to see the supplies and surpluses displayed in graphs.  
	 * If a dealer runs out of customers or drug supply, after X number of cycles of the simulation, they are eliminated. 
	*/
	@ScheduledMethod(start = 1, interval = Settings.DealersParams.resupplyInterval, priority = 4)
	public void supplyRegular(){
		SupplyOption supplyOption = Settings.Resupply.getSupplyOption();;
		if ( supplyOption.equals(SupplyOption.Automatic) == true) {
			return;
		}
		this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));						
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 4)
	public void supplyAutomatic() {
		if (this.drugs <= 0.0) {
			this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));	
		}
	}
	
	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 4)
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
	@ScheduledMethod(start = 1, interval = Settings.DealersParams.TimeToLeaveMarket, priority = 1)
	public void dropOut() {		
		double currentTick = ContextCreator.getTickCount();
		Context context = ContextUtils.getContext(this);
		if (	(this.lastTimeZeroDrug != -1
					&& currentTick - lastTimeZeroDrug > Settings.DealersParams.TimeToLeaveMarket)
				|| isLastTransaction(context, currentTick)
			) {
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
			TransEdge edge = (TransEdge) itr.next();
			if (edge.getTransactionList().isEmpty() == false) {
				int size = edge.getTransactionList().size();				
				Transaction transaction = (Transaction) edge.getTransactionList().get(size-1);
				if (currentTick - transaction.getTime() <= Settings.DealersParams.TimeToLeaveMarket) {
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

	public double getPriceInUnits() {
		return priceInUnits;
	}

	public void setPriceInUnits(double priceInUnits) {
		this.priceInUnits = priceInUnits;
	}
}