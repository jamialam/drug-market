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

public class DealerStyle3D extends DefaultStyle3D<Person>{
	public String getLabel(Person o, String currentLabel) {
		if(o instanceof Dealer)
			return( "" + (float)((Dealer)o).getSalesToday());
		return null;
	}
	public TaggedAppearance getAppearance(Person t, TaggedAppearance taggedAppearance, Object shapeID) {
		if (taggedAppearance == null || taggedAppearance.getTag() == null) {
			taggedAppearance = new TaggedAppearance("DEFAULT");
			//		      AppearanceFactory.setMaterialAppearance(taggedAppearance.getAppearance(),t.getColor());
		}
		if(t instanceof Dealer){
			DealerType type = (DealerType)((Dealer) t).getType();
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

	public float[] getScale(Person o) {
		float ratio = (float) 0.3;
		if(o instanceof Dealer){
			float today = (float)((Dealer)o).getSalesToday();
			float yesterday =(float)((Dealer)o).getSalesYesterday();
			float size = 0;  	
			if( yesterday != 0.0){
				size  = (today /yesterday) * today * 0.005f;
				
			}
			else{
				size = (float) 0.3;
			}
				
			if(size == 0.0){
				size = ratio;
			}
			float[] array = {size , size , size };
			return array;
		}
		else{
			float[] array = {(float) (ratio), (float) (ratio), (float) (ratio)};
			return array;
		}

	}

}
