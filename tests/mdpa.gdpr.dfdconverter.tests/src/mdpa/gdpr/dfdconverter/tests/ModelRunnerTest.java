package mdpa.gdpr.dfdconverter.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.*;
import org.dataflowanalysis.converter.PCMConverter;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Test;

import mdpa.gdpr.dfdconverter.DFD2GDPR;
import mdpa.gdpr.dfdconverter.GDPR2DFD;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.TraceModel;
import mdpa.gdpr.metamodel.GDPR.LegalAssessmentFacts;

public class ModelRunnerTest {		

    // Set the root directory where all subfolders are located
    private static final String ROOT_DIR = "C:\\Users\\Huell\\Documents\\Studium\\HIWI\\ExampleModels\\bundles\\org.dataflowanalysis.examplemodels\\casestudies\\TUHH-Models\\";
    private static final String ROOT_DIR_PCM = "C:\\Users\\Huell\\Documents\\Studium\\HIWI\\ExampleModels\\bundles\\org.dataflowanalysis.examplemodels\\casestudies\\";
    private static final String resultFolderBase = "C:\\Users\\Huell\\Documents\\Studium\\HIWI\\GDPR2DFD-Converter\\tests\\mdpa.gdpr.dfdconverter.tests\\results\\Models\\";
    private static final String resultFolderBasePCM = "C:\\Users\\Huell\\Documents\\Studium\\HIWI\\GDPR2DFD-Converter\\tests\\mdpa.gdpr.dfdconverter.tests\\results\\Models\\PCM\\";


    public static Collection<File[]> data() {
        List<File[]> testData = new ArrayList<>();

        File root = new File(ROOT_DIR);
        File[] subFolders = root.listFiles(File::isDirectory);
        if (subFolders == null) {
            return testData; // no subfolders found
        }

        // For each subfolder, find matching .model1/.model2 pairs
        for (File subFolder : subFolders) {
            // Map baseName -> File for .model1
            // Map baseName -> File for .model2
            // OR we can do simpler logic by scanning once and matching
            File[] files = subFolder.listFiles();
            if (files == null) {
                continue;
            }

            // Step 1: store .model1 files in a map by their baseName (the part before .model1)
            // Step 2: check if there's a corresponding .model2 in the same subfolder
            // A simple approach: read all files into two maps

            // For quick matching, use a dictionary of <baseName, File>
            java.util.Map<String, File> model1Map = new java.util.HashMap<>();
            java.util.Map<String, File> model2Map = new java.util.HashMap<>();

            for (File f : files) {
                String name = f.getName();
                if (name.endsWith(".dataflowdiagram")) {
                    String baseName = name.substring(0, name.length() - ".dataflowdiagram".length());
                    model1Map.put(baseName, f);
                } else if (name.endsWith(".datadictionary")) {
                    String baseName = name.substring(0, name.length() - ".datadictionary".length());
                    model2Map.put(baseName, f);
                }
            }

            // Now find all baseNames that exist in both maps
            for (String baseName : model1Map.keySet()) {
                if (model2Map.containsKey(baseName)) {
                    File model1File = model1Map.get(baseName);
                    File model2File = model2Map.get(baseName);

                    // Add to test data
                    testData.add(new File[] { model1File, model2File });
                }
            }
        }

        return testData;
    }

