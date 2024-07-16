package mdpa.gdpr.dfdconverter;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryPackage;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.*;
import mdpa.gdpr.metamodel.GDPR.*;
import tools.mdsd.modelingfoundations.identifier.Entity;

public class DFD2GDPR {	

	private DataFlowDiagram dfd;
	private DataDictionary dd;
	private LegalAssessmentFacts laf;
	private TraceModel dfd2gdprTrace;
	private TraceModel gdpr2dfdTrace;
	
	private GDPRFactory gdprFactory;
	private TracemodelFactory traceModelFactory;
	
	private Map<Node, Processing> mapNodeToProcessing = new HashMap<>();	
	private Map<Label, Entity> labelToEntityMap = new HashMap<>();
	private Map<Entity, Entity> mapCopiesForTraceModelContainment = new HashMap<>();
		
	private ResourceSet rs;
	
	private Resource ddResource;
	private Resource dfdResource;
	private Resource tmResource;	
	
	public DFD2GDPR(String dfdFile, String ddFile, String traceModelFile) {
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(dataflowdiagramPackage.eNS_URI, dataflowdiagramPackage.eINSTANCE);
		rs.getPackageRegistry().put(datadictionaryPackage.eNS_URI, datadictionaryPackage.eINSTANCE);
		rs.getPackageRegistry().put(TracemodelPackage.eNS_URI, TracemodelPackage.eINSTANCE);
		
		gdprFactory = GDPRFactory.eINSTANCE;
		laf = gdprFactory.createLegalAssessmentFacts();
		
		traceModelFactory = TracemodelFactory.eINSTANCE;
		dfd2gdprTrace = traceModelFactory.createTraceModel();
		
		dfdResource = rs.getResource(URI.createFileURI(dfdFile), true);		
		tmResource = rs.getResource(URI.createFileURI(traceModelFile), true);
		ddResource = rs.getResource(URI.createFileURI(ddFile), true);
		
		dd = (DataDictionary) ddResource.getContents().get(0);
		dfd = (DataFlowDiagram) dfdResource.getContents().get(0);	
		gdpr2dfdTrace = (TraceModel) tmResource.getContents().get(0);
	}
	
	public DFD2GDPR(String dfdFile, String ddFile) {
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(dataflowdiagramPackage.eNS_URI, dataflowdiagramPackage.eINSTANCE);
		rs.getPackageRegistry().put(datadictionaryPackage.eNS_URI, datadictionaryPackage.eINSTANCE);
		rs.getPackageRegistry().put(TracemodelPackage.eNS_URI, TracemodelPackage.eINSTANCE);
		
		gdprFactory = GDPRFactory.eINSTANCE;
		laf = gdprFactory.createLegalAssessmentFacts();
		
		traceModelFactory = TracemodelFactory.eINSTANCE;
		dfd2gdprTrace = traceModelFactory.createTraceModel();
		
		dfdResource = rs.getResource(URI.createFileURI(dfdFile), true);		
		ddResource = rs.getResource(URI.createFileURI(ddFile), true);
		
		dd = (DataDictionary) ddResource.getContents().get(0);
		dfd = (DataFlowDiagram) dfdResource.getContents().get(0);	
	}
	
	public DFD2GDPR(DataFlowDiagram dfd, DataDictionary dd, TraceModel gdpr2dfdTrace) {
		gdprFactory = GDPRFactory.eINSTANCE;
		laf = gdprFactory.createLegalAssessmentFacts();
		
		traceModelFactory = TracemodelFactory.eINSTANCE;
		dfd2gdprTrace = traceModelFactory.createTraceModel();
		this.dfd = dfd;
		this.dd = dd;
		this.gdpr2dfdTrace = gdpr2dfdTrace;
	}
	
	public DFD2GDPR(DataFlowDiagram dfd, DataDictionary dd) {
		gdprFactory = GDPRFactory.eINSTANCE;
		laf = gdprFactory.createLegalAssessmentFacts();
		
		traceModelFactory = TracemodelFactory.eINSTANCE;
		dfd2gdprTrace = traceModelFactory.createTraceModel();
		this.dfd = dfd;
		this.dd = dd;
	}
	
