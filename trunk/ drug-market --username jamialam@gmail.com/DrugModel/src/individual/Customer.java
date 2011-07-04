package individual;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;
import cern.jet.random.Uniform;
import drugmodel.ContextCreator;
import drugmodel.SNEdge;
import drugmodel.Settings;
import drugmodel.Settings.DealerSelection;
import drugmodel.Settings.Endorsement;
import drugmodel.Settings.SupplyOption;
import drugmodel.Settings.TaxType;
import drugmodel.TransEdge;
import drugmodel.Transaction;

public class Customer extends Person {
	/** Number of dealers known at the time of start or entry into the simulation.*/
	private int initKnownDealers; 
	private double initialBudget;
	private Context context;
	private Transaction lastTransaction;
	private double tax; 

	public Customer() {
		initKnownDealers = Uniform.staticNextIntFromTo(Settings.minDealerCustomerlinks, Settings.maxDealerCustomerlinks);
		initializeInventory();
		context = (Context)ContextUtils.getContext(this);
		lastTransaction = null;
	}	

	private void initializeInventory() {
		this.initialBudget = Settings.Budget.returnInitialBudget();
		addMoney(initialBudget);
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			tax = Settings.Tax.returnTaxCost();
		}
		else {
			tax = Settings.Tax.returnDrugInUnits();
		}
	}

	/**
	 *  The purchase is based on two factors: 
	 *  1) the expected price of the drug and,
	 *   2)  what is offered. 
	 *   If they have competing deals from dealers they always determine the “best” deal, 
	 *   i.e., they evaluate the units sold at the lowest price, i.e., the lowest number from above.
	 *   Of course, they can only make this determination after they have bought the drug.
	 */
	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 3)
	public void buyDrugs() {
		Transaction deal;
		if (Settings.DealersParams.dealerSelection.equals(DealerSelection.NetworkBest)) {
			deal = returnBestDealsFromNetwork();
		}
		else if (Settings.DealersParams.dealerSelection.equals(DealerSelection.MyBest)) {
			deal = returnMyBestDeal();			
		}
		else {
			deal = null;
		}
		Dealer dealer = null;
		if (deal != null) {
			dealer = (Dealer) returnMyConnection(deal.getDealerID(), Settings.transactionnetwork);
		}
		else {
			dealer = returnRandomDealer();
		}

		if (Settings.errorLog) {
			if (dealer == null || dealer instanceof Dealer == false) {
				System.err.println("Dealer is null or not found. In buyDrugs(). Me: " + personID);
			}
		}		 
		int currentTick = (int) ContextCreator.getTickCount();
		double quantity = dealer.returnDrugInUnits();
		//This is calculated implicitly.
		double cost = quantity * Settings.price_per_gram;
		
		Endorsement endorsement = Endorsement.None;
		if (deal != null) {
			endorsement = cost <= deal.getDrugCost() ? Endorsement.Good : Endorsement.Bad;
		}

		Transaction transaction = new Transaction(dealer.getPersonID(), personID, currentTick, cost, quantity, endorsement);
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		TransEdge edge;
		if (transactionNetwork.getEdge(this, dealer) == null) {
			edge = new TransEdge(this, dealer);
			transactionNetwork.addEdge(edge);
		}
		else {
			edge = (TransEdge) transactionNetwork.getEdge(this, dealer);	
		}		
		edge.addTransaction(transaction);
		this.lastTransaction = transaction;
		
		//Now pay tax or commission
		if (deal != null) {
			if (deal.getCustomerID() != this.personID) {
				Customer connection = (Customer) returnMyConnection(deal.getCustomerID(), Settings.socialnetwork);
				payTax(connection);
			}
		}
	}
	
	private void payTax (Customer connection) {
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			double amount = connection.getTax();
			deductMoney(amount);
			connection.addMoney(amount);
		}
		else {
			double drug = connection.getTax();
			deductDrug(drug);
			connection.addDrug(drug);
		}
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

	@ScheduledMethod(start = 1, interval = 1, priority = 2)
	public void useDrugs() {	
		double consumedDrug = Settings.consumptionStepsPerUnit;
		deductDrug(consumedDrug);		
		if (Settings.errorLog) {
			System.err.println("Customer: " + personID + " consumes : " + consumedDrug + " now has: " + drugs);
		}
	}

	/**
	 * In this method we also pay tax to the one who had sent a 'good deal' that we used.  
	 * 
	 * PreCond: The customer has a 'good' deal and the settings parameter allows sharing the good deal with acquaintances.
	 * This method is called when the customer decides to share the deal with fellow acquaintances
	 * This is a synchronized version .... that is first all the customers pass on their 'good' deals
	 * after a day, and then if in the next day, any customer needs to purchase a drug, they have an up to date 
	 * information ...
	 */
	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 1)
	public void shareDeals() {
		int currentTick = (int) ContextCreator.getTickCount();
		if (lastTransaction.getEndorsement().equals(Endorsement.Good)
				&& currentTick - lastTransaction.getTime() <= Settings.stepsInDay) {
			Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
			Iterator itr = socialNetwork.getAdjacent(this).iterator();
			Transaction transaction = new Transaction(lastTransaction);
			transaction.setTaxAmount(this.tax);
			while (itr.hasNext()) {
				Customer acquaintance = (Customer) itr.next();
				if (Math.random() <= Settings.CustomerParams.shareDealProb) {
					SNEdge edge = (SNEdge) socialNetwork.getEdge(this, acquaintance);
					edge.addTransaction(new Transaction(transaction));				
				}
			}		
		}
	}

	/*private Dealer returnMyDealer(int dealerID) {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getAdjacent(this).iterator();
		Dealer dealer = null;
		while (itr.hasNext()) {
			dealer = (Dealer) itr.next();
			if (dealer.getPersonID() == dealerID) {
				break;
			}
		}
		if (Settings.errorLog) {
			if (dealer == null || dealer instanceof Dealer == false) {
				System.err.println("Dealer is null or not found. In getDealer(Id). id: " + dealerID);
			}
		}
		return dealer;
	}	*/
	
	private Person returnMyConnection(int connectionID, String networkName) {
		Network network;
		if (networkName == Settings.socialnetwork) {
			network = (Network)(context.getProjection(Settings.socialnetwork));
		}
		else if (networkName == Settings.transactionnetwork) {
			network = (Network)(context.getProjection(Settings.transactionnetwork));
		}
		else {
			if (Settings.errorLog) {
				System.err.println("Network name is unknown. In returnMyConnection. Name: " + networkName);
			}
			return null;
		}
		Person connection = null;
		Iterator itr = network.getAdjacent(this).iterator();		
		while (itr.hasNext()) {
			Person person = (Person) itr.next();
			if (person.getPersonID() == connectionID) {
				connection = person;
				break;
			}
		}		
		if (connection == null) {
			if (Settings.errorLog) {
				System.err.println("Connection not found. In returnMyConnection. PersonID: " + connectionID);
			}			
		}
		return connection; 
	}

	protected Transaction returnBestDealsFromNetwork() {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getEdges().iterator();
		double bestValue = Double.MAX_VALUE;		
		Transaction bestDeal = null;
		ArrayList<Transaction> deals = new ArrayList<Transaction>();
		while (itr.hasNext()) { 
			TransEdge transEdge = (TransEdge) itr.next();
			if (transEdge.getLastTransactionIndex() != -1) {
				deals.add(transEdge.getLastTransaction());
			}
		}
		Network socialNetwork = (Network) (context.getProjection(Settings.socialnetwork));
		itr = socialNetwork.getEdges().iterator();
		while (itr.hasNext()) {
			SNEdge snEdge = (SNEdge) itr.next();
			if (snEdge.getLastTransactionIndex() != -1) {
				deals.add(snEdge.getLastTransaction());
			}
		}		
		if (deals.isEmpty()) {
			return null;
		}
		Collections.sort(deals, new Comparator<Transaction>() {
			public int compare(Transaction trans1, Transaction trans2) {
				double cost1 = trans1.getDrugCost() + trans1.getTaxAmount();
				double cost2 = trans2.getDrugCost() + trans2.getTaxAmount();				
				if (trans1.getDealerID() != trans2.getDealerID()) {					
					return cost1 > cost2 ? +1 : cost1 == cost2 ? 0 : -1;				
				}
				else {
					return trans1.getTime() < trans2.getTime() ? +1 : (trans1.getTime() == trans2.getTime()) ? 0 : -1;
				}
			}
		});
		
		return deals.get(0);
	}

	protected Transaction returnMyBestDeal() {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getEdges().iterator();
		double bestValue = Double.MAX_VALUE;		
		Transaction bestDeal = null;
		ArrayList<Transaction> deals = new ArrayList<Transaction>();
		while (itr.hasNext()) { 
			TransEdge transEdge = (TransEdge) itr.next();
			if (transEdge.getLastTransactionIndex() != -1) {
				deals.add(transEdge.getLastTransaction());
			}
		}
		if (deals.size() > 0) {			
			Collections.shuffle(deals);
			Collections.sort(deals, new Comparator<Transaction>() {
				public int compare(Transaction t1, Transaction t2) {
					return t1.getDrugCost() < t2.getDrugCost() ? +1 
							: (t1.getDrugCost() == t2.getDrugCost()) ? 0 : -1;
				}
			});
			bestDeal = deals.get(0);
		}	
		return bestDeal;
	}	

	/** Returns a random dealer from my assigned dealers. */
	protected Dealer returnRandomDealer() {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Dealer dealer = (Dealer) transactionNetwork.getRandomAdjacent(this);
		if (Settings.errorLog) {
			if (dealer == null || dealer instanceof Dealer == false) {
				System.err.println("Dealer is null or not found. In getRandomDealer(). ");
			}
		}		
		return dealer;
	}

	/*	private Dealer getBestDealer() {
		Transaction bestDeal = (Transaction) getBestDeal();
		Dealer bestDealer = null;
		if (bestDeal != null) {
			bestDealer = (Dealer) getDealer(bestDeal.getDealerID());
		}
		else {
			bestDealer = (Dealer) ((Network) context.getProjection(Settings.transactionnetwork)).getRandomAdjacent(this);			
		}
		if (Settings.errorLog) {
			if (bestDeal == null) {
				System.err.println("Best Deal not found. In getBestDealer(). ");
			}
			if (bestDealer == null) {
				System.err.println("Best Dealer not found. In getBestDealer(). ");
			}
		}		
		return bestDealer;
	}*/

	public int getInitKnownDealers() {
		return initKnownDealers;
	}

	public void setInitKnownDealers(int numInitialDealers) {
		this.initKnownDealers = numInitialDealers;
	}

	public double getInitialBudget() {
		return initialBudget;
	}

	public void setInitialBudget(double budget) {
		this.initialBudget = budget;
	}

	public double getTax() {
		return tax;
	}
}