package DisplayStyle;

import individual.Customer;
import individual.Dealer;

import individual.Person;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;

import repast.simphony.visualization.visualization2D.style.DefaultStyle2D;
import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;

public class AgentStyle extends DefaultStyleOGL2D {
	  public String getLabel(Object o) {
			if (o instanceof Person || o instanceof Dealer || o instanceof Customer){
				Person p = (Person)o;
//				return p.getReason().toString();
				return ("p-"+p.getPersonID());
			}
			else
				return "";
		  }
	  public Font getLabelFont(Object object) {
		    return new Font(null, Font.PLAIN, 10);
		   
		  }
	  public Color getLabelColor(Object object) {
		    return Color.BLACK;
		  }

/*	public Paint getPaint(Object o){
		if (o instanceof Person || o instanceof NonActivist || o instanceof Activist){
			double ideology = ((Person)o).getIdeology();
			if(ideology <= 0.2)
				return Color.RED;
			else if((ideology > 0.2) && (ideology <= 0.4) )
				return Color.PINK;
			else if((ideology > 0.4) && (ideology <= 0.6) )
				return Color.BLUE;
			else if((ideology > 0.6) && (ideology <= 0.8) )
				return Color.YELLOW;
			else if((ideology > 0.8) && (ideology <= 1.0) )
				return Color.MAGENTA;
			else
				return Color.GREEN;
		}
		else if(o instanceof Issue)
			return Color.MAGENTA;
		return Color.GREEN;
	}
*/
/*	@Override
	public Color getColor(Object o){
		if (o instanceof Person || o instanceof NonActivist || o instanceof Activist){
			double opinion = ((Person)o).getIdeological_bias();//getOpinion();
			double socialpressure = ((Person)o).getSocial_pressure();//calcSocialPressure();
			double chance = ((Person)o).getChance();//.calc_AttendProcession();
//			double go_due_to_sp=0, dont_go_due_to_sp=0;
			if(chance <= 0.0)
			{
				if(opinion == 0.0 && socialpressure == 0.0)
					return Color.BLUE;
				else
				if(opinion < 0.0 && socialpressure == 0.0)
					return Color.BLACK;
				else
				if(opinion < 0.0 && socialpressure < 0.0)
					return Color.BLACK;
				else if(opinion < 0.0 && socialpressure > 0.0 )
					return Color.GRAY;
				else if(opinion > 0.0 && socialpressure < 0.0){
					
					return Color.MAGENTA;
				}
				else if(opinion > 0.0 && socialpressure > 0.0){
					System.out.println(opinion + " , " + socialpressure+" , " + chance);
					return Color.CYAN;
				}
			}
			else
			{
				if(opinion <= 0.0 && socialpressure <= 0.0){
					System.out.println(opinion + " , " + socialpressure +" , " + chance );
					return Color.CYAN;
				}
				else
				if(opinion > 0.0 && socialpressure == 0.0)
					return Color.RED;
				else if(opinion < 0.0 && socialpressure > 0.0 )
					return Color.PINK;
				else if(opinion > 0.0 && socialpressure < 0.0)
					return Color.ORANGE;
				else if(opinion > 0.0 && socialpressure > 0.0)
					return Color.RED;
			}
		}
		else if(o instanceof Issue)
			return Color.MAGENTA;
		return Color.GREEN;
	}
	
	

	@Override
	public float getScale(Object o) {
		double chance = ((Person)o).getChance();//.calc_AttendProcession();

		if (o instanceof Person)
			return (float) (chance * 6.0);
		return 3f;
	}
*/
}