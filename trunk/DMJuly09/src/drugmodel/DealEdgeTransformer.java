package drugmodel;

import org.apache.commons.collections15.Transformer;

import repast.simphony.space.graph.RepastEdge;

public class DealEdgeTransformer implements Transformer {

	public Object transform(Object obj) {
		if (obj instanceof Transaction) {
			return ((Transaction) obj).getTime();
		} else {
			return 0.0;
		}
	}

}
