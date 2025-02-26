package mdpa.gdpr.dfdconverter.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
import mdpa.gdpr.dfdconverter.GDPR2DFD;
import mdpa.gdpr.metamodel.GDPR.Collecting;
import mdpa.gdpr.metamodel.GDPR.Consent;
import mdpa.gdpr.metamodel.GDPR.Controller;
import mdpa.gdpr.metamodel.GDPR.GDPRFactory;
import mdpa.gdpr.metamodel.GDPR.LegalAssessmentFacts;
import mdpa.gdpr.metamodel.GDPR.LegalBasis;
import mdpa.gdpr.metamodel.GDPR.NaturalPerson;
import mdpa.gdpr.metamodel.GDPR.PersonalData;
import mdpa.gdpr.metamodel.GDPR.Processing;
import mdpa.gdpr.metamodel.GDPR.Purpose;

public class ScalabilityTests {
	private static dataflowdiagramFactory dfdFactory = dataflowdiagramFactory.eINSTANCE;
	private static datadictionaryFactory ddFactory = datadictionaryFactory.eINSTANCE;
	private static GDPRFactory gdprFactory = GDPRFactory.eINSTANCE;
	private static final String resultFolder = "C:\\Users\\Huell\\Documents\\Studium\\HIWI\\GDPR2DFD-Converter\\tests\\mdpa.gdpr.dfdconverter.tests\\results\\Scalability\\";
	
