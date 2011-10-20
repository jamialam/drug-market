package drugmodel;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.ContextUtils;

import socialnetwork.Settings.CollectiveActionReason;
import socialnetwork.Settings.IssueName;
import socialnetwork.Settings.WEIGHTS_INDEX;

import cern.jet.random.Exponential;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;

public class Person extends Object {
	protected static int lastID = -1;
	protected int PersonID;
	protected double ideology;
	protected double orientation;
	protected HashMap<IssueName,Double> relevance =  new HashMap<IssueName,Double>();
	protected double tolerance;
	protected double[] weights = new double[WEIGHTS_INDEX.SIZE];
	protected CollectiveActionReason reason;

	protected double chance = 0,relevance_weight=0, ideological_bias = 0, media_influence=0, social_pressure=0;
	
	private PrintWriter writer;


	@Watch( watcheeClassName = "socialnetwork.Issue",
			watcheeFieldNames = "issueRaised",
			query = "colocated",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE,
			scheduleTriggerDelta = 1, scheduleTriggerPriority = 4,
			triggerCondition = "$watchee.isIssueRaised() == true"
	)
	public void make_initial_opinion(socialnetwork.Issue watched) {
		this.relevance_weight = getRelevanceWeight(watched);
		
		calc_ideological_bais(watched);
		social_pressure = 0;
		//media is not priming or framing right now. 
		//TODO introduce feedback effect.
		//calc_media_influence(watched);
		calc_AttendProcession(watched);
//		System.out.println("In make initial opinion, chance : "+ chance );

		double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		/*print("-----------------------------------------------------------");
		print("Time: " + currentTick + " In make-initial-opinion().");
		print("Time: " + currentTick + " "  + printProfile());
		print("-----------------------------------------------------------");*/
	}
	
	public void printMeetPeopleHeader(Person person, double influence1, double influence2) {
		if (Settings.INDIVIDUAL_LOG == true) {
			double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			print("-----------------------------------------------------------");
			print("Meeting started.");
			print("Time: " + currentTick + ". Person-" + getPersonID() + " meets Person-"+person.getPersonID());
			print("Person-" + getPersonID() + " has infleunce on Personn-"+person.getPersonID() + " == " + influence2);
			print("Person-" + person.getPersonID() + " has infleunce on Personn-"+getPersonID() + " == " + influence1);
			print("Time: " + currentTick + ". Before exchange opinion.");
			print(printProfile());
			print(person.printProfile());					
		}
	}
	
	public void printMeetPeopleFooter(Person person, Issue issue) {
		if (Settings.INDIVIDUAL_LOG == true) {
			double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			print("Time: " + currentTick +". After exchange opinion.");
			print(printProfile());
			print(person.printProfile());
			print("Time: " + currentTick +". Calculating chances.");
			print("Person-"+getPersonID() + " has chance: " + calc_AttendProcession(issue));
			print("Person-"+person.getPersonID() + " has chance: " + person.calc_AttendProcession(issue));
			print("Time: " + currentTick + ". After updating profile of Person-"+getPersonID() + " and Person-"+person.getPersonID());
			print(printProfile());
			print(person.printProfile());
			print("Meeting Ended.");
			print("-----------------------------------------------------------");					
		}
	}
	
	public void flushLog() {
		if (Settings.INDIVIDUAL_LOG == true) {
			writer.flush();
		}
	}
	
	public void closeLog() {
		if (Settings.INDIVIDUAL_LOG == true) {
			writer.close();
		}
	}
	
