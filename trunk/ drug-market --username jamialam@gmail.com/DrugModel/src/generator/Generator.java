package generator;


import individual.Person;

import java.util.HashSet;

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

public class Generator extends Settings {
	static final boolean selfLoops = false;
	static boolean symmetrical = true;
	static boolean directed = Settings.SocaialNetworkParam.DIRECTED;
	static double clusteringCoefficient = Settings.SocaialNetworkParam.CLUSTERING_COEFFICIENT;
	static int degree = Settings.SocaialNetworkParam.DEGREE;
	static double rewireProb = SocaialNetworkParam.REWIRE_PROB;
	static int edgesToAttach = Settings.SocaialNetworkParam.EDGES_TO_ATTACH;
	static String name = Settings.SocaialNetworkParam.NAME;

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

	private static Network returnBarabasiAlbertNetwork(Context context) {
		double propInitialVertices = 0.01;
		int initVertices = (int)(Settings.initCustomers*propInitialVertices);
		ContextJungNetwork acquaintanceNetwork = (ContextJungNetwork)NetworkFactoryFinder.createNetworkFactory(null).createNetwork(name, context, directed, new SNEdgeCreator());
		BarabasiAlbertGen<Person, SNEdge<Person>> generator 
			= new BarabasiAlbertGen<Person, SNEdge<Person>>(context, initVertices, new HashSet<Person>(), edgesToAttach, directed);
		generator.evolveGraph((int)(Settings.initCustomers-initVertices));
		acquaintanceNetwork.setGraph(generator.create());
		return acquaintanceNetwork;
	}

	private static Network returnKlienbergSmallWorldNetwork(Context context) {
		int latticeSize = (int)(Math.ceil(Math.sqrt((double)Settings.initCustomers)));
		ContextJungNetwork acquaintanceNetwork = (ContextJungNetwork)NetworkFactoryFinder.createNetworkFactory(null).createNetwork(name, context, directed, new SNEdgeCreator());
		KleinbergSmallWorldGen<Person, SNEdge<Person>> generator 
			= new KleinbergSmallWorldGen<Person, SNEdge<Person>>(context, latticeSize, clusteringCoefficient, directed);
		acquaintanceNetwork.setGraph(generator.create());				
		return acquaintanceNetwork;
	}

	@SuppressWarnings("unchecked")
	private static Network returnEppsteinPowerLaw(Context context) {
//		long iterations = 100000;
		long iterations = 1000;
		ContextJungNetwork acquaintanceNetwork = (ContextJungNetwork)NetworkFactoryFinder.createNetworkFactory(null).createNetwork(name, context, directed, new SNEdgeCreator());
		double edges = (Settings.initCustomers*degree)/2;
		EppsteinPowerLawGen<Person, SNEdge<Person>> generator 	
			= new EppsteinPowerLawGen<Person, SNEdge<Person>>(context, Settings.initCustomers, (int)edges, iterations, directed); 	
		acquaintanceNetwork.setGraph(generator.create());
		return acquaintanceNetwork;
	}
	
	private static Network returnWattsStrogatzSW(Context context) {
		NetworkGenerator gen = new WattsBetaSmallWorldGenerator (rewireProb, degree, symmetrical);	
		NetworkBuilder builder = new NetworkBuilder(SocaialNetworkParam.NAME, context, directed);
		builder.setEdgeCreator(new SNEdgeCreator());
		builder.setGenerator(gen);
		Network acquaintanceNetwork = builder.buildNetwork();		
		return acquaintanceNetwork;
	}
	
	@SuppressWarnings("unchecked")
	private static Network returnErdosRenyiRandom(Context context) {
		double n = (double)(Settings.initCustomers);
		double edges = (Settings.initCustomers*degree)/2;
		double allpos = (n*(n-1))/2;
		double density = edges/allpos;
		NetworkGenerator gen = new RandomDensityGenerator(density, selfLoops, symmetrical);	
		NetworkBuilder builder = new NetworkBuilder(SocaialNetworkParam.NAME, context, directed);
		builder.setEdgeCreator(new SNEdgeCreator());
		builder.setGenerator(gen);
		Network acquaintanceNetwork = builder.buildNetwork();		
		return acquaintanceNetwork;
	}
}