package DataCollection;
import drugmodel.Settings;
import drugmodel.Settings.DropOutReason;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;

public class DropOutCollector {

	private int PoliceArrestToday; 
	private int SurplusDropOutsToday;
	private int NoCustomerDropOutsToday;
	
	public DropOutCollector(){
		this.PoliceArrestToday =0;
		this.SurplusDropOutsToday = 0;
		this.NoCustomerDropOutsToday = 0;
	}

	/*@Watch( watcheeClassName = "individual.Dealer",
			watcheeFieldNames = "resaonOfDropOut",
			query = "colocated",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE,
			scheduleTriggerDelta = 1, scheduleTriggerPriority = 1
			//			triggerCondition = "$watchee.isIssueRaised() == true"
	)*/
	public void recordTransaction(individual.Dealer dealer){
		DropOutReason reason  = dealer.getResaonOfDropOut();
		if(reason == DropOutReason.NotDroppingOut){
			return;
		}
		else if(reason == DropOutReason.PoliceArrest){
			++PoliceArrestToday;
		}
		if(reason == DropOutReason.Surplus){
			++SurplusDropOutsToday;
		}
		else if(reason == DropOutReason.NoCustomer){
			++NoCustomerDropOutsToday;
		}
		else{
			System.err.println("Unknown Reason Found");
		}
	}
	@ScheduledMethod(start = Settings.StepsInDay, interval = Settings.StepsInDay, priority = 1)
	public void resetCollectors(){
		this.PoliceArrestToday =0;
		this.SurplusDropOutsToday = 0;
		this.NoCustomerDropOutsToday = 0;
	
	}

	public int getPoliceArrestToday() {
		return PoliceArrestToday;
	}

	public int getSurplusDropOutsToday() {
		return SurplusDropOutsToday;
	}

	public int getNoCustomerDropOutsToday() {
		return NoCustomerDropOutsToday;
	}
}
