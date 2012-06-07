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
import drugmodel.Settings.MyDealerSelection;
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
	private double totalTaxCollected;
	private double totalUnitsPurchased;
/** Number of current social network links that this customer has. Used for display*/
	private int curLinks ;
	private double TaxPaidToday;
	private double MoneySpendToday;
	private double TaxCollectedToday;
	private double UnitsPurchasedToday;
	private double curTaxCollected;


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
		this.totalTaxCollected = 0.0;
		this.totalUnitsPurchased = 0.0;
		this.TaxPaidToday = 0.0;
		this.MoneySpendToday = 0.0;
		this.TaxCollectedToday = 0.0;
		this.UnitsPurchasedToday = 0.0;
		this.curTaxCollected = 0.0;
	}

	/** This method is called once. Priority is set to 10 so that it's called earliest at the start, after the social network
	 * 	is created. */
	@ScheduledMethod(start = 1, interval = 0, priority = 10)
	public void initMaxLink() {
		Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
		maxLinks = socialNetwork.getInDegree(this);
		curLinks = maxLinks;		
	}
	@ScheduledMethod(start = Settings.StepsInDay, interval = Settings.StepsInDay, priority = 1)
	public void resetTaxVar(){
		this.TaxPaidToday = 0.0;
		this.MoneySpendToday = 0.0;
		this.TaxCollectedToday = 0.0;
		this.UnitsPurchasedToday = 0.0;
	}
	/** Agents have unlimited resources to buy drugs and the model only keeps track of how much they spend and use (in units).
	 * 	Currently, the income interval is Homogeneous. */
	@ScheduledMethod(start = 1, interval = 1, priority = 6)
	public void income() {
		
		double currentTick = ContextCreator.getTickCount();
		if (	(Settings.Resupply.getIncomeOptionForCustomer().equals(SupplyOption.Automatic)
				&& this.money < Settings.PricePerGram + Settings.Tax.returnMaxTax()	)
				|| currentTick % Settings.CustomerParams.incomeInterval == 0) {
			this.addMoney(Settings.Resupply.incomeForCustomer(this.money));
		}
	}	

	/** Consume drugs at every time step (=10 minutes). If the quantity of drugs is less than Settings.consumptionUnitsPerStep
	 * 	then call buyDrugsandEndorseDeals()*/
	@ScheduledMethod(start = 1, interval = 1, priority = 4)
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
		int numTries = Settings.CustomerParams.NumTriesMakeLink;

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
						System.out.println("link made b/w "+this.personID + "  and  " + customer.personID +  "  at time: " + ContextCreator.getTickCount() /48);
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
					this.removeConnection(customer.getPersonID(), currentTick);
					customer.removeConnection(this.personID, currentTick); 					