	/**
	 * Performs the transformation on the models provided in the Constructor
	 */
	public void transform() {
		laf.setId(dfd.getId());
		
		parseLabels();
		
		if (gdpr2dfdTrace != null) handleTraceModel();
		
		dfd.getNodes().forEach(this::annotateData);
				
		dfd.getFlows().stream().forEach(flow -> transformFlow(flow));
		
		dfd.getFlows().stream().forEach(flow -> {
			FlowElement flowElement = traceModelFactory.createFlowElement();
			flowElement.setFlow(flow);
			flowElement.setDestinationID(flow.getDestinationNode().getId());
			flowElement.setSourceID(flow.getSourceNode().getId());
			dfd2gdprTrace.getFlowList().add(flowElement);
		});
		
		mapNodeToProcessing.forEach((node, processing) -> {
			Trace trace = traceModelFactory.createTrace();
			trace.setNode(node);
			trace.setProcessing(processing);
			dfd2gdprTrace.getTracesList().add(trace);
		});			
	}
	
	/**
	 * Saves the created Model instances at the provided locations
	 * @param gdprFile Location where gdpr instance is to be saved
	 * @param traceModelFile Location where the traceModel instance is to be saved
	 */
	public void save(String gdprFile, String traceModelFile) {
		Resource gdprResource = createAndAddResource(gdprFile, new String[] {"gdpr"} ,rs);
		Resource tmResource = createAndAddResource(traceModelFile, new String[] {"tracemodel"} ,rs);
		
		gdprResource.getContents().add(laf);
		tmResource.getContents().add(dfd2gdprTrace);
		
		saveResource(gdprResource);
		saveResource(tmResource);
		saveResource(ddResource);
		saveResource(dfdResource);
	}
	
	/**
	 * Restores information from tracemodel
	 */
	private void handleTraceModel() {		
		gdpr2dfdTrace.getTracesList().forEach(trace -> {
			var node = trace.getNode();
			var processingFromTrace = trace.getProcessing();
			var processing = cloneProcessing(processingFromTrace);
			
			processing.setId(node.getId());
			processing.setEntityName(node.getEntityName());
			
			
			mapNodeToProcessing.put(node, processing);		
		});
		
		gdpr2dfdTrace.getTracesList().forEach(trace -> {
			var processing = (Processing) mapCopiesForTraceModelContainment.get(trace.getProcessing());
			for (var followingProcessing : trace.getProcessing().getFollowingProcessing()) {
				processing.getFollowingProcessing().add((Processing) mapCopiesForTraceModelContainment.get(followingProcessing));
			}
			
				
		});
	}
	
	//Cloning because of ECore Containment
	private Data cloneData(Data data) {
		if (mapCopiesForTraceModelContainment.containsKey(data)) return (Data) mapCopiesForTraceModelContainment.get(data);
		
		Data clone;
		if (data instanceof PersonalData personalData) {
			clone = GDPRFactory.eINSTANCE.createPersonalData();
			personalData.getDataReferences().forEach(reference -> {
				((PersonalData)clone).getDataReferences().add((NaturalPerson)cloneRole(reference));
			});
		} else clone = GDPRFactory.eINSTANCE.createData();
		clone.setEntityName(data.getEntityName());
		clone.setId(data.getId());
		
		mapCopiesForTraceModelContainment.put(data, clone);
		
		return clone;
	}
	
	private Role cloneRole(Role role) {
		if (mapCopiesForTraceModelContainment.containsKey(role)) return (Role) mapCopiesForTraceModelContainment.get(role);
		
		Role clone;
		if (role instanceof Controller) clone = GDPRFactory.eINSTANCE.createController();
		else clone = GDPRFactory.eINSTANCE.createNaturalPerson();
		if (role.getName() != null) clone.setName(role.getName());
		clone.setEntityName(role.getEntityName());
		
		mapCopiesForTraceModelContainment.put(role, clone);
		
		return clone;
	}
	
	private Purpose clonePurpose(Purpose purpose) {
		if (mapCopiesForTraceModelContainment.containsKey(purpose)) return (Purpose) mapCopiesForTraceModelContainment.get(purpose);
	
		Purpose clone = GDPRFactory.eINSTANCE.createPurpose();
		clone.setEntityName(purpose.getEntityName());
		clone.setId(purpose.getId());
		
		mapCopiesForTraceModelContainment.put(purpose, clone);
		
		return clone;
	}
	
