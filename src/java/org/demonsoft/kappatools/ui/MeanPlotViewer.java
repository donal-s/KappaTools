package org.demonsoft.kappatools.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.demonsoft.kappatools.tools.MeanPlotTools;
import org.demonsoft.kappatools.tools.Observable;
import org.demonsoft.kappatools.tools.TimePoint;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;

public class MeanPlotViewer implements ActionListener {


	private static final String WINDOW_TITLE = "Mean Plot Viewer v" + Version.VERSION;

    private static enum ToolbarMode {
        START, PROCESSING, COMPLETE
    }
    
    private static final String STATUS_STARTING_SIMULATION = "Starting simulation, please wait...";
    
    private static final String TOOLBAR_BUTTON_IMAGE_PATH = "/toolbarButtonGraphics/";

    private static final String ACTION_OPEN = "open";

    JFrame frame;
    private ChartPanel cellMeanChartPanel;
    private JTextArea consoleTextArea;
    private JTextArea debugTextArea;
    private PrintStream consoleStream;
    private PrintStream debugStream;
    JToolBar toolbar;

    private JTabbedPane tabbedPane;
    private JScrollPane consoleTextPane;
    
    JButton toolbarButtonOpen;

    protected JTextArea textAreaData;
    JLabel textStatus;

    private XYIntervalSeriesCollection cellChartData;
    
    Dimension minimumSize;
    
    private File[] inputFiles;

    
    private static final String STATUS_COMPLETE = "Processing complete.";
    private static final String STATUS_PLOTTING = "Plotting results.";
    
