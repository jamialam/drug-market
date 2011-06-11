package individual;

import repast.simphony.engine.schedule.ScheduledMethod;
import cern.jet.random.Uniform;
import drugmodel.ContextCreator;
import drugmodel.Settings;
import drugmodel.Settings.SupplyOption;

public class Customer extends Person {
	/** Number of dealers known at the time of start or entry into the simulation.*/
	private int initKnownDealers; 
	private double initialBudget;
	
	public double getInitialBudget() {
		return initialBudget;
	}

	public void setInitialBudget(double budget) {
		this.initialBudget = budget;
	}

	public Customer() {
		initKnownDealers = Uniform.staticNextIntFromTo(Settings.minDealerCustomerlinks, Settings.maxDealerCustomerlinks);
		initializeInventory();
	}

	private void initializeInventory() {
		this.initialBudget = Settings.Budget.returnInitialBudget();
		addMoney(initialBudget);
	}
		
	/**
	 *  They buy based on two factors: 1) the expected price of the drug and 2) 
	 *  what is offered. If they have competing deals from dealers they always determine the �best� deal,
	 *  i.e., they evaluate the units sold at the lowest price, i.e., the lowest number from above.
	 *   Of course, they can only make this determination after they have bought the drug.
	*/
	public void buyDrugs() {
		// (this.money )
	}
	
	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 4)
	public void income() {
		double currentTick = ContextCreator.getTickCount();
		if (Settings.Resupply.getSupplyOption().equals(SupplyOption.Automatic)) {
			if (this.money <= 0.0) {
				this.addMoney(Settings.Resupply.income(this.money));	
			}	
		}
		else {
			if (currentTick % Settings.CustomerParams.incomeInterval == 0) {
				this.addMoney(Settings.Resupply.income(this.money));		
			}
		}
	}	

	@ScheduledMethod(start = 1, interval = 1, priority = 3)
	public void useDrugs() {	
		double consumedDrug = Settings.consumptionStepsPerUnit;
		deductDrug(consumedDrug);		
		if (Settings.errorLog) {
			System.out.println("Customer: " + personID + " consumes : " + consumedDrug + " now has: " + drugs);
		}
	}

	public int getInitKnownDealers() {
		return initKnownDealers;
	}

	public void setInitKnownDealers(int numInitialDealers) {
		this.initKnownDealers = numInitialDealers;
	}	
}