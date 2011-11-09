package DisplayStyle;

import individual.Dealer;
import individual.Person;

import java.awt.Color;

import drugmodel.ContextCreator;
import drugmodel.Settings;
import drugmodel.Settings.DealerType;


import repast.simphony.visualization.visualization3D.AppearanceFactory;
import repast.simphony.visualization.visualization3D.style.DefaultStyle3D;
import repast.simphony.visualization.visualization3D.style.TaggedAppearance;

public class AgentStyle3D extends DefaultStyle3D<Person>{
	  public String getLabel(Person o, String currentLabel) {
		 	if(o instanceof Dealer)
		  		return( "" + (float)((Dealer)o).getTotalSales());
		 	  return null;
		  }
	  public TaggedAppearance getAppearance(Person t, TaggedAppearance taggedAppearance, Object shapeID) {
		    if (taggedAppearance == null || taggedAppearance.getTag() == null) {
		      taggedAppearance = new TaggedAppearance("DEFAULT");
//		      AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),t.getColor());
		    }
		    if(t instanceof Dealer){
		    	DealerType type = (DealerType)((Dealer) t).getDealerType();
				
		    	if(type == DealerType.Old)
					AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),Color.YELLOW);
				else if(type == DealerType.Greedy)
					AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),Color.GREEN);
				else
					AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),Color.RED);
			}		   
		    else {
		      AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),Color.CYAN);
		    }
		    return taggedAppearance;
		    
		  }
/*  public float[] getScale(Person o) {
		  
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
		  		float[] array = {(float) (1.0f), (float) (1.0f), (float) (1.0f)};
		  		return array;
		  	}
 		  }
*/	 
	  public float[] getScale(Person o) {
		  float ratio = (float)Settings.InitDealers / (float)Settings.InitCustomers;
		  	if(o instanceof Dealer){
		  		float size = (float)((Dealer)o).getTotalSales();
		  		if(size == 0.0)
		  			size = ratio;
		  		else
		  			size = (size/(float)ContextCreator.getTickCount())  * 0.05f;//ratio;
		  		float[] array = {size , size , size };
		  		return array;
		  	}
		  	else{
		  	//	float[] array = {(float) (1.0f), (float) (1.0f), (float) (1.0f)};
		  		float[] array = {(float) (ratio), (float) (ratio), (float) (ratio)};
		  		return array;
		  	}
		  }
	
}
