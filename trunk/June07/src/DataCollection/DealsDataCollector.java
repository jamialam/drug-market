package DataCollection;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.space.graph.RepastPajekEdgeTransformer;
import repast.simphony.space.graph.RepastPajekVertexTransformer;

import drugmodel.DealEdgeTransformer;
import drugmodel.Settings;
import drugmodel.Settings.Endorsement;
import drugmodel.Transaction;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.PajekNetWriter;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class DealsDataCollector {
	private int totalNoneDealsToday; 
	private int totalGoodDealsToday;
	private int totalUnSoldBadDealsToday;
	private int totalSoldBadDealsToday;
	private int totalDealerNotFoundBadDealsToday;
	private int totalMyDealerDealsToday;
	private int totalSharedDealerDealsToday;
	
	public DealsDataCollector(){
		totalNoneDealsToday = 0; 
		totalGoodDealsToday = 0;
		totalUnSoldBadDealsToday = 0;
		totalSoldBadDealsToday = 0;
		totalDealerNotFoundBadDealsToday = 0;
		totalMyDealerDealsToday = 0;
		totalSharedDealerDealsToday = 0;
	}

	/*@Watch( watcheeClassName = "individual.Customer",
			watcheeFieldNames = "lastTransaction",
			query = "colocated",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE,
			scheduleTriggerDelta = 1, scheduleTriggerPriority = 1
			//			triggerCondition = "$watchee.isIssueRaised() == true"
	)*/
	public void recordTransaction(individual.Customer customer){
		Transaction t = customer.getLastTransaction();
		if(t.getWhoseDealUsed() != t.getCustomerID()){
			++totalSharedDealerDealsToday;
		}
		else {
			++totalMyDealerDealsToday;
		}
		if(t.getEndorsement() == Endorsement.Good){
			++totalGoodDealsToday;
		}
		else if(t.getEndorsement() == Endorsement.SoldBad){
			++totalSoldBadDealsToday;
		}
		else if(t.getEndorsement() == Endorsement.DealerNotFoundBad){
			++totalDealerNotFoundBadDealsToday;
		}
		else if(t.getEndorsement() == Endorsement.UnSoldBad){
			++totalUnSoldBadDealsToday;
		}
		else if(t.getEndorsement() == Endorsement.None){
			++totalNoneDealsToday;
		}
	}
	@ScheduledMethod(start = Settings.StepsInDay, interval = Settings.StepsInDay, priority = 1)
	public void resetCollectors(){
		totalNoneDealsToday = 0; 
		totalGoodDealsToday = 0;
		totalUnSoldBadDealsToday = 0;
		totalSoldBadDealsToday = 0;
		totalDealerNotFoundBadDealsToday = 0;
		totalMyDealerDealsToday = 0;
		totalSharedDealerDealsToday = 0;
	}

	public int getTotalNoneDealsToday() {
		return totalNoneDealsToday;
	}

	public int getTotalGoodDealsToday() {
		return totalGoodDealsToday;
	}

	public int getTotalUnSoldBadDealsToday() {
		return totalUnSoldBadDealsToday;
	}

	public int getTotalSoldBadDealsToday() {
		return totalSoldBadDealsToday;
	}

	public int getTotalDealerNotFoundBadDealsToday() {
		return totalDealerNotFoundBadDealsToday;
	}

	public int getTotalMyDealerDealsToday() {
		return totalMyDealerDealsToday;
	}

	public int getTotalSharedDealerDealsToday() {
		return totalSharedDealerDealsToday;
	}

}