	private static final int max = 6;
	
	
	@Test
	public void runDFDTest() {	
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFolder + "timing-results-nodes.txt"))) {
        
        // We'll record timing for the last 10 iterations.
        // The 1st iteration (i=0) will be treated as a warm-up and not recorded.
        long[][] recordedDurations = new long[10][7];
        int recordIndex = 0;

        // Run the loop 11 times in total
        IntStream.range(0, 11).forEach(i -> {
        	for (int j = 0; j < max; j ++) {
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
	
	                try {
						writer.write("Iteration " + i + " took " + durationMs + " ms " +
						             "with " + dataFlowDiagram.getNodes().size() + " nodes.");
						writer.newLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	                
	            } 
	            
	        }
        }
        );
     // Calculate and write the average of the 10 recorded durations
        
        for (int j = 0; j < max; j++ ) {
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
	
	private static LegalAssessmentFacts createProcessingLAF(int exponent) {
		LegalAssessmentFacts laf = gdprFactory.createLegalAssessmentFacts();
		
		Collecting start = gdprFactory.createCollecting();
		PersonalData data = gdprFactory.createPersonalData();
		NaturalPerson user = gdprFactory.createNaturalPerson();
		data.getDataReferences().add(user);
		laf.getProcessing().add(start);
		laf.getInvolvedParties().add(user);
		laf.getData().add(data);
		start.setEntityName("collecting");
		data.setEntityName("data");
		user.setEntityName("user");
		
		Purpose purposeCollecting = gdprFactory.createPurpose();
		purposeCollecting.setEntityName("purposeCollecting");
		start.getPurpose().add(purposeCollecting);
		laf.getPurposes().add(purposeCollecting);
		
		Consent consent = gdprFactory.createConsent();
		consent.setEntityName("consent");
		consent.getForPurpose().add(purposeCollecting);
		consent.setPersonalData(data);
		consent.setConsentee(user);
		start.getOnTheBasisOf().add(consent);
		laf.getLegalBases().add(consent);
		
		Controller controller = gdprFactory.createController();
		start.setResponsible(controller);
		start.getOutputData().add(data);
		controller.setEntityName("Controller");
		laf.getInvolvedParties().add(controller);
		
		List<Processing> currentProcessing = new ArrayList<>();
		currentProcessing.add(start);
		
		
		for (int i = 0; i < exponent; i++) {
			List<Processing> newCurrent = new ArrayList<>();
			for (Processing current : currentProcessing) {
				for (int j = 0; j < (i == 0 ? 9 : 10); j++) {
					Processing processing = gdprFactory.createProcessing();
					processing.getInputData().add(data);
					if (i < exponent - 1) processing.getOutputData().add(data);
					processing.getOnTheBasisOf().add(consent);
					processing.setResponsible(controller);
					processing.getPurpose().add(purposeCollecting);
					current.getFollowingProcessing().add(processing);
				
					newCurrent.add(processing);
					laf.getProcessing().add(processing);
				}			
			}
			currentProcessing = newCurrent;
		}		
		
		
		return laf;
	}
	
	public LegalAssessmentFacts createRoleLaf(int exponent) {
		LegalAssessmentFacts laf = createProcessingLAF(1);
		
		PersonalData data = laf.getData().stream().filter(PersonalData.class::isInstance).map(PersonalData.class::cast).findAny().orElseThrow();
		
		for (int i = 0; i < Math.pow(10, exponent) - 2; i++) {
			NaturalPerson person = gdprFactory.createNaturalPerson();
			person.setEntityName("person" + i);
			data.getDataReferences().add(person);
			laf.getInvolvedParties().add(person);
			
			Consent consent = gdprFactory.createConsent();
			consent.setConsentee(person);
			consent.getForPurpose().addAll(laf.getPurposes());
			consent.setPersonalData(data);
			
			laf.getProcessing().forEach(processing -> processing.getOnTheBasisOf().add(consent));
			laf.getLegalBases().add(consent);			
		}
			
		return laf;
	}
	
	public LegalAssessmentFacts createPurposeLaf(int exponent) {
		LegalAssessmentFacts laf = createProcessingLAF(1);
		Consent consent = laf.getLegalBases().stream().filter(Consent.class::isInstance).map(Consent.class::cast).findAny().orElseThrow();
		
		for (int i = 0; i < Math.pow(10, exponent) - 1; i++) { 
			Purpose purpose = gdprFactory.createPurpose();
			laf.getPurposes().add(purpose);
			laf.getProcessing().forEach(processing -> processing.getPurpose().add(purpose));
			consent.getForPurpose().add(purpose);
		} 
		
		return laf;
	}
	
	@Test
	public void runGDPRProcessingTest() {	
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFolder + "timing-results-processing.txt"))) {
        
        // We'll record timing for the last 10 iterations.
        // The 1st iteration (i=0) will be treated as a warm-up and not recorded.
        long[][] recordedDurations = new long[10][7];
        int recordIndex = 0;

        // Run the loop 11 times in total
        for (int i = 0; i < 11; i++) {
        	for (int j = 0; j < max; j ++) {
	
	           LegalAssessmentFacts laf = createProcessingLAF(j); 
	            GDPR2DFD converter = new GDPR2DFD(laf);
	
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
	                             "with " + laf.getProcessing().size() + " processing elements.");
	                writer.newLine();
	            } 
	            
	        }
        }
     // Calculate and write the average of the 10 recorded durations
        
        for (int j = 0; j < max; j++ ) {
        	long sum = 0;
	        for (int i = 0; i < 10; i++) {
		        sum += recordedDurations[i][j];
	        }
	        double average = sum / ((double) 10);
	        writer.write("Average of the 10 recorded runs for " + Math.pow(10, j) + " processing:" + average + " ms.");
	        writer.newLine();
        }
       

        
    } catch (IOException e) {
        e.printStackTrace();
    }    
}
	
	@Test
	public void runGDPRoleTest() {	
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFolder + "timing-results-roles.txt"))) {
        
        // We'll record timing for the last 10 iterations.
        // The 1st iteration (i=0) will be treated as a warm-up and not recorded.
        long[][] recordedDurations = new long[10][7];
        int recordIndex = 0;

        // Run the loop 11 times in total
        for (int i = 0; i < 11; i++) {
        	for (int j = 0; j < max; j ++) {
	
	           LegalAssessmentFacts laf = createRoleLaf(j); 
	            GDPR2DFD converter = new GDPR2DFD(laf);
	
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
	                             "with " + laf.getInvolvedParties().size() + " role elements.");
	                writer.newLine();
	            } 
	            
	        }
        }
     // Calculate and write the average of the 10 recorded durations
        
        for (int j = 0; j < max; j++ ) {
        	long sum = 0;
	        for (int i = 0; i < 10; i++) {
		        sum += recordedDurations[i][j];
	        }
	        double average = sum / ((double) 10);
	        writer.write("Average of the 10 recorded runs for " + (Math.pow(10, j) - 1) + " roles:" + average + " ms.");
	        writer.newLine();
        }
       

        
    } catch (IOException e) {
        e.printStackTrace();
    }    
}
	
	@Test
	public void runGDPRPurposeTest() {	
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFolder + "timing-results-purposes.txt"))) {
        
        // We'll record timing for the last 10 iterations.
        // The 1st iteration (i=0) will be treated as a warm-up and not recorded.
        long[][] recordedDurations = new long[10][7];
        int recordIndex = 0;

        // Run the loop 11 times in total
        for (int i = 0; i < 11; i++) {
        	for (int j = 0; j < max; j ++) {
	
	           LegalAssessmentFacts laf = createPurposeLaf(j);
	            GDPR2DFD converter = new GDPR2DFD(laf);
	
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
	                             "with " + laf.getPurposes().size() + " purpose elements.");
	                writer.newLine();
	            } 
	            
	        }
        }
     // Calculate and write the average of the 10 recorded durations
        
        for (int j = 0; j < max; j++ ) {
        	long sum = 0;
	        for (int i = 0; i < 10; i++) {
		        sum += recordedDurations[i][j];
	        }
	        double average = sum / ((double) 10);
	        writer.write("Average of the 10 recorded runs for " + Math.pow(10, j) + " purposes:" + average + " ms.");
	        writer.newLine();
        }
       

        
    } catch (IOException e) {
        e.printStackTrace();
    }    
}
	
}
