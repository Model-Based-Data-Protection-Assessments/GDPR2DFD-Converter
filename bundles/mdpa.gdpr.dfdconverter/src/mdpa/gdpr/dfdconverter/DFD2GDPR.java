package mdpa.gdpr.dfdconverter;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryPackage;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.External;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.dfd.dataflowdiagram.Process;
import org.dataflowanalysis.dfd.dataflowdiagram.Store;
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
	private Map<String, Entity> mapIdToElement = new HashMap<>();
	
	private Set<Label> resolvedLinkageLabel = new HashSet<>();
	
	private Map<Label, Entity> labelToEntityMap = new HashMap<>();
		
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
		
		
		tmResource = rs.getResource(URI.createFileURI(traceModelFile), true);
		dfdResource = rs.getResource(URI.createFileURI(dfdFile), true);
		ddResource = rs.getResource(URI.createFileURI(ddFile), true);
		
		dd = (DataDictionary) ddResource.getContents().get(0);
		dfd = (DataFlowDiagram) dfdResource.getContents().get(0);	
		gdpr2dfdTrace = (TraceModel) tmResource.getContents().get(0);
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
	
	/**
	 * Performs the transformation on the models provided in the Constructor
	 */
	public void transform() {
		laf.setId(dfd.getId());
		handleTraceModel();
		
		
		dfd.getNodes().stream().forEach(node -> {
			Processing processing = mapNodeToProcessing.getOrDefault(node, null);
			if (processing == null) {
				processing = gdprFactory.createProcessing();
				processing.setEntityName(node.getEntityName());
				processing.setResponsible(getControllerFromNode(node));
				processing.getOnTheBasisOf().addAll(getLegalBasesFromNode(node));
				processing.getPurpose().addAll(getPurposesFromNode(node));			
			}
			
			
		});
		
		
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
		
		for (var node : dfd.getNodes()) {
			 node.getProperties().clear();
		}
		
		
		List<LabelType> gdprLabels = new ArrayList<>();
		dd.getLabelTypes().forEach(lt -> {
			String name = lt.getEntityName();
			if (name.equals("GDPRElement") || name.equals("GDPRNode") || name.equals("GDPRLink"))
				gdprLabels.add(lt);
		});		
		dd.getLabelTypes().removeAll(gdprLabels);	
		
		
	}
	
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
	
	private void handleTraceModel() {
		gdpr2dfdTrace.getTracesList().forEach(trace -> {
			var node = trace.getNode();
			var processing = trace.getProcessing();
			mapNodeToProcessing.put(node, processing);
			
			processing.getOnTheBasisOf().clear();
			processing.getOnTheBasisOf().addAll(getLegalBasesFromNode(node));
			
			processing.getPurpose().clear();
			processing.getPurpose().addAll(getPurposesFromNode(node));
			
			processing.setResponsible(getControllerFromNode(node));
		});
	}
	
	private void parseLabels() {
		dd.getLabelTypes().forEach(labelType -> {
			if (labelType.getEntityName().equals("Roles")) {
				labelType.getLabel().forEach(label -> {
					Role role;
					if (labelType.getEntityName().startsWith(Controller.class.getSimpleName())) {
						role = gdprFactory.createController();
					} else if (labelType.getEntityName().startsWith(NaturalPerson.class.getSimpleName())) {
						role = gdprFactory.createNaturalPerson();
					} else {
						role = gdprFactory.createNaturalPerson(); //TODO Change once LAF model is changed
					}
					if (label.getEntityName().contains(":")) {
						role.setEntityName(label.getEntityName().split(":")[1]);
					} else {
						role.setEntityName(label.getEntityName());
					}
					
					labelToEntityMap.put(label, role);
					laf.getInvolvedParties().add(role);
				});
			}
			if (labelType.getEntityName().equals("LegalBases")) {
				labelType.getLabel().forEach(label -> {
					LegalBasis legalBasis;
					if (labelType.getEntityName().startsWith(Consent.class.getSimpleName())) {
						legalBasis = gdprFactory.createConsent();
					} else if (labelType.getEntityName().startsWith(PerformanceOfContract.class.getSimpleName())) {
						legalBasis = gdprFactory.createPerformanceOfContract();
					} else if (labelType.getEntityName().startsWith(ExerciseOfPublicAuthority.class.getSimpleName())) {
						legalBasis = gdprFactory.createExerciseOfPublicAuthority();
					} else if (labelType.getEntityName().startsWith(Obligation.class.getSimpleName())) {
						legalBasis = gdprFactory.createObligation();
					} else {
						legalBasis = gdprFactory.createLegalBasis(); //TODO Change once LAF model is changed
					}
					if (label.getEntityName().contains(":")) {
						legalBasis.setEntityName(label.getEntityName().split(":")[1]);
					} else {
						legalBasis.setEntityName(label.getEntityName());
					}
					
					labelToEntityMap.put(label, legalBasis);
					laf.getLegalBases().add(legalBasis);
				});
			}
			if (labelType.getEntityName().equals("Purposes")) {
				labelType.getLabel().forEach(label -> {
					var purpose = gdprFactory.createPurpose();
					if (label.getEntityName().contains(":")) {
						purpose.setEntityName(label.getEntityName().split(":")[1]);
					} else {
						purpose.setEntityName(label.getEntityName());
					}
					labelToEntityMap.put(label, purpose);
					laf.getPurposes().add(purpose);
				});			
			}
			if (labelType.getEntityName().equals("Data")) {
				labelType.getLabel().forEach(label -> {
					Data data;
					if (labelType.getEntityName().startsWith(Data.class.getSimpleName())) {
						data = gdprFactory.createData();
					} else if (labelType.getEntityName().startsWith(PersonalData.class.getSimpleName())) {
						data = gdprFactory.createPersonalData();
					} else {
						data = gdprFactory.createData(); //TODO Change once LAF model is changed
					}
					if (label.getEntityName().contains(":")) {
						data.setEntityName(label.getEntityName().split(":")[1]);
					} else {
						data.setEntityName(label.getEntityName());
					}
					
					labelToEntityMap.put(label, data);
					laf.getData().add(data);
				});			
			}
		});
	}
	
	private Controller getControllerFromNode(Node node) {			
		for (Label label : node.getProperties()) {
			var entity = labelToEntityMap.getOrDefault(label, null);
			if (entity != null && entity instanceof Controller controller) {
				return controller;
			}
		}
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

	private List<Data> getDataFromNode(Node node) {
		List<Data> dataElements = new ArrayList<>();
		node.getProperties().forEach(label -> {
			var entity = labelToEntityMap.getOrDefault(label, null);
			if (entity != null && entity instanceof Data data) {
				dataElements.add(data);
			}
		});
		return dataElements;
	}
	
	
	/**
	 * Creates a Processing of the right type through using the descriptor label or through node type if no label is present
	 * @param node Node to be transformed into a Processing element
	 * @return created processing element
	 */
	private Processing resolveProcessingTypeLabel(Node node) {
		List<String> filteredLabels = node.getProperties().stream()
				.map(label -> label.getEntityName())
				.filter(name -> name.startsWith("GDPR::ofType:"))
				.toList();
		
		if (filteredLabels.size() == 0) {
			if (node instanceof Store) return gdprFactory.createStoring();
			if (node instanceof External) return gdprFactory.createCollecting();
			if (node instanceof Process) return gdprFactory.createProcessing();
		}	
		
		String type = filteredLabels.get(0).replace("GDPR::ofType:", "").replace("Impl", "");
			
		
		if (type.equals(Usage.class.getSimpleName())) {
			return gdprFactory.createUsage();
		} else if (type.equals(Transferring.class.getSimpleName())) {
			return gdprFactory.createTransferring();
		} else if (type.equals(Storing.class.getSimpleName())) {
			return gdprFactory.createStoring();
		} else if (type.equals(Collecting.class.getSimpleName())) {
			return gdprFactory.createCollecting();
		} else if (type.equals(Processing.class.getSimpleName())) {
			return gdprFactory.createProcessing();
		} else {
			throw new IllegalArgumentException("Processing Type Label contains invalid Type");
		}
	}
	
	/**
	 * Transforms a flow into the following processing attribute of processing
	 * @param flow
	 */
	private void transformFlow (Flow flow) {
		Processing source = mapNodeToProcessing.get(flow.getSourceNode());
		Processing destination = mapNodeToProcessing.get(flow.getDestinationNode());
		
		source.getFollowingProcessing().add(destination);
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
	

	public DataFlowDiagram getDfd() {
		return dfd;
	}

	public DataDictionary getDd() {
		return dd;
	}

	public LegalAssessmentFacts getLaf() {
		return laf;
	}

	public TraceModel getTracemodel() {
		return dfd2gdprTrace;
	}
}