	public void print(String str) {
		if (Settings.INDIVIDUAL_LOG == true) {
			writer.println(str);
		}
	}

/*	public void calc_media_influence(Issue issue){
		media_influence = issue.getHotness() * weights[WEIGHTS_INDEX.MEDIA_INFLUENCE];		
	}
*/
	/*
	 * calculate social pressure using last interaction only. ppl have different influence on each other.
	 * Influence weight are used.  		
	*/
	public int getInDegree(){
		Context context = ContextUtils.getContext(this);
		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		return acquaintanceNetwork.getInDegree(this);
	}
	public int getOutnDegree(){
		Context context = ContextUtils.getContext(this);
		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		return acquaintanceNetwork.getOutDegree(this);
	}
	public int getDegree(){
		Context context = ContextUtils.getContext(this);
		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		return acquaintanceNetwork.getDegree(this);
	}
	public int getPositiveRelevantAcq(){
		int p_rel = 0 ; 
		Context context = ContextUtils.getContext(this);
		//ASUMING one issue in context
		Iterator<Object> itr_issues = context.getObjects(Issue.class).iterator();	
		Issue issue = null;
		if(itr_issues.hasNext())
			issue = (Issue)itr_issues.next();
		else
			System.out.println("issue found null.");

		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		Iterator itr = acquaintanceNetwork.getAdjacent(this).iterator();
		while(itr.hasNext()){
			Person p = (Person)itr.next();
			if( p.getRelevanceWeight(issue) > 0 ) 
				++p_rel;
		}
		return p_rel;
		
	}
	public int getNegativeRelevantAcq(){
		int n_rel = 0 ; 
		Context context = ContextUtils.getContext(this);
		//ASUMING one issue in context
		Iterator<Object> itr_issues = context.getObjects(Issue.class).iterator();	
		Issue issue = null;
		if(itr_issues.hasNext())
			issue = (Issue)itr_issues.next();
		else
			System.out.println("issue found null.");

		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		Iterator itr = acquaintanceNetwork.getAdjacent(this).iterator();
		while(itr.hasNext()){
			Person p = (Person)itr.next();
			if( p.getRelevanceWeight(issue) < 0.0 ) 
				++n_rel;
		}
		return n_rel;
		
	}
	
