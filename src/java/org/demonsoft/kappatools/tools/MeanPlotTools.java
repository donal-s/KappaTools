package org.demonsoft.kappatools.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;


public class MeanPlotTools {

	public static class SinglePlotFile {
		
		public static final int TIME_COLUMN = 0;
		
		public String[] columns;
		public double[][] data;
		
		public SinglePlotFile(String plotData) {
			if (plotData == null) {
				throw new NullPointerException();
			}
			
			String[] lines = StringUtils.split(plotData, "\n\r");
			if (lines.length < 2) {
				throw new IllegalArgumentException("No data in plot file");
			}
			
			String columnNames = lines[0];
			if (columnNames.startsWith("#")) {
				columnNames = columnNames.substring(1);
			}
			columns = StringUtils.split(columnNames, " \t'");
			if (columns.length < 2 || !"time".equals(columns[TIME_COLUMN])) {
				throw new IllegalArgumentException("Not a time plot file");
			}
			
			data = new double[lines.length - 1][columns.length];
			for (int lineIndex = 0; lineIndex < lines.length - 1; lineIndex++) {
				String dataLine = lines[lineIndex + 1];
				String[] dataItems = StringUtils.split(dataLine);
				if (dataItems.length != columns.length) {
					throw new IllegalArgumentException("Wrong number of values on line " + (lineIndex + 1));
				}
				try {
					for (int columnIndex=0; columnIndex < columns.length; columnIndex++) {
		                double value;
		                if ("INF".equalsIgnoreCase(dataItems[columnIndex])) {
		                    value = Double.POSITIVE_INFINITY;
		                }
		                else if ("-INF".equalsIgnoreCase(dataItems[columnIndex])) {
		                    value = Double.NEGATIVE_INFINITY;
		                }
		                else if ("NAN".equalsIgnoreCase(dataItems[columnIndex]) || "-NAN".equalsIgnoreCase(dataItems[columnIndex])) {
		                    value = Double.NaN;
		                }
		                else {
		                    value = Double.parseDouble(dataItems[columnIndex]);
		                }

						data[lineIndex][columnIndex] = value;
					}
				}
				catch (NumberFormatException ex) {
					throw new IllegalArgumentException("Invalid value on line " + (lineIndex + 1));
				}
			}
			
		}
	}
	
    @SuppressWarnings("null")
	public static List<TimePoint> getDataPoints(File[] inputFiles) throws IOException {
    	if (inputFiles == null) {
    		throw new NullPointerException();
    	}
    	if (inputFiles.length == 0) {
    		throw new IllegalArgumentException("Input file list is empty");
    	}
    	
		List<SinglePlotFile> individualPlots = new ArrayList<SinglePlotFile>();
		for (File inputFile : inputFiles) {
			String fileData = FileUtils.readFileToString(inputFile);
			individualPlots.add(new SinglePlotFile(fileData));
		}
    	SinglePlotFile firstFile = null;
    	for (SinglePlotFile current : individualPlots) {
    		if (firstFile == null) {
    			firstFile = current;
    		}
    		else {
    			if (firstFile.data.length != current.data.length || !Arrays.equals(firstFile.columns, current.columns)) {
    				throw new IllegalArgumentException("Incompatible data files");
    			}
    	    	for (int timeIndex = 0; timeIndex < current.data.length; timeIndex++) {
    	    		if (firstFile.data[timeIndex][SinglePlotFile.TIME_COLUMN] != current.data[timeIndex][SinglePlotFile.TIME_COLUMN]) {
    	    			throw new IllegalArgumentException("Incompatible data files");
    	    		}
    	    	}
    		}
    	}
    			
    	List<TimePoint> result = new ArrayList<TimePoint>();
    	for (int timeIndex = 0; timeIndex < firstFile.data.length; timeIndex++) {
    		TimePoint point = new TimePoint(firstFile.data[timeIndex][SinglePlotFile.TIME_COLUMN]);
    		result.add(point);
    		for (int columnIndex = 1; columnIndex < firstFile.columns.length; columnIndex++) {
    			
    			double total = 0;
    	    	for (SinglePlotFile current : individualPlots) {
    	    		double value = current.data[timeIndex][columnIndex];
    	    		if (!Double.isInfinite(value) && !Double.isNaN(value)) {
    	    			total += value;
    	    		}
    	    	}
    			double mean = total / individualPlots.size();
    			
    			double varianceSum = 0;
    			for (SinglePlotFile current : individualPlots) {
    				double value = current.data[timeIndex][columnIndex];
    	    		if (!Double.isInfinite(value) && !Double.isNaN(value)) {
    	    			varianceSum += Math.pow(value - mean, 2);
	                }
	            }
    			
    			double stdDev = Math.sqrt(varianceSum / individualPlots.size());

    			point.observables.add(new Observable(firstFile.columns[columnIndex], mean, stdDev));
    		}
    	}
    	
		return result;
	}

    public static String getTimepointOutput(List<TimePoint> timePoints) {
    	if (timePoints == null) {
    		throw new NullPointerException();
    	}
    	if (timePoints.size() == 0) {
    		throw new IllegalArgumentException("Input timepoint list is empty");
    	}
    	
    	StringBuilder builder = new StringBuilder();
    	builder.append("# time");
    	
    	TimePoint firstPoint = timePoints.get(0);
    	for (Observable current : firstPoint.observables) {
    		builder.append(" '").append(current.name).append("_mean' '").append(current.name).append("_dev'");
    	}
    	builder.append("\n");
    	
    	for (TimePoint timePoint : timePoints) {
    		builder.append(" ").append(timePoint.time);
        	for (Observable current : timePoint.observables) {
        		builder.append(" ").append(current.mean).append(" ").append(current.stdDev);
        	}
        	builder.append("\n");
    	}
    	
		return builder.toString();
	}


}
