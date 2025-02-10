package mdpa.gdpr.dfdconverter.tests;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.dfd.dataflowdiagram.Process;
import org.dataflowanalysis.dfd.dataflowdiagram.Store;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import mdpa.gdpr.dfdconverter.DFD2GDPR;

public class ScalabilityTests {
	private static dataflowdiagramFactory dfdFactory = dataflowdiagramFactory.eINSTANCE;
	private static datadictionaryFactory ddFactory = datadictionaryFactory.eINSTANCE;
	
	
	@Test
	public void runDFDTest() {	
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Huell\\Documents\\Studium\\HIWI\\GDPR2DFD-Converter\\tests\\mdpa.gdpr.dfdconverter.tests\\results\\Scalability\\timing-results.txt"))) {
        
        // We'll record timing for the last 10 iterations.
        // The 1st iteration (i=0) will be treated as a warm-up and not recorded.
        long[][] recordedDurations = new long[10][7];
        int recordIndex = 0;

        // Run the loop 11 times in total
        for (int i = 0; i < 11; i++) {
        	for (int j = 0; j < 7; j ++) {
	            var dataDictionary = ddFactory.createDataDictionary();
	            var dataFlowDiagram = dfdFactory.createDataFlowDiagram();
	
	            dfdBuilder(dataFlowDiagram, dataDictionary, j);
	            DFD2GDPR converter = new DFD2GDPR(dataFlowDiagram, dataDictionary);
	
	            // Start timing
	            long startTime = System.nanoTime();
	
	            // The main operation you want to measure
	            converter.transform();
	
	            // End timing
	            long endTime = System.nanoTime();
	
	            // Convert nanoseconds to milliseconds
	            long durationMs = (endTime - startTime) / 1_000_000;
	
	            // We only record (and write) the timing data for i > 0
	            if (i > 0) {
	                recordedDurations[i-1][j] = durationMs;
	
	                writer.write("Iteration " + i + " took " + durationMs + " ms " +
	                             "with " + dataFlowDiagram.getNodes().size() + " nodes.");
	                writer.newLine();
	            } 
	            
	        }
        }
     // Calculate and write the average of the 10 recorded durations
        
        for (int j = 0; j < 7; j++ ) {
        	long sum = 0;
	        for (int i = 0; i < 10; i++) {
		        sum += recordedDurations[i][j];
	        }
	        double average = sum / ((double) 10);
	        writer.write("Average of the 10 recorded runs for " + Math.pow(10, j) + " nodes:" + average + " ms.");
	        writer.newLine();
        }
       

        
    } catch (IOException e) {
        e.printStackTrace();
    }    
}
	
	private static void dfdBuilder(DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary, int exponent) {
		Node start = dfdFactory.createExternal();
		Behavior behavior = ddFactory.createBehavior();
		start.setBehavior(behavior);
		dataDictionary.getBehavior().add(behavior);
		dataFlowDiagram.getNodes().add(start);
		
		
		List<Node> currentNodes = new ArrayList<>();	
		currentNodes.add(start);
		for (int i = 0; i < exponent; i++) {
			List<Node> newCurrentNodes = new ArrayList<>();
			if (i == 0) {
				currentNodes.forEach(node -> newCurrentNodes.addAll(createFollowers(9, node, dataFlowDiagram, dataDictionary, Store.class)));
			} else if (i == exponent - 1) {
				currentNodes.forEach(node -> newCurrentNodes.addAll(createFollowers(10, node, dataFlowDiagram, dataDictionary, Store.class)));
			} else {
				currentNodes.forEach(node -> newCurrentNodes.addAll(createFollowers(10, node, dataFlowDiagram, dataDictionary, Process.class)));
			}			
			currentNodes = newCurrentNodes;
		}
	}
	
	private static List<Node> createFollowers(int numberOfFollowers, Node node, DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary, Class<? extends Node> nodeType) {
		List<Node> followers = new ArrayList<>();
		for (int i = 0; i < numberOfFollowers; i++) {
			Node follower;
			if (nodeType.equals(Store.class)) {
				follower = dfdFactory.createStore();
			} else {
				follower = dfdFactory.createProcess();
			}
			Behavior behavior = ddFactory.createBehavior();
			follower.setBehavior(behavior);
			dataDictionary.getBehavior().add(behavior);
			dataFlowDiagram.getNodes().add(follower);
			
			dataFlowDiagram.getFlows().add(createAndAndAddFlow(node, follower));
			followers.add(follower);
		}
		return followers;
	}
	
	
	private static Flow createAndAndAddFlow(Node sourceNode, Node destinationNode) {
		Flow flow = dfdFactory.createFlow();
		flow.setSourceNode(sourceNode);
		flow.setDestinationNode(destinationNode);
		
		Pin sourcePin = ddFactory.createPin();
		sourcePin.setEntityName("out_");
		sourceNode.getBehavior().getOutPin().add(sourcePin);
		flow.setSourcePin(sourcePin);
		
		Pin destinationPin = ddFactory.createPin();
		destinationNode.getBehavior().getInPin().add(destinationPin);
		flow.setDestinationPin(destinationPin);
		
		return flow;
	}
}
