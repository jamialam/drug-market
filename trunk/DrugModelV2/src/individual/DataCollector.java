package individual;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;



import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.space.graph.RepastPajekEdgeTransformer;
import repast.simphony.space.graph.RepastPajekVertexTransformer;

import drugmodel.DealEdgeTransformer;
import drugmodel.Settings;
import drugmodel.Settings.Endorsement;
import drugmodel.Transaction;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.PajekNetWriter;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class DataCollector {
	private ArrayList<Double> unitsSold; 
	private ArrayList<Transaction> deals ; 
	private DelegateForest<Integer, Transaction> myGraph = null;

	public DataCollector(ArrayList<Transaction> inital_deals){
		unitsSold = new ArrayList<Double>();
		deals = new ArrayList<Transaction>(inital_deals);
		myGraph = new DelegateForest<Integer,Transaction>();

		for(Transaction d : inital_deals){
	//		System.out.println("Adding root: "  + d.getID());
			DelegateTree<Integer, Transaction> tree = new DelegateTree<Integer, Transaction>();
			tree.setRoot(new Integer(d.getTransactionID()));
			myGraph.addTree(tree);


		}
	//	System.out.println("Graph roots: " + myGraph.getRoots().size());
	/*	for (Integer root : myGraph.getRoots()) {
	//		System.out.println("root: " + root);
		}
	*/}

	@Watch( watcheeClassName = "individual.Customer",
			watcheeFieldNames = "lastTransaction",
			query = "colocated",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE,
			scheduleTriggerDelta = 1, scheduleTriggerPriority = 1
			//			triggerCondition = "$watchee.isIssueRaised() == true"
	)
	public void recordTransaction(individual.Customer customer){
		Transaction t = customer.getLastTransaction();
		if(t.getWhoseDealUsed() != t.getCustomerID())
			myGraph.addEdge(t,t.getDealUsed(),t.getTransactionID());
		else
			myGraph.addVertex(t.getTransactionID());

		deals.add(customer.getLastTransaction());
	//	System.out.println("deal used: " + customer.getLastTransaction().getDealUsed() + " transactionID : "+ customer.getLastTransaction().getID()); 
	//	System.out.println("parent: "+t.getDealUsed() + " child:" + t.getID() );


	}


	public void save() throws IOException {
		PajekNetWriter<Integer, Transaction> netwriter = new PajekNetWriter<Integer, Transaction>();
		netwriter.save(myGraph, "network.net",new ToStringLabeller<Integer>(),
				new DealEdgeTransformer());
	}

	public void saveInPajek() throws IOException {
		String fname = "networkDec.net";
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fname)));		
		HashMap<Integer, Boolean> vertices = new HashMap<Integer, Boolean>();

		for (Integer deal : myGraph.getVertices()) {
			vertices.put(deal, true);
		}

		HashMap<Integer, Integer> verticesP = new HashMap<Integer, Integer>();
		int counter = 1;
		writer.write("*Vertices " + vertices.size());
		writer.newLine();
		for (Iterator<Integer> it = vertices.keySet().iterator(); it.hasNext(); ){
			Integer id = it.next();
			Collection<Transaction> childTs = myGraph.getOutEdges(id);
			int goodEndorsed = 0;
			int badEndorsed = 0;
			for(Transaction childT : childTs){
				if(childT.getEndorsement() == Endorsement.Good)
					goodEndorsed++;
				else if(childT.getEndorsement() == Endorsement.Bad)
					badEndorsed++;
			}
			String color = "Red bc Black";
			if( badEndorsed  > 0)
				color = "Yellow bc Black";
			writer.write("" + counter + " d-" + id.intValue() + " 0.0 0.0 0.0 ic " + color);
			//			writer.write("" + counter);
			writer.newLine();
			verticesP.put(id, new Integer(counter));
			counter++;
		}

		Collection<Transaction> d_set = new HashSet<Transaction>();
		boolean directed = myGraph instanceof DirectedGraph;
		// If it is strictly one or the other, no need to create extra sets
		if (directed) {
			d_set.addAll(myGraph.getEdges());
		}
		if (!d_set.isEmpty()) {
			writer.write("*Arcs");
			writer.newLine();
		}

		for (Transaction e : d_set) {
			int source_id = verticesP.get(e.getDealUsed());
			int target_id = verticesP.get(e.getTransactionID()); 
			double time = ((double)(e.getTime())) / (double ) Settings.StepsInDay;
			int	day = (int) Math.ceil(time); 
	
			if(e.getWhoseDealUsed() == e.getCustomerID() )
				writer.write(source_id + " " + target_id + " " + day + " c Black" );
			else
				writer.write(source_id + " " + target_id + " " + day + " c Green" );

			writer.newLine();				
		}
	writer.flush();
	writer.close();		
}
public String getTransactionHistory(){
	String history = "deal_used , transactionId, thisID , dealerID , endorsement,time \n"; 

	for(Transaction t : deals ){
		history += t.getDealUsed() +  "," + t.getTransactionID() +  ","+ t.getDealer().getPersonID()+","  +t.getCustomerID() +  "," +t.getEndorsement() +  "," + t.getTime() + "\n";
	}
	return history;
}

/*	public String returnUnitsSold(){
		Context context = ContextUtils.getContext(this);
		for (int index  = 0 ; index < 5000; index++){
			unitsSold.add(index, (double) -1);
		}
		int max_index = -1;
		Iterator dealers = context.getObjects(Dealer.class).iterator();
		while(dealers.hasNext()){
			Dealer cur_dealer = (Dealer) dealers.next();
			unitsSold.set(cur_dealer.personID - Settings.initCustomers , cur_dealer.getUnitsToSell());
			if(max_index < cur_dealer.personID )
				max_index = cur_dealer.personID;
		}
		if(ContextCreator.getTickCount() == 1){
			String title = "D0"; 
			for (int index  = 1 ; index <  5000; index++){
				title += (" , D" + index); 
			}
			return title;
		}
		int length  =unitsSold.subList(0, max_index - Settings.initCustomers).toString().length();
		return unitsSold.subList(0, max_index - Settings.initCustomers).toString().substring(1, length-1);
	}
 */}
