package DisplayStyle;

import java.awt.Color;

import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.DefaultEdgeStyleOGL2D;

public class EdgeStyle extends DefaultEdgeStyleOGL2D {
	 public Color getColor(RepastEdge<?> edge) {
	/*	 Person source = (Person)edge.getSource();
		 Person target = (Person)edge.getTarget();
		 if(source.getColor() == target.getColor())
			 return source.getColor();
		 else
	*/		 return Color.LIGHT_GRAY;
}

	 public int getLineWidth(RepastEdge<?> edge) {
		    return 2;
		  }

}
