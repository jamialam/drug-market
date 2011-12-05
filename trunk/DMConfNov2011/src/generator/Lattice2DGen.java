/*
 * This is an adaptation from the Jung Project code and is duly acknowledged. See the licence and Jung below. 
 */
/*
 * Copyright (c) 2009, the JUNG Project and the Regents of the University 
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

import drugmodel.SNEdge;
import drugmodel.Settings;

import repast.simphony.context.Context;

import edu.uci.ics.jung.algorithms.generators.GraphGenerator;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;

/**
 * Simple generator of an m x n lattice where each vertex
 * is incident with each of its neighbors (to the left, right, up, and down).
 * May be toroidal, in which case the vertices on the edges are connected to
 * their counterparts on the opposite edges as well.
 * 
 * <p>If the graph factory supplied has a default edge type of {@code EdgeType.DIRECTED},
 * then edges will be created in both directions between adjacent vertices.
 * 
 * @author Joshua O'Madadhain
 */
public class Lattice2DGen<V, E> implements GraphGenerator<V, E> {
	protected int row_count;
	protected int col_count;
	protected boolean is_toroidal;
	protected boolean is_directed;
	private List<Person> v_array;
	@SuppressWarnings("rawtypes")
	private Context context;


	/**
	 * Constructs a generator of square lattices of size {@code latticeSize} 
	 * with the specified parameters.
	 * 
	 * @param graph_factory used to create the {@code Graph} for the lattice
	 * @param vertex_factory used to create the lattice vertices
	 * @param edge_factory used to create the lattice edges
	 * @param latticeSize the number of rows and columns of the lattice
	 * @param isToroidal if true, the created lattice wraps from top to bottom and left to right
	 */
	@SuppressWarnings("rawtypes")
	public Lattice2DGen(Context _context, int latticeSize, boolean isToroidal, boolean isDirected) {
		this(_context, latticeSize, latticeSize, isToroidal, isDirected);
	}

	/**
	 * Creates a generator of {@code row_count} x {@code col_count} lattices 
	 * with the specified parameters.
	 * 
	 * @param graph_factory used to create the {@code Graph} for the lattice
	 * @param vertex_factory used to create the lattice vertices
	 * @param edge_factory used to create the lattice edges
	 * @param row_count the number of rows in the lattice
	 * @param col_count the number of columns in the lattice
	 * @param isToroidal if true, the created lattice wraps from top to bottom and left to right
	 */
	@SuppressWarnings("rawtypes")
	public Lattice2DGen(Context _context, int row_count, int col_count, boolean isToroidal, boolean isDirected)
	{
		if (row_count < 2 || col_count < 2)
		{
			throw new IllegalArgumentException("Row and column counts must each be at least 2.");
		}
		this.context = _context;
		this.row_count = row_count;
		this.col_count = col_count;
		this.is_toroidal = isToroidal;
		this.is_directed = isDirected;
	}

	/**
	 * @see edu.uci.ics.jung.algorithms.generators.GraphGenerator#create()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes"})
	public Graph create()
	{
		int vertex_count = row_count * col_count;
		v_array = new ArrayList<Person>(vertex_count);
		if (vertex_count != Settings.InitCustomers) {
			System.err.println("Error creating Lattice2D grid. Initpopulation not a perfect square.");
			System.exit(1);
		}		
		Graph<Person,SNEdge> graph = new DirectedSparseGraph<Person, SNEdge>();
		int count = 0;
		Iterator itr = context.getObjects(Person.class).iterator();		
		while (itr.hasNext()) {
			Person person = (Person) itr.next();
        	graph.addVertex(person);
        	v_array.add(count, person);
        	count++;
		}		
		
/*		for (int i = 0; i < vertex_count; i++)
		{
			Person v = new Person();
			graph.addVertex(v);
			v_array.add(i, v);
		}*/

		int start = is_toroidal ? 0 : 1;
		int end_row = is_toroidal ? row_count : row_count - 1;
		int end_col = is_toroidal ? col_count : col_count - 1;

		// fill in edges
		// down
		for (int i = 0; i < end_row; i++)
			for (int j = 0; j < col_count; j++)
				graph.addEdge(new SNEdge<Person>((Person)getVertex(i,j), (Person)getVertex(i+1, j), is_directed)
						, (Person)getVertex(i,j), (Person)getVertex(i+1, j));
		// right
		for (int i = 0; i < row_count; i++)
			for (int j = 0; j < end_col; j++)
				graph.addEdge(new SNEdge<Person>((Person)getVertex(i,j), (Person)getVertex(i, j+1), is_directed)
						, (Person)getVertex(i,j), (Person)getVertex(i, j+1));

		// if the graph is directed, fill in the edges going the other direction...
		if (is_directed == true) {
			// up
			for (int i = start; i < row_count; i++)
				for (int j = 0; j < col_count; j++)
	            	graph.addEdge(new SNEdge<Person>((Person)getVertex(i,j), (Person)getVertex(i-1, j), is_directed)
	                		, (Person)getVertex(i,j), (Person)getVertex(i-1, j));
			// left
			for (int i = 0; i < row_count; i++)
				for (int j = start; j < col_count; j++)
	            	graph.addEdge(new SNEdge<Person>((Person)getVertex(i,j), (Person)getVertex(i, j-1), is_directed)
	                		, (Person)getVertex(i,j), (Person)getVertex(i, j-1));
		}

		return graph;
	}

	/**
	 * Returns the number of edges found in a lattice of this generator's specifications.
	 * (This is useful for subclasses that may modify the generated graphs to add more edges.)
	 */
	public int getGridEdgeCount()
	{
		int boundary_adjustment = (is_toroidal ? 0 : 1);
		int vertical_edge_count = col_count * (row_count - boundary_adjustment);
		int horizontal_edge_count = row_count * (col_count - boundary_adjustment);

		return (vertical_edge_count + horizontal_edge_count) * (is_directed ? 2 : 1);
	}

	protected int getIndex(int i, int j)
	{
		return ((mod(i, row_count)) * col_count) + (mod(j, col_count));
	}

	protected int mod(int i, int modulus) 
	{
		int i_mod = i % modulus;
		return i_mod >= 0 ? i_mod : i_mod + modulus;
	}

	/**
	 * Returns the vertex at position ({@code i mod row_count, j mod col_count}).
	 */
	protected Person getVertex(int i, int j)
	{
		return v_array.get(getIndex(i, j));
	}

	/**
	 * Returns the {@code i}th vertex (counting row-wise).
	 */
	protected Person getVertex(int i)
	{
		return v_array.get(i);
	}

	/**
	 * Returns the row in which vertex {@code i} is found.
	 */
	protected int getRow(int i)
	{
		return i / row_count;
	}

	/**
	 * Returns the column in which vertex {@code i} is found.
	 */
	protected int getCol(int i)
	{
		return i % col_count;
	}
}