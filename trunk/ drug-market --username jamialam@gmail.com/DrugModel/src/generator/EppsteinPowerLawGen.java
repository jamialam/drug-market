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
package generator;


import individual.Person;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import drugmodel.SNEdge;

import repast.simphony.context.Context;

import edu.uci.ics.jung.algorithms.generators.GraphGenerator;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;

/**
 * Graph generator that generates undirected graphs with power-law degree distributions.
 * @author Scott White
 * @see "A Steady State Model for Graph Power Law by David Eppstein and Joseph Wang"
 */
public class EppsteinPowerLawGen<V,E> implements GraphGenerator<V,E> {
	private int mNumVertices;
	private int mNumEdges;
	private long mNumIterations;
	private double mMaxDegree;
	private Random mRandom;
	private boolean directed;
	@SuppressWarnings("unchecked")
	private Context context;
	
	/**
	 * Creates an instance with the specified factories and specifications.
	 * @param graphFactory the factory to use to generate the graph
	 * @param vertexFactory the factory to use to create vertices
	 * @param edgeFactory the factory to use to create edges
	 * @param numVertices the number of vertices for the generated graph
	 * @param numEdges the number of edges the generated graph will have, should be Theta(numVertices)
	 * @param r the number of iterations to use; the larger the value the better the graph's degree
	 * distribution will approximate a power-law
	 */
	@SuppressWarnings("unchecked")
	public EppsteinPowerLawGen(Context _context, int numVertices, int numEdges, long r, boolean isDirected) {
		this.context = _context;
		this.mNumVertices = numVertices;
		this.mNumEdges = numEdges;
		this.mNumIterations = r;
		this.mRandom = new Random();
		this.directed = isDirected;;
	}

	@SuppressWarnings("unchecked")
	protected Graph<Person,SNEdge<Person>> initializeGraph() {
		Graph<Person,SNEdge<Person>> graph = null;
		graph = new DirectedSparseGraph<Person, SNEdge<Person>>();
		Iterator itr = context.getObjects(Person.class).iterator();
		while (itr.hasNext()) {
			Person person = (Person) itr.next();
        	graph.addVertex(person);
		}
/*        for(int i=0; i<mNumVertices; i++) {
        	Person person = new Person();
        	graph.addVertex(person);
        }*/
        List<Person> vertices = new ArrayList<Person>(graph.getVertices());
        while (graph.getEdgeCount() < mNumEdges) {
            Person u = vertices.get((int) (mRandom.nextDouble() * mNumVertices));
            Person v = vertices.get((int) (mRandom.nextDouble() * mNumVertices));
            if (!graph.isSuccessor(v,u)) {
            	graph.addEdge(new SNEdge<Person>(u, v, directed), u, v);
            }
        }
        double maxDegree = 0;
        for (Person v : graph.getVertices()) {
            maxDegree = Math.max(graph.degree(v),maxDegree);
        }
        mMaxDegree = maxDegree; //(maxDegree+1)*(maxDegree)/2;
		return graph;
	}

	/**
	 * Generates a graph whose degree distribution approximates a power-law.
	 * @return the generated graph
	 */
	@SuppressWarnings("unchecked")
	public Graph create() {
		Graph<Person,SNEdge<Person>> graph = initializeGraph();
		List<Person> vertices = new ArrayList<Person>(graph.getVertices());
		for (long rIdx = 0; rIdx < mNumIterations; rIdx++) {
			Person v = null;
			int degree = 0;
			do {
				v = vertices.get((int) (mRandom.nextDouble() * mNumVertices));
				degree = graph.degree(v);
			} while (degree == 0);
			List<SNEdge<Person>> edges = new ArrayList<SNEdge<Person>>(graph.getIncidentEdges(v));
			SNEdge<Person> randomExistingEdge = edges.get((int) (mRandom.nextDouble()*degree));
			// FIXME: look at email thread on a more efficient RNG for arbitrary distributions
			Person x = vertices.get((int) (mRandom.nextDouble() * mNumVertices));
			Person y = null;
			do {
				y = vertices.get((int) (mRandom.nextDouble() * mNumVertices));

			} while (mRandom.nextDouble() > ((graph.degree(y)+1)/mMaxDegree));
			if (!graph.isSuccessor(y,x) && x != y) {
				graph.removeEdge(randomExistingEdge);
				graph.addEdge(new SNEdge<Person>(x, y, directed), x, y);
			}
		}
		return graph;
	}

	/**
	 * Sets the seed for the random number generator.
	 * @param seed input to the random number generator.
	 */
	public void setSeed(long seed) {
		mRandom.setSeed(seed);
	}
}