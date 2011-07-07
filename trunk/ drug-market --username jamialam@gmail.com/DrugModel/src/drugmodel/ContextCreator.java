package drugmodel;

/**
 * @author shah
 */

import individual.Customer;
import individual.Dealer;

import java.util.Iterator;

import generator.Generator;
import cern.jet.random.Uniform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactory;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.graph.Network;

public class ContextCreator implements ContextBuilder<Object> {
	private static double currentTick = -1;
	public static Context<Object> mainContext;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Context build(Context<Object> context) {
		context.setId("drugmodel");
		ContextCreator.mainContext = context;

		GeographyParameters<Object> geoparams = new GeographyParameters<Object>();
		GeographyFactory factory = GeographyFactoryFinder.createGeographyFactory(null);
		Geography neighbourhood = factory.createGeography("city", context, geoparams);
		GeometryFactory fac = new GeometryFactory(); 

		// We first create customers 
		for (int i=0; i<Settings.initCustomers; i++) {
			Customer customer = new Customer();
			context.add(customer);			
			Coordinate coord = new Coordinate(Uniform.staticNextIntFromTo(0,Settings.maxCoordinate),Uniform.staticNextIntFromTo(0,Settings.maxCoordinate));
			Point geom = fac.createPoint(coord); 
			neighbourhood.move(customer, geom); 
		}
		//First embed all the customer agents in to a network. 
		@SuppressWarnings("unused")
		Network socialnetwork = Generator.returnNetwork(context, Settings.generator_type);

		for (int i=0; i<Settings.initDealers; i++) {
			Dealer dealer = new Dealer();
			context.add(dealer);
			Coordinate coord = new Coordinate(Uniform.staticNextIntFromTo(0,Settings.maxCoordinate),Uniform.staticNextIntFromTo(0,Settings.maxCoordinate));
			Point geom = fac.createPoint(coord); 
			neighbourhood.move(dealer, geom);
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
					transactionnetwork.addEdge(new TransactionEdge(customer, dealer, false));
				}
			} while(transactionnetwork.getDegree(customer) < customer.getInitKnownDealers());
			if (Settings.errorLog) {
				System.out.println("Customer: " + customer.getPersonID() + " has limit: " + customer.getInitKnownDealers()
						+ " and has degree: " + transactionnetwork.getDegree(customer));
			}

		}

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters params = ScheduleParameters.createRepeating(1,1,1);
		schedule.schedule(params, this, "updateCurrentTick");

		// If running in batch mode, schedule the simulation to stop time
		if(RunEnvironment.getInstance().isBatch()){
			RunEnvironment.getInstance().endAt(Settings.endTime);		
		}				
		return context;
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
}