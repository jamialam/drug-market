package generator;

/**
 * Adapted from W. Giodano and Scott White's test class for BarabasiAlbertGenerator (JUNG) see below. 
 */
/**
 * @author W. Giordano, Scott White
 */

import individual.Customer;
import individual.Person;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.collections15.Factory;

import drugmodel.SNEdge;
import drugmodel.SNEdgeCreator;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.space.graph.Network;

import edu.uci.ics.jung.algorithms.generators.random.BarabasiAlbertGenerator;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;

public class BarabasiAlbertGen {
	int init_vertices = 1;
	int edges_to_add_per_timestep = 3;
	Network network;
	int num_timesteps = 1;
	Context context;
	boolean directed;
	
	public BarabasiAlbertGen(String name, Context context, int totalVertices, int degree, boolean directed) {
		this.context = context;
		network = NetworkFactoryFinder.createNetworkFactory(null).createNetwork(name, context, directed, new SNEdgeCreator());
		num_timesteps = totalVertices - init_vertices;
		edges_to_add_per_timestep = degree;		
		this.directed = directed;
	}
	
	public Network createNetwork() {		
		HashMap<Integer, Person> personMap = new HashMap<Integer, Person>();		
		Iterator itr = context.getAgentLayer(Customer.class).iterator();		
		while(itr.hasNext()) {
			Person person = (Person) itr.next();			
			personMap.put(person.getPersonID(), person);
		} 
		
		Factory<Graph<Integer,Number>> graphFactory =
				new Factory<Graph<Integer,Number>>() {
			public Graph<Integer,Number> create() {
				return new SparseMultigraph<Integer,Number>();
			}
		};
		Factory<Integer> vertexFactory = 
				new Factory<Integer>() {
			int count;
			public Integer create() {
				return count++;
			}};
			Factory<Number> edgeFactory = 
					new Factory<Number>() {
				int count;
				public Number create() {
					return count++;
				}};

				BarabasiAlbertGenerator<Integer,Number> generator = 
						new BarabasiAlbertGenerator<Integer,Number>(graphFactory, vertexFactory, edgeFactory,
								init_vertices,edges_to_add_per_timestep, new HashSet<Integer>());
				generator.evolveGraph(num_timesteps);
				Graph<Integer, Number> graph = generator.create();
				Iterator<Integer> vertexItr = graph.getVertices().iterator();
				try {
					while (vertexItr.hasNext()) {
						Integer vertex = (Integer) vertexItr.next();
//						System.out.println("Vertex: " + vertex + " outdegree: " + graph.outDegree(vertex)
//								//+ graph.getIncidentEdges(vertex).toString()
//								//+ " inedges:" + graph.getInEdges(vertex).toString()
//								//+ "outedges: "+ graph.getOutEdges(vertex).toString());
//								+ " succ: " + graph.getSuccessors(vertex).toString()
//								+ " pred: " + graph.getPredecessors(vertex).toString());
						Person source = personMap.get(vertex);
						Person target = null;
						try {
							for (Integer successor : graph.getSuccessors(vertex)) {
								
								target = personMap.get(successor);
								SNEdge edge = new SNEdge(source, target, directed);
								try {
									network.addEdge(edge);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							
						} catch (Exception e) {
							e.printStackTrace();
						}
							
					} 
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				graph = null;								
				return network;
	}
}