	public void calc_social_pressue(Issue issue){
		Context context = ContextUtils.getContext(this);
		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		Iterator<RepastEdge<Person>> itr_edges = acquaintanceNetwork.getEdges(this).iterator();				

		double influence_weights=0;
		double networkweight = 0.0;		
		double weight = 0.0;
		PersonInteractionData data=null;
		
		while (itr_edges.hasNext()) {
			SNEdge edge = (SNEdge) itr_edges.next();
			data = edge.getLastInteraction();
			//int size = edge.getInteractions().size();
			if (data != null) {
				//data = (PersonInteractionData) edge.getInteractions().get(size-1);
				weight = edge.getInfluenceOn(this);
				if (data.personID_1 == this.PersonID) {
					//alter is personID_2
					networkweight += data.chanceofID2 * weight;
				}
				else {
					//alter is personID_1
					networkweight += data.chanceofID1 * weight;
				}
				influence_weights += weight;
//				System.out.println(this.PersonID + " , " + weight );
			}
		}
		if (influence_weights > 0) {
			networkweight /= influence_weights;	
		}
		social_pressure = networkweight;
//		System.out.println("SP: : " + social_pressure);
	}

/*	
	public void calc_social_pressue(Issue issue){
		Context context = ContextUtils.getContext(this);
		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		Iterator<RepastEdge<Person>> itr_edges = acquaintanceNetwork.getEdges(this).iterator();				

		double influence_weights=0;
		double networkweight = 0.0;		
		double weight = 0.0;
		PersonInteractionData data;
		
		while (itr_edges.hasNext()) {
			SNEdge edge = (SNEdge) itr_edges.next();
			int size = edge.getInteractions().size();
			if (size > 0) {
				data = (PersonInteractionData) edge.getInteractions().get(size-1);
				weight = edge.getInfluenceOn(this);
				if (data.personID_1 == this.PersonID) {
					//alter is personID_2
					networkweight += data.chanceofID2 * weight;
				}
				else {
					//alter is personID_1
					networkweight += data.chanceofID1 * weight;
				}
				influence_weights += weight;
//				System.out.println(this.PersonID + " , " + weight );
			}
		}
		if (influence_weights > 0) {
			networkweight /= influence_weights;	
		}
		social_pressure = networkweight;
//		System.out.println("SP: : " + social_pressure);
	}
*/	public double getRelevance()
	{
		return relevance_weight;
	}
	public String printProfile() {
		return "Person-"+PersonID+" has ideology " + ideology + ", bias " + ideological_bias 
				+ ", tolerance " + tolerance + ", relevance " + relevance_weight + ", social pressure " + social_pressure;
	}
/*
 * calculate social pressure using last interaction only. ppl have mutual influence on each other. No infleunce weight
 * are used.  		
*//*	public void calc_social_pressue(Issue issue){
		double networkweight = 0.0;		
		Context context = ContextUtils.getContext(this);

		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		Iterator<RepastEdge<Person>> itr_edges = acquaintanceNetwork.getEdges(this).iterator();				
		double count=0;
		while (itr_edges.hasNext()) {
			SNEdge edge = (SNEdge) itr_edges.next();
			PersonInteractionData data;
			int size = edge.getInteractions().size();
			if (size > 0) {

				data = (PersonInteractionData) edge.getInteractions().get(size-1);
				if (data.personID_1 == this.PersonID) {
					//alter is personID_2
					networkweight += data.chanceofID2;
				}
				else {
					//alter is personID_1
					networkweight += data.chanceofID1;
				}
				count++;
			}
		}
		if (count > 0) {
			networkweight /= count;	
		}
		social_pressure = networkweight;
	}
*/
	/*
	 * calculate social pressure using whole interaction history only. ppl have mutual influence on each other. No infleunce weight
	 * are used.  		
	*/
/*
	public void calc_social_pressue(Issue issue){
		double networkweight = 0.0;		
		double individualweight = 0.0;
		double count=0;

		Context context = ContextUtils.getContext(this);
		Network<Person> acquaintanceNetwork = (Network<Person>) context.getProjection("AcquaintanceNetwork");
		Iterator<RepastEdge<Person>> itr_edges = acquaintanceNetwork.getEdges(this).iterator();				

		ArrayList<PersonInteractionData> InteractionHistory = new ArrayList<PersonInteractionData>();

		while (itr_edges.hasNext()) {
			individualweight = 0.0;
			SNEdge edge = (SNEdge) itr_edges.next();
			int size = edge.getInteractions().size();
			if (size > 0) {
				InteractionHistory = edge.getInteractions();
				for (PersonInteractionData interaction : InteractionHistory){
					if(interaction.personID_1 == this.PersonID){

						individualweight += interaction.chanceofID2;
						//					System.out.println("acq ID: " + interaction.personID_2 + "  chance: " + interaction.chanceofID2);
					}
					else {
						//alter is personID_1
						individualweight += interaction.chanceofID1;
						//				System.out.println("acq ID: " + interaction.personID_1 + "  chance: " + interaction.chanceofID1);

					}
				}
				//				System.out.println("  total chance: " + individualweight + "  interactions: " + InteractionHistory.size());
				networkweight += individualweight/InteractionHistory.size();
				count++;
			}
		}
		if (count > 0) {
			networkweight /= count;	
		}
		//	System.out.println("Person ID: " + this.PersonID + "  socialPressure: " + networkweight);
		social_pressure = networkweight;
	}
*/

	// ASSUMING only one issue in context
/*	@Watch( watcheeClassName = "socialnetwork.Issue",
			watcheeFieldNames = "hotness",
			query = "colocated",
			whenToTrigger = WatcherTriggerSchedule.LATER,
			scheduleTriggerDelta = 1, scheduleTriggerPriority = 3
			//			triggerCondition = "$watchee.isIssueRaised() == true"
	)
	public void updateMediaInfluence(socialnetwork.Issue watched){
				double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount(); 
		Context context = ContextUtils.getContext(this);
		Network opinionNetwork = (Network)context.getProjection("OpinionNetwork");
		OpinionEdge edge = (OpinionEdge) opinionNetwork.getEdge(this, watched);
		 
		calc_media_influence(watched);
		calc_AttendProcession(watched);

		//		edge.addInteraction(new OpinionUpdateData(currentTick, relevance_weight+ idelogical_bais,social_pressure+media_influence ));
	}
*/
	
	public void updataSocialPressure(Issue issue){
		calc_social_pressue(issue);
		//		calc_AttendProcession(issue);
	}

