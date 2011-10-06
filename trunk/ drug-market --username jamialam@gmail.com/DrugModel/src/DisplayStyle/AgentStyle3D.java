package DisplayStyle;

import individual.Dealer;
import individual.Person;

import java.awt.Color;


import repast.simphony.visualization.visualization3D.AppearanceFactory;
import repast.simphony.visualization.visualization3D.style.DefaultStyle3D;
import repast.simphony.visualization.visualization3D.style.TaggedAppearance;

public class AgentStyle3D extends DefaultStyle3D<Person>{
	  public String getLabel(Person o, String currentLabel) {
		 	if(o instanceof Dealer)
		  		return( "" + (float)((Dealer)o).getSale());
		 	  return null;
		  }
	  public TaggedAppearance getAppearance(Person t, TaggedAppearance taggedAppearance, Object shapeID) {
		    if (taggedAppearance == null || taggedAppearance.getTag() == null) {
		      taggedAppearance = new TaggedAppearance("DEFAULT");
//		      AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),t.getColor());
		    }
		    if(t instanceof Dealer){
			      AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),Color.YELLOW);
		    }
		    else {
		      AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),Color.CYAN);
		    }
		    return taggedAppearance;
		    
		  }
	  public float[] getScale(Person o) {
		  	
		  	if(o instanceof Dealer){
		  		float size = (float)((Dealer)o).getSale();
		  		if(size == 0.0)
		  			size = 0.1f;
		  		else
		  			size /= 1000;
		  		float[] array = {size * 0.1f, size * 0.1f, size * 0.1f};
		  		return array;
		  	}
		  	else{
		  		float[] array = {(float) (0.5 * 2.0f), (float) (0.5 * 2.0f), (float) (0.5 * 2.0f)};
		  		return array;
		  	}
 		  }
	 

}
