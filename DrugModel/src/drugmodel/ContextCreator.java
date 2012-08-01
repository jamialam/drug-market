package drugmodel;

import individual.Customer;
import individual.Dealer;
import individual.Person;

import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import DataCollection.DataCollector;
import DataCollection.DealsDataCollector;
import DataCollection.DropOutCollector;
import DataCollection.ModelParams;

import drugmodel.Settings.Endorsement;
import drugmodel.Settings.GeneratorType;
import drugmodel.Settings.TransactionType;

import generator.Generator;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.PropertyEquals;
import repast.simphony.query.Query;
import repast.simphony.query.space.projection.Linked;
import repast.simphony.space.graph.Network;
import repast.simphony.ui.RSApplication;


public class ContextCreator implements ContextBuilder<Object> {
	private static double currentTick = -1;
	public static Context mainContext;
//	public static DataCollector dataCollector = null;
	


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Context build(Context<Object> context) {	
		context.setId("drugmodel");
		ContextCreator.mainContext = context;
		Person.lastID = -1;
		currentTick = -1;
		Transaction.lastID = -1;
//		dataCollector = null;

		Parameters p = RunEnvironment.getInstance().getParameters();
/*
		Settings.CustomerParams.MyDealerSelectionProb = (Double)p.getValue("mydealerselectionprob");
		Settings.CustomerParams.MinshareDealProb = (Double)p.getValue("sharedealprob");
		Settings.CustomerParams.MaxshareDealProb = (Double)p.getValue("sharedealprob");
		Settings.DealersParams.GreedynewDealerProb = (Double)p.getValue("greedynewdealerprob");
		Settings.DealersParams.updatePriceMode = Settings.DealersParams.convert((String) p.getValue("updatepricemode"));
		
		Settings.InitCustomers = (Integer)p.getValue("initcustomerpopulation");
		Settings.InitDealers = (Integer)p.getValue("initdealerpopulation");
		Settings.DealersParams.SurplusLimit = ((Integer)p.getValue("surpluslimit")* Settings.UnitsPerGram );
		Settings.DealersParams.AverageMaxDealsLimit = ((Integer)p.getValue("maxdeals")*1.0);

*//*		System.out.println("init customer: " +Settings.initCustomers);
		System.out.println("init dealer: " +Settings.initDealers);
		System.out.println("surplus limit: " +Settings.DealersParams.surplusLimit);
		System.out.println("max deals limit: " +Settings.DealersParams.maxDealsLimit);*/

		System.out.println("Test function");
		/**  We first create customer agents */ 
		for (int i=0; i<Settings.InitCustomers; i++) {
			Customer customer = new Customer(context);
			context.add(customer);			
		}

		/** First embed all the customer agents in to a network. */ 
		@SuppressWarnings("unused")
		Network socialnetwork = Generator.returnNetwork(context, Settings.generator_type);
				
		/**  Now creating dealer agents */
		for (int i=0; i<Settings.InitDealers; i++) {
			Dealer dealer = new Dealer();
			context.add(dealer);
		}

		/** Now generate the customer-dealer network - Unconnected and Undirected. */  
		Network transactionnetwork = NetworkFactoryFinder.createNetworkFactory(null).createNetwork(
				Settings.transactionnetwork, context, false, new TransactionEdgeCreator());

		/** Now assign to each customer, some dealers from a range of [minDealers, maxDealers] (default:[1,3]) 
		 *  They are given a random connection to between 1-3 dealers. 
		 * */ 
		Iterator custItr = context.getObjects(Customer.class).iterator();
		ArrayList<Transaction> deals = new ArrayList<Transaction>(); 
		while(custItr.hasNext()) {
			Customer customer = (Customer) custItr.next(); 
			while(transactionnetwork.getDegree(customer) < customer.getInitKnownDealers()) {
				Dealer dealer = (Dealer)(context.getRandomObjects(Dealer.class, 1).iterator().next());
				if (transactionnetwork.evaluate(new Linked(dealer, customer)) == false )  { 
					TransactionEdge edge = new TransactionEdge(customer, dealer, 0.0);
					// WARNING: explicitly setting time to -1
					Transaction transaction = new Transaction(dealer, customer.getPersonID(), 
							(int) -1 ,(Settings.PricePerGram / Settings.UnitsPerGram ), Settings.UnitsPerGram, 
							Endorsement.None, -1, -1, TransactionType.directDeal);
					deals.add(transaction);
					edge.addTransaction(transaction);
					transactionnetwork.addEdge(edge);
					System.out.println("Customer: " + customer.getPersonID() + " linked to Dealer :" + dealer.getPersonID());
				}
			}			
			if (Settings.outputLog) {
				System.out.println("Customer: " + customer.getPersonID() + " has limit: " + customer.getInitKnownDealers()
						+ " and has degree: " + transactionnetwork.getDegree(customer));
			}
		}
		
		Iterator dealerItr = context.getObjects(Dealer.class).iterator();
		System.out.println("-------------At Start--------");

		for(Dealer d; dealerItr.hasNext(); ){
			d= (Dealer) dealerItr.next();
//			System.out.println("Dealer ID:  " + d.getPersonID() + " degree: " + transactionnetwork.getDegree(d));
	
			System.out.println(d.getPersonID() + " , " + transactionnetwork.getDegree(d) +  " , " + 
								d.getSalesToday() + " , " + d.getDealsToday() +  " , " + d.getUnitsToSell()+ " , " + d.getDealerType() );
		}
		/** Network verification*/
		if(Settings.errorLog) {
			System.out.println("network verification: " + verifyNetwork());
		}

/*		if(RunEnvironment.getInstance().isBatch() == false) {
			makeControlPanel();
		}
		else {
*/			RunEnvironment.getInstance().endAt(Settings.endTime);		
//		}

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		/* Priority set to 10 to make sure it's always executed first. */
		ScheduleParameters params = ScheduleParameters.createRepeating(1,1,10);
		schedule.schedule(params, this, "updateTickCount");

		/*		dataCollector = new DataCollector(deals);
		context.add(dataCollector);
		ScheduleParameters params1 = ScheduleParameters.createAtEnd(1);		
		schedule.schedule(params1, this, "writeGraph");*/				

		context.add(new DealsDataCollector());
		//context.add(new ModelParams());
		context.add(new DropOutCollector());
		
/*		ScheduleParameters params1 = ScheduleParameters.createAtEnd(1);		
		schedule.schedule(params1, this, "writeDealerDegree");				
*/
		return context;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean verifyNetwork() {
		Iterator customerItr = mainContext.getObjects(Customer.class).iterator();
		Network socialNetwork = (Network) (mainContext.getProjection(Settings.socialnetwork));

		Iterator links;
		Customer ego, alter;
		while(customerItr.hasNext()){
			ego = (Customer) customerItr.next();
			links = socialNetwork.getAdjacent(ego).iterator();
			while(links.hasNext()){
				alter = (Customer) links.next();
				if(!socialNetwork.isPredecessor(ego,alter) || !socialNetwork.isSuccessor(ego, alter)) {
					return false;
				}
			}
		}
		return true;
	}

	public void updateTickCount() {
		currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
/*		if(currentTick % Settings.GarbageTime == 0)
			System.gc();
*/	}

/*	public void writeGraph(){
		try {
			dataCollector.save();
			dataCollector.saveInPajek();
		} catch (IOException e) {e.printStackTrace();}
	}
*/
/*	public void writeDealerDegree(){
		Iterator dealerItr = mainContext.getObjects(Dealer.class).iterator();
		Network transactionnetwork = (Network) mainContext.getProjection(Settings.transactionnetwork);
		System.out.println("-------------At END--------");
		for(Dealer d; dealerItr.hasNext(); ){
			d= (Dealer) dealerItr.next();
			System.out.println(d.getPersonID() + " , " + transactionnetwork.getDegree(d) +  " , " + 
					d.getSalesToday() + " , " + d.getDealsToday() +  " , " + d.getUnitsToSell()+ " , " + d.getDealerType() );
		}
		
	}
*//*	public static int getNumOfOld(){
		Query<Dealer> dealerQuery = new PropertyEquals<Dealer>(mainContext, "dealerType" , Settings.DealerType.Old);
		

	}
*/	
	public static double getTickCount() {
		return currentTick;
	}

	private void makeControlPanel() {
		JPanel jPanel = new JPanel();
		jPanel.setBorder(new TitledBorder("Parameter Slider Panel"));
		jPanel.setLayout(new GridLayout(6,2,15,0));//6 rows, 2 cols,
		JSlider sliderR;
		sliderR = setSlider(0, 100, 30, 10, 5);                
		jPanel.add(new JLabel("Share Deal percentage"));        
		jPanel.add(sliderR); 

		RSApplication.getRSApplicationInstance().addCustomUserPanel(jPanel);		
	}

	public JSlider setSlider(
			int min,  // Slider minimum value
			int max,  // Slider maximum value
			int init, // Slider initial value
			int mjrTkSp, // Major tick spacing
			int mnrTkSp) // Minor tick spacing
	{
		JSlider slider;
		slider = new JSlider(JSlider.HORIZONTAL, min, max, init);
		slider.setPaintTicks( true );
		slider.setMajorTickSpacing( mjrTkSp );
		slider.setMinorTickSpacing( mnrTkSp );
		slider.setPaintLabels( true );
		slider.addChangeListener(new SliderListener());
		return slider;
	}
}

/* Code not working properly*/ 
class SliderListener implements ChangeListener {
	public void stateChanged(ChangeEvent e) {
		/*		JSlider source = (JSlider)e.getSource();
		if (!source.getValueIsAdjusting()) {
			int fps = (int)source.getValue();
			Settings.CustomerParams.MinshareDealProb = (double)fps/100.0;
			Settings.CustomerParams.MaxshareDealProb = (double)fps/100.0;
			System.out.println(Settings.CustomerParams.MaxshareDealProb);
		}*/    
	}
}