package individual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.projection.Linked;
import repast.simphony.space.graph.Network;
import cern.jet.random.Uniform;
import drugmodel.ContextCreator;
import drugmodel.SNEdge;
import drugmodel.Settings;
import drugmodel.Settings.Endorsement;
import drugmodel.Settings.ShareDealMode;
import drugmodel.Settings.SupplyOption;
import drugmodel.Settings.TaxType;
import drugmodel.TransactionEdge;
import drugmodel.Transaction;

@SuppressWarnings({ "rawtypes", "unchecked" })
/** Customer class. */
public class Customer extends Person {
	/** Number of dealers known at the time of start or entry into the simulation.*/
	private int initKnownDealers;
	/** Maximum number of social network links that this customer can have. */
	private int maxLinks;
	/** Initial budget that is assigned to this customer.*/
	private double initialBudget;
	/** Reference to the main context.*/
	private Context context;
	/** Last transaction made by this customer. ... ??? */
	private Transaction lastTransaction;
	/** Tax that is charged by this customer when it shares a deal and is used by the other customer.*/
	private double tax;
	/** Number bad endorsements allowed before this customer decides to drop a social network link with other customer.*/	
	private int numBadAllowed;
	/** Probability to share deals with other customers (given sharing mode)*/
	private double shareDealProb;
	/** This stores the accumulated amount that was not consumed because the customer could not find a dealer. */
	private double drugToBeConsumedPreviously;
	/** Interval to evaluate social network for the number of bad deals shared by others. */	
	private int evaluationInterval;
	/** Stores the data of dropped links. Key: ID of the customer with whom link was dropped. Value: Time when dropped. */
	protected HashMap<Integer, Double> droppedLinks;

	// for charts and display
	private double totalTaxPaid;
	private double totalMoneySpend;
	/** Number of current social network links that this customer has. Used for display*/
	private int curLinks ;

