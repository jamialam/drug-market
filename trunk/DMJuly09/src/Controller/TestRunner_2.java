package Controller;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import repast.simphony.batch.BatchScenarioLoader;
import repast.simphony.data2.engine.DataInitActionCreator;
import repast.simphony.data2.engine.DataSetsActionCreator;
import repast.simphony.data2.engine.FileSinkComponentControllerAction;
import repast.simphony.engine.controller.Controller;
import repast.simphony.engine.controller.DefaultController;
import repast.simphony.engine.environment.AbstractRunner;
import repast.simphony.engine.environment.ControllerAction;
import repast.simphony.engine.environment.ControllerRegistry;
import repast.simphony.engine.environment.DefaultRunEnvironmentBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.parameter.DefaultParameters;
import repast.simphony.parameter.ParameterSchema;
import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.SweeperProducer;
import repast.simphony.util.collections.Tree;

public class TestRunner_2 extends AbstractRunner {


	private RunEnvironmentBuilder runEnvironmentBuilder;
	protected Controller controller;
	protected boolean pause = false;
	protected Object monitor = new Object();
	protected SweeperProducer producer;
	private ISchedule schedule;
	private double batch_no;

	public TestRunner_2(double batch ) {
		runEnvironmentBuilder = new DefaultRunEnvironmentBuilder(this, true);
		controller = new DefaultController(runEnvironmentBuilder);
		controller.setScheduleRunner(this);
		batch_no = batch;
	}

	public void load(File scenarioDir, HashMap<String,String> param_map) throws Exception{
		BatchScenarioLoader loader;
		if (scenarioDir.exists()) {

			loader = new BatchScenarioLoader(scenarioDir);
			ControllerRegistry registry = loader.load(runEnvironmentBuilder);
			controller.setControllerRegistry(registry);
//			System.out.println(controller.getControllerRegistry().getMasterContextId());
			Tree<ControllerAction> tree = controller.getControllerRegistry().getActionTree("drugmodel");
			System.out.println(tree.size());
			Collection<ControllerAction> c  = tree.getChildren(tree.getRoot());

			Iterator i = c.iterator();
			controller.setControllerRegistry(registry);

			ControllerAction b;
			while(i.hasNext()){
				ControllerAction a = (ControllerAction) i.next();
				System.out.println("a : " + a.getClass() + "  " + a.toString());
				Collection<ControllerAction> c1 = tree.getChildren(a);
				Iterator i1 = c1.iterator();
				ControllerAction b1;
				while(i1.hasNext()){
					ControllerAction a1 = (ControllerAction) i1.next();
					System.out.println("a1" + a1.getClass() + "  " + a1.toString());
					if(a1 instanceof FileSinkComponentControllerAction){
						FileSinkComponentControllerAction f = (FileSinkComponentControllerAction)a1;
						System.out.println(f.getDescriptor().getFileName());
						String oldfilename = f.getDescriptor().getFileName();
						int lastindex = oldfilename.lastIndexOf(".csv");
						String newfilename = oldfilename.substring(0,lastindex);
						newfilename = newfilename +"-" + batch_no + oldfilename.substring(lastindex, oldfilename.length()) ;
										
						f.getDescriptor().setFileName(newfilename  );
						System.out.println(f.getDescriptor().getFileName());

					}
			}
			}

		} else {
			System.err.println("Scenario not found");
			throw new IllegalArgumentException(
					"Invalid scenario " + scenarioDir.getAbsolutePath());
			//          return;
		}

		controller.batchInitialize();
		Parameters p = loader.getParameters();

		Set<Map.Entry<String, String>> set = param_map.entrySet();
		for (Map.Entry<String, String> entry : set) {
			ParameterSchema ps = p.getSchema().getDetails(entry.getKey());
			if(ps == null){
				System.err.println(entry.getKey() + "  not found in schema.");
			}
			else{
			Object o = ps.fromString(entry.getValue());
			p.setValue(entry.getKey(), o);
			}
		}	

		controller.runParameterSetters(p);
	}

	public void runInitialize(){
	DefaultParameters defaultParameters = new DefaultParameters();
	defaultParameters.addParameter("randomSeed", "randomSeed", Number.class, 1,true);
	controller.runInitialize(defaultParameters);
	schedule = RunState.getInstance().getScheduleRegistry().getModelSchedule();	
/*
		controller.runInitialize(null);
		schedule = RunState.getInstance().getScheduleRegistry().getModelSchedule();
*/}

	public void cleanUpRun(){
		controller.runCleanup();
	}
	public void cleanUpBatch(){
		controller.batchCleanup();
	}

	// returns the tick count of the next scheduled item
	public double getNextScheduledTime(){
		return ((Schedule)RunEnvironment.getInstance().getCurrentSchedule()).peekNextAction().getNextTime();
	}

	// returns the number of model actions on the schedule
	public int getModelActionCount(){
		return schedule.getModelActionCount();
	}

	// returns the number of non-model actions on the schedule
	public int getActionCount(){
		return schedule.getActionCount();
	}

	// Step the schedule
	public void step(){
		schedule.execute();
	}

	// stop the schedule
	public void stop(){
		if ( schedule != null )
			schedule.executeEndActions();
	}

	public void setFinishing(boolean fin){
		schedule.setFinishing(fin);
	}

	public void execute(RunState toExecuteOn) {
		// required AbstractRunner stub.  We will control the
		//  schedule directly.
	}
}

