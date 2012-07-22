package test;

import cern.jet.random.Exponential;

public class exp_dist {
	public static void main(String[] args) {
		for (int i=0; i < 500 ; i++)
			System.out.println(Math.floor( Exponential.staticNextDouble(8.0)*10 ) ) ;
		
	}
}