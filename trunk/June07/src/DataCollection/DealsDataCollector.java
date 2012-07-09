package DataCollection;

import individual.Customer;

import java.util.Iterator;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastPajekEdgeTransformer;
import repast.simphony.space.graph.RepastPajekVertexTransformer;

import drugmodel.ContextCreator;
import drugmodel.DealEdgeTransformer;
import drugmodel.SNEdge;
import drugmodel.Settings;
import drugmodel.Settings.Endorsement;
import drugmodel.Settings.TransactionType;
import drugmodel.Transaction;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.PajekNetWriter;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class DealsDataCollector {
	
	private int DirectDealsToday;
	private int BrokeredDealsToday;

	private int DNDealsToday; 
	private int DGDealsToday;
	private int DUBDealsToday;
	private int DSBDealsToday;
	private int DDBDealsToday;
	
	private int SNDealsToday; 
	private int SGDealsToday;
	private int SUBDealsToday;
	private int SSBDealsToday;
	private int SDBDealsToday;
	
	private int NBrokerToday; 
	private int GBrokerToday;
	private int UBBrokerToday;
	private int SBBrokerToday;
	private int DBBrokerToday;
	

	public DealsDataCollector(){
		DNDealsToday = 0; 
		DGDealsToday = 0;
		DUBDealsToday = 0;
		DSBDealsToday = 0;
		DDBDealsToday = 0;
	
		DirectDealsToday = 0;
		BrokeredDealsToday = 0;

		SNDealsToday = 0;
		SGDealsToday =0;
		SUBDealsToday = 0;
		SSBDealsToday =0;
		SDBDealsToday =0;

		NBrokerToday = 0; 
		GBrokerToday = 0 ;
		UBBrokerToday =0 ;
		SBBrokerToday = 0 ;
		DBBrokerToday = 0;

	}

	@Watch( watcheeClassName = "individual.Customer",
			watcheeFieldNames = "lastTransaction",
			query = "colocated",
			whenToTrigger = WatcherTriggerSchedule.LATER,
			scheduleTriggerDelta = 1, scheduleTriggerPriority = 1
			)
	public void recordTransaction(individual.Customer customer){
		Transaction t = customer.getLastTransaction();
		if(t.getWhoseDealUsed() != t.getCustomerID() && t.getTransactionType() == TransactionType.Broker){
			++DirectDealsToday;
			if(t.getEndorsement() == Endorsement.Good){
				++SGDealsToday;
			}
			else if(t.getEndorsement() == Endorsement.SoldBad){
				++SSBDealsToday;
			}
			else if(t.getEndorsement() == Endorsement.DealerNotFoundBad){
				++SDBDealsToday;
			}
			else if(t.getEndorsement() == Endorsement.UnSoldBad){
				++SUBDealsToday;
			}
			else if(t.getEndorsement() == Endorsement.None){
				++SNDealsToday;
			}

			Network socialNetwork = (Network)(ContextCreator.mainContext.getProjection(Settings.socialnetwork));
			Iterator itr = socialNetwork.getAdjacent(customer).iterator();
			while (itr.hasNext()) {
				Customer broker = (Customer) itr.next();
				if (broker.getPersonID() == t.getWhoseDealUsed()) {
					SNEdge outEdge = (SNEdge) socialNetwork.getEdge(customer,broker);	
					Endorsement endorsement = outEdge.getLastEndorsement();
					if(endorsement == Endorsement.Good){
						++this.GBrokerToday;
					}
					else if(endorsement == Endorsement.SoldBad){
						++this.SBBrokerToday;
					}
					else if(endorsement == Endorsement.None){
						++this.NBrokerToday;
					}
					else if(endorsement == Endorsement.UnSoldBad){
						++this.UBBrokerToday;
						
					}
					else if(endorsement == Endorsement.DealerNotFoundBad){
						++this.DBBrokerToday;
					}
					
					break;
				}
			}
		}
		else {
			++this.DirectDealsToday;
			if(t.getEndorsement() == Endorsement.Good){
				++DGDealsToday;
			}
			else if(t.getEndorsement() == Endorsement.SoldBad){
				++DSBDealsToday;
			}
			else if(t.getEndorsement() == Endorsement.DealerNotFoundBad){
				++DDBDealsToday;
			}
			else if(t.getEndorsement() == Endorsement.UnSoldBad){
				++DUBDealsToday;
			}
			else if(t.getEndorsement() == Endorsement.None){
				++DNDealsToday;
			}
		}
	}
	@ScheduledMethod(start = Settings.StepsInDay, interval = Settings.StepsInDay, priority = 1)
	public void resetCollectors(){
		DNDealsToday = 0; 
		DGDealsToday = 0;
		DUBDealsToday = 0;
		DSBDealsToday = 0;
		DDBDealsToday = 0;
	
		DirectDealsToday = 0;
		BrokeredDealsToday = 0;

		SNDealsToday = 0;
		SGDealsToday =0;
		SUBDealsToday = 0;
		SSBDealsToday =0;
		SDBDealsToday =0;

		NBrokerToday = 0; 
		GBrokerToday = 0 ;
		UBBrokerToday =0 ;
		SBBrokerToday = 0 ;
		DBBrokerToday = 0;
	}

	public int getDirectDealsToday() {
		return DirectDealsToday;
	}

	public int getBrokeredDealsToday() {
		return BrokeredDealsToday;
	}

	public int getDNDealsToday() {
		return DNDealsToday;
	}

	public int getDGDealsToday() {
		return DGDealsToday;
	}

	public int getDUBDealsToday() {
		return DUBDealsToday;
	}

	public int getDSBDealsToday() {
		return DSBDealsToday;
	}

	public int getDDBDealsToday() {
		return DDBDealsToday;
	}

	public int getSNDealsToday() {
		return SNDealsToday;
	}

	public int getSGDealsToday() {
		return SGDealsToday;
	}

	public int getSUBDealsToday() {
		return SUBDealsToday;
	}

	public int getSSBDealsToday() {
		return SSBDealsToday;
	}

	public int getSDBDealsToday() {
		return SDBDealsToday;
	}

	public int getNBrokerToday() {
		return NBrokerToday;
	}

	public int getGBrokerToday() {
		return GBrokerToday;
	}

	public int getUBBrokerToday() {
		return UBBrokerToday;
	}

	public int getSBBrokerToday() {
		return SBBrokerToday;
	}

	public int getDBBrokerToday() {
		return DBBrokerToday;
	}

	
}