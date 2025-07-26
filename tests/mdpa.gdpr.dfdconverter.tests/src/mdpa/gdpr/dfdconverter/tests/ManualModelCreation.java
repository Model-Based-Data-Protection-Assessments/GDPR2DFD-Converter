package mdpa.gdpr.dfdconverter.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.External;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.dfd.dataflowdiagram.Store;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.junit.jupiter.api.Test;

import mdpa.gdpr.dfdconverter.DFD2GDPR;
import mdpa.gdpr.dfdconverter.GDPR2DFD;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.TraceModel;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.TracemodelFactory;
import mdpa.gdpr.metamodel.GDPR.*;
import mdpa.gdpr.metamodel.GDPR.LegalAssessmentFacts;

public class ManualModelCreation {
	final static dataflowdiagramFactory dfdFactory = dataflowdiagramFactory.eINSTANCE;
	final static datadictionaryFactory ddFactory = datadictionaryFactory.eINSTANCE;
	final static TracemodelFactory tmFactory = TracemodelFactory.eINSTANCE;
	final static GDPRFactory gdprFactory = GDPRFactory.eINSTANCE;    	
	
	@Test
	 public void manualModels() {  				
	    var dfd = dfdFactory.createDataFlowDiagram();
	    var dd = ddFactory.createDataDictionary();
	    var tm = tmFactory.createTraceModel();
	    var laf = gdprFactory.createLegalAssessmentFacts();
		 
		buildSion(dfd, dd, tm, laf);
		
		runTransformations(dfd, dd, tm, laf, "sion");
		
		dfd = dfdFactory.createDataFlowDiagram();
	    dd = ddFactory.createDataDictionary();
	    tm = tmFactory.createTraceModel();
	    laf = gdprFactory.createLegalAssessmentFacts();
		 
		buildAlshareef(dfd, dd, tm, laf);
		
		runTransformations(dfd, dd, tm, laf, "alshareef");
	 }
	 
	 private void runTransformations(DataFlowDiagram dfd, DataDictionary dd, TraceModel trace, LegalAssessmentFacts laf, String name) {
		 File subFolder = new File(ModelRunnerTest.resultFolderBase, name);
         // Make sure the subfolder exists
         if (!subFolder.exists()) {
             subFolder.mkdirs();
         }
		 var resultFolder = subFolder.toString() + "\\";
         
		 ResourceSet rs = new ResourceSetImpl();
		 rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		 var dfdresource = createAndAddResource(resultFolder + name + ".dataflowdiagram", new String[] {".dataflowdiagram"}, rs);
		 var ddresource = createAndAddResource(resultFolder + name + ".datadictionary", new String[] {".datadictionary"}, rs);
		 dfdresource.getContents().add(dfd);
		 ddresource.getContents().add(dd);
		 saveResource(dfdresource);
		 saveResource(ddresource);
		 var gdprResource = createAndAddResource(resultFolder + name + "D2G.gdpr", new String[] {".gdpr"}, rs);
		 var tmResource = createAndAddResource(resultFolder + name + "D2G.tracemodel", new String[] {".tracemodel"}, rs);
		 gdprResource.getContents().add(laf);
		 tmResource.getContents().add(trace);
		 saveResource(gdprResource);
		 saveResource(tmResource);
		 
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
	     	
	     	LegalAssessmentFacts laf2 = dfd2gdpr2.getLegalAssessmentFacts();
	     	trace = dfd2gdpr2.getDFD2GDPRTrace();

	     	
	     	GDPR2DFD gdpr2dfd2 = new GDPR2DFD(laf2, dd2, trace);
	     	gdpr2dfd2.transform();
	     	gdpr2dfd2.save(resultFolder + name + "D2G2D2G2D.dataflowdiagram", resultFolder + name + "D2G2D2G2D.datadictionary", resultFolder + name + "D2G2D2G2D.tracemodel");
	     	
	     	        	
	     	ModelRunnerTest.annotateMetaData(resultFolder + name, resultFolder);
	 }
	 
