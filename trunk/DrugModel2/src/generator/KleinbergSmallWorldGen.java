package generator;

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

import individual.Person;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import drugmodel.SNEdge;

import repast.simphony.context.Context;
import edu.uci.ics.jung.graph.Graph;

/**
 * Graph generator that produces a random graph with small world properties. 
 * The underlying model is an mxn (optionally toroidal) lattice. Each node u 
 * has four local connections, one to each of its neighbors, and
 * in addition 1+ long range connections to some node v where v is chosen randomly according to
 * probability proportional to d^-alpha where d is the lattice distance between u and v and alpha
 * is the clustering exponent.
 * 
 * @see "Navigation in a small world J. Kleinberg, Nature 406(2000), 845."
 * @author Joshua O'Madadhain
 */
public class KleinbergSmallWorldGen<V, E> extends Lattice2DGen<V, E> {
    private double clustering_exponent;
    private Random random;
    private int num_connections = 1;
    
    @SuppressWarnings("rawtypes")
	public KleinbergSmallWorldGen(Context _context, int latticeSize, double clusteringExponent, boolean isDirected) {
        this(_context, latticeSize, latticeSize, clusteringExponent, isDirected);
    }

    /**
     * @param graph_factory
     * @param vertex_factory
     * @param edge_factory
     * @param row_count
     * @param col_count
     * @param clusteringExponent
     */
    @SuppressWarnings({ "rawtypes" })
	public KleinbergSmallWorldGen(Context _context, int row_count, int col_count, double clusteringExponent, boolean isDirected) {
    	super(_context, row_count, col_count, true, isDirected);
        clustering_exponent = clusteringExponent;
        initialize();
    }

    /**
     * @param graph_factory
     * @param vertex_factory
     * @param edge_factory
     * @param row_count
     * @param col_count
     * @param clusteringExponent
     * @param isToroidal
     */
    /*public KleinbergSmallWorldGen(int row_count, int col_count, double clusteringExponent, boolean isToroidal) {
        super(row_count, col_count, isToroidal);
        clustering_exponent = clusteringExponent;
        initialize();
    }*/

    private void initialize() {
        this.random = new Random();
    }
    
    /**
     * Sets the {@code Random} instance used by this instance.  Useful for 
     * unit testing.
     */
    public void setRandom(Random random)
    {
        this.random = random;
    }
    
    /**
     * Sets the seed of the internal random number generator.  May be used to provide repeatable
     * experiments.
     */
    public void setRandomSeed(long seed) 
    {
        random.setSeed(seed);
    }

    /**
     * Sets the number of new 'small-world' connections (outgoing edges) to be added to each vertex.
     */
    public void setConnectionCount(int num_connections)
    {
        if (num_connections <= 0)
        {
            throw new IllegalArgumentException("Number of new connections per vertex must be >= 1");
        }
        this.num_connections = num_connections;
    }

    /**
     * Returns the number of new 'small-world' connections to be made to each vertex.
     */
    public int getConnectionCount()
    {
        return this.num_connections;
    }
    
    /**
     * Generates a random small world network according to the parameters given
     * @return a random small world graph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Graph create() {
        Graph graph = super.create();        
        // TODO: For toroidal graphs, we can make this more clever by pre-creating the WeightedChoice object
        // and using the output as an offset to the current vertex location.
        WeightedChoice<Person> weighted_choice;        
        // Add long range connections
        for (int i = 0; i < graph.getVertexCount(); i++)
        {
            Person source =  (Person)getVertex(i);
            int row = getRow(i);
            int col = getCol(i);
            int row_offset = row < row_count/2 ? -row_count : row_count;
            int col_offset = col < col_count/2 ? -col_count : col_count;

            Map<Person, Float> vertex_weights = new HashMap<Person, Float>();
            for (int j = 0; j < row_count; j++)
            {
                for (int k = 0; k < col_count; k++)
                {
                    if (j == row && k == col)
                        continue;
                    int v_dist = Math.abs(j - row);
                    int h_dist = Math.abs(k - col);
                    if (is_toroidal)
                    {
                        v_dist = Math.min(v_dist, Math.abs(j - row+row_offset));
                        h_dist = Math.min(h_dist, Math.abs(k - col+col_offset));
                    }
                    int distance = v_dist + h_dist;
                    if (distance < 2)
                        continue;
                    else
                        vertex_weights.put(getVertex(j,k), (float)Math.pow(distance, -clustering_exponent));
                }
            }

            for (int j = 0; j < this.num_connections; j++) {
                weighted_choice = new WeightedChoice<Person>(vertex_weights, random);
                Person target = weighted_choice.nextItem();
                graph.addEdge(new SNEdge<Person>(source, target, is_directed), source, target);
            }
        }
        return graph;
    }
}