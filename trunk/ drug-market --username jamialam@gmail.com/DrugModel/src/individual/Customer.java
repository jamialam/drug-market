package individual;

import cern.jet.random.Uniform;
import drugmodel.Settings;

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
	 *  what is offered. If they have competing deals from dealers they always determine the “best” deal,
	 *  i.e., they evaluate the units sold at the lowest price, i.e., the lowest number from above.
	 *   Of course, they can only make this determination after they have bought the drug.
	*/
	public void buyDrugs() {
	}

	public void useDrugs() {	
	}

	public void move() {		
	}

	public int getInitKnownDealers() {
		return initKnownDealers;
	}

	public void setInitKnownDealers(int numInitialDealers) {
		this.initKnownDealers = numInitialDealers;
	}	
}