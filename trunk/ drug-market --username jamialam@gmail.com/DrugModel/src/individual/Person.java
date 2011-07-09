package individual;

import drugmodel.Settings;
import repast.simphony.engine.environment.RunEnvironment;

public class Person extends Object {
	protected static int lastID = -1;
	protected int personID;
	protected double entryTick; 
	protected double exitTick;
	/** Amount of drugs in grams. */
	protected double drugs;
	/** Money available to purchase drugs. */
	protected double money;
	
	public Person() {
		this.personID = ++lastID;
		entryTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		exitTick = -1;
		money = 0d;
		drugs = 0d;
	}
	
	public void addMoney(double amount) {
		money += amount;
	}
	
	public void deductMoney(double amount) {
		if (Settings.errorLog && amount > money) {
			System.out.println("Amount: " + amount + " to be deducted is larger than money: " + money + ". Seting money zero.");
			money = 0d;
		}
		else {
			money -= amount;
		}			
	}
	
	/** Adds drug in grams. */
	public void addDrug(double amount) {
		drugs += amount;
	}
	
	/** Deduct drug in grams. */
	public void deductDrug(double amount) {
		if (Settings.errorLog && amount > drugs) {
			System.err.println("Amount: " + amount + " to be deducted is larger than drugs: " + drugs + ". Seting drugs zero." 
					+ (this instanceof  Dealer ? " Dealer" : " Customer"));
			drugs = 0d;
		}
		else {
			drugs -= amount;
		}		
	}
	
	/* Currently, all agents are fixed. This will be implemented later on. */
	public void move() {		
	}
		
	public int getPersonID() {
		return personID;
	}

	public double getEntryTick() {
		return entryTick;
	}

	public void setEntryTick(double entryTick) {
		this.entryTick = entryTick;
	}

	public double getExitTick() {
		return exitTick;
	}

	public void setExitTick(double exitTick) {
		this.exitTick = exitTick;
	}

	public Double getDrugs() {
		return drugs;
	}

	public void setDrugs(Double drugs) {
		this.drugs = drugs;
	}

	public Double getMoney() {
		return money;
	}

	public void setMoney(Double money) {
		this.money = money;
	}
}