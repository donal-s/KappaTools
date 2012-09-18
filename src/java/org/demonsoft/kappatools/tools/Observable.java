package org.demonsoft.kappatools.tools;

public class Observable {
	public final String name;
	public final double mean;
	public final double stdDev;
	
	public Observable(String name, double mean, double stdDev) {
		this.name = name;
		this.mean = mean;
		this.stdDev = stdDev;
	}
}