package drugmodel;


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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Context build(Context<Object> context) {
		context.setId("drugmodel");
		
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
		Network socialnetwork = Generator.returnNetwork(context, Settings.generator_type);

		for (int i=0; i<Settings.initDealers; i++) {
			Dealer dealer = new Dealer();
			context.add(dealer);
			Coordinate coord = new Coordinate(Uniform.staticNextIntFromTo(0,Settings.maxCoordinate),Uniform.staticNextIntFromTo(0,Settings.maxCoordinate));
			Point geom = fac.createPoint(coord); 
			neighbourhood.move(dealer, geom);
		}

		 //Now generate the customer-dealer network - Unconnected. 
		Network transactionnetwork = NetworkFactoryFinder.createNetworkFactory(null).createNetwork("transactionnetwork", context, false, new TransEdgeCreator());

		//Now assign to each customer, some dealers from a range of [minDealers, maxDealers] (default:[1,3]) 
		Iterator custItr = context.getObjects(Customer.class).iterator();
		while(custItr.hasNext()) {
			Customer customer = (Customer) custItr.next(); 
			do {
				Dealer dealer = (Dealer)(context.getRandomObjects(Dealer.class, 1).iterator().next());
				if (transactionnetwork.getEdge(customer, dealer) == null) {
					transactionnetwork.addEdge(new TransEdge(customer, dealer, false));
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

		/*		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters params = ScheduleParameters.createOneTime(1,4);
		schedule.schedule(params, this, "createIssue");

		ISchedule schedule1 = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters params1 = ScheduleParameters.createRepeating(1,1,3);
		schedule1.schedule(params1, this, "meetPeople");

		ISchedule schedule2 = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters params2 = ScheduleParameters.createRepeating(1,7,4);
		schedule2.schedule(params2, this, "turnOver");*/

		/*	ISchedule schedule3 = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters params3 = ScheduleParameters.createRepeating(1,100,1);
		schedule2.schedule(params3, this, "garbageclean");*/
		/*		ISchedule schedule3 = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters params6 = ScheduleParameters.createAtEnd(1);
		schedule3.schedule(params6, this, "writePajek");		
		 */
		
		// If running in batch mode, schedule the simulation to stop time
		if(RunEnvironment.getInstance().isBatch()){
			RunEnvironment.getInstance().endAt(Settings.endTime);		
		}				
		return context;
	}
	
	public void updateTickCount() {
		currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}
	
	public static double getTickCount() {
		return currentTick;
	}

}