	//	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public double calc_AttendProcession(Issue issue){
		double opinion = (1-this.tolerance) * (relevance_weight + ideological_bias);
		double socialpressure = this.tolerance * (social_pressure );//+ media_influence) ) ;
/*		chance = ( (1-this.tolerance) * (relevance_weight + idelogical_bais) 
								+ this.tolerance * (social_pressure ));//+ media_influence) ) ;
*/		
		chance = opinion + socialpressure;
		calcReason(opinion, socialpressure);
//		System.out.println(PersonID +" , " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
		return chance;	
	}

	
	public void calcReason(double opinion, double socialpressure){

		if( chance > Settings.cut_off){
			if(opinion <= Settings.cut_off && socialpressure <= 0.0)
				reason = CollectiveActionReason.Going_Neg_OP_Neg_SP;
			else if(opinion <= Settings.cut_off && socialpressure > 0.0 )
				reason = CollectiveActionReason.Going_Neg_Zero_OP_Pos_SP;
			else if(opinion > Settings.cut_off && socialpressure < 0.0)
				reason = CollectiveActionReason.Going_Pos_OP_Neg_SP;
			else if(opinion > Settings.cut_off && socialpressure > 0.0)
				reason = CollectiveActionReason.Going_Pos_OP_Pos_SP;
			else if(opinion > Settings.cut_off && socialpressure == 0.0)
				reason = CollectiveActionReason.Going_Pos_OP_Zero_SP;

		}
		else
		{
			if(opinion <= Settings.cut_off && socialpressure < 0.0)
				reason = CollectiveActionReason.NotGoing_Neg_OP_Neg_SP;
			else if(opinion <= Settings.cut_off && socialpressure > 0.0 )
				reason = CollectiveActionReason.NotGoing_Neg_OP_Pos_SP;
			else if(opinion >= Settings.cut_off && socialpressure < 0.0)
				reason = CollectiveActionReason.NotGoing_Pos_Zero_OP_Neg_SP;
			else if (opinion < Settings.cut_off && socialpressure == 0) 
				reason = CollectiveActionReason.NotGoing_Neg_OP_Zero_SP;					
			else if(opinion > Settings.cut_off && socialpressure > 0.0)
				reason = CollectiveActionReason.NotGoing_Pos_OP_Pos_SP;			
		}

	}
	
	public double getRelevanceWeight(Issue issue) {
		double weight = 0.0;
		if(relevance.containsKey(issue.getName()) == true){
			weight = relevance.get(issue.getName());
		}
		return weight;
	}
	public double getOpinion(){
		return relevance_weight + ideological_bias;
	}
	
	
	public boolean isActivist(){
		return false;
	}
	public boolean isNonActivist(){
		return false;
	}

	public Person() throws IOException {
		PersonID = ++lastID;
		Settings.PERSON_PARAMS.setIdeology(this);
		setOrientation();
		Settings.PERSON_PARAMS.setTolerance(this);
		//CHANGE MADE????????????
		//setRelevance();
		setWeights();
		String suffix = Settings.getDate() + "_" + Settings.getTime();
		String fname = Settings.prefix+"P-"+PersonID+"-"+suffix+".txt";
		if (Settings.INDIVIDUAL_LOG == true) {
			writer = new PrintWriter(new FileWriter(fname));
		}
	}
	
	