    public MeanPlotViewer() throws Exception {

        frame = new JFrame(WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();

        cellMeanChartPanel = new ChartPanel(ChartFactory.createXYLineChart("", "Time", "Quantity", null, PlotOrientation.VERTICAL, true, false, false));
        tabbedPane.add(cellMeanChartPanel, "Mean chart");

        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextPane = new JScrollPane(consoleTextArea);
        tabbedPane.add(consoleTextPane, "Console Output");
        
        consoleStream = new PrintStream(new ConsoleOutputStream(consoleTextArea));
        System.setOut(consoleStream);
                
        debugTextArea = new JTextArea();
        debugTextArea.setEditable(false);
        JScrollPane textPane = new JScrollPane(debugTextArea);
        tabbedPane.add(textPane, "Debug Output");
        
        debugStream = new PrintStream(new ConsoleOutputStream(debugTextArea));
        System.setErr(debugStream);
                
        textAreaData = new JTextArea();
        textAreaData.setEditable(false);
        textPane = new JScrollPane(textAreaData);
        tabbedPane.add(textPane, "Data");

        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        createToolbar();
        
        JPanel statusPanel = new JPanel();
        textStatus = new JLabel("Ready");
        statusPanel.add(textStatus);
        frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);

        frame.pack();
        minimumSize = new Dimension(frame.getWidth() + 70, frame.getHeight());
        frame.setSize(minimumSize);
        frame.setMinimumSize(minimumSize);

        frame.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                if (frame.getWidth() < minimumSize.width) {
                    frame.setSize(minimumSize.width, frame.getHeight());
                }
                if (frame.getHeight() < minimumSize.height) {
                    frame.setSize(frame.getWidth(), minimumSize.height);
                }
            }
        });
        
        setStatus(ToolbarMode.START, "Ready");

        frame.setVisible(true);
    }

    private void createToolbar() {
        toolbar = new JToolBar();

        toolbarButtonOpen = makeToolbarButton("general/Open24", ACTION_OPEN, "Open Kappa file", "Open");
        toolbar.add(toolbarButtonOpen);

        frame.getContentPane().add(toolbar, BorderLayout.NORTH);

    }

    private JButton makeToolbarButton(String imageName, String actionCommand, String toolTipText, String altText) {
        String imgLocation = TOOLBAR_BUTTON_IMAGE_PATH + imageName + ".gif";
        URL imageURL = MeanPlotViewer.class.getResource(imgLocation);

        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(this);

        if (imageURL != null) {
            button.setIcon(new ImageIcon(imageURL, altText));
        }
        else {
            button.setText(altText);
            System.err.println("Resource not found: " + imgLocation);
        }

        return button;
    }

    


    
    void runSimulation() {
    	if (inputFiles == null || inputFiles.length == 0) {
    		return;
    	}
    	
    	try {
            consoleTextArea.setText("");
            debugTextArea.setText("");
            textAreaData.setText("");
            
            setStatus(ToolbarMode.PROCESSING, STATUS_STARTING_SIMULATION);

            List<TimePoint> timePoints = MeanPlotTools.getDataPoints(inputFiles);
            
            setStatus(ToolbarMode.PROCESSING, STATUS_PLOTTING);
            
            String simulationName = "Aggregate Plot";

            boolean firstPoint = true;
            for (TimePoint timePoint : timePoints) {
            	if (firstPoint) {
            		createCellMeanChart(simulationName, timePoint);
            		firstPoint = false;
            	}
            	else {
            		observation(timePoint);
            	}
            }

            for (int index = 0; index < cellChartData.getSeriesCount(); index++) {
                cellChartData.getSeries(index).setNotify(true);
                cellChartData.getSeries(index).fireSeriesChanged();
            }
            
            textAreaData.setText(MeanPlotTools.getTimepointOutput(timePoints));
            
            setStatus(ToolbarMode.COMPLETE, STATUS_COMPLETE);

        }
        catch (Exception e) {
            handleException(e);
            setStatus(ToolbarMode.START, "Processing failed. Check console for error");
        }
    }
    
    private void setStatus(final ToolbarMode mode, final String statusText) {
        try {
		    SwingUtilities.invokeAndWait(new Runnable() {
		        @Override
				public void run() {
		            toolbarButtonOpen.setEnabled(mode != ToolbarMode.PROCESSING);
		            toolbar.repaint();
		            textStatus.setText(statusText);
		        }
		    });
        }
        catch (Exception e1) {
            handleException(e1);
        }
    }

	private void createCellMeanChart(String simulationName, TimePoint timePoint) {
        cellChartData = new XYIntervalSeriesCollection();
        for (Observable observable : timePoint.observables) {
            double mean = observable.mean;
            double stdDev = observable.stdDev;
            XYIntervalSeries series = new XYIntervalSeries(observable.name);
            series.setNotify(false);
            series.add(timePoint.time, timePoint.time, timePoint.time, mean, mean - stdDev, mean + stdDev);
            cellChartData.addSeries(series);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(simulationName, "Time", "Quantity", cellChartData, PlotOrientation.VERTICAL, true, false, false);
        DeviationRenderer renderer = new DeviationRenderer(true, false);
        renderer.setSeriesFillPaint(0, Color.RED);
        renderer.setSeriesFillPaint(1, Color.GREEN);
        renderer.setSeriesFillPaint(2, Color.BLUE);
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesPaint(1, Color.GREEN);
        renderer.setSeriesPaint(2, Color.BLUE);
        chart.getXYPlot().setRenderer(renderer);
        
        cellMeanChartPanel.setChart(chart);
    }


    
    @Override
	public void actionPerformed(ActionEvent event) {
        if (ACTION_OPEN.equals(event.getActionCommand())) {
            actionOpenFile();
        }
    }



    private void actionOpenFile() {
        JFileChooser fileChooser;
        if (inputFiles != null && inputFiles.length > 0) {
            fileChooser = new JFileChooser(inputFiles[0].getParentFile());
        }
        else {
            fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
        }
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setApproveButtonText("Plot");
        
        if (inputFiles != null && inputFiles.length > 0) {
            fileChooser.setSelectedFiles(inputFiles);
        }
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            inputFiles = fileChooser.getSelectedFiles();
            new Thread() {
                @Override
                public void run() {
                    runSimulation();
                }
            }.start();
        }
    }


    private void handleException(Exception e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
        tabbedPane.setSelectedComponent(consoleTextPane);
    }


    private void observation(final TimePoint timePoint) {
        try {
        	int index = 0;
            for (Observable observable : timePoint.observables) {
            	double mean = observable.mean;
            	double stdDev = observable.stdDev;
                XYIntervalSeries series = cellChartData.getSeries(index++);
                series.add(timePoint.time, timePoint.time, timePoint.time, mean, mean - stdDev, mean + stdDev);
            }
        }
        catch (Exception e) {
            handleException(e);
        }

    }
    
    static class ConsoleOutputStream extends FilterOutputStream {

        private JTextArea textArea;
        
        public ConsoleOutputStream(JTextArea textArea) {
            super(new ByteArrayOutputStream());
            this.textArea = textArea;
        }

        @Override
        public void write(byte[] b) throws IOException {
            textArea.append(new String(b));
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            textArea.append(new String(b, off, len));
        }
        
        @Override
        public void write(int b) throws IOException {
            textArea.append(Character.toString((char) b));
        }
    }

    

    public static void main(String[] args) throws Exception {
        MeanPlotViewer plotViewer = new MeanPlotViewer();
        plotViewer.runSimulation();
//        if (args.length == 1) {
//            File kappaFile = new File(args[0]);
//            if (kappaFile.exists()) {
//                simulator.openFile(kappaFile);
//                simulator.runSimulation();
//            }
//        }
    }
}
