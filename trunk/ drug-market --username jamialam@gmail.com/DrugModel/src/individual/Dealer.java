package individual;


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
	private double timeToLeaveMarket;
	private double lastTimeZeroDrug;
	
	public Dealer() {
		setDrugs(Settings.Resupply.constantDrugsUnits);
		timeToLeaveMarket = Settings.DealersParams.TimeToLeaveMarket;
		lastTimeZeroDrug = -1;
	}
	
	//We want to make this a watcher query
	public void sellDrugs(){		
	}
	
	public double getDeal(){
		if (ContextCreator.getTickCount() <= Settings.initialPhase) {
			return Settings.units_per_grams;
		}
		else {
			return 0;
		}
	}

	public void changeOffer(){
		// if sell is down increase units_per_grams else do inverse
		/*	Dealers will then change their offers based on the amount of sales they are making. After establishing a mean number of sales, standard deviations can signal to a dealer agent when make changes, e.g, “low” sales = -1 standard deviation in the number of sales; “high” sales = + 1 standard deviation. At these points the dealer will unilaterally change their deal. 

	Dealers will change their deals by offering more or less drug (changing the units they sell). Price will remain the same ($120). If sales are down, they want to attract more customers and will offer better deals, i.e., instead of selling 12 units for $120 (10) they change to selling 12+1 units for $120 (9.23). If sales are up, they will reduce their deals, i.e., instead of selling 12 units for $120 (10), they sell 12-1 units for $120 (10.9). 
		 */
	}

	/** 
	 * 	Dealers are supplied an inventory of drugs on a schedule, based on the cycles of the model, with a standard amount of drug (we could start with 12 grams supplied every three weeks). They all start with the same amount. If dealers run out of drug supplies to sell 
	 * before their scheduled resupply they are automatically resupplied. If at the resupply time dealer still has drug to sell there can be two options (to play with in experiments). 
	 * The first option, dealers are resupplied with the difference between what is remaining and their original supply amount. The second option, dealers could be resupplied with the standard amount and will have to deal with the “surplus.” 
	 * Dealer agents could grow as there business increases, shrink as their business decreases, and change colors if in the “black” at there last resupply deadline. Alternatively, will only a few dealers it might be nice to see the supplies and surpluses displayed in graphs.  
	 * If a dealer runs out of customers or drug supply, after X number of cycles of the simulation, they are eliminated. 
	*/
	@ScheduledMethod(start = 1, interval = Settings.Resupply.resupplyInterval, priority = 4)
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
	
	@ScheduledMethod(start = 1, interval = 1, priority = 4)
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
			if (currentTick % Settings.Resupply.resupplyInterval == 0) {
				this.addDrug(Settings.Resupply.resupplyDrugs(this.drugs));		
			}
		}
		if (this.drugs > 0) {
			lastTimeZeroDrug = -1;
		}
	}
	
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
	
	
}