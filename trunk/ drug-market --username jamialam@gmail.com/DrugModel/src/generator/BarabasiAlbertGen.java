package generator;

/*
 * This is an adaptation from the Jung Project code and is duly acknowledged. See the licence and Jung below. 
 */

/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */

import individual.Person;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import drugmodel.SNEdge;

import repast.simphony.context.Context;

import edu.uci.ics.jung.algorithms.generators.EvolvingGraphGenerator;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.MultiGraph;
import edu.uci.ics.jung.graph.util.Pair;


/**
 * <p>Simple evolving scale-free random graph generator. At each time
 * step, a new vertex is created and is connected to existing vertices
 * according to the principle of "preferential attachment", whereby 
 * vertices with higher degree have a higher probability of being 
 * selected for attachment.</p>
 * 
 * <p>At a given timestep, the probability <code>p</code> of creating an edge
 * between an existing vertex <code>v</code> and the newly added vertex is
 * <pre>
 * p = (degree(v) + 1) / (|E| + |V|);
 * </pre>
 * 
 * <p>where <code>|E|</code> and <code>|V|</code> are, respectively, the number 
 * of edges and vertices currently in the network (counting neither the new
 * vertex nor the other edges that are being attached to it).</p>
 * 
 * <p>Note that the formula specified in the original paper
 * (cited below) was
 * <pre>
 * p = degree(v) / |E|
 * </pre>
 * </p>
 * 
 * <p>However, this would have meant that the probability of attachment for any existing
 * isolated vertex would be 0.  This version uses Lagrangian smoothing to give
 * each existing vertex a positive attachment probability.</p>
 * 
 * <p>The graph created may be either directed or undirected (controlled by a constructor
 * parameter); the default is undirected.  
 * If the graph is specified to be directed, then the edges added will be directed
 * from the newly added vertex u to the existing vertex v, with probability proportional to the 
 * indegree of v (number of edges directed towards v).  If the graph is specified to be undirected,
 * then the (undirected) edges added will connect u to v, with probability proportional to the 
 * degree of v.</p> 
 * 
 * <p>The <code>parallel</code> constructor parameter specifies whether parallel edges
 * may be created.</p>
 * 
 * @see "A.-L. Barabasi and R. Albert, Emergence of scaling in random networks, Science 286, 1999."
 * @author Scott White
 * @author Joshua O'Madadhain
 * @author Tom Nelson - adapted to jung2
 */
public class BarabasiAlbertGen<V,E> implements EvolvingGraphGenerator<V,E> {
	private Graph<Person, SNEdge<Person>> myGraph = null;
	private int mNumEdgesToAttachPerStep;
	private int mElapsedTimeSteps;
	private Random mRandom;
	protected List<Person> vertex_list;
	protected int init_vertices;
	protected Map<Person, Integer> index_my_vertex;

	private boolean directed = false;
	@SuppressWarnings("rawtypes")
	private Context context;
	private ArrayList<Person> persons;

