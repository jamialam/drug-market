package drugmodel;

import java.util.ArrayList;

import repast.simphony.space.graph.RepastEdge;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SNEdge<T> extends RepastEdge {
	//private EdgeDataMap<Integer, Information> informationData;	
	private ArrayList<Information> informationList;
	
	public SNEdge(Object source, Object target, boolean directed, double weight) {
		super(source, target, directed, weight);
		initialize();
	}

	public SNEdge(Object source, Object target,	boolean directed) {
		super(source, target, directed);
		initialize();
	}	
	
	private void initialize() {
		informationList = new ArrayList<Information>();
		//informationData = new EdgeDataMap<Integer, Information>();
	}

	public ArrayList<Information> getInformationList() {
		return informationList;
	}

	public void setInformationList(ArrayList<Information> informationData) {
		this.informationList = informationData;
	}
	
/*	public EdgeDataMap<Integer, Information> getInformationData() {
		return informationData;
	}

	public void setInformationData(EdgeDataMap<Integer, Information> informationData) {
		this.informationData = informationData;
	}*/
}