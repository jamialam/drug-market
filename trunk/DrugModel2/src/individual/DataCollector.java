package individual;

import java.util.ArrayList;
import java.util.Iterator;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;
import drugmodel.ContextCreator;
import drugmodel.Settings;

public class DataCollector {
	private ArrayList<Double> unitsSold; 
	public DataCollector(){
		unitsSold = new ArrayList<Double>();
	}
	public String returnUnitsSold(){
		Context context = ContextUtils.getContext(this);
		for (int index  = 0 ; index < Settings.initDealers * 3; index++){
			unitsSold.add(index, (double) -1);
		}
		int max_index = -1;
		Iterator dealers = context.getObjects(Dealer.class).iterator();
		while(dealers.hasNext()){
			Dealer cur_dealer = (Dealer) dealers.next();
			unitsSold.set(cur_dealer.personID - Settings.initCustomers , cur_dealer.getUnitsToSell());
			if(max_index < cur_dealer.personID )
				max_index = cur_dealer.personID;
		}
		if(ContextCreator.getTickCount() == 1){
			String title = "D0"; 
			for (int index  = 1 ; index < Settings.initDealers * 3; index++){
				title += (" , D" + index); 
			}
			return title;
		}
		int length  =unitsSold.subList(0, max_index - Settings.initCustomers).toString().length();
		return unitsSold.subList(0, max_index - Settings.initCustomers).toString().substring(1, length-1);
	}
}