/*	private void setRelevance(){
		int num = Uniform.staticNextIntFromTo(0,2);
		int index=-1;
		double weight = 0.0;
		while(num > 0){
			index = Uniform.staticNextIntFromTo(0,IssueName.values().length-1);
			weight = Uniform.staticNextDoubleFromTo(-1,1);
			relevance.put((IssueName.values()[index]), (Double) weight);
			num--;
		}
	}*/

	private void setOrientation(){
		double radical = 0.50; 
		if(ideology <= 20 || ideology >= 80)
			radical = 0.75;
		if(Math.random() < radical )
			orientation =  Uniform.staticNextDoubleFromTo(0,0.5);
		else
			orientation =  Uniform.staticNextDoubleFromTo(0.5,1);
	}

	private void setWeights(){
		weights[WEIGHTS_INDEX.IDEOLOGY] 		 = Uniform.staticNextDoubleFromTo(0,1);
		weights[WEIGHTS_INDEX.ORIENTATION] 		 = Uniform.staticNextDoubleFromTo(0,1);
		weights[WEIGHTS_INDEX.MEDIA_INFLUENCE]	 = Uniform.staticNextDoubleFromTo(0,1);
		weights[WEIGHTS_INDEX.NETWORK_INFLUENCE] = Uniform.staticNextDoubleFromTo(0,1);
	}


	public double[] influenceBothWay(Person p) {
		double[] wts = new double[2];
		double overlap = Math.min(ideology + tolerance, p.getIdeology() + p.getTolerance())
		- Math.max(ideology - tolerance, p.getIdeology() - p.getTolerance());
		//RA - relative agreement
		double RA = (double)((overlap/tolerance) - 1);
		//speed factor
		double mu = Settings.PERSON_PARAMS.influenceSpeedFactor;

		wts[0] = p.getIdeology() + (mu*RA*(ideology - p.getIdeology()));
		wts[1] = p.getTolerance() + (mu*RA*(tolerance - p.getTolerance())); 
//		System.out.println("overlap: " + overlap);
		if (overlap < 0.0  ) {
			System.out.println("overlap: " + overlap);

			wts[0] = p.getIdeology() + (mu*RA*(ideology - p.getIdeology()));
			wts[1] = p.getTolerance() + (mu*RA*(tolerance - p.getTolerance())); 
		}
		else {
			wts[0] = p.getIdeology();
			wts[1] = p.getTolerance();
		}
		return wts;
	}


	public int getPersonID() {
		return PersonID;
	}
	public void setPersonID(int id) {
		PersonID = id;
	}
	public double getIdeology() {
		return ideology;
	}
	public void setIdeology(double ideology) {
		this.ideology = ideology;
	}

	public double getOrientation() {
		return orientation;
	}

	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}

	public double getTolerance() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	public double getChance() {
		return chance;
	}

	public void setChance(double chance) {
		this.chance = chance;
	}

	public double getIdeological_bias() {
		return ideological_bias;
	}

	public void setIdeological_bias(double idelogical_bais) {
		this.ideological_bias = idelogical_bais;
	}

	public double getMedia_influence() {
		return media_influence;
	}

	public void setMedia_influence(double media_influence) {
		this.media_influence = media_influence;
	}

	public double getSocial_pressure() {
		return social_pressure;
	}

	public void setSocial_pressure(double social_pressure) {
		this.social_pressure = social_pressure;
	}
	public double getMediaWeight(Issue issue){
		return weights[WEIGHTS_INDEX.MEDIA_INFLUENCE];
	}

	public CollectiveActionReason getReason() {
		return reason;
	}
	public int getReasonInt(){
		return reason.ordinal();
	}
	private void setTolerance(Issue issue){
		//double weight = this.getRelevanceWeight(issue);
		double weight = Math.abs(this.getRelevanceWeight(issue));
		this.tolerance = Settings.TOLERANCE_MAPPING_PARAMS.map(weight);
		}

