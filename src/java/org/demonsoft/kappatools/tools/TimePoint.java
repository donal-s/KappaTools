package org.demonsoft.kappatools.tools;

import java.util.ArrayList;
import java.util.List;


public class TimePoint {
	
	public final List<Observable> observables = new ArrayList<Observable>();
	public final double time;
	
	// Constructor for unit tests
	public TimePoint(double time, List<Observable> observables) {
		this.time = time;
		this.observables.addAll(observables);
	}

	public TimePoint(double time) {
		this.time = time;
	}
	
}