	public Customer(Context _context) {
		this.context = _context;
		this.initKnownDealers = Settings.CustomerParams.returnInitKnownDealers();
		this.initialBudget = Settings.CustomerParams.returnInitialBudget();
		addMoney(initialBudget);
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			this.tax = Settings.Tax.setInitialFlatFee();
		}
		else {
			this.tax = Settings.Tax.setInitialDrugPercent();
		}
		this.numBadAllowed = Settings.CustomerParams.returnBadAllowed();
		this.lastTransaction = null;
		/* In Person class: Initial drugs. */
		this.drugs = Settings.CustomerParams.returnInitialDrugs();
		this.shareDealProb = Settings.CustomerParams.returnShareDealProb();
		this.drugToBeConsumedPreviously = 0.0;
		this.evaluationInterval = Uniform.staticNextIntFromTo(1,Settings.CustomerParams.CustomerConnectionEvaluationInterval);
		this.maxLinks = 0;
		this.curLinks = 0; 
		droppedLinks = new HashMap<Integer, Double>();
		this.totalTaxPaid = 0.0;
		this.totalMoneySpend = 0.0;
	}

	/** This method is called once. Priority is set to 10 so that it's called earliest at the start, after the social network
	 * 	is created. */
	@ScheduledMethod(start = 1, interval = 0, priority = 10)
	public void initMaxLink() {
		Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
		maxLinks = socialNetwork.getInDegree(this);
		curLinks = maxLinks;		
	}

	/** Agents have unlimited resources to buy drugs and the model only keeps track of how much they spend and use (in units).
	 * 	Currently, the income interval is Homogeneous. */
	@ScheduledMethod(start = 1, interval = 1, priority = 4)
	public void income() {
		double currentTick = ContextCreator.getTickCount();
		if ((Settings.Resupply.getIncomeOptionForCustomer().equals(SupplyOption.Automatic)
				&& this.money < Settings.PricePerGram + Settings.Tax.returnMaxTax())
				|| currentTick % Settings.CustomerParams.incomeInterval == 0) {
			this.addMoney(Settings.Resupply.incomeForCustomer(this.money));
		}
	}	

	/** Consume drugs at every time step (=10 minutes). If the quantity of drugs is less than Settings.consumptionUnitsPerStep
	 * 	then call buyDrugsandEndorseDeals()*/
	@ScheduledMethod(start = 1, interval = 1, priority = 3)
	public void consumeDrugs() {	
		if (this.drugs >= Settings.consumptionUnitsPerStep) {
			deductDrug(Settings.consumptionUnitsPerStep);
		}
		// The amount of current drugs is less than consumptionUnitsPerSteps, so try to buy drugs. 
		else {
			/* Now try to buy the drugs. */
			boolean buyDrug = buyDrugsandEndorseDeals();
			/* Could not buy drugs */
			if (buyDrug == false) {
				this.drugToBeConsumedPreviously += Settings.consumptionUnitsPerStep;
				System.err.println("Customer "+ this.personID+ " time: " + ContextCreator.getTickCount() +" couldnt buy drug. drug to be consumed : " + this.drugToBeConsumedPreviously);
			}			
		}
	}

	/** The period within 1-7*48 ticks per week (in time steps), in which this customer tries to make links.
	 * 	For a customer, this means that it tries to make new links once per  days.
	 * 	Only one link is made per function call.  
	 */
	@ScheduledMethod(start = 1, interval = 1, priority = 2)
	public void makeLinks() {
		double currentTick = ContextCreator.getTickCount();
		/** Period determining the time step when this function's body should execute. */
		double period = this.evaluationInterval + Settings.StepsInDay;
		/** Number of customers with whom to try to make links. It exits if no success or as soon as a link is made.*/
		int numTries = 3;

		if (currentTick == period || currentTick % Settings.CustomerParams.CustomerConnectionEvaluationInterval == period) {
			Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
			if(socialNetwork.getInDegree(this) < this.maxLinks) { 
				Iterator itr_customers = context.getRandomObjects(Customer.class, numTries).iterator();
				while (itr_customers.hasNext()){
					Customer customer = (Customer) itr_customers.next();
					if (socialNetwork.getInDegree(customer) < customer.getMaxLinks()   
							&& socialNetwork.isAdjacent(this, customer) == false
							&& this.personID != customer.personID
							&& droppedLinks.containsKey(customer.personID) == false
							&& Math.random() <= numCommonDealers(customer) * Settings.CustomerParams.MakeLinkProb){
						//Now make edge
						SNEdge edgeto = new SNEdge(this, customer, true);
						SNEdge edgefrom = new SNEdge(customer, this, true);
						socialNetwork.addEdge(edgeto);
						socialNetwork.addEdge(edgefrom);
						if(socialNetwork.isPredecessor(this, customer) == false
								|| socialNetwork.isSuccessor(this, customer) == false) {
							System.err.println("edge not created properly.");
						}							
						else {
							this.curLinks++;
							customer.setCurLinks(customer.getCurLinks() + 1);

						}
						//						System.out.println("link made b/w "+this.personID + "  and  " + customer.personID +  "  at time: " + ContextCreator.getTickCount() /48);
						break;
					}
				}
			}
		}
	}

	/** */
	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void updateDroppedLinks() {
		if(droppedLinks.isEmpty()) {
			return;
		}		
		Iterator itr = droppedLinks.keySet().iterator();
		ArrayList<Integer> tobeRemoved = new ArrayList<Integer>();
		while(itr.hasNext()){
			Integer id = (Integer) itr.next();
			double time = droppedLinks.get(id);
			if(ContextCreator.getTickCount() - time > Settings.CustomerParams.TimeFrameToForgetDropLink){
				tobeRemoved.add(id);
				//				System.out.println("this:" + this.personID + " remove " + id +" from drop list at time: " + ContextCreator.getTickCount()/48);
			}
		}
		for(Integer id : tobeRemoved){
			droppedLinks.remove(id);
		}
	}

	/** Returns the number of common dealers with this @param customer */
	public int numCommonDealers(Customer customer){
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator myDealersItr = transactionNetwork.getAdjacent(this).iterator();
		int numCommonDealer=0;
		while (myDealersItr.hasNext()) {
			Dealer dealer = (Dealer) myDealersItr.next();
			if (transactionNetwork.evaluate(new Linked(dealer, customer)) == true){
				numCommonDealer++;
				if(transactionNetwork.isAdjacent(this, dealer) == false
						|| transactionNetwork.isAdjacent(customer, dealer) == false) {
					System.err.println("evalute not working properly. Dealer is not adjacent.");
				}
			}
		}
		return numCommonDealer;
	}

	/** Evaluate social network links and drop those with whom the number of bad endorsements exceeds the limit. */
	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void evaluateLinks() {
		double currentTick = ContextCreator.getTickCount(); 
		if(currentTick == this.evaluationInterval 
				||	currentTick % Settings.CustomerParams.CustomerConnectionEvaluationInterval == this.evaluationInterval) {
			//		System.out.println("customer:" + this.personID + "currrent tick:" + ContextCreator.getTickCount()+ " evaluationInterval:" + this.evaluationInterval);			
			Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
			Iterator itr = socialNetwork.getOutEdges(this).iterator();
			ArrayList<SNEdge> removedLinks = new ArrayList<SNEdge>();
			while (itr.hasNext()) {
				SNEdge edge = (SNEdge) itr.next();				
				if (edge.returnNumBadEndorsement(Settings.CustomerParams.DefaultTimeFrameToDropLink) > this.numBadAllowed) {
					Customer customer = (Customer) edge.getTarget();					
					removedLinks.add((SNEdge)socialNetwork.getEdge(this, customer));
					removedLinks.add((SNEdge)socialNetwork.getEdge(customer, this));										
					removeConnection(customer.getPersonID(), currentTick);
					customer.removeConnection(this.personID, currentTick); 					
					//					System.out.println("this:" + this.getPersonID() + " drops " + customer.getPersonID() + "  at time: " + ContextCreator.getTickCount() / 48);
				}
			}
			for (SNEdge edge : removedLinks) {
				socialNetwork.removeEdge(edge);
			}
		}
	}


	/** Remove connection and decrement current links. */
	public void removeConnection(Integer customerID, Double time) {
		droppedLinks.put(personID, time);
		curLinks--;		
	}

	/**
	 *  The purchase is based on two factors: 
	 *  1) the expected price of the drug and,
	 *   2)  what is offered. 
	 *   If they have competing deals from dealers they always determine the “best” deal, 
	 *   i.e., they evaluate the units sold at the lowest price, i.e., the lowest number from above.
	 *   Of course, they can only make this determination after they have bought the drug.
	 */
	public boolean buyDrugsandEndorseDeals() {
		/* it shouldn't happen as income() priority is higher than priority of this function */
		if(this.money < Settings.PricePerGram + Settings.Tax.returnMaxTax() ){ 
			System.err.println("Available money: " + this.getMoney() + " is less than 120. Couldnt buy.");
			return false;
		}

		Transaction deal = null;
		Dealer dealer = null;
		ArrayList<Transaction> deals = null;
		/* Probability to choose one's own dealer */
		double prob = Math.random();
		/* Choose my own dealer. */ 
		if (prob <= Settings.CustomerParams.DealerSelectionProb) {
			deals = returnMyDealsBestSorted();			
			if(deals != null){
				for(Transaction transaction : deals){
					deal = transaction;
					dealer = transaction.getDealer();
					if(context.contains(dealer) == true) {
						break;
					}
				}
			}
		}		
		/* choose deals from network when prob > my own deal selection prob OR couldn't find one's own dealer */ 
		if (prob > Settings.CustomerParams.DealerSelectionProb  
				|| deal == null 
				|| dealer == null
		) {
			if (Settings.CustomerParams.shareDealMode == ShareDealMode.Proactive) {
				deals = returnSortedDealsFromNetworkfromPrevShared(); // this is the other implementation relying on shareDeals();
			}
			//Else, shareDealMode is OnDemand
			else {
				deals = returnSortedDealsFromNetwork();			
			}
			if (deals != null) {
				for(Transaction transaction : deals){
					deal = transaction;
					dealer = transaction.getDealer();
					if(context.contains(dealer) == true) {
						break;
					}
				}
			}
		}

		// NO dealer found. exiting with error
		if (Settings.errorLog) {
			if (dealer == null || dealer instanceof Dealer == false) {
				System.err.println("Dealer is null or not found. In buyDrugs(). Me: " + personID);
				return false;
			}
		}		 

		double currentTick = ContextCreator.getTickCount();
		/* Get the units to be purchased from the Dealer. */
		double unitsPurchased = dealer.returnUnitsToSell();
		/* Get the actual current cost per unit to be purchased from the Dealer. */
		double costPerUnit = dealer.returnCostPerUnit();
		/* Endorse the deal here. */
		Endorsement endorsement = unitsPurchased >= deal.getDrugQtyInUnits() ? Endorsement.Good : Endorsement.Bad;

		/* If I am using someone else's shared deal. This endorse occurs even in the case when the dealer did not
		 * sell the drug and I went to the dealer. Does not happen in the Automatic Drug Supply mode. */
		if (deal.getCustomerID() != this.personID) {			
			updateEndorsement(deal, deal.getCustomerID(), endorsement);
		}

		Transaction transaction = new Transaction(dealer, personID, currentTick, costPerUnit, unitsPurchased, endorsement, deal.getTransactionID(), deal.getCustomerID());
		//		System.out.println("Deal used: " + deal.getID() + "  whose deal it was: " + deal.getCustomerID() + " who was the dealer: " + deal.getDealer().getParentID()  + " new transacation done : " + transaction.getID());

		/* Actual selling by the dealer happens here. */
		if (dealer.sellDrug(transaction) == true) {
			/* Actual buying from the dealer happens here.*/
			buyDrug(transaction);

			Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
			TransactionEdge edge;

			/* If not connected previously to this dealer. */
			if (transactionNetwork.getEdge(this, dealer) == null) {
				edge = new TransactionEdge(this, dealer);
				transactionNetwork.addEdge(edge);
			}
			else {
				edge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);	
			}		
			/* Add this new transaction on the edge. */
			edge.addTransaction(transaction);			
			this.lastTransaction = transaction;		

			/* Now pay tax or commission */
			if (deal.getCustomerID() != this.personID) {
				Customer connection = (Customer) returnMyConnection(deal.getCustomerID(), Settings.socialnetwork);
				if (connection != null) {
					payTax(connection, unitsPurchased);
				}
				else {
					System.err.println("Person to pay tax, not found.");
				}
			}

			/* Share a good deal if the share deal mode is Pro-active. */
			if(Settings.CustomerParams.shareDealMode == Settings.ShareDealMode.Proactive) {
				shareDeal();
			}			
			return true;
		}
		return false;
	}

	/** Update @param endorsement for a shared @param deal that was shared by the customer @param connectionID */
	private void updateEndorsement(Transaction deal, int connectionID, Endorsement endorsement) {
		boolean updated = false;
		Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
		Iterator itr = socialNetwork.getAdjacent(this).iterator();
		while (itr.hasNext()) {
			Customer customer = (Customer) itr.next();
			if (customer.getPersonID() == connectionID) {
				SNEdge outEdge = (SNEdge) socialNetwork.getEdge(this, customer);	
				double currentTick = ContextCreator.getTickCount();
				outEdge.addEndorsement(deal, endorsement, currentTick);
				updated = true;
				//Debugging purpose only. 
				if (((Person) outEdge.getSource()).getPersonID() != this.personID
						|| ((Person) outEdge.getTarget()).getPersonID() != connectionID) {
					System.err.println("Edge Not Found. Time: " + deal.getTime() + "Source: " + personID + " Target: " + connectionID);
				}
				break;
			}
		}
		if (updated == false) {
			if (Settings.errorLog) {
				System.err.println("Customer is null. In update Endorsement. I am: " + personID + " updating endorsement of: " + connectionID);
			}
		}
	}

	/** Add drugs and deduct the cost for this transaction. */
	private void buyDrug(Transaction transaction) {
		addDrug(transaction.getDrugQtyInUnits());
		deductMoney(Settings.PricePerGram);
		this.totalMoneySpend += Settings.PricePerGram;
	}

	/** Pay tax to this @param connection based on the @param drugQtyInUnits purchased. */
	private void payTax (Customer connection, double drugQtyInUnits) {
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			double taxAmount = connection.getTax();
			deductMoney(taxAmount);
			connection.addMoney(taxAmount);
			totalTaxPaid += taxAmount;
		}
		else {
			double drug_percent = drugQtyInUnits * connection.getTax()/100;
			if (this.drugs < drug_percent) {
				System.err.println("Me: " + personID + " Couldnt pay tax to " + connection.getPersonID() + " because no less drugs in quantity.");
			}
			deductDrug(drug_percent);
			connection.addDrug(drug_percent);
			totalTaxPaid += drug_percent;
		}
	}

	/** Called for Pro-active mode only. Shared deal based on either sharing per deal OR sharing per connection. */
	public void shareDeal() {
		if (Settings.CustomerParams.shareDealWay.equals(Settings.ShareDealWay.X_Percent_of_Deals)) {
			share_X_percentDealsWithNetwork();
		}	
		else {		/* Share deal with X percent Network */
			shareDealWith_X_pecentNetwork();
		}
	}

	/**
	 *  Agents only transmit information about their own deals, and agents only transmit if a deal is “good.”
	 *  In other words, if a deal they receive from a dealer is better than the one they previously received it is “good.”
	 *  deal is being shared with x% of the network connections.
	 *  Used for the Pro-active Mode only.
	 */
	public void shareDealWith_X_pecentNetwork() {
		double currentTick = ContextCreator.getTickCount();
		if (this.lastTransaction != null
				&& this.lastTransaction.getEndorsement().equals(Endorsement.Good)
				/* Happens in the last 48 time steps, i.e. same day in the simulation. */
				&& currentTick - this.lastTransaction.getTime() <= Settings.StepsInDay) {
			Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
			Iterator itr = socialNetwork.getAdjacent(this).iterator();
			while (itr.hasNext()) {
				Customer acquaintance = (Customer) itr.next();
				if (Math.random() <= this.shareDealProb){
					/* It should be an outer edge. */ 
					SNEdge edge = (SNEdge) socialNetwork.getEdge(this, acquaintance);				
					edge.addTransaction(this.lastTransaction);
				}
			}		
		}
	}

	/**  Agents only transmit information about their own deals, and agents only transmit if a deal is “good.”
	 *  In other words, if a deal they receive from a dealer is better than the one they previously received it is “good.”
	 *  x% of the good deals are being shared with  network  connections.
	 *  Used for the Pro-active Mode only.
	 */
	public void share_X_percentDealsWithNetwork() {
		double currentTick = ContextCreator.getTickCount();
		if (this.lastTransaction != null
				&& this.lastTransaction.getEndorsement().equals(Endorsement.Good)
				/* Happens in the last 48 time steps, i.e. same day in the simulation. */
				&& currentTick - this.lastTransaction.getTime() <= Settings.StepsInDay) {
			Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
			Iterator itr = socialNetwork.getAdjacent(this).iterator();
			if (Math.random() <= this.shareDealProb){
				while (itr.hasNext()) {
					Customer acquaintance = (Customer) itr.next();
					/* It should be an outer edge. */ 
					SNEdge edge = (SNEdge) socialNetwork.getEdge(this, acquaintance);				
					edge.addTransaction(this.lastTransaction);
				}
			}		
		}
	}

	/**
	 * This method assumes that the deals are shared to ego by alters when ego is going to the market for a purchase.
	 * It is independent of shareDeals() method. Here deals are shared inside this function.
	 * Used for the On-demand mode. 
	 */
	protected ArrayList<Transaction> returnSortedDealsFromNetwork() {
		ArrayList<Transaction> deals = returnMyDeals();
		if (deals == null) {
			deals = new ArrayList<Transaction>();
		}
		Network socialNetwork = (Network) (context.getProjection(Settings.socialnetwork));
		Iterator itr = socialNetwork.getAdjacent(this).iterator();
		while (itr.hasNext()) {
			Customer customer = (Customer) itr.next();
			if (Math.random() <= customer.getShareDealProb()
					&& customer.getLastTransaction() != null
					&& customer.getLastTransaction().getEndorsement().equals(Endorsement.Good)){ 
				SNEdge edge = (SNEdge) socialNetwork.getEdge(customer, this);
				if(edge == null){
					System.err.println("Edge null.: " + socialNetwork.isAdjacent(customer, this));
					if(!ContextCreator.verifyNetwork()) {
						System.err.println("Network verification failed");
					}
				}
				else {
					deals.add(customer.getLastTransaction());
					edge.addTransaction(customer.getLastTransaction());
				}
			}			
		}		
		if (deals.isEmpty()) {
			return null;
		}
		Collections.sort(deals, new TransactionComparator());
		return deals;
	}

	/** 
	 * This method assumes that the deal is shared by customers to their links, given the share prob parameter,
	 * at the time of their own purchases. One of the consequences is that the dealer may not exist anymore in 
	 * the market and so we check the last deals shared before they are being sorted for the 'best'.
	 * Used for the Pro-active mode.
	 */
	protected ArrayList<Transaction> returnSortedDealsFromNetworkfromPrevShared() {
		ArrayList<Transaction> deals = returnMyDeals();
		if (deals == null) {
			deals = new ArrayList<Transaction>();
		}
		Network socialNetwork = (Network) (context.getProjection(Settings.socialnetwork));
		/* Getting In-edges from the social network to get deals that have been shared already by my network. */
		Iterator itr = socialNetwork.getInEdges(this).iterator();		
		while (itr.hasNext()) {
			SNEdge snEdge = (SNEdge) itr.next();
			if (snEdge.returnLastTransactionIndex() != -1) {
				/* Latest transaction that was shared by the acquaintance to Me */
				Transaction acqLastTransaction = snEdge.returnLastTransaction();
				/* We make sure that whether a dealer exists of not.  Lee Q.*/ 
				if (context.contains(acqLastTransaction.getDealer())) {
					deals.add(acqLastTransaction);
				}
			}
		}				
		if (deals.isEmpty()) {
			return null;
		}	
		Collections.sort(deals, new TransactionComparator());
		return deals;
	}

	/** Returns a list sorted by the best dealers from my own transaction network. */
	protected ArrayList<Transaction> returnMyDealsBestSorted() {
		ArrayList<Transaction> deals = returnMyDeals();
		if (deals != null) {			
			Collections.shuffle(deals);
			/* Return the sorted ArrayList deals. */
			Collections.sort(deals, new Comparator<Transaction>() {
				public int compare(Transaction t1, Transaction t2) {
					return t1.getDrugQtyInUnits().doubleValue() < t2.getDrugQtyInUnits().doubleValue() ? +1 
							: (t1.getDrugQtyInUnits().doubleValue() == t2.getDrugQtyInUnits().doubleValue()) 
							? (t1.getTime().doubleValue() < t2.getTime().doubleValue() ? +1 : (t1.getTime().doubleValue() == t2.getTime().doubleValue()) ? 0 : -1)
									: -1;
				}
			});	
		}
		return deals;
	}
	
	/** This comparator uses cost but not quantity so we want to find minimum cost per unit with most recent time stamp. */
	class TransactionComparator implements Comparator<Transaction> {
		public int compare(Transaction t1, Transaction t2) {
			double tax1 = getPersonID() != t1.getCustomerID() ? ((Customer) returnMyConnection(t1.getCustomerID(), Settings.socialnetwork)).getTax() : 0d;
			double tax2 = getPersonID() != t2.getCustomerID() ? ((Customer) returnMyConnection(t2.getCustomerID(), Settings.socialnetwork)).getTax() : 0d;				
			double cost1 = 0;				
			double cost2 = 0;
			if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {					
				cost1 = (Settings.PricePerGram + tax1)/t1.getDrugQtyInUnits();
				cost2 = (Settings.PricePerGram + tax2)/t2.getDrugQtyInUnits();					
			}
			else if (Settings.Tax.taxType.equals((TaxType.AmountDrug))) {
				cost1 = Settings.PricePerGram / ( t1.getDrugQtyInUnits() - (t1.getDrugQtyInUnits() * (tax1/100) ) ); 
				cost2 = Settings.PricePerGram / ( t2.getDrugQtyInUnits() - (t2.getDrugQtyInUnits() * (tax2/100) ) );										
			}				
			return cost1 > cost2 ? +1 : cost1 == cost2 
					? (t1.getTime().doubleValue() < t2.getTime().doubleValue() ? +1 : (t1.getTime().doubleValue() == t2.getTime().doubleValue()) ? 0 : -1)
							: -1;
		}				
	}

	/** Returns a random dealer from my assigned dealers. */
	protected Dealer returnMyRandomDealer() {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Dealer dealer = (Dealer) transactionNetwork.getRandomAdjacent(this);
		if (Settings.errorLog) {
			if (dealer == null || dealer instanceof Dealer == false) {
				System.err.println("Dealer is null or not found. In getRandomDealer(). ");
			}
		}		
		return dealer;
	}

	/** Return a list of all last deals of each of the dealer I am connected to in the transaction network. 
	 * It is not sorted here.
	 * returns null if the list is empty. 
	 */
	private ArrayList<Transaction> returnMyDeals() {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getAdjacent(this).iterator();		
		ArrayList<Transaction> deals = new ArrayList<Transaction>();
		while (itr.hasNext()) { 
			Dealer dealer = (Dealer) itr.next();
			TransactionEdge transactionEdge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);
			if (transactionEdge.getLastTransactionIndex() != -1) {
				deals.add(transactionEdge.getLastTransaction());
			}
		}
		if (deals.size() > 0) {
			return deals;
		}
		else {
			return null;
		}
	}

	/** Returns an agent connection either Customer or Dealer given the network name @param networkName */
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
	

	public int getInitKnownDealers() {
		return initKnownDealers;
	}
	public double getInitialBudget() {
		return initialBudget;
	}
	public double getTax() {
		return tax;
	}
	public double getTotalTaxPaid() {
		return totalTaxPaid;
	}
	public double getTotalMoneySpend() {
		return totalMoneySpend;
	}
	public double getShareDealProb() {
		return shareDealProb;
	}
	public int getMaxLinks() {
		return maxLinks;
	}
	public double getDrugToBeConsumedPreviously() {
		return drugToBeConsumedPreviously;
	}
	public Transaction getLastTransaction() {
		return lastTransaction;
	}
	public int getCurLinks() {
		return curLinks;
	}

	public void setCurLinks(int curLinks) {
		this.curLinks = curLinks;
	}
}