//					System.out.println("ME:" + this.getPersonID() + " drops " + customer.getPersonID() + "  at time: " + ContextCreator.getTickCount() / 48);
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
	 *   If they have competing deals from dealers they always determine the �best� deal, 
	 *   i.e., they evaluate the units sold at the lowest price, i.e., the lowest number from above.
	 *   Of course, they can only make this determination after they have bought the drug.
	 */
	public boolean buyDrugsandEndorseDeals() {
		/* it shouldn't happen as income() priority is higher than priority of this function */
		if(this.money < Settings.PricePerGram + Settings.Tax.returnMaxTax() ){ 
			System.err.println("Available money: " + this.getMoney() + " is less than 120. Couldnt buy.");
			return false;
		}
		/*choose either from own deal or from netowrk */
		Transaction deal = chooseDeal();
		// NO dealer found. exiting with error
		if (deal == null) {
			System.err.println(" Me: " + personID + " Dealer is null or not found. In buyDrugs(). ");
			return false;
		}		 

		//get dealer from the deal
		Dealer dealer = deal.getDealer();

		double currentTick = ContextCreator.getTickCount();
		/* Get the units to be purchased from the Dealer. */
		double unitsPurchased = dealer.returnUnitsToSell();
		/* Get the actual current cost per unit to be purchased from the Dealer. */
		double costPerUnit = dealer.returnCostPerUnit();
		/* Endorse the deal here. */
		Endorsement endorsement = Endorsement.None;
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		/*using his own deal. endorse only dealer based on last purchase from this dealer and add this transaction on transaction edge.*/
		if(this.personID == deal.getCustomerID()){
			endorsement = calcMyDealEndorsement(unitsPurchased, dealer);
			Transaction transaction = new Transaction(dealer, personID, currentTick, costPerUnit, unitsPurchased, endorsement, deal.getTransactionID(), deal.getCustomerID());
			transaction.print();
			/* Actual selling by the dealer happens here. */
			dealer.sellDrug(transaction);
			/* Actual buying from the dealer happens here.*/
			buyDrug(transaction);
			this.lastTransaction = transaction;		
			TransactionEdge edge;
			if (transactionNetwork.getEdge(this, dealer) != null) {
				edge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);	
				edge.addTransaction(transaction);	
			}
			else {
				System.out.println("ME : " + personID + " my dealer edge not found. ");
			}		
		}
		/* using acquaintance's deal. endorse based on his menu current meun of deals. endorse acquaintance, pay tax.
		 * endorse and add dealer only if the deal is better. i.e endorse good.  
		 */
		else if(this.personID !=  deal.getCustomerID() ){
			endorsement = calcSharedDealEndorsement(unitsPurchased, dealer.getPersonID() );
			Transaction transaction = new Transaction(dealer, personID, currentTick, costPerUnit, unitsPurchased,
					endorsement, deal.getTransactionID(), deal.getCustomerID());
			transaction.print();
			/* Actual selling by the dealer happens here. */
			dealer.sellDrug(transaction);
			/* Actual buying from the dealer happens here.*/
			buyDrug(transaction);
			this.lastTransaction = transaction;		
			System.out.println("In buyDrugsandEndorseDeals this.id != deal.getcustomerID. linked :" + transactionNetwork.evaluate(new Linked(dealer, this))  );
			/* If not connected previously to this dealer then add link only if endorsement is good*/
			if (transactionNetwork.evaluate(new Linked(dealer, this)) == false && endorsement == Endorsement.Good) {
				TransactionEdge edge = new TransactionEdge(this, dealer);
				transactionNetwork.addEdge(edge);
				edge.addTransaction(transaction);
				System.out.println("ME: " + this.personID  + " add dealer: " +dealer.personID + " in my network ");
			}
			//if already in my dealer network
			else if(transactionNetwork.evaluate(new Linked(dealer, this)) == true) {
				TransactionEdge edge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);	
				/* Add this new transaction on the edge. */
				edge.addTransaction(transaction);	
				System.out.println("ME: " + this.personID  + " already have dealer: " +dealer.personID + " in my network ");

			}		
			/* Now pay tax or commission  and endorse the acquaintance */
			Customer connection = (Customer) returnMyConnection(deal.getCustomerID(), Settings.socialnetwork);
			if (connection != null) {
				payTax(connection, unitsPurchased,transaction.getTransactionID());
				/* If I am using someone else's shared deal. This endorse occurs even in the case when the dealer did not
				 * sell the drug and I went to the dealer. Does not happen in the Automatic Drug Supply mode. */
				updateEndorsement(transaction, deal.getCustomerID(), endorsement);
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


	protected Transaction chooseDeal(){
		Transaction deal = null;
		boolean dealerFound = false;
		/* Probability to choose one's own dealer */
		double prob = Math.random();
		/* Choose my own dealer. */ 
		if (prob <= Settings.CustomerParams.MyDealerSelectionProb) {
			deal = returnMyBestDeal();			
			if(deal == null)
				System.out.println("ME: "+  personID+"  Tried my deals...... In chooseDeal: returnMyBestDeal return deal is null.");

			if(deal != null)
				dealerFound = true;
		}		
		/* choose deals from network when prob > my own deal selection prob OR couldn't find one's own dealer */ 
		if (prob > Settings.CustomerParams.MyDealerSelectionProb  
				|| dealerFound == false ) {
			deal = returnDealFromNetwork();			
			if(deal == null)
				System.out.println("ME: " + personID + "  Tried network deals...... In chooseDeal: returnDealFromNetwork return deal is null " );
		}
		if (deal == null  && prob > Settings.CustomerParams.MyDealerSelectionProb) {
			deal = returnMyBestDeal();			
			if(deal == null)
				System.out.println("ME: " + personID + "  Tried my deals...... In chooseDeal: returnDealFromNetwork return deal is null " );
		}
		return deal;
	}

	protected Endorsement calcMyDealEndorsement(double unitsPurchased, Dealer dealer){
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		TransactionEdge  dealerEdge  = (TransactionEdge) transactionNetwork.getEdge(this, dealer);
		if(dealerEdge == null){
			System.err.println("ME: " + personID + " Dealer: " + dealer.getPersonID() + " my dealer edge not found. ");
			return Endorsement.None;
		}
		if(unitsPurchased >= dealerEdge.getLastTransaction().getDrugQtyInUnits() ){  
			System.out.println("ME: " + personID +  " Dealer: " + dealer.getPersonID() + " my own deal units purchased : "  + unitsPurchased   +  " endorsement: " + Endorsement.Good);
			return Endorsement.Good;
		}
	/*	else if(unitsPurchased == dealerEdge.getLastTransaction().getDrugQtyInUnits() ){  
			System.out.println("ME: " + personID +  " Dealer: " + dealer.getPersonID() + " my own deal units purchased : "  + unitsPurchased   +  " endorsement: " + Endorsement.None);
			return Endorsement.None;
		}
*/
		else{
			System.out.println("ME: " + personID +  " Dealer: " + dealer.getPersonID()+ " my own deal units purchased : "  + unitsPurchased   +  " endorsement: " + Endorsement.SoldBad);
			return Endorsement.SoldBad;
		}
	}

	/** Endorses a shared deal. */
	protected Endorsement calcSharedDealEndorsement(double unitsPurchased, int dealerID ){
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		if(transactionNetwork.getDegree(this) == 0 ){
			return Endorsement.Good; 
		}

		Iterator myDealers = transactionNetwork.getEdges(this).iterator();
		double lowestUnitsPurchased = Settings.DealersParams.MaxUnitsToSell + 1;

		while(myDealers.hasNext()){
			TransactionEdge dealerEdge = (TransactionEdge) myDealers.next();
			Person t = (Person) dealerEdge.getTarget();
			Person s = (Person) dealerEdge.getSource();

			if( t instanceof Dealer ){
				System.out.println("ME: " + personID + " Dealer: " + (t).getPersonID() 
						+ " MY dealer Menu : " + dealerEdge.getLastTransaction().getDrugQtyInUnits());
			}
			else if( s instanceof Dealer ){
				System.out.println("ME: " + personID + " Dealer: " + ((Dealer)s).getPersonID() 
						+ " MY dealer Menu : " + dealerEdge.getLastTransaction().getDrugQtyInUnits());
			}

			if(lowestUnitsPurchased > dealerEdge.getLastTransaction().getDrugQtyInUnits()) { 
					// IMP CHANGE not to sort on dummy transaction
					//				&& dealerEdge.getLastTransaction().getTime() > 0 
				lowestUnitsPurchased = dealerEdge.getLastTransaction().getDrugQtyInUnits();
			}
		}
		
		/**
		 * The system has two different qualitative and quantitative outputs. With >= we get
		 * a different effect. The effect of > and >= depends upon the initial phase of old dealers when the old 
		 * dealer sell for 12 units fixed.
		 * 
		 *  not understood fully yet about the effect of '='
		 *  get results for both cases
		 */
		if(unitsPurchased > lowestUnitsPurchased ){
			System.out.println("ME: " + personID + " Dealer: " + dealerID + " units purchased : "  + unitsPurchased   +  " endorsement: " + Endorsement.Good);
			return Endorsement.Good;
		}
		else if(unitsPurchased == lowestUnitsPurchased ){
			System.out.println("ME: " + personID + " Dealer: " + dealerID + " units purchased : "  + unitsPurchased   +  " endorsement: " + Endorsement.None);
			return Endorsement.None;
//			return Endorsement.SoldBad;//Good;
		}
		else{
			System.out.println("ME: " + personID + " Dealer: " + dealerID +  " units purchased : "  + unitsPurchased   +  " endorsement: " + Endorsement.SoldBad);

			return Endorsement.SoldBad;
		}
	}
	
	
	/** Update @param endorsement for a @param transaction based on the deal that was shared by the customer @param connectionID */
	private void updateEndorsement(Transaction transaction, int connectionID, Endorsement endorsement) {
		boolean updated = false;
		Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
		Iterator itr = socialNetwork.getAdjacent(this).iterator();
		while (itr.hasNext()) {
			Customer customer = (Customer) itr.next();
			if (customer.getPersonID() == connectionID) {
				SNEdge outEdge = (SNEdge) socialNetwork.getEdge(this, customer);	
				double currentTick = ContextCreator.getTickCount();
				outEdge.addEndorsement(transaction, endorsement, currentTick);
	//			System.out.println("ME: " + personID + " endorsing my connection on this on my transaction: "  + transaction.getTransactionID()  +  " endorsement: " + endorsement + " based on deal ID: "+ transaction.getDealUsed());

				updated = true;
				//Debugging purpose only. 
				if (((Person) outEdge.getSource()).getPersonID() != this.personID
						|| ((Person) outEdge.getTarget()).getPersonID() != connectionID) {
//					System.err.println("Edge Not Found. Time: " + transaction.getTime() + "Source: " + personID + " Target: " + connectionID);
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
		this.MoneySpendToday += Settings.PricePerGram;
		this.totalUnitsPurchased += transaction.getDrugQtyInUnits();
		this.UnitsPurchasedToday += transaction.getDrugQtyInUnits();
		System.out.println("ME: " + this.personID  + " buying drug...transaction ID: " + transaction.getTransactionID()+ "  paying: 120..... getting Drug in Units: "+ transaction.getDrugQtyInUnits());
}
/*	private void buyDrugNew(Transaction transaction) {
		addDrug(transaction.getDrugQtyInUnits());
		double amount = this.curTaxCollected - Settings.PricePerGram;
		if(amount  >= 0.0 ){
			this.curTaxCollected -= Settings.PricePerGram ;
			System.out.println("ME:"+ this.personID +  " tax amount sufficient to buy drug..." );
		}
		else{
			amount = Settings.PricePerGram - this.curTaxCollected ;
			this.curTaxCollected = 0.0;
			deductMoney(amount);
			this.totalMoneySpend += amount;
			this.MoneySpendToday += amount;
			System.out.println("ME:"+ this.personID +  " tax is NOT sufficient. remaining amount to pay to buy drug...." + amount);
	
		}
//		deductMoney(Settings.PricePerGram);
//		this.totalMoneySpend += Settings.PricePerGram;
//		this.MoneySpendToday += Settings.PricePerGram;
		this.totalUnitsPurchased += transaction.getDrugQtyInUnits();
		this.UnitsPurchasedToday += transaction.getDrugQtyInUnits();
		System.out.println("ME: " + this.personID  + " buying drug...transaction ID: " + transaction.getTransactionID()+ "  paying: " + amount +  " getting Drug in Units: "+ transaction.getDrugQtyInUnits());
}
	private void payTaxNew (Customer connection, double drugQtyInUnits, int transaction_id) {
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			double taxAmount = connection.getTax();
			double amount = this.curTaxCollected - taxAmount;
			if(amount  >= 0.0 ){
				this.curTaxCollected -= taxAmount ;
				System.out.println("ME:"+ this.personID +  " tax amount sufficient to pay tax..." );
			}
			else{
				amount = taxAmount - this.curTaxCollected ;
				this.curTaxCollected = 0.0;
				deductMoney(amount);
				this.totalMoneySpend += amount;
				this.MoneySpendToday += amount;
	//			totalTaxPaid += amount;
	//			this.TaxPaidToday += amount;
				System.out.println("ME:"+ this.personID +  " tax is NOT sufficient. remaining amount to pay to tax...." + amount);

			}
			connection.addMoney(taxAmount);
			connection.addTaxCollected(taxAmount);
			totalTaxPaid += taxAmount;
			this.TaxPaidToday += taxAmount;
			System.out.println("ME: " + this.personID  + " buying drug...transaction ID: " + transaction_id+ "  paying tax: " + taxAmount +  " to customer ID: " + connection.getPersonID());
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
*/
	/** Pay tax to this @param connection based on the @param drugQtyInUnits purchased. */
	private void payTax (Customer connection, double drugQtyInUnits, int transaction_id) {
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			double taxAmount = connection.getTax();
			deductMoney(taxAmount);
			connection.addMoney(taxAmount);
			connection.addTaxCollected(taxAmount);
			totalTaxPaid += taxAmount;
			this.TaxPaidToday += taxAmount;
			System.out.println("ME: " + this.personID  + " buying drug...transaction ID: " + transaction_id+ "  paying tax: " + taxAmount +  " to customer ID: " + connection.getPersonID());
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

	private void addTaxCollected(double taxAmount) {
		// TODO Auto-generated method stub
		this.totalTaxCollected += taxAmount;
		this.TaxCollectedToday += taxAmount;
		this.curTaxCollected +=taxAmount;
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
	 *  Agents only transmit information about their own deals, and agents only transmit if a deal is �good.�
	 *  In other words, if a deal they receive from a dealer is better than the one they previously received it is �good.�
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

	/**  Agents only transmit information about their own deals, and agents only transmit if a deal is �good.�
	 *  In other words, if a deal they receive from a dealer is better than the one they previously received it is �good.�
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
	 * Used for both On-demand and pro-active modes. 
	 */
	protected Transaction returnDealFromNetwork(){
		Network socialNetwork = (Network) (context.getProjection(Settings.socialnetwork));
		ArrayList<SNEdge> preferenceOutEdgeList = returnMyCustomerPreferenceList();

		if(Settings.CustomerParams.shareDealMode == ShareDealMode.OnDemand){
//			System.out.println("Me: " + personID + "returnDealfromNetwork:  prefernece list size: " + preferenceOutEdgeList.size() );

			for (SNEdge edge : preferenceOutEdgeList) {
				Customer customer = (Customer) edge.getTarget();
				//				System.out.println("ME: "+ personID+ " customer.getLastTransaction().getEndorsement().equals(Endorsement.Good) " + customer.getLastTransaction().getEndorsement().equals(Endorsement.Good));
				if (Math.random() <= customer.getShareDealProb()
						&& customer.getLastTransaction() != null
						&& customer.getLastTransaction().getEndorsement().equals(Endorsement.Good)){ 

					SNEdge InEdge = (SNEdge) socialNetwork.getEdge(customer, this);

					if(InEdge == null){
						System.err.println("Edge null.: " + socialNetwork.isAdjacent(customer, this));
						if(!ContextCreator.verifyNetwork()) {
							System.err.println("Network verification failed");
						}
					}
					else {
						Transaction deal = customer.getLastTransaction();
						InEdge.addTransaction(deal);
						Dealer dealer = deal.getDealer();
						if( context.contains(dealer) == false){
							//this was before; we were storing deal. now we are creating a dummy transaction
							//to put with the DealerNotFoundBad endorsement.
							Transaction transaction = new Transaction(dealer, this.personID, ContextCreator.getTickCount(), 
									0, 0, Endorsement.DealerNotFoundBad, deal.getTransactionID(), deal.getCustomerID());
							updateEndorsement(transaction, deal.getCustomerID(), Endorsement.DealerNotFoundBad);
							this.lastTransaction = transaction;		
							//							updateEndorsement(deal, deal.getCustomerID(), Endorsement.DealerNotFoundBad);
							System.out.println("network share deal. dealer not found. " );
						}
						else if(dealer.canSellDrugs() == false ){
							Transaction transaction = new Transaction(dealer, this.personID, ContextCreator.getTickCount(), 
									0, 0, Endorsement.UnSoldBad, deal.getTransactionID(), deal.getCustomerID());
							updateEndorsement(transaction, deal.getCustomerID(), Endorsement.UnSoldBad);
							this.lastTransaction = transaction;		

//							updateEndorsement(deal, deal.getCustomerID(), Endorsement.UnSoldBad);
							System.out.println("network share deal. dealer cant sell. " );

						}
						else {
							System.out.println("ME: " + personID + " network share deal. deal found. whose deal: " + deal.getCustomerID() );
							return deal;
						}
					}
				}
			}		
			System.out.println( "Me: " + personID + " network share deal. NO deal not found. " );
			return null;
		}		
		else if(Settings.CustomerParams.shareDealMode == ShareDealMode.Proactive){
			for (SNEdge edge : preferenceOutEdgeList) {
				Customer customer = (Customer) edge.getTarget();
				SNEdge InEdge = (SNEdge) socialNetwork.getEdge(customer, this);
				if(InEdge == null){
					System.err.println("InEdge not work. network verification required.");
				}
				else{
					Transaction deal = InEdge.returnLastTransaction();
					Dealer dealer = deal.getDealer();
					if( context.contains(dealer) == false){
						Transaction transaction = new Transaction(dealer, this.personID, ContextCreator.getTickCount(), 
								0, 0, Endorsement.DealerNotFoundBad, deal.getTransactionID(), deal.getCustomerID());
						updateEndorsement(transaction, deal.getCustomerID(), Endorsement.DealerNotFoundBad);
//						updateEndorsement(deal, deal.getCustomerID(), Endorsement.DealerNotFoundBad);
					}
					else if(dealer.canSellDrugs() == false ){
						Transaction transaction = new Transaction(dealer, this.personID, ContextCreator.getTickCount(), 
								0, 0, Endorsement.UnSoldBad, deal.getTransactionID(), deal.getCustomerID());
					
						updateEndorsement(transaction, deal.getCustomerID(), Endorsement.UnSoldBad);
						//updateEndorsement(deal, deal.getCustomerID(), Endorsement.UnSoldBad);
					}
					else {
						return deal;
					}
				}
			}
			return null;
		}
		else {
			System.err.println("share mode set to unknown value.");
			return null;
		}
	}
	
	protected ArrayList<SNEdge> returnMyCustomerPreferenceList(){
		Network socialNetwork = (Network) (context.getProjection(Settings.socialnetwork));
		Iterator itr = socialNetwork.getOutEdges(this).iterator();
		ArrayList<SNEdge> preferenceList = new ArrayList<SNEdge>();
		System.out.println("ME: " + this.personID +  " social network degree : "  + socialNetwork.getDegree(this));
		while(itr.hasNext()){
			SNEdge edge = (SNEdge) itr.next();				
			preferenceList.add(edge);
		}
		Collections.shuffle(preferenceList);

	/*	System.out.println("prefernce list size in return my list : " + preferenceList.size() );
		for(SNEdge e : preferenceList){
			System.out.println("ME: " + personID +  " customer: " + ((Customer)e.getTarget()).getPersonID() + " good endorsement: " + e.returnNumGoodEndorsement(Settings.CustomerParams.TimeFrameToSortCustomersOnGoodDeals ));
		}
	*/	if(Settings.CustomerParams.customerPreference.equals(Settings.CustomerPreference.Random)){
			return preferenceList;
		}
		else if(Settings.CustomerParams.customerPreference.equals(Settings.CustomerPreference.Preferred)){
//			Collections.sort(preferenceList, new CustomerComparator());
			Collections.sort(preferenceList, new CustomerGoodBadComparator());
			System.out.println("prefernce list size in return my list : " + preferenceList.size() );
/*			for(SNEdge e : preferenceList){
				System.out.println("After sorting ME: " + personID +  " customer: " + ((Customer)e.getTarget()).getPersonID() + " good endorsement: " + e.returnNumGoodEndorsement(Settings.CustomerParams.TimeFrameToSortCustomersOnGoodDeals ));
			}
*/			return preferenceList;
		}
		else{
			System.err.println("Customer preferecne is set to unknown parameter.");
			return null;
		}
	}
	/** This comparator uses number of good deals shared by a customer in last month. */
	class CustomerComparator implements Comparator<SNEdge> {

		public int compare(SNEdge e1, SNEdge e2) {

			int goodDeals1 = e1.returnNumGoodEndorsement(Settings.CustomerParams.TimeFrameToSortCustomersOnGoodDeals );
			int goodDeals2 = e2.returnNumGoodEndorsement(Settings.CustomerParams.TimeFrameToSortCustomersOnGoodDeals );
			//			System.out.println("num of good deal 1: " + goodDeals1 + " num of good deals 2 :" + goodDeals2 );
			return goodDeals1 < goodDeals2 ? +1 : goodDeals1 == goodDeals2 
					? 0 : -1;
		}
	}
	
	class CustomerGoodBadComparator implements Comparator<SNEdge> {
		public int compare(SNEdge e1, SNEdge e2) {
			int diff1 = e1.returnNumGoodEndorsement(Settings.CustomerParams.TimeFrameToSortCustomersOnGoodDeals )
							- e1.returnNumBadEndorsement(Settings.CustomerParams.TimeFrameToSortCustomersOnGoodDeals);
			int diff2 = e2.returnNumGoodEndorsement(Settings.CustomerParams.TimeFrameToSortCustomersOnGoodDeals )
						- e2.returnNumBadEndorsement(Settings.CustomerParams.TimeFrameToSortCustomersOnGoodDeals);
			//			System.out.println("num of good deal 1: " + goodDeals1 + " num of good deals 2 :" + goodDeals2 );
			return diff1 < diff2 ? +1 : diff1 == diff2 ? 0 : -1;
		}
	}

	protected Transaction returnMyBestDeal(){
		ArrayList<Transaction> preferenceList = returnMyDealsBestSorted();
		if(preferenceList == null){
			System.err.println("ME:" + this.personID + " Deals are empty in returnMyBestDeal.");
			return null;
			
		}
		for (Transaction deal : preferenceList) {
			Dealer dealer = deal.getDealer();
			if(dealer.canSellDrugs() == false ){
				Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
				TransactionEdge edge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);
				if (edge != null) {
					Transaction transaction = new Transaction(dealer, personID, ContextCreator.getTickCount(), 0.0,0.0, 
							Endorsement.UnSoldBad, deal.getTransactionID(), deal.getCustomerID());
					edge.addTransaction(transaction);
					this.lastTransaction = transaction;		
				}
				else {
					System.err.println("My dealer edge not found." );
				}		
			}
			else {//can sell drugs
				return deal;
			}
		}//no dealer found
		return null;
	}

	/** Returns a list sorted by the best dealers from my own transaction network. */
	protected ArrayList<Transaction> returnMyDealsBestSorted() {
		ArrayList<Transaction> deals = returnMyDeals();
		/*	for(Transaction t : deals){
			System.out.println("CustomerID: " + this.personID +  " deal id: " + t.getTime()  );
		}
		 */
		if (deals == null) {
			System.err.println("ME:" + this.personID + " Deals are empty in returnMyDealsBestSorted.");
		}
		else{
			Collections.shuffle(deals);
			
			if(Settings.CustomerParams.myDealerSelection == MyDealerSelection.Random ){
				return deals;//Collections.shuffle(deals);
			}

			/* Return the sorted ArrayList deals. */
			else if(Settings.CustomerParams.myDealerSelection == MyDealerSelection.Preferred ){
				Collections.sort(deals, new Comparator<Transaction>() {
					public int compare(Transaction t1, Transaction t2) {
						return t1.getDrugQtyInUnits().doubleValue() < t2.getDrugQtyInUnits().doubleValue() ? +1 
								: (t1.getDrugQtyInUnits().doubleValue() == t2.getDrugQtyInUnits().doubleValue()) 
								? (t1.getTime().doubleValue() < t2.getTime().doubleValue() ? +1 : (t1.getTime().doubleValue() == t2.getTime().doubleValue()) ? 0 : -1)
										: -1;
					}
				});
			}
			else{
				System.out.println("MyDealerSelection param set to unknown.");
			}
		}

		/*		for(Transaction t : deals){
			System.out.println("customer id: "+personID +  " In return my sorted deals:  decending Quantity: " + t.getDrugQtyInUnits() + " time: " + t.getTime());
		}
		 */		return deals;
	}

	/** Return a list of all last deals of each of the dealer I am connected to in the transaction network. 
	 * It is not sorted here.
	 * returns null if the list is empty. 
	 */
	private ArrayList<Transaction> returnMyDeals() {
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr = transactionNetwork.getAdjacent(this).iterator();		
		ArrayList<Transaction> deals = new ArrayList<Transaction>();
		System.out.println("ME: " + this.personID +  " transaction network degree : "  + transactionNetwork.getDegree(this));

		while (itr.hasNext()) { 
			Dealer dealer = (Dealer) itr.next();
			TransactionEdge transactionEdge = (TransactionEdge) transactionNetwork.getEdge(this, dealer);
			if (transactionEdge.getLastTransactionIndex() != -1) {
				deals.add(transactionEdge.getLastTransaction());
			}
		}
		if (deals.size() > 0) {
			/*			for(Transaction t : deals){
				System.out.println("customer id: "+personID +  " In return my deals:  decending Quantity: " + t.getDrugQtyInUnits() + " time: " + t.getTime());
			}
			 */
			return deals;
		}
		else {
			System.err.println("ME: "  +this.personID+ " My deals are empty in returnMyDeals.");
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
	
	public int getOutDegree(){
		Network network = (Network)(context.getProjection(Settings.socialnetwork));
		return network.getOutDegree(this);
	}
	public int getCustomerDealerDegree(){
		Network network = (Network)(context.getProjection(Settings.transactionnetwork));
		return network.getDegree(this);
	}

	public double getTotalTaxCollected() {
		return totalTaxCollected;
	}

	public double getTotalUnitsPurchased() {
		return totalUnitsPurchased;
	}
	public double getActualPricePaidPerGram(){
		if(totalUnitsPurchased == 0.0){
			return 0.0;
		}
		return ( ((this.totalMoneySpend + this.totalTaxPaid - this.totalTaxCollected) / this.totalUnitsPurchased ) * Settings.UnitsPerGram ); 
//		return ( ((this.totalMoneySpend + this.totalTaxPaid) / this.totalUnitsPurchased ) * Settings.UnitsPerGram ); 

	}
	public double getTodayActualPricePaidPerGram(){
		if(this.UnitsPurchasedToday == 0.0){
			return 0.0;
		}
		//double tax = this.curTaxCollected;
		//curTaxCollected =0.0;
		return ( ((this.MoneySpendToday ) / this.UnitsPurchasedToday ) ); 
		//return ( ((this.MoneySpendToday ) / this.UnitsPurchasedToday ) * Settings.UnitsPerGram ); 
		//		return ( ((this.totalMoneySpend + this.totalTaxPaid) / this.totalUnitsPurchased ) * Settings.UnitsPerGram ); 

	}
	@ScheduledMethod(start = Settings.StepsInDay, interval = Settings.StepsInDay, priority = 3)
	public void print(){
		
		System.out.println("PRINTTICK: " + ContextCreator.getTickCount() +" " + personID + "  " +  this.TaxPaidToday +" " 
				+	this.MoneySpendToday+
				"  " +  this.TaxCollectedToday + " " + 	this.UnitsPurchasedToday + " "
				+  this.getTodayActualPricePaidPerGram());
	}

	public double getTaxPaidToday() {
		System.out.println("tax today:"+ TaxPaidToday );
		return TaxPaidToday;
	}

	public double getMoneySpendToday() {
		return MoneySpendToday;
	}

	public double getTaxCollectedToday() {
		return TaxCollectedToday;
	}

	public double getUnitsPurchasedToday() {
		return UnitsPurchasedToday;
	}
}