/*
	public void calc_ideological_bais(Issue issue){
		double id_bais = 1 - (Math.abs(this.ideology - issue.getIdeology()) ) ;
		this.ideological_bias = Settings.OPINION_MAPPING_PARAMS.map(id_bais);
	}
	public void updateIdeologicalBais(Issue issue){
		calc_ideological_bais(issue);
		//		calc_AttendProcession(issue);
	}
	public void setRelevance(Issue issue) {		
		double weight = Uniform.staticNextDoubleFromTo(Settings.PERSON_PARAMS.minRelevance,
				Settings.PERSON_PARAMS.maxRelevance);	
		//If negative 
		if (Uniform.staticNextDouble() <= 1-Settings.PERSON_PARAMS.percentPositiveRelevant) {
			weight *= -1;
		}
		double min =0.0, max =1.0;
		if(issue.getIdeology()- 0.15 >= 0.0)
			min = issue.getIdeology()- 0.15;
		if(issue.getIdeology()+ 0.15 <= 1.0)
			max = issue.getIdeology() + 0.15;

		if(weight > 0.0)
			this.ideology = Uniform.staticNextDoubleFromTo(min,max); 

		else{
			min -= 0.20;
			max += 0.20;
			if(min <= 0.0 || min < (1-max) )
				this.ideology = Uniform.staticNextDoubleFromTo(max,1.0);
			else if(max >= 1.0  || min > (1- max) )
				this.ideology = Uniform.staticNextDoubleFromTo(0.0,min);
		}
		this.calc_ideological_bais(issue);
		relevance.put(issue.getName(), (Double)weight); 
		this.setTolerance(issue);
//		System.out.println("relevenace weight: " + weight + "  ideology : " + this.ideology  + " issue ideology: "  + issue.getIdeology());
//		System.out.println("opinion weight: " + this.idelogical_bais + "  Tolerance: " + this.tolerance); 		
	}	
	public double[] influence(Person p, double inf_of_this) {
		double[] wts = new double[2];
		double overlap = Math.min(ideology + tolerance, p.getIdeology() + p.getTolerance())
		- Math.max(ideology - tolerance, p.getIdeology() - p.getTolerance());
		//RA - relative agreement
		double RA = (double)((overlap/tolerance) - 1);
		//speed factor
		double mu = Settings.PERSON_PARAMS.influenceSpeedFactor;
 		if (Settings.EDGE_INFLUENCE) {
			mu *= inf_of_this;	
		}

		if (overlap > tolerance) {
//			System.out.println("overlap: " + overlap);
			wts[0] = p.getIdeology() + (mu*RA*(ideology - p.getIdeology()));
			wts[1] = p.getTolerance() + (mu*RA*(tolerance - p.getTolerance())); 
		}
		else {
			wts[0] = p.getIdeology();
			wts[1] = p.getTolerance();
		}
		return wts;
	}
	*/
	public void calc_ideological_bais(Issue issue){		
//		this.ideological_bias = Uniform.staticNextDoubleFromTo(-0.7, 1.0);
	//	this.ideological_bias = Normal.staticNextDouble(0.25, 0.25);
//		this.ideological_bias = Settings.OpinionGenerator.returnExponential();
		//this.ideological_bias = Settings.OpinionGenerator.returnNegativeExponential();
	//	this.ideological_bias = Settings.OpinionGenerator.returnSigmoid();
		//this.ideological_bias = Uniform.staticNextDoubleFromTo(-1.0, 1.0);
		this.ideological_bias = Settings.OpinionGenerator.returnVal();
	}
	
	public void updateIdeologicalBais(Issue issue){	
	}
	
	public void setRelevance(Issue issue) {		
		double weight = Uniform.staticNextDoubleFromTo(Settings.PERSON_PARAMS.minRelevance,
				Settings.PERSON_PARAMS.maxRelevance);	
		//If negative 
		if (Uniform.staticNextDouble() <= 1-Settings.PERSON_PARAMS.percentPositiveRelevant) {
			weight *= -1;
		}
		double min = 0.75, max =1.0;
		if(weight > 0.0)
			this.ideological_bias = Uniform.staticNextDoubleFromTo(min, max); 

		else{
			this.ideological_bias = Uniform.staticNextDoubleFromTo(-max, -min);
		}
		relevance.put(issue.getName(), (Double)weight); 
		this.setTolerance(issue);
//		System.out.println("relevenace weight: " + weight + "  ideology : " + this.ideology  + " issue ideology: "  + issue.getIdeology());
//		System.out.println("opinion weight: " + this.idelogical_bais + "  Tolerance: " + this.tolerance); 		
	}	
	public double[] influence(Person p, double inf_of_this) {
		double[] wts = new double[2];
		double overlap = Math.min(this.ideological_bias + tolerance, p.getIdeological_bias() + p.getTolerance())
		- Math.max(this.ideological_bias - tolerance, p.getIdeological_bias() - p.getTolerance());
		//RA - relative agreement
		double RA = (double)((overlap/tolerance) - 1);
		//speed factor -
		//10% of the influence
		double mu = Settings.PERSON_PARAMS.influenceSpeedFactor;
		if (Settings.EDGE_INFLUENCE) {
			mu *= inf_of_this;	
		}
		
		//		System.out.println("overlap: " + overlap);
		if (overlap > tolerance) {
//			System.out.println("overlap: " + overlap);
			wts[0] = p.getIdeological_bias() + (mu*RA*(this.ideological_bias - p.getIdeological_bias()));
			wts[1] = p.getTolerance() + (mu*RA*(tolerance - p.getTolerance())); 
		}
		else {
			wts[0] = p.getIdeological_bias();
			wts[1] = p.getTolerance();
		}
		return wts;
	}
	
}