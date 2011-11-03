package drugmodel;

import individual.Customer;
import individual.DataCollector;
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

import drugmodel.Settings.Endorsement;

import generator.Generator;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.space.graph.Network;
import repast.simphony.ui.RSApplication;

public class ContextCreator implements ContextBuilder<Object> {
	private static double currentTick = -1;
	public static Context<Object> mainContext;
	public static DataCollector dataCollector = null;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Context build(Context<Object> context) {	
		context.setId("drugmodel");
		ContextCreator.mainContext = context;
		Person.lastID = -1;
		currentTick = -1;
		Transaction.lastID = -1;
		dataCollector = null;

		/*		Parameters p = RunEnvironment.getInstance().getParameters();

		Settings.initCustomers = (Integer)p.getValue("initcustomerpopulation");
		Settings.initDealers = (Integer)p.getValue("initdealerpopulation");
		Settings.DealersParams.surplusLimit = ((Integer)p.getValue("surpluslimit")* Settings.unitsPerGram );
		Settings.DealersParams.maxDealsLimit = ((Integer)p.getValue("maxdeals")*1.0);

		System.out.println("init customer: " +Settings.initCustomers);
		System.out.println("init dealer: " +Settings.initDealers);
		System.out.println("surplus limit: " +Settings.DealersParams.surplusLimit);
		System.out.println("max deals limit: " +Settings.DealersParams.maxDealsLimit);*/

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

		/** Now assign to each customer, some dealers from a range of [minDealers, maxDealers] (default:[1,3]) */ 
		Iterator custItr = context.getObjects(Customer.class).iterator();
		ArrayList<Transaction> deals = new ArrayList<Transaction>(); 
		while(custItr.hasNext()) {
			Customer customer = (Customer) custItr.next(); 
			do {
				Dealer dealer = (Dealer)(context.getRandomObjects(Dealer.class, 1).iterator().next());
				if (transactionnetwork.getEdge(customer, dealer) == null) { 
					TransactionEdge edge = new TransactionEdge(customer, dealer, false);
					// WARNING: explicitly setting time to -1
					Transaction transaction = new Transaction(dealer, customer.getPersonID(), 
							(int) -1 ,(Settings.PricePerGram / Settings.UnitsPerGram ), Settings.UnitsPerGram, 
							Endorsement.None, -1, -1);
					deals.add(transaction);
					edge.addTransaction(transaction);
					transactionnetwork.addEdge(edge);
				}
			} while(transactionnetwork.getDegree(customer) < customer.getInitKnownDealers());			
			if (Settings.outputLog) {
				System.out.println("Customer: " + customer.getPersonID() + " has limit: " + customer.getInitKnownDealers()
						+ " and has degree: " + transactionnetwork.getDegree(customer));
			}

		}

		/** Network verification*/
		if(Settings.errorLog) {
			System.out.println("network verification: " + verifyNetwork());
		}

		if(RunEnvironment.getInstance().isBatch() == false) {
			makeControlPanel();
		}
		else {
			RunEnvironment.getInstance().endAt(Settings.endTime);		
		}

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		/* Priority set to 10 to make sure it's always executed first. */
		ScheduleParameters params = ScheduleParameters.createRepeating(1,1,10);
		schedule.schedule(params, this, "updateTickCount");

		/*		dataCollector = new DataCollector(deals);
		context.add(dataCollector);
		ScheduleParameters params1 = ScheduleParameters.createAtEnd(1);		
		schedule.schedule(params1, this, "writeGraph");*/				

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
		if(currentTick % Settings.GarbageTime == 0)
			System.gc();
	}

	public void writeGraph(){
		try {
			dataCollector.save();
			dataCollector.saveInPajek();
		} catch (IOException e) {e.printStackTrace();}
	}

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