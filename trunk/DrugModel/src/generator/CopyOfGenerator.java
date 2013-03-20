package generator;

/**
 * @author shah
 */

import individual.Person;

import java.util.HashSet;

import com.sun.media.jai.opimage.MaxCRIF;

import drugmodel.SNEdge;
import drugmodel.SNEdgeCreator;
import drugmodel.Settings;


import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.context.space.graph.NetworkGenerator;
import repast.simphony.context.space.graph.RandomDensityGenerator;
import repast.simphony.context.space.graph.WattsBetaSmallWorldGenerator;
import repast.simphony.space.graph.Network;

public class CopyOfGenerator extends Settings {
	static final boolean selfLoops = false;
	static boolean symmetrical = true;
	static boolean directed = Settings.SocialNetworkParam.DIRECTED;
	static double clusteringCoefficient = Settings.SocialNetworkParam.CLUSTERING_COEFFICIENT;
	static int degree = Settings.SocialNetworkParam.DEGREE;
	static double rewireProb = SocialNetworkParam.REWIRE_PROB;
	static int edgesToAttach = Settings.SocialNetworkParam.EDGES_TO_ATTACH;
	static String name = Settings.SocialNetworkParam.NAME;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Network returnNetwork(Context context, GeneratorType genType) {
		Network network = null;
		switch (genType) {
		case Unconnected:
			network = (ContextJungNetwork)NetworkFactoryFinder.createNetworkFactory(null).createNetwork(name, context, directed, new SNEdgeCreator());
			break;
		case BarabasiAlbert:			
			network = returnBarabasiAlbertNetwork(context);
			break;
		case KleinbergSmallWorld:
			network = returnKlienbergSmallWorldNetwork(context);
			break;
		case WattsSmallWorld: 
			network = returnWattsStrogatzSW(context);
			break;
		case ErdosRenyiRandom:
			network = returnErdosRenyiRandom(context);
			break;
		case EppsteinPowerLaw:
			network = returnEppsteinPowerLaw(context);
			break;
		default:
			System.err.println("Wrong generator type sent: " + genType.toString());
		System.exit(1);
		break;
		}
		return network;
	}

	@SuppressWarnings({ "rawtypes"})
	private static Network returnBarabasiAlbertNetwork(Context context) {
		BarabasiAlbertGen generator = new BarabasiAlbertGen(name, context, Settings.InitCustomers, edgesToAttach, directed);
		Network acquaintanceNetwork = generator.createNetwork();
		double avgIndegree = 0; double indegree = 0; double maxInDegree = 0; double minInDegree = Double.MAX_VALUE;
		double avgOutdegree = 0; double outdegree = 0; double maxOutDegree = 0; double minOutDegree = Double.MAX_VALUE;
		
		for (Object vertex : acquaintanceNetwork.getNodes()) {
			Person person = (Person) vertex;
			indegree = acquaintanceNetwork.getInDegree(vertex);
			outdegree = acquaintanceNetwork.getOutDegree(vertex);
			avgIndegree += indegree;
			avgOutdegree += outdegree;
			
			if (indegree > maxInDegree) {
				maxInDegree = indegree;				
			}
			if (indegree < minInDegree) {
				minInDegree = indegree;
			}
			
			if (outdegree > maxOutDegree) {
				maxOutDegree = outdegree;				
			}
			if (outdegree < minOutDegree) {
				minOutDegree = outdegree;
			}

		}
		System.out.println("Degree: : " + acquaintanceNetwork.getDegree() + " directed: " + acquaintanceNetwork.isDirected() 
				+ " size: " + acquaintanceNetwork.size() + " numEdges: " + acquaintanceNetwork.numEdges()
				+ " avgInDegree: " + avgIndegree + " minInDegree: " + minInDegree + " maxInDegree: " + maxInDegree
				+ " avgInDegree: " + avgOutdegree + " minInDegree: " + minOutDegree + " maxOutDegree: " + maxOutDegree);		
		return acquaintanceNetwork;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Network returnKlienbergSmallWorldNetwork(Context context) {
		int latticeSize = (int)(Math.ceil(Math.sqrt((double)Settings.InitCustomers)));
		ContextJungNetwork acquaintanceNetwork = (ContextJungNetwork)NetworkFactoryFinder.createNetworkFactory(null).createNetwork(name, context, directed, new SNEdgeCreator());
		KleinbergSmallWorldGen<Person, SNEdge<Person>> generator 
			= new KleinbergSmallWorldGen<Person, SNEdge<Person>>(context, latticeSize, clusteringCoefficient, directed);
		acquaintanceNetwork.setGraph(generator.create());				
		return acquaintanceNetwork;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Network returnEppsteinPowerLaw(Context context) {
//		long iterations = 100000;
		long iterations = 1000;
		ContextJungNetwork acquaintanceNetwork = (ContextJungNetwork)NetworkFactoryFinder.createNetworkFactory(null).createNetwork(name, context, directed, new SNEdgeCreator());
		double edges = (Settings.InitCustomers*degree)/2;
		EppsteinPowerLawGen<Person, SNEdge<Person>> generator 	
			= new EppsteinPowerLawGen<Person, SNEdge<Person>>(context, Settings.InitCustomers, (int)edges, iterations, directed); 	
		acquaintanceNetwork.setGraph(generator.create());
		return acquaintanceNetwork;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Network returnWattsStrogatzSW(Context context) {
		NetworkGenerator gen = new WattsBetaSmallWorldGenerator (rewireProb, degree, symmetrical);	
		NetworkBuilder builder = new NetworkBuilder(SocialNetworkParam.NAME, context, directed);
		builder.setEdgeCreator(new SNEdgeCreator());
		builder.setGenerator(gen);
		Network acquaintanceNetwork = builder.buildNetwork();		
		return acquaintanceNetwork;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Network returnErdosRenyiRandom(Context context) {
		double n = (double)(Settings.InitCustomers);
		double edges = (Settings.InitCustomers*degree)/2;
		double allpos = (n*(n-1))/2;
		double density = edges/allpos;
		NetworkGenerator gen = new RandomDensityGenerator(density, selfLoops, symmetrical);	
		NetworkBuilder builder = new NetworkBuilder(SocialNetworkParam.NAME, context, directed);
		builder.setEdgeCreator(new SNEdgeCreator());
		builder.setGenerator(gen);
		Network acquaintanceNetwork = builder.buildNetwork();		
		return acquaintanceNetwork;
	}
}