    @Test
    public void testModelPair() {
        // Here is where you do something with model1File and model2File
        // For example, parse them, compare them, run some logic, etc.

        var data = convertPalladio();
        data.stream().forEach(file -> {
        
        	File dfdFile = file[0];
        	File ddFile = file[1];
        	String name = dfdFile.getName().substring(0, dfdFile.getName().length() - ".dataflowdiagram".length());
        	String resultFolder = resultFolderBase + name + "\\";
        	
        	ResourceSet rs = new ResourceSetImpl();
        	rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    		rs.getPackageRegistry().put(dataflowdiagramPackage.eNS_URI, dataflowdiagramPackage.eINSTANCE);
        	Resource dfdResource = rs.getResource(URI.createFileURI(dfdFile.toString()), true);
        	Resource ddResource = rs.getResource(URI.createFileURI(ddFile.toString()), true);
        	EcoreUtil.resolveAll(rs);
        	DataFlowDiagram dfd = (DataFlowDiagram) dfdResource.getContents().get(0);
        	DataDictionary dd = (DataDictionary) ddResource.getContents().get(0);
        	
        	if (dfdFile.getParentFile().toString().contains("PCM")) {
        		resultFolder = dfdFile.getParentFile().toString() + "\\";
        		System.out.println(resultFolder);
        	}
        	else {
	        	File subFolder = new File(resultFolderBase, name);
	            // Make sure the subfolder exists
	            if (!subFolder.exists()) {
	                subFolder.mkdirs();
	            }
	        	
	            try {
	                // Copy the DFD file
	                Files.copy(dfdFile.toPath(),
	                           Paths.get(subFolder.getAbsolutePath(), dfdFile.getName()),
	                           StandardCopyOption.REPLACE_EXISTING);
	
	                // Copy the DD file
	                Files.copy(ddFile.toPath(),
	                           Paths.get(subFolder.getAbsolutePath(), ddFile.getName()),
	                           StandardCopyOption.REPLACE_EXISTING);
	            } catch (IOException e) {
	                // Decide how you want to handle or log the error
	                e.printStackTrace();
	            }
        	}
        	DFD2GDPR dfd2gdpr = new DFD2GDPR(dfdFile.toString(), ddFile.toString());
        	dfd2gdpr.transform();
        	dfd2gdpr.save(resultFolder + name + "D2G.gdpr", resultFolder + name + "D2G.tracemodel");        	
        	
        	LegalAssessmentFacts laf = dfd2gdpr.getLegalAssessmentFacts();
        	TraceModel trace = dfd2gdpr.getDFD2GDPRTrace();

        	assertEquals(dfd.getNodes().size(), laf.getProcessing().size());
        	
        	GDPR2DFD gdpr2dfd = new GDPR2DFD(laf, dd, trace);
        	gdpr2dfd.transform();
        	gdpr2dfd.save(resultFolder + name + "D2G2D.dataflowdiagram", resultFolder + name + "D2G2D.datadictionary", resultFolder + name + "D2G2D.tracemodel");        	
        	
        	
        	DataFlowDiagram dfd2 = gdpr2dfd.getDataFlowDiagram();
        	DataDictionary dd2 = gdpr2dfd.getDataDictionary();   
        	trace = gdpr2dfd.getGDPR2DFDTrace();

        	assertEquals(dfd2.getNodes().size(), laf.getProcessing().size());
        	
        	DFD2GDPR dfd2gdpr2 = new DFD2GDPR(dfd2, dd2, trace);
        	dfd2gdpr2.transform();
        	dfd2gdpr2.save(resultFolder + name + "D2G2D2G.gdpr", resultFolder + name + "D2G2D2G.tracemodel");
        	
        	LegalAssessmentFacts laf2 = dfd2gdpr.getLegalAssessmentFacts();
        	trace = dfd2gdpr2.getDFD2GDPRTrace();

        	assertEquals(dfd.getNodes().size(), laf2.getProcessing().size());
        	
        	GDPR2DFD gdpr2dfd2 = new GDPR2DFD(laf2, dd2, trace);
        	gdpr2dfd2.transform();
        	gdpr2dfd2.save(resultFolder + name + "D2G2D2G2D.dataflowdiagram", resultFolder + name + "D2G2D2G2D.datadictionary", resultFolder + name + "D2G2D2G2D.tracemodel");
        	
        	assertEquals(gdpr2dfd2.getDataFlowDiagram().getNodes().size(), dfd.getNodes().size());
        	        	
        	annotateMetaData(resultFolder + name, resultFolder);
        
        });
        // ... your actual test logic here ...
    }
	
    public static Collection<File[]> dataPCM() {
        List<File[]> testData = new ArrayList<>();

        File root = new File(ROOT_DIR_PCM);
        File[] subFolders = root.listFiles(File::isDirectory);
        if (subFolders == null) {
            return testData; // no subfolders found
        }

        // For each subfolder, find matching .model1/.model2 pairs
        for (File subFolder : subFolders) {
            // Map baseName -> File for .model1
            // Map baseName -> File for .model2
            // OR we can do simpler logic by scanning once and matching
            File[] files = subFolder.listFiles();
            if (files == null) {
                continue;
            }

            // Step 1: store .model1 files in a map by their baseName (the part before .model1)
            // Step 2: check if there's a corresponding .model2 in the same subfolder
            // A simple approach: read all files into two maps

            // For quick matching, use a dictionary of <baseName, File>
            java.util.Map<String, File> model1Map = new java.util.HashMap<>();
            java.util.Map<String, File> model2Map = new java.util.HashMap<>();
            java.util.Map<String, File> model3Map = new java.util.HashMap<>();

            for (File f : files) {
                String name = f.getName();
                if (name.endsWith(".usagemodel")) {
                    String baseName = name.substring(0, name.length() - ".usagemodel".length());
                    model1Map.put(baseName, f);
                } else if (name.endsWith(".allocation")) {
                    String baseName = name.substring(0, name.length() - ".allocation".length());
                    model2Map.put(baseName, f);
                } else if (name.endsWith(".nodecharacteristics")) {
                    String baseName = name.substring(0, name.length() - ".nodecharacteristics".length());
                    model3Map.put(baseName, f);
                }
            }

            // Now find all baseNames that exist in both maps
            for (String baseName : model1Map.keySet()) {
                if (model2Map.containsKey(baseName) && model3Map.containsKey(baseName)) {
                    File model1File = model1Map.get(baseName);
                    File model2File = model2Map.get(baseName);
                    File model3File = model3Map.get(baseName);

                    // Add to test data
                    testData.add(new File[] { model1File, model2File, model3File});
                }
            }
        }

        return testData;
    }
    
    
    public static Collection<File[]> convertPalladio() {
    	List<File[]> testData = new ArrayList<>();
    	var pcmData = dataPCM();
    	System.out.println(pcmData.size());
    	pcmData.stream().forEach(file -> {
    		String modelLocation = file[0].getParentFile().toString();
    		String name = file[0].getParentFile().getName();
    		File usageFile = file[0];
    		File allocationFile = file[1];
    		File nodeCharFile = file[2];
    		
    		String resultFolder = resultFolderBasePCM + "\\" + name + "\\";
        	
        	File subFolder = new File(resultFolderBasePCM, name);
            // Make sure the subfolder exists
	            if (!subFolder.exists()) {
	                subFolder.mkdirs();
	            }
    		PCMConverter pcmConverter = new PCMConverter();
        	var dfd = pcmConverter.pcmToDFD("", usageFile.toString(), allocationFile.toString(), nodeCharFile.toString());
        	pcmConverter.storeDFD(dfd, resultFolder + name);
        	testData.add(new File[] {new File(resultFolder + name + ".dataflowdiagram"), new File(resultFolder + name + ".datadictionary")});
    	});
    	return testData;
    }
    
