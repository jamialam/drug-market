package Controller;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import repast.simphony.engine.environment.RunEnvironment;

public class TestMain_2 {
	private String scenario_dir;
	private double batch_no = 0.0;
	private int num_runs = 5;
	private double endTime = 10.0;
	private String parameter_file_name;
	private HashMap<String, String> param_map = new HashMap<String, String>();
	
	
	public static void main(String[] args){
		TestMain_2 tm = new TestMain_2();
		tm.parseArguments(args);
		tm.loadParamMap();
		File file = new File(tm.scenario_dir); // the scenario dir
		TestRunner_2 runner = new TestRunner_2(tm.batch_no);
		
/*		File file = new File("C:/Users/sadaf/workspace/NullProject/NullProject.rs"); // the scenario dir
		TestRunner_2 runner = new TestRunner_2(2.0);
*/		
		try {
			runner.load(file, tm.param_map);     // load the repast scenario
		} catch (Exception e) {
			e.printStackTrace();
		}

	
		// Run the sim a few times to check for cleanup and init issues.
		for(int i=0; i<tm.num_runs; i++){
			
			runner.runInitialize();  // initialize the run
		
			RunEnvironment.getInstance().endAt(tm.endTime);

			while (runner.getActionCount() > 0){  // loop until last action is left
				if (runner.getModelActionCount() == 0) {
					runner.setFinishing(true);
				}
				runner.step();  // execute all scheduled actions at next tick
				if(RunEnvironment.getInstance().getCurrentSchedule().getTickCount() == tm.endTime){
					RunEnvironment.getInstance().getCurrentSchedule().setFinishing(true);
				}
			}

			runner.stop();          // execute any actions scheduled at run end
			runner.cleanUpRun();
		}
		runner.cleanUpBatch();    // run after all runs complete
	}
	
	boolean parseArguments(String[] args){
		if(args.length != 5){
			System.err.println("Arguments not found.");
			return false;
		}
		else{
			scenario_dir = args[0];
			parameter_file_name = args[1];
			batch_no = Double.parseDouble(args[2].trim());
			num_runs = Integer.parseInt(args[3].trim());
			endTime = Double.parseDouble(args[4].trim());
		}
	return true;
	}

	void loadParamMap(){
		File csvfile = new File(parameter_file_name);
		String header, param_val = "";
		
		try {
			BufferedReader bufRdr  = new BufferedReader(new FileReader(csvfile));
			header = bufRdr.readLine();
			StringTokenizer st = new StringTokenizer(header,",");
			int curline = 0;
			while(curline < batch_no ){
				String s = bufRdr.readLine();
				if(s !=null)
					param_val = s;
				++curline;
			}
			StringTokenizer val = new StringTokenizer(param_val,",");
			
			
			while (st.hasMoreTokens() && val.hasMoreTokens()){
				String key = st.nextToken();
				String value = val.nextToken();
				if(key.equals("batch_no") == true){
					if(Double.parseDouble(value.trim()) != batch_no ){
						System.err.println(this.parameter_file_name + " is unsorted or sorted in descending order. line no. " + Double.parseDouble(value.trim())+ " read doesnt represent batch no. " +batch_no );
						
					}
				}
				else{
					param_map.put(key,value);
				}
			}
			
		} catch (FileNotFoundException e  ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("in load param : " + param_map.get("initpopulation"));
	}
}