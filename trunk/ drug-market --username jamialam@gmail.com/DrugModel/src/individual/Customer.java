package individual;

/** 
 * @author shah
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import cern.jet.random.Uniform;
import drugmodel.ContextCreator;
import drugmodel.SNEdge;
import drugmodel.Settings;
import drugmodel.Settings.DealerSelection;
import drugmodel.Settings.Endorsement;
import drugmodel.Settings.SupplyOption;
import drugmodel.Settings.TaxType;
import drugmodel.TransactionEdge;
import drugmodel.Transaction;

public class Customer extends Person {
	/** Number of dealers known at the time of start or entry into the simulation.*/
	private int initKnownDealers; 
	private double initialBudget;
	@SuppressWarnings("rawtypes")
	private Context context;
	private Transaction lastTransaction;
	private double tax;
	private int numBadAllowed;

	@SuppressWarnings("rawtypes")
	public Customer(Context _context) {
		this.context = _context;
		initialize();
	}
	
	private void initialize() {
		this.initKnownDealers = Uniform.staticNextIntFromTo(Settings.minDealerCustomerlinks, Settings.maxDealerCustomerlinks);
		this.initialBudget = Settings.Budget.returnInitialBudget();
		addMoney(initialBudget);
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			this.tax = Settings.Tax.returnTaxCost();
		}
		else {
			this.tax = Settings.Tax.returnPercentageInUnits();
		}
		this.numBadAllowed = Settings.CustomerParams.returnBadAllowed();
		this.lastTransaction = null;
		this.drugs = Uniform.staticNextDoubleFromTo(0,12);
	}