	 private static void buildAlshareef(DataFlowDiagram dfd, DataDictionary dd, TraceModel tm, LegalAssessmentFacts laf) {
		 Map<Node, Processing> mapNodeToProcessing = new HashMap<>();
		 
		 var appStore = createNode("App Store", External.class, mapNodeToProcessing, dfd, dd, laf, tm);
		 var smartphone = createNode("Smartphone", null, mapNodeToProcessing, dfd, dd, laf, tm);
		 var musicStore = createNode("Music Store", null, mapNodeToProcessing, dfd, dd, laf, tm);
		 var speaker = createNode("Speaker", null, mapNodeToProcessing, dfd, dd, laf, tm);
		 var provider = createNode("Provider", null, mapNodeToProcessing, dfd, dd, laf, tm);
		 var deviceOwner = createNode("Device owner", null, mapNodeToProcessing, dfd, dd, laf, tm);
		 var router = createNode("Router", Store.class, mapNodeToProcessing, dfd, dd, laf, tm);
		 var thirdPartyPartner = createNode("Third-party partner (children.stories.com)", Store.class, mapNodeToProcessing, dfd, dd, laf, tm);

		 var appStore2Smartphone = createFlow("App (install)", appStore, smartphone, mapNodeToProcessing, dfd);
		 var smartphone2MusicStore = createFlow("Password (login)", smartphone, musicStore, mapNodeToProcessing, dfd);
		 var smartphone2Provider = createFlow("App password (login)", smartphone, provider, mapNodeToProcessing, dfd);
		 var smartphone2Speaker = createFlow("Password, WiFi ID (register)", smartphone, speaker, mapNodeToProcessing, dfd);
		 
		 var provider2Smartphone = createFlow("Login response (login)", provider, smartphone, mapNodeToProcessing, dfd);
		 var provider2ThirdParty = createFlow("Stats of Voice data, CMD pairs (marketing)", provider, thirdPartyPartner, mapNodeToProcessing, dfd);
		 var provider2Speaker = createFlow("Token (authenticate), CMD (trigger Music Store, streaming, notify user)", provider, speaker, mapNodeToProcessing, dfd);

		 var deviceOwner2Speaker = createFlow("Voice request 1 (CMD processing, voice processing)", deviceOwner, speaker, mapNodeToProcessing, dfd);
		 var deviceOwner2Smartphone = createFlow("Password (login)", deviceOwner, smartphone, mapNodeToProcessing, dfd);
		 var deviceOwner2MusikStore = createFlow("Password (login)", deviceOwner, musicStore, mapNodeToProcessing, dfd);
		 
		 var speaker2DeviceOwner = createFlow("CMD (notify user), music (streaming)", speaker, deviceOwner, mapNodeToProcessing, dfd);
		 var speaker2MusicStore = createFlow("Playlist request (streaming)", speaker, musicStore, mapNodeToProcessing, dfd);
		 var speaker2Router = createFlow("Password, WiFi ID (register)", speaker, router, mapNodeToProcessing, dfd);
		 var speaker2Provider = createFlow("Request token, Token (authenticate), Voice data (CMD processing)", speaker, provider, mapNodeToProcessing, dfd);
		 
		 var musicStore2Speaker = createFlow("Music (streaming)", musicStore, speaker, mapNodeToProcessing, dfd);
		 var musikStore2Smartphone = createFlow("Token (authenticate)", musicStore, smartphone, mapNodeToProcessing, dfd);

		 var purposeInstall = createAndAddPurpose("instal", List.of(smartphone).stream().map(mapNodeToProcessing::get).toList(), laf);
		 var purposelogin = createAndAddPurpose("login", List.of(smartphone, provider, musicStore).stream().map(mapNodeToProcessing::get).toList(), laf);
		 var purposeAuthenticate = createAndAddPurpose("authenticate", List.of(musicStore, smartphone, provider, speaker).stream().map(mapNodeToProcessing::get).toList(), laf);
		 var purposeRegister = createAndAddPurpose("register", List.of(speaker, router).stream().map(mapNodeToProcessing::get).toList(), laf);
		 var purposeCMDprocessing = createAndAddPurpose("CMD processing", List.of(speaker, provider, thirdPartyPartner).stream().map(mapNodeToProcessing::get).toList(), laf);
		 var purposeNotify = createAndAddPurpose("notify user", List.of(speaker, deviceOwner).stream().map(mapNodeToProcessing::get).toList(), laf);
		 var purposetrigger = createAndAddPurpose("trigger music store", List.of(speaker).stream().map(mapNodeToProcessing::get).toList(), laf);
		 var purposeStreaming = createAndAddPurpose("streaming", List.of(musicStore, speaker, deviceOwner).stream().map(mapNodeToProcessing::get).toList(), laf);
		 var purposeVoice = createAndAddPurpose("voice processing", List.of(speaker).stream().map(mapNodeToProcessing::get).toList(), laf);
		 
		 var user = gdprFactory.createNaturalPerson();
		 user.setEntityName("user");
		 var guest = gdprFactory.createNaturalPerson();
		 user.setEntityName("guest");
		 laf.getInvolvedParties().add(user);
		 laf.getInvolvedParties().add(guest);
		 
		 var dataInstall = createAndAddData("", laf, null);
		 var dataLogin = createAndAddData("", laf, List.of(user, guest));
		 var dataAuthenticate = createAndAddData("", laf, List.of(user, guest));
		 var dataRegister = createAndAddData("", laf, List.of(user, guest));
		 var dataCMDprocessing = createAndAddData("", laf, List.of(user, guest));
		 var dataNotify = createAndAddData("", laf, List.of(user, guest));
		 var dataTrigger = createAndAddData("", laf, List.of(user, guest));
		 var dataStreaming = createAndAddData("", laf, List.of(user, guest));
		 var dataVoice = createAndAddData("", laf, List.of(user, guest));

		 addOutgoingDataAndTrace(appStore2Smartphone, mapNodeToProcessing, List.of(dataInstall), tm);
		 addOutgoingDataAndTrace(deviceOwner2Smartphone, mapNodeToProcessing, List.of(dataLogin), tm);
		 addOutgoingDataAndTrace(deviceOwner2MusikStore, mapNodeToProcessing, List.of(dataLogin), tm);
		 addOutgoingDataAndTrace(deviceOwner2Speaker, mapNodeToProcessing, List.of(dataCMDprocessing, dataVoice), tm);
		 addOutgoingDataAndTrace(smartphone2MusicStore, mapNodeToProcessing, List.of(dataAuthenticate), tm);
		 addOutgoingDataAndTrace(smartphone2Provider, mapNodeToProcessing, List.of(dataLogin), tm);
		 addOutgoingDataAndTrace(smartphone2Speaker, mapNodeToProcessing, List.of(dataRegister), tm);
		 addOutgoingDataAndTrace(musicStore2Speaker, mapNodeToProcessing, List.of(dataStreaming), tm);
		 addOutgoingDataAndTrace(musikStore2Smartphone, mapNodeToProcessing, List.of(dataAuthenticate), tm);
		 addOutgoingDataAndTrace(provider2Speaker, mapNodeToProcessing, List.of(dataAuthenticate, dataTrigger, dataStreaming, dataNotify), tm);
		 addOutgoingDataAndTrace(provider2Smartphone, mapNodeToProcessing, List.of(dataLogin), tm);
		 addOutgoingDataAndTrace(provider2ThirdParty, mapNodeToProcessing, List.of(dataCMDprocessing), tm);
		 addOutgoingDataAndTrace(speaker2DeviceOwner, mapNodeToProcessing, List.of(dataNotify, dataStreaming), tm);
		 addOutgoingDataAndTrace(speaker2MusicStore, mapNodeToProcessing, List.of(dataStreaming), tm);
		 addOutgoingDataAndTrace(speaker2Provider, mapNodeToProcessing, List.of(dataAuthenticate, dataCMDprocessing), tm);
		 addOutgoingDataAndTrace(speaker2Router, mapNodeToProcessing, List.of(dataRegister), tm);
	 
		 Stream.of(dataLogin, dataAuthenticate, dataRegister, dataCMDprocessing, dataNotify, dataTrigger, dataStreaming, dataVoice).forEach(data -> {
			 var consent = gdprFactory.createConsent();
			 consent.setConsentee(user);
			 consent.setPersonalData((PersonalData)data);
			 laf.getLegalBases().add(consent);
			 mapNodeToProcessing.values().forEach(p -> p.getOnTheBasisOf().add(consent));
		 });
		 
		 var controller = gdprFactory.createController();
		 controller.setEntityName("Controller");
		 laf.getInvolvedParties().add(controller);
		 mapNodeToProcessing.values().forEach(p -> p.setResponsible(controller));
	 }
	  