	private LegalBasis cloneLegalBasis(LegalBasis legalBasis) {
		if(mapCopiesForTraceModelContainment.containsKey(legalBasis)) return (LegalBasis) mapCopiesForTraceModelContainment.get(legalBasis);
		
		LegalBasis clone;
		if (legalBasis instanceof Consent consent) {
			clone = GDPRFactory.eINSTANCE.createConsent();
			((Consent)clone).setConsentee((NaturalPerson)cloneRole(consent.getConsentee()));
		} else if (legalBasis instanceof PerformanceOfContract contract) {
			clone = GDPRFactory.eINSTANCE.createPerformanceOfContract();
			contract.getContractingParty().forEach(party -> {
				((PerformanceOfContract)clone).getContractingParty().add(cloneRole(party));
			});
		} else if (legalBasis instanceof ExerciseOfPublicAuthority) {
			clone = GDPRFactory.eINSTANCE.createExerciseOfPublicAuthority();
		} else if (legalBasis instanceof Obligation) {
			clone = GDPRFactory.eINSTANCE.createObligation();
		} else clone = GDPRFactory.eINSTANCE.createLegalBasis();
		
		legalBasis.getForPurpose().forEach(purpose -> {
			clone.getForPurpose().add(clonePurpose(purpose));
		});
		
		if (legalBasis.getPersonalData() != null) clone.setPersonalData((PersonalData)cloneData(legalBasis.getPersonalData()));
	
		clone.setEntityName(legalBasis.getEntityName());
		clone.setId(legalBasis.getId());
		
		mapCopiesForTraceModelContainment.put(legalBasis, clone);
		
		return clone;
	}
	
	private Processing cloneProcessing(Processing processing) {
		if (mapCopiesForTraceModelContainment.containsKey(processing)) return (Processing) mapCopiesForTraceModelContainment.get(processing);
	
		Processing clone;
		if ( processing instanceof Collecting) clone= GDPRFactory.eINSTANCE.createCollecting();
		else if (processing instanceof Storing) clone = GDPRFactory.eINSTANCE.createStoring();
		else if (processing instanceof Transferring) clone = GDPRFactory.eINSTANCE.createTransferring();
		else if (processing instanceof Usage) clone = GDPRFactory.eINSTANCE.createUsage();
		else clone = GDPRFactory.eINSTANCE.createProcessing();
		
		
		if(processing.getResponsible() != null) clone.setResponsible((Role)cloneRole(processing.getResponsible()));
		
		processing.getInputData().forEach(data -> clone.getInputData().add(cloneData(data)));
		processing.getOutputData().forEach(data -> clone.getInputData().add(cloneData(data)));
		processing.getOnTheBasisOf().forEach(legalBasis -> clone.getOnTheBasisOf().add(cloneLegalBasis(legalBasis)));
		processing.getPurpose().forEach(purpose -> clone.getPurpose().add(clonePurpose(purpose)));
		
		
		mapCopiesForTraceModelContainment.put(processing, clone);
		
		return clone;
	}
	