	/**
	 * Constructs a new instance of the generator, whose output will be an undirected graph,
	 * and which will use the current time as a seed for the random number generation.
	 * @param init_vertices     number of vertices that the graph should start with
	 * @param numEdgesToAttach the number of edges that should be attached from the
	 * new vertex to pre-existing vertices at each time step
	 */
	@SuppressWarnings("rawtypes")
	public BarabasiAlbertGen(Context _context, int init_vertices, HashSet<Person> seedVertices, int numEdgesToAttach, boolean _directed) {
		assert init_vertices > 0 : "Number of initial unconnected 'seed' vertices " + 
		"must be positive";
		assert numEdgesToAttach > 0 : "Number of edges to attach " +
		"at each time step must be positive";
		mNumEdgesToAttachPerStep = numEdgesToAttach;
		mRandom = new Random();
		this.context = _context;
		this.persons = new ArrayList<Person>(); 
		this.init_vertices = init_vertices;
		this.directed = _directed;
		mNumEdgesToAttachPerStep = numEdgesToAttach;
		myGraph = new DirectedSparseGraph<Person, SNEdge<Person>>();
		initialize(seedVertices);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initialize(HashSet seedVertices) {
		vertex_list = new ArrayList<Person>(2*init_vertices);
		index_my_vertex = new HashMap<Person, Integer>(2*init_vertices);
		
		Iterator itr = context.getObjects(Person.class).iterator();
		while (itr.hasNext()) {
			Person person = (Person) itr.next();
			persons.add(person);
		}
		
		for (int i=0; i<init_vertices; i++) {
			Person person = (Person) persons.get(i);
			myGraph.addVertex(person);
			vertex_list.add(person);
			index_my_vertex.put(person, i);
			seedVertices.add(person);
		}
		
/*		for (int i=0; i<init_vertices; i++) {
			Person person = new Person();
			myGraph.addVertex(person);
			vertex_list.add(person);
			index_my_vertex.put(person, i);
			seedVertices.add(person);
		}
*/		mElapsedTimeSteps = 0;
	}

	public void evolveGraph(int numTimeSteps) {
		for (int i = 0; i < numTimeSteps; i++) {
			evolve(i+init_vertices);
			mElapsedTimeSteps++;
		}
	}

	private void createRandomEdge(Collection<Person> preexistingNodes, Person newPerson, Set<Pair<Person>> added_pairs) {
		Person attach_point;
		boolean created_edge = false;
		Pair<Person> endpoints;
		do {
			attach_point = vertex_list.get(mRandom.nextInt(vertex_list.size()));
			endpoints = new Pair<Person>(newPerson, attach_point);
			// if parallel edges are not allowed, skip attach_point if <newVertex, attach_point>
			// already exists; note that because of the way edges are added, we only need to check
			// the list of candidate edges for duplicates.
			if (!(myGraph instanceof MultiGraph)) {
				if (added_pairs.contains(endpoints)) {
					continue;
				}
				if (directed == false 
						&& added_pairs.contains(new Pair<Person>(attach_point, newPerson))) {
					continue;
				}
			}
			double degree = myGraph.inDegree(attach_point);
			// subtract 1 from numVertices because we don't want to count newVertex
			// (which has already been added to the graph, but not to vertex_index)
			double attach_prob = (degree + 1) / (myGraph.getEdgeCount() + myGraph.getVertexCount() - 1);
			if (attach_prob >= mRandom.nextDouble()) {
				created_edge = true;
			}
		} while (!created_edge);
		added_pairs.add(endpoints);

		if (directed == false) {
			added_pairs.add(new Pair<Person>(attach_point, newPerson));
		}
	}

	private void evolve(int index) {
		Collection<Person> preexistingNodes = myGraph.getVertices();
		//Person newVertex = new Person();
		Person newPerson = (Person) persons.get(index);
		myGraph.addVertex(newPerson);
		// generate and store the new edges; don't add them to the graph
		// yet because we don't want to bias the degree calculations
		// (all new edges in a timestep should be added in parallel)
		Set<Pair<Person>> added_pairs = new HashSet<Pair<Person>>(mNumEdgesToAttachPerStep*3);
		for (int i = 0; i < mNumEdgesToAttachPerStep; i++) {
			createRandomEdge(preexistingNodes, newPerson, added_pairs);
		}
		for (Pair<Person> pair : added_pairs) {
			Person v1 = pair.getFirst();
			Person v2 = pair.getSecond();
			if (directed  == false
					|| !myGraph.isNeighbor(v1, v2)) {
				myGraph.addEdge(new SNEdge<Person>(v1, v2, directed), pair);
			}				
		}
		// now that we're done attaching edges to this new vertex, 
		// add it to the index
		vertex_list.add(newPerson);
		index_my_vertex.put(newPerson, new Integer(vertex_list.size() - 1));
	}

	public int numIterations() {
		return mElapsedTimeSteps;
	}

	@SuppressWarnings("rawtypes")
	public Graph create() {
		return myGraph;
	}

	public boolean isDirected() {
		return directed;
	}

	public void setDirected(boolean directed) {
		this.directed = directed;
	}
}