	 	private static Purpose createAndAddPurpose(String name, List<Processing> processings, LegalAssessmentFacts laf) {
	 		var purpose = gdprFactory.createPurpose();
	 		purpose.setEntityName(name);
	 		laf.getPurposes().add(purpose);
	 		
	 		processings.forEach(processing -> processing.getPurpose().add(purpose));
	 		return purpose;
	 	}
	 	
	 	private static Data createAndAddData(String name, LegalAssessmentFacts laf, List<NaturalPerson> persons) {
	 		Data data;
	 		if (persons != null) {
	 			data = gdprFactory.createPersonalData();
	 			((PersonalData)data).getDataReferences().addAll(persons);
	 		} else data = gdprFactory.createData();
	 		data.setEntityName(name);
	 		laf.getData().add(data);
	 		return data;
	 	}
	 
	    private static void buildSion(DataFlowDiagram dfd, DataDictionary dd, TraceModel tm, LegalAssessmentFacts laf) {	    	    	
	    	Map<Node, Processing> mapNodeToProcessing = new HashMap<>();
	    	
	    	var sensor = createNode("Sensor", External.class, mapNodeToProcessing, dfd, dd, laf, tm);
	    	var dataSync = createNode("Data Sync", null, mapNodeToProcessing, dfd, dd, laf, tm);
	    	var clinicalRiskAssessment = createNode("Clinical Risk Assessment", null, mapNodeToProcessing, dfd, dd, laf, tm);
	    	var patientData = createNode("Patient Data", Store.class, mapNodeToProcessing, dfd, dd, laf, tm);
	    	var gpPortal = createNode("GP Portal", null, mapNodeToProcessing, dfd, dd, laf, tm);
	    	var gp = createNode("GP", External.class, mapNodeToProcessing, dfd, dd, laf, tm);
	    	
	    	var sensor2dataSync = createFlow("send sensor data", sensor, dataSync, mapNodeToProcessing, dfd);
	    	var dataSync2sensor = createFlow("recieve config updates", dataSync, sensor, mapNodeToProcessing, dfd);
	    	var dataSync2clinicalRiskAssessment = createFlow("send data", dataSync, clinicalRiskAssessment, mapNodeToProcessing, dfd);
	    	var clinicalRiskAssessment2dataSync = createFlow("retrieve config updates", clinicalRiskAssessment, dataSync, mapNodeToProcessing, dfd);
	    	var clinicalRiskAssessment2patientData = createFlow("store data", clinicalRiskAssessment, patientData, mapNodeToProcessing, dfd);
	    	var patientData2clinicalRiskAssessment = createFlow("retrieve config", patientData, clinicalRiskAssessment, mapNodeToProcessing, dfd);
	    	var patientData2gpPortal = createFlow("retrieve data", patientData, gpPortal, mapNodeToProcessing, dfd);
	    	var gpPortal2patientData = createFlow("request", gpPortal, patientData, mapNodeToProcessing, dfd);
	    	var gpPortal2gp = createFlow("patient data", gpPortal, gp, mapNodeToProcessing, dfd);
	    	var gp2gpPortal = createFlow("consult patient data", gp, gpPortal, mapNodeToProcessing, dfd);
	    	
	    	var patient = gdprFactory.createNaturalPerson();
	    	patient.setEntityName("Patient");
	    	patient.setName("Patient");
	    	laf.getInvolvedParties().add(patient);	    	
	    	
	    	var personalData = gdprFactory.createPersonalData();
	    	personalData.setEntityName("");
	    	personalData.getDataReferences().add(patient);
	    	laf.getData().add(personalData);
	    	
	    	var riskData = gdprFactory.createData();
	    	riskData.setEntityName("Risk Data");
	    	laf.getData().add(riskData);
	    	
	    	var controller = gdprFactory.createController();
	    	controller.setEntityName("Controller");
	    	laf.getInvolvedParties().add(controller);
	    	
	    	var purpose = gdprFactory.createPurpose();
	    	purpose.setEntityName("GP purpose");
	    	laf.getPurposes().add(purpose);
	    	
	    	mapNodeToProcessing.values().forEach(processing -> processing.setResponsible(controller));
	    	
	    	addOutgoingDataAndTrace(sensor2dataSync, mapNodeToProcessing, List.of(personalData), tm);
	    	addOutgoingDataAndTrace(dataSync2clinicalRiskAssessment, mapNodeToProcessing, List.of(personalData), tm);
	    	addOutgoingDataAndTrace(clinicalRiskAssessment2patientData, mapNodeToProcessing, List.of(personalData), tm);
	    	addOutgoingDataAndTrace(patientData2gpPortal, mapNodeToProcessing, List.of(personalData, riskData), tm);
	    	addOutgoingDataAndTrace(gpPortal2gp, mapNodeToProcessing, List.of(personalData, riskData), tm);
	   
	    	addOutgoingDataAndTrace(dataSync2sensor, mapNodeToProcessing, null, tm);
	    	addOutgoingDataAndTrace(clinicalRiskAssessment2dataSync, mapNodeToProcessing, null, tm);
	    	addOutgoingDataAndTrace(patientData2clinicalRiskAssessment, mapNodeToProcessing, null, tm);
	    	addOutgoingDataAndTrace(gpPortal2patientData, mapNodeToProcessing, null, tm);
	    	addOutgoingDataAndTrace(gp2gpPortal, mapNodeToProcessing, null, tm);
	    }
	    