//Change by SA	
//	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 5)
	@ScheduledMethod(start = 1, interval = 1, priority = 5)
	public void income() {
		double currentTick = ContextCreator.getTickCount();
		if (Settings.Resupply.getIncomeOption().equals(SupplyOption.Automatic)) {
//			if (this.money <= 0.0) {
			if (this.money < Settings.pricePerGram + Settings.Tax.returnMaxTax()) {
				this.addMoney(Settings.Resupply.income(this.money));
//				this.addMoney(Settings.Resupply.income_and_tax(this.money));
			}	
		}
		else {
			if (currentTick % Settings.CustomerParams.incomeInterval == 0) {
				this.addMoney(Settings.Resupply.income(this.money));		
//				this.addMoney(Settings.Resupply.income_and_tax(this.money));
			}
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 4)
	public void buyDrugsandEndorseDeals() {
	//added by SA
		
		if(this.money < Settings.pricePerGram){
			if (Settings.errorLog ) 
				System.err.println("Available money: " + this.getMoney() + " is less than 120. Couldnt buy.");
			return;
		}
		Transaction deal = null;
		Dealer dealer = null;
		if (Settings.DealersParams.dealerSelection.equals(DealerSelection.NetworkBest)) {
//			deal = returnBestDealsFromNetworkfromPrevShared(); // this is the other implementation relying on shareDeals();
			deal = returnBestDealsFromNetwork();			
			}
		else if (Settings.DealersParams.dealerSelection.equals(DealerSelection.MyBest)) {
			deal = returnMyBestDeal();			
		}		
		if (deal != null 
				&& Settings.DealersParams.dealerSelection != DealerSelection.Random) {
			dealer = deal.getDealer();
		}
		else {
			dealer = returnMyRandomDealer();
		}

		if (Settings.errorLog) {
			if (dealer == null || dealer instanceof Dealer == false) {
				System.err.println("Dealer is null or not found. In buyDrugs(). Me: " + personID);
				return;
			}
		}		 

		int currentTick = (int) ContextCreator.getTickCount();
		double unitsPurchased = dealer.returnUnitsToSell();
		//Cost is kept fixed.
		double costPerUnit = dealer.returnCostPerUnit();		
		Endorsement endorsement = Endorsement.None;

		if (deal != null
				&& deal.getCustomerID() != this.personID) {
			endorsement = unitsPurchased >= deal.getDrugQtyInUnits() ? Endorsement.Good : Endorsement.Bad;
//			updateEndorsement(deal.getCustomerID(), endorsement);
			updateEndorsement(deal , deal.getCustomerID() , endorsement );
		}
		//It is my own dealer and there wasn't a deal information used. 
		else if (lastTransaction != null) {
			endorsement = unitsPurchased >= lastTransaction.getDrugQtyInUnits() ? Endorsement.Good : Endorsement.Bad;
		}

		Transaction transaction = new Transaction(dealer, personID, currentTick, costPerUnit, unitsPurchased, endorsement);

		buyDrug(transaction);
		dealer.sellDrug(transaction);

		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		TransactionEdge edge;
		if (transactionNetwork.getEdge(this, dealer) == null) {
			edge = new TransactionEdge(this, dealer);
			transactionNetwork.addEdge(edge);
		}
		else {
			edge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);	
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void updateEndorsement(Transaction deal , int connectionID, Endorsement endorsement) {
		boolean updated = false;
		Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
		Iterator itr = socialNetwork.getAdjacent(this).iterator();
		while (itr.hasNext()) {
			Customer customer = (Customer) itr.next();
			if (customer.getPersonID() == connectionID) {
				SNEdge outEdge = (SNEdge) socialNetwork.getEdge(this, customer);
				double currentTick = ContextCreator.getTickCount();
				//changed by SA
				outEdge.addEndorsement(deal, endorsement, currentTick);
				updated = true;
				break;
			}
		}		
		if (updated == false) {
			if (Settings.errorLog) {
				System.err.println("Customer is null. In update Endorsement. I am: " + personID + " updating endorsement of: " + connectionID);
			}
		}
	}

	private void buyDrug(Transaction transaction) {
		addDrug(transaction.getDrugQtyInUnits());
		deductMoney(Settings.pricePerGram);
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
	
	/**
	 * In this method we also pay tax to the one who had sent a 'good deal' that we used.  
	 * 
	 * PreCond: The customer has a 'good' deal and the settings parameter allows sharing the good deal with acquaintances.
	 * This method is called when the customer decides to share the deal with fellow acquaintances
	 * This is a synchronized version .... that is first all the customers pass on their 'good' deals
	 * after a day, and then if in the next day, any customer needs to purchase a drug, they have an up to date 
	 * information ...
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	//@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 3)
	public void shareDeals() {
		int currentTick = (int) ContextCreator.getTickCount();
		if (lastTransaction != null
				&& lastTransaction.getEndorsement().equals(Endorsement.Good)
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@ScheduledMethod(start = 1, interval = Settings.stepsInDay, priority = 2)
	public void evaluateConnections() {
		Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
		Iterator itr = socialNetwork.getOutEdges(this).iterator();
		ArrayList<SNEdge> removedLinks = new ArrayList<SNEdge>();
		while (itr.hasNext()) {
			SNEdge edge = (SNEdge) itr.next();
			//Added by SA
			if(shouldDropLinkTF(edge, Settings.CustomerParams.defaultTimeFrameToDropLink)){
//			if (shouldDropLink(edge)) {
				//Remove network link with other customer
				Customer customer = (Customer) edge.getTarget();
				removedLinks.add((SNEdge)socialNetwork.getEdge(this, customer));
				removedLinks.add((SNEdge)socialNetwork.getEdge(customer, this));
				System.out.println("this:" + this.getPersonID() + " drops " + customer.getPersonID() );
			}
		}
		for (SNEdge edge : removedLinks) {
			socialNetwork.removeEdge(edge);
		}
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void useDrugs() {	
		double consumedDrug = Settings.consumptionStepsPerUnit;
		//Add by SA
		if(this.drugs < Settings.consumptionStepsPerUnit){
//			System.out.println("Customer: " + personID + " has no drug to consume : " + consumedDrug + " Buy now." );
			this.buyDrugsandEndorseDeals();
		}
		
		deductDrug(consumedDrug);		
		if (Settings.errorLog) {
//			System.out.println("Customer: " + personID + " has no drug to consume : " + consumedDrug + " Buy now." );
//			System.out.println("Customer: " + personID + " consumes : " + consumedDrug + " now has: " + drugs);
		}
	}


	@SuppressWarnings("rawtypes")
/*	private boolean shouldDropLink(SNEdge edge) {
		int numBadEndorsements = edge.returnNumBadEndorsements();
		if (numBadEndorsements >= this.numBadAllowed) {
			return true;
		}
		else {
			return false;
		}
	}
*/
	private boolean shouldDropLinkTF(SNEdge edge, double time_frame) {
		if(edge.returnNumBadEndorsement(Settings.CustomerParams.defaultTimeFrameToDropLink) > this.numBadAllowed ){
			return true;
		}
		else {
			return false;
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
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

	/**
	 * This method assumes that the deals are shared to ego by alters when ego is going to the market for a purchase.
	 * It is independent of shareDeals() method. Here deals are shared inside this function. 
	 */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Transaction returnBestDealsFromNetwork() {
		Transaction bestDeal = null;
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getAdjacent(this).iterator();		
		ArrayList<Transaction> deals = new ArrayList<Transaction>();
		while (itr.hasNext()) { 
			Dealer dealer = (Dealer) itr.next();
			TransactionEdge transEdge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);
			if (transEdge.getLastTransactionIndex() != -1) {
				deals.add(transEdge.getLastTransaction());
			}
		}
		
		Network socialNetwork = (Network) (context.getProjection(Settings.socialnetwork));
		itr = socialNetwork.getAdjacent(this).iterator();
		while (itr.hasNext()) {
			Customer customer = (Customer) itr.next();
			Iterator itrDeal = transactionNetwork.getAdjacent(customer).iterator();
			while (itrDeal.hasNext()) {
				if (Math.random() <= Settings.CustomerParams.shareDealProb) {
					Dealer dealer = (Dealer) itrDeal.next();
					TransactionEdge transEdge = (TransactionEdge) transactionNetwork.getEdge(customer, dealer);
					if (transEdge.getLastTransactionIndex() != -1) {
						Transaction transaction = transEdge.getLastTransaction();
						transaction.setTaxAmount(this.tax);
						deals.add(transaction);
						//Added by SA
						SNEdge edge = (SNEdge) socialNetwork.getEdge(customer, this);
						if(edge == null){
							System.err.println("Edge null.");
							System.err.println(socialNetwork.isAdjacent(customer, this));
							if(!ContextCreator.verifyNetwork())
								System.out.println("Network verification failed");
						}
						try{
							edge.addTransaction(transaction);
						}catch (Exception e) {
							// TODO: handle exception
							e.printStackTrace();
							System.out.println("exception caught");
						}
					}					
				}
			}				
		}
		if (deals.isEmpty()) {
			return null;
		}		
/*		for (Transaction deal : deals){
			System.out.println(deal.getDrugQtyInUnits());
		}*/
		Collections.sort(deals, new Comparator<Transaction>() {
				public int compare(Transaction trans1, Transaction trans2) {
				double cost1 = 0;
				double cost2 = 0;
				//Added by SA
				if(trans1 == null || trans2 == null){
					System.err.println("trans obj null.. ");
				}
				
				if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {					
					cost1 = (Settings.pricePerGram + trans1.getTaxAmount())/trans1.getDrugQtyInUnits();
					cost2 = (Settings.pricePerGram + trans2.getTaxAmount())/trans2.getDrugQtyInUnits();
				}
				else if (Settings.Tax.taxType.equals((TaxType.AmountDrug))) {
					cost1 = -1*(trans1.getDrugQtyInUnits() - (trans1.getDrugQtyInUnits()*Settings.Tax.returnPercentageInUnits())); 
					cost2 = -1*(trans2.getDrugQtyInUnits() - (trans2.getDrugQtyInUnits()*Settings.Tax.returnPercentageInUnits()));					
				}
				if (trans1.getDealer().getPersonID() != trans2.getDealer().getPersonID()) {					
					return cost1 > cost2 ? +1 : cost1 == cost2 ? 0 : -1;				
				}
				else {
					return trans1.getTime() < trans2.getTime() ? +1 : (trans1.getTime() == trans2.getTime()) ? 0 : -1;
				}
			}
		});		

		bestDeal = deals.get(0);
		return bestDeal;
	}
	
	/** 
	 * This method assumes that the deal is shared by customers to their links, given the share prob parameter,
	 * at the time of their own purchases. One of the consequences is that the dealer may not exist anymore in 
	 * the market and so we check the last deals shared before they are being sorted for the 'best'.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Transaction returnBestDealsFromNetworkfromPrevShared() {
		Transaction bestDeal = null;
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getAdjacent(this).iterator();		
		ArrayList<Transaction> deals = new ArrayList<Transaction>();
		while (itr.hasNext()) { 
			Dealer dealer = (Dealer) itr.next();
			TransactionEdge transEdge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);
			if (transEdge.getLastTransactionIndex() != -1) {
				deals.add(transEdge.getLastTransaction());
			}
		}
		Network socialNetwork = (Network) (context.getProjection(Settings.socialnetwork));
		itr = socialNetwork.getInEdges(this).iterator();		
		while (itr.hasNext()) {
			SNEdge snEdge = (SNEdge) itr.next();
			if (snEdge.returnLastTransactionIndex() != -1) {
				Transaction lastTransaction = snEdge.returnLastTransaction(); 
				if (context.contains(lastTransaction.getDealer())) {
					deals.add(lastTransaction);
				}
			}
		}				
		if (deals.isEmpty()) {
			return null;
		}		
		Collections.sort(deals, new Comparator<Transaction>() {
			public int compare(Transaction trans1, Transaction trans2) {
				double cost1 = 0;
				double cost2 = 0;
				if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {					
					cost1 = (Settings.pricePerGram + trans1.getTaxAmount())/trans1.getDrugQtyInUnits();
					cost2 = (Settings.pricePerGram + trans2.getTaxAmount())/trans2.getDrugQtyInUnits();
				}
				else if (Settings.Tax.taxType.equals((TaxType.AmountDrug))) {
					cost1 = -1*(trans1.getDrugQtyInUnits() - (trans1.getDrugQtyInUnits()*Settings.Tax.returnPercentageInUnits())); 
					cost2 = -1*(trans2.getDrugQtyInUnits() - (trans2.getDrugQtyInUnits()*Settings.Tax.returnPercentageInUnits()));					
				}
				if (trans1.getDealer().getPersonID() != trans2.getDealer().getPersonID()) {					
					return cost1 > cost2 ? +1 : cost1 == cost2 ? 0 : -1;				
				}
				else {
					return trans1.getTime() < trans2.getTime() ? +1 : (trans1.getTime() == trans2.getTime()) ? 0 : -1;
				}
			}
		});		

		bestDeal = deals.get(0);
		return bestDeal;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Transaction returnMyBestDeal() {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getAdjacent(this).iterator();		
		Transaction bestDeal = null;
		ArrayList<Transaction> deals = new ArrayList<Transaction>();
		while (itr.hasNext()) { 
			Dealer dealer = (Dealer) itr.next();
			TransactionEdge transEdge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);
			if (transEdge.getLastTransactionIndex() != -1) {
				deals.add(transEdge.getLastTransaction());
			}
		}
		if (deals.size() > 0) {			
			Collections.shuffle(deals);
			Collections.sort(deals, new Comparator<Transaction>() {
				public int compare(Transaction t1, Transaction t2) {
					return t1.getDrugQtyInUnits() > t2.getDrugQtyInUnits() ? +1 
							: (t1.getCostPerUnit() == t2.getCostPerUnit()) ? 0 : -1;
				}
			});
			bestDeal = deals.get(0);
		}	
		return bestDeal;
	}	

	/** Returns a random dealer from my assigned dealers. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Dealer returnMyRandomDealer() {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		//System.out.println("Customer Dealer degree: " + transactionNetwork.getDegree(this));
		Dealer dealer = (Dealer) transactionNetwork.getRandomAdjacent(this);
		if (Settings.errorLog) {
			if (dealer == null || dealer instanceof Dealer == false) {
				System.err.println("Dealer is null or not found. In getRandomDealer(). ");
			}
		}		
		return dealer;
	}

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