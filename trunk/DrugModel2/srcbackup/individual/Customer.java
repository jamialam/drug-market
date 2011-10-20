package individual;

/** 
 * @author shah
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.projection.Linked;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;
import cern.jet.random.Uniform;
import drugmodel.ContextCreator;
import drugmodel.SNEdge;
import drugmodel.Settings;
import drugmodel.Settings.DealerSelection;
import drugmodel.Settings.Endorsement;
import drugmodel.Settings.ShareDealMode;
import drugmodel.Settings.SupplyOption;
import drugmodel.Settings.TaxType;
import drugmodel.TransactionEdge;
import drugmodel.Transaction;

@SuppressWarnings({ "rawtypes", "unchecked" })

public class Customer extends Person {
	/** Number of dealers known at the time of start or entry into the simulation.*/
	private int initKnownDealers; 
	private double initialBudget;
	private Context context;
	private Transaction lastTransaction;
	private double tax;
	private int numBadAllowed;
	private double shareDealProb;
	private double drugToBeConsumedPreviously; 
	private int evaluationInterval;
	private int maxLinks;
	protected HashMap<Integer, Double> droppedLinks;

	// for charts and display
	private double totalTaxPaid;
	private double totalMoneySpend;
	
	public Customer(Context _context) {
		this.context = _context;
		initialize();
	}

	private void initialize() {
		this.initKnownDealers = Uniform.staticNextIntFromTo(Settings.minDealerCustomerlinks, Settings.maxDealerCustomerlinks);
		this.initialBudget = Settings.Budget.returnInitialBudget();
		addMoney(initialBudget);
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			this.tax = Settings.Tax.setInitialFlatFee();
		}
		else {
			this.tax = Settings.Tax.setInitialDrugPercent();
		}
		this.numBadAllowed = Settings.CustomerParams.returnBadAllowed();
		this.lastTransaction = null;
		this.drugs = Uniform.staticNextDoubleFromTo(0,12);
		this.shareDealProb = Settings.CustomerParams.returnShareDealProb();
		this.drugToBeConsumedPreviously = 0.0;
		this.evaluationInterval = Uniform.staticNextIntFromTo(1,Settings.CustomerParams.CustomerConnectionEvaluationInterval);
		this.maxLinks = 0;
		droppedLinks = new HashMap<Integer, Double>();
		this.totalTaxPaid = 0.0;
		this.totalMoneySpend = 0.0;
	}

	@ScheduledMethod(start = 1, interval = 0, priority = 10)
	public void initMaxLink() {
		Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
		maxLinks = socialNetwork.getInDegree(this);
		System.out.println("personID: " + this.personID +  "  maxLinks: " +maxLinks);
	}
	/**
	 * Agents have unlimited resources to buy drugs and the model only keeps track of how much they spend and use (in units). (Automatic)
	 */
	@ScheduledMethod(start = 1, interval = 1, priority = 4)
	public void income() {
		double currentTick = ContextCreator.getTickCount();

		if (Settings.Resupply.getIncomeOption().equals(SupplyOption.Automatic)) {
			if (this.money < Settings.pricePerGram + Settings.Tax.returnMaxTax()) {
				this.addMoney(Settings.Resupply.income(this.money));
			}	
		}
		else {
			if (currentTick % Settings.CustomerParams.incomeInterval == 0) {
				this.addMoney(Settings.Resupply.income(this.money));		
			}
		}
	}	

	/**
	 * 
	 */
	@ScheduledMethod(start = 1, interval = 1, priority = 3)
	public void useDrugs() {	
		if(this.drugs < Settings.consumptionUnitsPerStep){
			if (this.buyDrugsandEndorseDeals() == false){
				this.drugToBeConsumedPreviously += Settings.consumptionUnitsPerStep;
				System.err.println("Customer "+ this.personID+ " time: " + ContextCreator.getTickCount() +" couldnt buy drug. drug to be consumed : " + this.drugToBeConsumedPreviously);
			}
		}
		deductDrug(Settings.consumptionUnitsPerStep);
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 2)
	public void makeLinks() {
		if(ContextCreator.getTickCount() == this.evaluationInterval+ 3 
				||	(ContextCreator.getTickCount() % (Settings.CustomerParams.CustomerConnectionEvaluationInterval ) ) == this.evaluationInterval +3 ) {

			Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
			if(socialNetwork.getInDegree(this) < this.maxLinks ){ 
				Iterator itr_customers = context.getRandomObjects(Customer.class,3 ).iterator();
				while( itr_customers.hasNext() ){
					Customer customer = (Customer) itr_customers.next();
					if(socialNetwork.getInDegree(customer) < customer.getMaxLinks()   
							&& socialNetwork.isAdjacent(this,customer) == false
							&& this.personID != customer.personID
							&& droppedLinks.containsKey(customer.personID) != true
							&& Math.random() < numCommonDealer(this, customer)* Settings.CustomerParams.makeLinkProb ){
						SNEdge edgeto = new SNEdge(this,customer, true);
						SNEdge edgefrom = new SNEdge(customer,this, true);
						socialNetwork.addEdge(edgeto);
						socialNetwork.addEdge(edgefrom);
						if(socialNetwork.isPredecessor(this, customer) == false
								|| socialNetwork.isSuccessor(this,customer) == false)
							System.err.println("edge not created properly.");
		
						System.out.println("link made b/w "+this.personID + "  and  " + customer.personID +  "  at time: " + ContextCreator.getTickCount() /48);
						break;
					}
				}
			}
		}
	}
	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void updateDroppedLinks() {
		if(droppedLinks.isEmpty())
			return;
		Iterator itr = droppedLinks.keySet().iterator();
		ArrayList<Integer> tobeRemoved = new ArrayList<Integer>();
		while(itr.hasNext()){
			Integer id = (Integer) itr.next();
			double time = droppedLinks.get(id);
			if(ContextCreator.getTickCount() - time > Settings.CustomerParams.TimeFrameToForgetDropLink){
				tobeRemoved.add(id);
				//droppedLinks.remove((Integer)id);
				System.out.println("this:" + this.personID + " remove " + id +" from drop list at time: " + ContextCreator.getTickCount()/48);
			}
		}
		for(Integer id : tobeRemoved){
			droppedLinks.remove(id);
		}

	}


	public int numCommonDealer(Customer _this, Customer customer){
		Context context = ContextUtils.getContext(this);
		Network transactionNetwork = (Network)(context.getProjection(Settings.transactionnetwork));
		Iterator itr_thisDealers = transactionNetwork.getAdjacent(_this).iterator();

		int numCommonDealer=0;
		while(itr_thisDealers.hasNext() ){
			Dealer thisDealer = (Dealer) itr_thisDealers.next();
			if(transactionNetwork.evaluate(new Linked(thisDealer, customer)) == true){
				numCommonDealer++;

				if(transactionNetwork.isAdjacent(_this,thisDealer) == false
						|| transactionNetwork.isAdjacent(customer, thisDealer) == false)
					System.err.println("evalute not working properly. DEaler is not adjacent.");
			}
		}
		//	System.out.println("num of common dealers: "+numCommonDealer);
		return numCommonDealer;
	}
	/*	@ScheduledMethod(start = 1, interval = 1, priority = 2)
	public void useDrugs() {	
		// have enough drug to clear all drug debt 
		if(this.drugs >= Settings.consumptionUnitsPerStep + this.drugToBeConsumedPreviously	){
			System.out.println("Customer "  + this.personID + " has enough drugs:"+ this.drugs+  ". consuming drug: " + (Settings.consumptionUnitsPerStep + this.drugToBeConsumedPreviously));
			deductDrug(Settings.consumptionUnitsPerStep + this.drugToBeConsumedPreviously );
			this.drugToBeConsumedPreviously = 0.0;
		}
		// buy drug and try getting regular dose and clearing previous drug debt
		else if(this.drugs < Settings.consumptionUnitsPerStep + this.drugToBeConsumedPreviously  
				&& this.buyDrugsandEndorseDeals() == true ){
			System.out.println("case 2");
			deductDrug(Settings.consumptionUnitsPerStep);
			if(this.drugToBeConsumedPreviously > 0.0){
				if(this.drugs < this.drugToBeConsumedPreviously){
					drugToBeConsumedPreviously = drugToBeConsumedPreviously - drugs;
					System.out.println("case 3");
					deductDrug(this.drugs);
				}
				else{
					System.out.println("case 4");
					deductDrug(this.drugToBeConsumedPreviously);
					this.drugToBeConsumedPreviously = 0.0;
				}
			}
		}
		// try getting regular dose only
		else if(this.drugs >= Settings.consumptionUnitsPerStep ){
			System.out.println("case 5");
			deductDrug(Settings.consumptionUnitsPerStep);
		}
		// cudnt get even regular dose 
		else{
			this.drugToBeConsumedPreviously += Settings.consumptionUnitsPerStep;
			System.out.println("Customer "+ this.personID+ " time: " + ContextCreator.getTickCount() +" couldnt buy drug. drug to be consumed : " + this.drugToBeConsumedPreviously);
		}
	}
	 */

	/**
	 * 
	 */
	//	@ScheduledMethod(start = 1, interval = Settings.CustomerParams.CustomerConnectionEvaluationInterval, priority = 1)
	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void evaluateLinks() {
		if(ContextCreator.getTickCount() == this.evaluationInterval 
				||	(ContextCreator.getTickCount() % (Settings.CustomerParams.CustomerConnectionEvaluationInterval ) ) == this.evaluationInterval ) {

			//		System.out.println("customer:" + this.personID + "currrent tick:" + ContextCreator.getTickCount()+ " evaluationInterval:" + this.evaluationInterval);
			Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
			Iterator itr = socialNetwork.getOutEdges(this).iterator();
			ArrayList<SNEdge> removedLinks = new ArrayList<SNEdge>();
			while (itr.hasNext()) {
				SNEdge edge = (SNEdge) itr.next();
				if(shouldDropLinkTF(edge, Settings.CustomerParams.defaultTimeFrameToDropLink)){
					Customer customer = (Customer) edge.getTarget();
					removedLinks.add((SNEdge)socialNetwork.getEdge(this, customer));
					removedLinks.add((SNEdge)socialNetwork.getEdge(customer, this));
					droppedLinks.put((Integer)customer.personID,(Double) ContextCreator.getTickCount());
					customer.droppedLinks.put((Integer)this.personID,(Double) ContextCreator.getTickCount());
					System.out.println("this:" + this.getPersonID() + " drops " + customer.getPersonID() + "  at time: " + ContextCreator.getTickCount() / 48);
				}
			}
			for (SNEdge edge : removedLinks) {
				socialNetwork.removeEdge(edge);
			}
		}
	}

	private boolean shouldDropLinkTF(SNEdge edge, double time_frame) {
		if(edge.returnNumBadEndorsement(time_frame) > this.numBadAllowed ){
			return true;
		}
		else {
			return false;
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

	public boolean buyDrugsandEndorseDeals() {
		//added by SA
		/** it shouldnt happen as income() priority is higher than priority of this function */
		if(this.money < Settings.pricePerGram + Settings.Tax.returnMaxTax() ){
			if (Settings.errorLog ) 
				System.err.println("Available money: " + this.getMoney() + " is less than 120. Couldnt buy.");
			return false;
		}

		Transaction deal = null;
		Dealer dealer = null;
		ArrayList<Transaction> deals = null;
		// choose your own dealer
		double  prob = Math.random();
		if (prob < Settings.DealersParams.dealerSelectionProb ) {
			deals = returnMyDealsBestSorted();			
			if(deals != null){
				for(Transaction t : deals){
					deal = t;
					dealer = t.getDealer();
					if(dealer != null){
						break;
					}
				}
			}
		}
		// choose deals from network 
		if( prob >= Settings.DealersParams.dealerSelectionProb  
				|| deal == null || dealer == null ) {
			if(Settings.CustomerParams.shareDealMode == ShareDealMode.ShareWithoutAsking )
				deals = returnSortedDealsFromNetworkfromPrevShared(); // this is the other implementation relying on shareDeals();
			else
				deals = returnSortedDealsFromNetwork();			
			if(deals != null){
				for(Transaction t : deals){
					deal = t;
					dealer = t.getDealer();
					if(dealer != null){
						//					System.out.println("Found network dealer.");	
						break;
					}
				}
			}

		}
		// coudn't found dealer, now select random dealer.
/*		if (deal == null || dealer == null) {
			dealer = returnMyRandomDealer();
		}
*/		// NO dealer found. exiting with error
		if (Settings.errorLog) {
			if (dealer == null || dealer instanceof Dealer == false) {
				System.err.println("Dealer is null or not found. In buyDrugs(). Me: " + personID);
				return false;
			}
		}		 

		int currentTick = (int) ContextCreator.getTickCount();
		double unitsPurchased = dealer.returnUnitsToSell();
		double costPerUnit = dealer.returnCostPerUnit();		
		Endorsement endorsement = Endorsement.None;

		if (deal != null
				&& deal.getCustomerID() != this.personID) {
			endorsement = unitsPurchased >= deal.getDrugQtyInUnits() ? Endorsement.Good : Endorsement.Bad;
			updateEndorsement(deal , deal.getCustomerID() , endorsement );
		}
		//It is my own dealer and there wasn't a deal information used. 
		else if (lastTransaction != null) {
			endorsement = unitsPurchased >= lastTransaction.getDrugQtyInUnits() ? Endorsement.Good : Endorsement.Bad;
		}

		Transaction transaction = new Transaction(dealer, personID, currentTick, costPerUnit, unitsPurchased, endorsement);
		//////////BIG CHANGE MADE 9/9/2011
		if(	dealer.sellDrug(transaction) == true ){ 
			buyDrug(transaction);
			//		buyDrug(transaction);
			//		dealer.sellDrug(transaction);

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
			if (deal != null &&  deal.getCustomerID() != this.personID) {
				Customer connection = (Customer) returnMyConnection(deal.getCustomerID(), Settings.socialnetwork);
				if(connection == null ){
					System.err.println("Person to pay tax, not found.");
				}
				else{
					payTax(connection, unitsPurchased);
			//		System.err.println("total tax paid." + this.totalTaxPaid);
					
				}
					
			}
			if(Settings.CustomerParams.shareDealMode == Settings.ShareDealMode.ShareWithoutAsking)
				shareDeal();
			return true;
		}
		return false;
	}
	/*	public boolean buyDrugsandEndorseDeals() {
		//added by SA
	 *//** it shouldnt happen as income() priority is higher than priority of this function *//*
		if(this.money < Settings.pricePerGram + Settings.Tax.returnMaxTax() ){
			if (Settings.errorLog ) 
				System.err.println("Available money: " + this.getMoney() + " is less than 120. Couldnt buy.");
			return false;
		}

		Transaction deal = null;
		Dealer dealer = null;
		if (Settings.DealersParams.dealerSelection.equals(DealerSelection.NetworkBest)) {

			if(Settings.CustomerParams.shareDealMode == ShareDealMode.ShareWithoutAsking )
				deal = returnBestDealsFromNetworkfromPrevShared(); // this is the other implementation relying on shareDeals();
			else
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
				return false;
			}
		}		 

		int currentTick = (int) ContextCreator.getTickCount();
		double unitsPurchased = dealer.returnUnitsToSell();
		double costPerUnit = dealer.returnCostPerUnit();		
		Endorsement endorsement = Endorsement.None;

		if (deal != null
				&& deal.getCustomerID() != this.personID) {
			endorsement = unitsPurchased >= deal.getDrugQtyInUnits() ? Endorsement.Good : Endorsement.Bad;
			updateEndorsement(deal , deal.getCustomerID() , endorsement );
		}
		//It is my own dealer and there wasn't a deal information used. 
		else if (lastTransaction != null) {
			endorsement = unitsPurchased >= lastTransaction.getDrugQtyInUnits() ? Endorsement.Good : Endorsement.Bad;
		}

		Transaction transaction = new Transaction(dealer, personID, currentTick, costPerUnit, unitsPurchased, endorsement);
//////////BIG CHANGE MADE 9/9/2011
		if(	dealer.sellDrug(transaction) == true ){ 
			buyDrug(transaction);
			//		buyDrug(transaction);
			//		dealer.sellDrug(transaction);

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
			if (deal != null &&  deal.getCustomerID() != this.personID) {
				Customer connection = (Customer) returnMyConnection(deal.getCustomerID(), Settings.socialnetwork);
				if(connection == null )
					System.err.println("Person to pay tax, not found.");
				else
					//	payTax(connection,deal.getDrugQtyInUnits());
					//	payTax(connection, transaction.getDrugQtyInUnits());
					payTax(connection, unitsPurchased);
			}
			if(Settings.CustomerParams.shareDealMode == Settings.ShareDealMode.ShareWithoutAsking)
				shareDeal();
			return true;
		}
		return false;
	}
	  */
	private void updateEndorsement(Transaction deal , int connectionID, Endorsement endorsement) {
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
		this.totalMoneySpend += Settings.pricePerGram;
	}

	private void payTax (Customer connection, double DrugQtyInUnits) {
		if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {
			double amount = connection.getTax();
			deductMoney(amount);
			connection.addMoney(amount);
			totalTaxPaid += amount;
			
		}
		else {
			double drug_percent = DrugQtyInUnits * connection.getTax()/100;
			if(this.drugs < drug_percent )
				System.err.println("couldnt pay tax...");
			deductDrug(drug_percent);
			connection.addDrug(drug_percent);
			totalTaxPaid += drug_percent;
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
	public void shareDeal() {
		if(Settings.CustomerParams.shareDealWay == Settings.ShareDealWay.X_percent_of_deals )
			share_X_percentDealsWithNetwork();
		else
			shareDealWith_X_pecentNetwork();
	}

	/**
	 *  Agents only transmit information about their own deals, and agents only transmit if a deal is “good.”
	 *  In other words, if a deal they receive from a dealer is better than the one they previously received it is “good.”
	 *  deal is being shared with x% of the network connections.
	 */
	public void shareDealWith_X_pecentNetwork() {
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
				if (Math.random() <= this.shareDealProb){//Settings.CustomerParams.shareDealProb) {
					SNEdge edge = (SNEdge) socialNetwork.getEdge(this, acquaintance);
					edge.addTransaction(new Transaction(transaction));				
				}
			}		
		}
	}

	/**  Agents only transmit information about their own deals, and agents only transmit if a deal is “good.”
	 *  In other words, if a deal they receive from a dealer is better than the one they previously received it is “good.”
	 *  x% of the good deals are being shared with  network  connections.
	 */
	public void share_X_percentDealsWithNetwork() {
		if(Settings.CustomerParams.shareDealMode == Settings.ShareDealMode.ShareWhenAsked)
			return;
		int currentTick = (int) ContextCreator.getTickCount();
		if (lastTransaction != null
				&& lastTransaction.getEndorsement().equals(Endorsement.Good)
				&& currentTick - lastTransaction.getTime() <= Settings.stepsInDay) {
			Network socialNetwork = (Network)(context.getProjection(Settings.socialnetwork));
			Iterator itr = socialNetwork.getAdjacent(this).iterator();
			Transaction transaction = new Transaction(lastTransaction);
			transaction.setTaxAmount(this.tax);
			if (Math.random() <= this.shareDealProb){//Settings.CustomerParams.shareDealProb) {
				while (itr.hasNext()) {
					Customer acquaintance = (Customer) itr.next();
					SNEdge edge = (SNEdge) socialNetwork.getEdge(this, acquaintance);
					edge.addTransaction(new Transaction(transaction));				
				}
			}		
		}
	}

	/**
	 * This method assumes that the deals are shared to ego by alters when ego is going to the market for a purchase.
	 * It is independent of shareDeals() method. Here deals are shared inside this function. 
	 */

	protected Transaction returnBestDealsFromNetwork() {
		Transaction bestDeal = null;
		//get your own deal
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
				if (Math.random() <= customer.getShareDealProb() ){ //Settings.CustomerParams.shareDealProb) {
					Dealer dealer = (Dealer) itrDeal.next();
					TransactionEdge transEdge = (TransactionEdge) transactionNetwork.getEdge(customer, dealer);
					if (transEdge.getLastTransactionIndex() != -1) {
						Transaction transaction = transEdge.getLastTransaction();
						/** Agents only transmit information about their own deals, and agents only transmit if a deal is “good.”
						 *  In other words, if a deal they receive from a dealer is better than the one they previously received it is “good.” **/
						if(transaction.getEndorsement() == Endorsement.Good ){
							transaction.setTaxAmount(customer.getTax());
							deals.add(transaction);
							//Added by SA
							SNEdge edge = (SNEdge) socialNetwork.getEdge(customer, this);
							if(edge == null){
								System.err.println("Edge null.");
								System.err.println(socialNetwork.isAdjacent(customer, this));
								if(!ContextCreator.verifyNetwork())
									System.err.println("Network verification failed");
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
		}
		if (deals.isEmpty()) {
			return null;
		}		
		Collections.sort(deals, new Comparator<Transaction>() {
			public int compare(Transaction trans1, Transaction trans2) {
				double cost1 = 0;
				double cost2 = 0;
				//Added by SA
				if(trans1 == null || trans2 == null){
					System.err.println("trans obj null.. ");
				}

				if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {			
					//here the cost is in dollars so the smallest will be selected.
					cost1 = (Settings.pricePerGram + trans1.getTaxAmount())/trans1.getDrugQtyInUnits();
					cost2 = (Settings.pricePerGram + trans2.getTaxAmount())/trans2.getDrugQtyInUnits();
				}
				else if (Settings.Tax.taxType.equals((TaxType.AmountDrug))) {

					cost1 = Settings.pricePerGram / ( trans1.getDrugQtyInUnits() - (trans1.getDrugQtyInUnits() * (trans1.getTaxAmount()/100) ) ); 
					cost2 = Settings.pricePerGram / ( trans2.getDrugQtyInUnits() - (trans2.getDrugQtyInUnits() * (trans2.getTaxAmount()/100) ) );					

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
	protected ArrayList<Transaction> returnSortedDealsFromNetwork() {
		Transaction bestDeal = null;
		//get your own deal
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
				if (Math.random() <= customer.getShareDealProb() ){ //Settings.CustomerParams.shareDealProb) {
					Dealer dealer = (Dealer) itrDeal.next();
					TransactionEdge transEdge = (TransactionEdge) transactionNetwork.getEdge(customer, dealer);
					if (transEdge.getLastTransactionIndex() != -1) {
						Transaction transaction = transEdge.getLastTransaction();
						/** Agents only transmit information about their own deals, and agents only transmit if a deal is “good.”
						 *  In other words, if a deal they receive from a dealer is better than the one they previously received it is “good.” **/
						if(transaction.getEndorsement() == Endorsement.Good ){
							transaction.setTaxAmount(customer.getTax());
							deals.add(transaction);
							//Added by SA
							SNEdge edge = (SNEdge) socialNetwork.getEdge(customer, this);
							if(edge == null){
								System.err.println("Edge null.");
								System.err.println(socialNetwork.isAdjacent(customer, this));
								if(!ContextCreator.verifyNetwork())
									System.err.println("Network verification failed");
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
		}
		if (deals.isEmpty()) {
			return null;
		}		
		Collections.sort(deals, new Comparator<Transaction>() {
			public int compare(Transaction trans1, Transaction trans2) {
				double cost1 = 0;
				double cost2 = 0;
				//Added by SA
				if(trans1 == null || trans2 == null){
					System.err.println("trans obj null.. ");
				}

				if (Settings.Tax.taxType.equals(TaxType.FlatFee)) {			
					//here the cost is in dollars so the smallest will be selected.
					cost1 = (Settings.pricePerGram + trans1.getTaxAmount())/trans1.getDrugQtyInUnits();
					cost2 = (Settings.pricePerGram + trans2.getTaxAmount())/trans2.getDrugQtyInUnits();
				}
				else if (Settings.Tax.taxType.equals((TaxType.AmountDrug))) {

					cost1 = Settings.pricePerGram / ( trans1.getDrugQtyInUnits() - (trans1.getDrugQtyInUnits() * (trans1.getTaxAmount()/100) ) ); 
					cost2 = Settings.pricePerGram / ( trans2.getDrugQtyInUnits() - (trans2.getDrugQtyInUnits() * (trans2.getTaxAmount()/100) ) );					

				}
				if (trans1.getDealer().getPersonID() != trans2.getDealer().getPersonID()) {					
					return cost1 > cost2 ? +1 : cost1 == cost2 ? 0 : -1;				
				}
				else {
					return trans1.getTime() < trans2.getTime() ? +1 : (trans1.getTime() == trans2.getTime()) ? 0 : -1;
				}
			}
		});		
		return deals;
	}

	/** 
	 * This method assumes that the deal is shared by customers to their links, given the share prob parameter,
	 * at the time of their own purchases. One of the consequences is that the dealer may not exist anymore in 
	 * the market and so we check the last deals shared before they are being sorted for the 'best'.
	 */
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
					cost1 = Settings.pricePerGram / ( trans1.getDrugQtyInUnits() - (trans1.getDrugQtyInUnits() * (trans1.getTaxAmount()/100) ) ); 
					cost2 = Settings.pricePerGram / ( trans2.getDrugQtyInUnits() - (trans2.getDrugQtyInUnits() * (trans2.getTaxAmount()/100) ) );										
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
	protected ArrayList<Transaction> returnSortedDealsFromNetworkfromPrevShared() {
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
					cost1 = Settings.pricePerGram / ( trans1.getDrugQtyInUnits() - (trans1.getDrugQtyInUnits() * (trans1.getTaxAmount()/100) ) ); 
					cost2 = Settings.pricePerGram / ( trans2.getDrugQtyInUnits() - (trans2.getDrugQtyInUnits() * (trans2.getTaxAmount()/100) ) );										
				}
				if (trans1.getDealer().getPersonID() != trans2.getDealer().getPersonID()) {					
					return cost1 > cost2 ? +1 : cost1 == cost2 ? 0 : -1;				
				}
				else {
					return trans1.getTime() < trans2.getTime() ? +1 : (trans1.getTime() == trans2.getTime()) ? 0 : -1;
				}
			}
		});		

		//bestDeal = deals.get(0);
		//return bestDeal;
		return deals;
	}

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
					return t1.getDrugQtyInUnits() < t2.getDrugQtyInUnits() ? +1 
							: (t1.getDrugQtyInUnits() == t2.getDrugQtyInUnits()) ? 0 : -1;
				}
			});
			bestDeal = deals.get(0);
		}	
		return bestDeal;
	}	
	protected ArrayList<Transaction> returnMyDealsBestSorted() {
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
		if (deals.size() > 0) {			
			Collections.shuffle(deals);
			Collections.sort(deals, new Comparator<Transaction>() {
				public int compare(Transaction t1, Transaction t2) {
					return t1.getDrugQtyInUnits() < t2.getDrugQtyInUnits() ? +1 
							: (t1.getDrugQtyInUnits() == t2.getDrugQtyInUnits()) ? 0 : -1;
				}
			});
		}	
		return deals;
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

}