	    private static void addOutgoingDataAndTrace(Flow flow, Map<Node, Processing> mapNodeToProcessing, List<Data> data, TraceModel tm) {
	    	var destProcessing = mapNodeToProcessing.get(flow.getDestinationNode());
	    	var sourceProcessing = mapNodeToProcessing.get(flow.getSourceNode());
	    	
	    	if (data == null) {		    	
		    	var flowTrace = tmFactory.createFlowTrace();
		    	flowTrace.setData(null);
		    	flowTrace.setDataFlow(flow);
		    	flowTrace.setSource(sourceProcessing);
		    	flowTrace.setDest(destProcessing);
		    	tm.getFlowTraces().add(flowTrace);
		    	return;
	    	}
	    	
	    	data.forEach(it -> {
	    		sourceProcessing.getOutputData().add(it);
		    	destProcessing.getInputData().add(it);
		    	
		    	var flowTrace = tmFactory.createFlowTrace();
		    	flowTrace.setData(it);
		    	flowTrace.setDataFlow(flow);
		    	flowTrace.setSource(sourceProcessing);
		    	flowTrace.setDest(destProcessing);
		    	tm.getFlowTraces().add(flowTrace);
	    	});	    	
	    }
	    
	    private static Node createNode(String name, Class<? extends Node> type, Map<Node, Processing> mapNodeToProcessing, DataFlowDiagram dfd, DataDictionary dd, LegalAssessmentFacts laf, TraceModel tm) {
	    	Node node;
	    	Processing processing;
	    	
	    	if (type == null) {
	    		node = dfdFactory.createProcess();
	    		processing = gdprFactory.createUsage();
	    	} else if (type.equals(External.class)) {
	    		node = dfdFactory.createExternal();
	    		processing = gdprFactory.createCollecting();
	    	} else if (type.equals(Store.class)) {
	    		node = dfdFactory.createStore();
	    		processing = gdprFactory.createStoring();
	    	} else {
	    		node = dfdFactory.createProcess();
	    		processing = gdprFactory.createUsage();
	    	}
	    	
	    	node.setEntityName(name);
	    	processing.setEntityName(name);
	    	
	    	if (mapNodeToProcessing != null) mapNodeToProcessing.put(node, processing);
	    	
	    	dfd.getNodes().add(node);
	    	if (laf != null) { laf.getProcessing().add(processing);
	    	
	    	var nodeTrace = tmFactory.createNodeTrace();
	    	nodeTrace.setDfdNode(node);
	    	nodeTrace.setGdprProcessing(processing);
	    	tm.getNodeTraces().add(nodeTrace);
	    	}
	    	var behavior = ddFactory.createBehavior();
	    	behavior.setEntityName(name + "_behavior");
	    	node.setBehavior(behavior);
	    	dd.getBehavior().add(behavior);
	    	
	    	return node;
	    }
	    
