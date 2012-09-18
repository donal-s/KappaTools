package org.demonsoft.kappatools.tools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.demonsoft.kappatools.tools.MeanPlotTools.SinglePlotFile;
import org.junit.Test;

public class MeanPlotToolsTest {

	@SuppressWarnings("unused")
	@Test
	public void testSinglePlotFile_constructor_invalid() {
		try {
			new SinglePlotFile(null);
			fail("null should have failed");
		}
		catch (NullPointerException ex) {
			// Expected exception
		}
		
		try {
			new SinglePlotFile("");
			fail("invalid should have failed");
		}
		catch (IllegalArgumentException ex) {
			// Expected exception
		}
		
		try {
			new SinglePlotFile("# time 'A' 'B' 'C'\n");
			fail("invalid should have failed");
		}
		catch (IllegalArgumentException ex) {
			// Expected exception
		}
		
		try {
			new SinglePlotFile("# event 'A' 'B' 'C'\n" + 
					" 0.000000E+00 1.000000E+04 1.000000E+00 10000\n");
			fail("invalid should have failed");
		}
		catch (IllegalArgumentException ex) {
			// Expected exception
		}
	
		try {
			new SinglePlotFile("# time 'A' 'B' 'C'\n" + 
					" 0.000000E+00 1.000000E+04 1.000000E+00\n");
			fail("invalid should have failed");
		}
		catch (IllegalArgumentException ex) {
			// Expected exception
		}
	}

	@Test
	public void testSinglePlotFile_constructor() {
		SinglePlotFile singlePlotFile = new SinglePlotFile(PLOT_INPUT);
		assertArrayEquals(PLOT_COLUMNS, singlePlotFile.columns);
		assertArrayEquals(PLOT_DATA, singlePlotFile.data);
	}
	
	@Test
	public void testGetDataPoints() throws Exception {
		try {
			MeanPlotTools.getDataPoints(null);
			fail("Null should have failed");
		}
		catch (NullPointerException ex) {
			// Expected exception
		}
		
		try {
			MeanPlotTools.getDataPoints(new File[0]);
			fail("empty should have failed");
		}
		catch (IllegalArgumentException ex) {
			// Expected exception
		}
		
		// Single file
		List<TimePoint> result = MeanPlotTools.getDataPoints(new File[] { new File("test/data/short_1.out") });
		String expected = 
				"# time 'A_mean' 'A_dev' 'B_mean' 'B_dev' 'C_mean' 'C_dev'\n" + 
				" 0.0 10000.0 0.0 0.0 0.0 10000.0 0.0\n" + 
				" 0.98 700.0 0.0 4190.0 0.0 9990.0 0.0\n" + 
				" 10.0 0.0 0.0 34703.0 0.0 9903.0 0.0\n";
		assertEquals(expected, MeanPlotTools.getTimepointOutput(result));

		// Multiple files
		result = MeanPlotTools.getDataPoints(new File[] { 
				new File("test/data/short_1.out"),
				new File("test/data/short_2.out"),
				new File("test/data/short_3.out"),
				});
		expected = 
				"# time 'A_mean' 'A_dev' 'B_mean' 'B_dev' 'C_mean' 'C_dev'\n" + 
				" 0.0 10000.0 0.0 0.0 0.0 10000.0 0.0\n" + 
				" 0.98 900.0 163.29931618554522 3991.6666666666665 282.609664063744 9991.666666666666 1.247219128924647\n" + 
				" 10.0 0.0 0.0 33896.0 2517.8627179945033 9896.0 5.715476066494082\n";
		assertEquals(expected, MeanPlotTools.getTimepointOutput(result));
	}

	@Test
	public void testGetTimepointOutput() {
		try {
			MeanPlotTools.getTimepointOutput(null);
			fail("Null should have failed");
		}
		catch (NullPointerException ex) {
			// Expected exception
		}
		
		try {
			MeanPlotTools.getTimepointOutput(new ArrayList<TimePoint>());
			fail("empty should have failed");
		}
		catch (IllegalArgumentException ex) {
			// Expected exception
		}
		
		List<TimePoint> inputPoints = new ArrayList<TimePoint>();
		
		inputPoints.add(new TimePoint(1.0, getList(
				new Observable("A", 3, 4),
				new Observable("B", 5, 6.5))));
		inputPoints.add(new TimePoint(2.0, getList(
				new Observable("A", 3.1, 4.1),
				new Observable("B", 5.1, 6.51))));
		
		String result = MeanPlotTools.getTimepointOutput(inputPoints);
		
		String expected = "# time 'A_mean' 'A_dev' 'B_mean' 'B_dev'\n" + 
				" 1.0 3.0 4.0 5.0 6.5\n" + 
				" 2.0 3.1 4.1 5.1 6.51\n";
		
		assertEquals(expected, result);
	}
	
	
    public static <T> List<T> getList(T... elements) {
        List<T> result = new ArrayList<T>();
        result.addAll(Arrays.asList(elements));
        return result;
    }



	private static final String PLOT_INPUT = 
		"# time 'A' 'B' 'C'\n" + 
		" 0.000000E+00 1.000000E+04 1.000000E+00 10000\n" + 
		" 1.000000E-02 1.000000E+04 2.000000E+00 10000\n" + 
		" 2.000000E-02 9.900000E+03 3.000000E+00 10000\n" +
		" INF -INF NAN -NAN\n";
	
	private static final String[] PLOT_COLUMNS = {
		"time", "A", "B", "C"
	};
	
	private static final double[][] PLOT_DATA = {
		{ 0.000000E+00, 1.000000E+04, 1.000000E+00, 10000 },
		{ 1.000000E-02, 1.000000E+04, 2.000000E+00, 10000 },
		{ 2.000000E-02, 9.900000E+03, 3.000000E+00, 10000 },
		{ Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, Double.NaN },
	};
	
}
