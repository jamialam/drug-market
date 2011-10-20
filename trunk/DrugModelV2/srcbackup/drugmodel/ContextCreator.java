package drugmodel;

/**
 * @author shah
 */

import individual.Customer;
import individual.DataCollector;
import individual.Dealer;

import java.awt.GridLayout;
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Context build(Context<Object> context) {
		context.setId("drugmodel");
		ContextCreator.mainContext = context;

		/*		GeographyParameters<Object> geoparams = new GeographyParameters<Object>();
		GeographyFactory factory = GeographyFactoryFinder.createGeographyFactory(null);
		Geography neighbourhood = factory.createGeography("city", context, geoparams);
		GeometryFactory fac = new GeometryFactory();*/ 

		// We first create customers 
		for (int i=0; i<Settings.initCustomers; i++) {
			Customer customer = new Customer(context);
			context.add(customer);			
			/*			Coordinate coord = new Coordinate(Uniform.staticNextIntFromTo(0,Settings.maxCoordinate),Uniform.staticNextIntFromTo(0,Settings.maxCoordinate));
			Point geom = fac.createPoint(coord); 
			neighbourhood.move(customer, geom);*/ 
		}

		//First embed all the customer agents in to a network. 
		@SuppressWarnings("unused")
		Network socialnetwork = Generator.returnNetwork(context, Settings.generator_type);
	/*	Iterator itr = context.getObjects(Customer.class).iterator();
		while(itr.hasNext()){
			Customer cus = (Customer) itr.next();
			System.out.println("personID: " + cus.getPersonID() + "  num of link : " + socialnetwork.getDegree(cus) +"  num of in link : " + socialnetwork.getInDegree(cus) +"  num of out link : " + socialnetwork.getOutDegree(cus)) ;
		}
	*/	for (int i=0; i<Settings.initDealers; i++) {
			Dealer dealer = new Dealer();
			context.add(dealer);
			/*			Coordinate coord = new Coordinate(Uniform.staticNextIntFromTo(0,Settings.maxCoordinate),Uniform.staticNextIntFromTo(0,Settings.maxCoordinate));
			Point geom = fac.createPoint(coord); 
			neighbourhood.move(dealer, geom);*/
		}

		//Now generate the customer-dealer network - Unconnected and Undirected.  
		Network transactionnetwork = NetworkFactoryFinder.createNetworkFactory(null).createNetwork(
				Settings.transactionnetwork, context, false, new TransactionEdgeCreator());

		//Now assign to each customer, some dealers from a range of [minDealers, maxDealers] (default:[1,3]) 
		Iterator custItr = context.getObjects(Customer.class).iterator();
		while(custItr.hasNext()) {
			Customer customer = (Customer) custItr.next(); 
			do {
				Dealer dealer = (Dealer)(context.getRandomObjects(Dealer.class, 1).iterator().next());
				if (transactionnetwork.getEdge(customer, dealer) == null) {
					//new code added 
					TransactionEdge edge = new TransactionEdge(customer, dealer, false);
					Transaction transaction = new Transaction(dealer, customer.getPersonID(), (int) currentTick,(Settings.pricePerGram / Settings.unitsPerGram ) , Settings.unitsPerGram,Endorsement.None);
					edge.addTransaction(transaction);
					transactionnetwork.addEdge(edge);
					
				}
			} while(transactionnetwork.getDegree(customer) < customer.getInitKnownDealers());
			if (Settings.errorLog) {
				System.out.println("Customer: " + customer.getPersonID() + " has limit: " + customer.getInitKnownDealers()
						+ " and has degree: " + transactionnetwork.getDegree(customer));
			}

		}

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		///////////////////////////////WHY priority is 10??
		ScheduleParameters params = ScheduleParameters.createRepeating(1,1,10);
		schedule.schedule(params, this, "updateTickCount");

		// If running in batch mode, schedule the simulation to stop time
		if(RunEnvironment.getInstance().isBatch()){
			RunEnvironment.getInstance().endAt(Settings.endTime);		
		}				
		if(verifyNetwork())
			System.out.println("network verification sucessfull.");
		context.add(new DataCollector());
		makeControlPanel();
		
		return context;
	}
	public static boolean verifyNetwork(){
		Iterator customerItr = mainContext.getObjects(Customer.class).iterator();
		Network socialNetwork = (Network) (mainContext.getProjection(Settings.socialnetwork));
		
		Iterator links;
		Customer _this, link;
		while(customerItr.hasNext()){
			_this = (Customer) customerItr.next();
			links = socialNetwork.getAdjacent(_this).iterator();
			while(links.hasNext()){
				link = (Customer) links.next();
				if(!socialNetwork.isPredecessor(_this,link) || !socialNetwork.isSuccessor(_this, link))
					return false;
			}
		}
		return true;
	}
	@SuppressWarnings("rawtypes")
	public static Dealer getDealer(int dealerID) {
		Iterator itr = mainContext.getObjects(Dealer.class).iterator();
		Dealer dealer = null;
		while (itr.hasNext()) {
			dealer = (Dealer) itr.next();
			if (dealer.getPersonID() == dealerID) {
				break;
			}
		}
		if (Settings.errorLog) {
			if (dealer == null) {
				System.err.println("Dealer null. Dealer ID called: " + dealerID); 
			}
		}
		return dealer;
	}

	public void updateTickCount() {
		currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}

	public static double getTickCount() {
		return currentTick;
	}
	private void makeControlPanel()
	{
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
class SliderListener implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if (!source.getValueIsAdjusting()) {
            int fps = (int)source.getValue();
            Settings.CustomerParams.minshareDealProb = (double)fps/100.0;
            Settings.CustomerParams.maxshareDealProb = (double)fps/100.0;
            System.out.println(Settings.CustomerParams.maxshareDealProb);
        }    
    }
}