	    private static Flow createFlow(String name, Node source, Node dest, Map<Node, Processing> mapNodeToProcessing, DataFlowDiagram dfd) {
	    	var flow = dfdFactory.createFlow();
	    	var sourcePin = ddFactory.createPin();
	    	var destPin = ddFactory.createPin();
	    	source.getBehavior().getOutPin().add(sourcePin);
	    	dest.getBehavior().getInPin().add(destPin);
	    	sourcePin.setEntityName("");
	    	destPin.setEntityName("");
	    	
	    	flow.setDestinationNode(dest);
	    	flow.setDestinationPin(destPin);
	    	flow.setSourceNode(source);
	    	flow.setSourcePin(sourcePin);
	    	flow.setEntityName(name);
	    	
	    	var sourceProcessing = mapNodeToProcessing.get(source);
	    	var destProcessing = mapNodeToProcessing.get(dest);
	    	
	    	if (!sourceProcessing.getFollowingProcessing().contains(destProcessing)) sourceProcessing.getFollowingProcessing().add(destProcessing);
	    	
	    	dfd.getFlows().add(flow);
	    	
	    	return flow;
	    }
	    

		//Copied from https://sdq.kastel.kit.edu/wiki/Creating_EMF_Model_instances_programmatically
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Resource createAndAddResource(String outputFile, String[] fileextensions, ResourceSet rs) {
		     for (String fileext : fileextensions) {
		        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileext, new XMLResourceFactoryImpl());
		     }		
		     URI uri = URI.createFileURI(outputFile);
		     Resource resource = rs.createResource(uri);
		     ((ResourceImpl)resource).setIntrinsicIDToEObjectMap(new HashMap());
		     return resource;
		  }
		
		@SuppressWarnings({"unchecked", "rawtypes"})
		 private void saveResource(Resource resource) {
		     Map saveOptions = ((XMLResource)resource).getDefaultSaveOptions();
		     saveOptions.put(XMLResource.OPTION_CONFIGURATION_CACHE, Boolean.TRUE);
		     saveOptions.put(XMLResource.OPTION_USE_CACHED_LOOKUP_TABLE, new ArrayList());
		     try {
		        resource.save(saveOptions);
		     } catch (IOException e) {
		        throw new RuntimeException(e);
		     }
		}
}