    public static void annotateMetaData(String fileName, String folder) {
    	StringBuilder builder = new StringBuilder();
    	var newFileName = fileName;
    	for (int i = 0; i < 3; i++) {
    		//Reload DD to get accurate numbers
        	var rs = new ResourceSetImpl();
        	rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        	rs.getPackageRegistry().put(dataflowdiagramPackage.eNS_URI, dataflowdiagramPackage.eINSTANCE);
        	var dfdResource = rs.getResource(URI.createFileURI(newFileName + ".dataflowdiagram"), true);
        	var ddResource = rs.getResource(URI.createFileURI(newFileName + ".datadictionary"), true);
        	EcoreUtil.resolveAll(rs);
        	var dfd = (DataFlowDiagram) dfdResource.getContents().get(0);
        	var dd = (DataDictionary) ddResource.getContents().get(0);
        	
    		builder.append("DFD Cycle " + (2 * i) + ":").append("\n");
    		builder.append("Number of Nodes: ").append(dfd.getNodes().size()).append("\n");
    		builder.append("Number of Flows: ").append(dfd.getFlows().size()).append("\n");
    		builder.append("Number of Properties: ").append(dfd.getNodes().stream().flatMap(n -> n.getProperties().stream()).count()).append("\n");
    		
    		builder.append("DD Cycle " + (2 * i) + ":").append("\n");
    		builder.append("Number of LabelTypes: ").append(dd.getLabelTypes().size()).append("\n");
    		builder.append("Number of Labels: ").append(dd.getLabelTypes().stream().flatMap(lt -> lt.getLabel().stream()).count()).append("\n");     
    		
    		
    		if (i == 0) newFileName += "D2G2D";
    		else newFileName += "2G2D";
    	}
    	
    	newFileName = fileName;
    	for (int i = 0; i < 4; i++) {
    		if (i == 0) newFileName += "D2G";
    		else if (i % 2 == 1) newFileName += "2D";
    		else newFileName += "2G";
    		
    		var rs = new ResourceSetImpl();
        	rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    		var tracemodelResource = rs.getResource(URI.createFileURI(newFileName + ".tracemodel"), true);
    		var tracemodelD2G = (TraceModel) tracemodelResource.getContents().get(0);
    		
    		builder.append("TraceModel Cycle " + i + ":").append("\n");
    		builder.append("Number of FlowTraces: ").append(tracemodelD2G.getFlowTraces().size()).append("\n");
    		builder.append("Number of FlowTraces: ").append(tracemodelD2G.getNodeTraces().size()).append("\n");
    		builder.append("Number of LabelTraces: ").append(tracemodelD2G.getLabelTraces().size()).append("\n");    		
			  
    	}
    	
    	newFileName = fileName + "D2G";
    	for (int i = 0; i < 2; i++) {
    		var rs = new ResourceSetImpl();
        	rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        	var gdprResource = rs.getResource(URI.createFileURI(newFileName + ".gdpr"), true);
			var laf = (LegalAssessmentFacts) gdprResource.getContents().get(0);
			
			builder.append("GDPR Cycle " + (i+1) + ":").append("\n");
			builder.append("Number of Processings: ").append(laf.getProcessing().size()).append("\n");
			builder.append("Number of Data: ").append(laf.getData().size()).append("\n");
			builder.append("Number of Purposes: ").append(laf.getPurposes().size()).append("\n");
			
			newFileName += "2D2G";
    	}		
    	
    	try (BufferedWriter writer = new BufferedWriter(new FileWriter(folder + "metadata.txt"))) {
    		writer.write(builder.toString());
    	} catch (Exception e) {
			// TODO: handle exception
		}
    } 
}