	/**
	 * Creates the GDPR specific objects from the labels holding the information
	 */
	private void parseLabels() {
		dd.getLabelTypes().forEach(labelType -> {
			if (labelType.getEntityName().equals("Controller")) {
	            labelType.getLabel().forEach(label -> {
	                Controller controller = gdprFactory.createController();
	                controller.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, controller);
	                laf.getInvolvedParties().add(controller);
	            });
	        } else if (labelType.getEntityName().equals("NaturalPerson")) {
	            labelType.getLabel().forEach(label -> {
	                NaturalPerson naturalPerson = gdprFactory.createNaturalPerson();
	                naturalPerson.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, naturalPerson);
	                laf.getInvolvedParties().add(naturalPerson);
	            });
	        } else if (labelType.getEntityName().equals("Consent")) {
	            labelType.getLabel().forEach(label -> {
	                Consent consent = gdprFactory.createConsent();
	                consent.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, consent);
	                laf.getLegalBases().add(consent);
	            });
	        } else if (labelType.getEntityName().equals("Obligation")) {
	            labelType.getLabel().forEach(label -> {
	                Obligation obligation = gdprFactory.createObligation();
	                obligation.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, obligation);
	                laf.getLegalBases().add(obligation);
	            });
	        } else if (labelType.getEntityName().equals("Contract")) {
	            labelType.getLabel().forEach(label -> {
	                PerformanceOfContract contract = gdprFactory.createPerformanceOfContract();
	                contract.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, contract);
	                laf.getLegalBases().add(contract);
	            });
	        } else if (labelType.getEntityName().equals("PublicAuthority")) {
	            labelType.getLabel().forEach(label -> {
	                ExerciseOfPublicAuthority authority = gdprFactory.createExerciseOfPublicAuthority();
	                authority.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, authority);
	                laf.getLegalBases().add(authority);
	            });
	        } else if (labelType.getEntityName().equals("Purposes")) {
	            labelType.getLabel().forEach(label -> {
	                Purpose purpose = gdprFactory.createPurpose();
	                purpose.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, purpose);
	                laf.getPurposes().add(purpose);
	            });
	        } else if (labelType.getEntityName().equals("Data")) {
	            labelType.getLabel().forEach(label -> {
	                Data data = gdprFactory.createData();
	                data.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, data);
	                laf.getData().add(data);
	            });
	        } else if (labelType.getEntityName().equals("PersonalData")) {
	            labelType.getLabel().forEach(label -> {
	                PersonalData personalData = gdprFactory.createPersonalData();
	                personalData.setEntityName(label.getEntityName());
	                labelToEntityMap.put(label, personalData);
	                laf.getData().add(personalData);
	            });
	        }
		});
	}
	
	
	//Parse Processing properties and annotate corresponding GDPR information
	
	private Controller getControllerFromNode(Node node) {			
		for (Label label : node.getProperties()) {
			var entity = labelToEntityMap.getOrDefault(label, null);
			if (entity != null && entity instanceof Controller controller) {
				return controller;
			}
		}
		return null;
	}
	
	private List<Purpose> getPurposesFromNode(Node node) {
		List<Purpose> purposes = new ArrayList<>();
		node.getProperties().forEach(label -> {
			var entity = labelToEntityMap.getOrDefault(label, null);
			if (entity != null && entity instanceof Purpose purpose) {
				purposes.add(purpose);
			}
		});
		return purposes;
	}

	private List<LegalBasis> getLegalBasesFromNode(Node node) {
		List<LegalBasis> legalBases = new ArrayList<>();
		node.getProperties().forEach(label -> {
			var entity = labelToEntityMap.getOrDefault(label, null);
			if (entity != null && entity instanceof LegalBasis legalBasis) {
				legalBases.add(legalBasis);
			}
		});
		return legalBases;
	}

	/**
	 * Infer input and output data from DFD flows and pins
	 * @param node
	 */
	private void annotateData(Node node) {
		var processing = mapNodeToProcessing.getOrDefault(node, null);
		if (processing == null) {
			processing = gdprFactory.createProcessing();
			processing.setEntityName(node.getEntityName());
			processing.setId(node.getId());
			mapNodeToProcessing.put(node, processing);
		}
		for (Pin pin : node.getBehaviour().getInPin()) {
			var name = pin.getEntityName();
			Data data = laf.getData().stream().filter(it -> it.getEntityName().equals(name)).findAny().orElseThrow();
			processing.getInputData().add(data);
		}
		for (Pin pin : node.getBehaviour().getOutPin()) {
			var name = pin.getEntityName();
			Data data = laf.getData().stream().filter(it -> it.getEntityName().equals(name)).findAny().orElseThrow();
			processing.getOutputData().add(data);
		}
			
		processing.getOnTheBasisOf().clear();
		processing.getOnTheBasisOf().addAll(getLegalBasesFromNode(node));
		
		processing.getPurpose().clear();
		processing.getPurpose().addAll(getPurposesFromNode(node));
		
		processing.setResponsible(getControllerFromNode(node));		

		laf.getProcessing().add(processing);
	}
	
	
	
	
	/**
	 * Transforms a flow into the following processing attribute of processing
	 * @param flow
	 */
	private void transformFlow (Flow flow) {
		Processing source = mapNodeToProcessing.get(flow.getSourceNode());
		Processing destination = mapNodeToProcessing.get(flow.getDestinationNode());
		
		if(!source.getFollowingProcessing().contains(destination))source.getFollowingProcessing().add(destination);
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
	

	public DataFlowDiagram getDataFlowDiagram() {
		return dfd;
	}

	public DataDictionary getDataDictionary() {
		return dd;
	}

	public LegalAssessmentFacts getLegalAssessmentFacts() {
		return laf;
	}

	public TraceModel getDFD2GDPRTraceModel() {
		return dfd2gdprTrace;
	}
}
