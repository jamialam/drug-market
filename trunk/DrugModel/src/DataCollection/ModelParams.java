package DataCollection;

import java.util.Iterator;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class ModelParams extends Object {
	public String getParams(){
		String names= "" , values= "", params ="";
		Parameters p = RunEnvironment.getInstance().getParameters();
		Iterator itr = p.getSchema().parameterNames().iterator();
		while(itr.hasNext()){
			String name = (String) itr.next();
			names += name;
			values += p.getValue(name);
			params += ( name + "," + p.getValue(name)+ "\r\n" );
		}
		return params;
	}
}
