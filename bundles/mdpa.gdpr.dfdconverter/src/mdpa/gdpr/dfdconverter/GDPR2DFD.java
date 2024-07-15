package mdpa.gdpr.dfdconverter;

import org.dataflowanalysis.dfd.datadictionary.*;
import org.dataflowanalysis.dfd.dataflowdiagram.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

import mdpa.gdpr.metamodel.GDPR.*;
import tools.mdsd.modelingfoundations.identifier.Entity;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

public class GDPR2DFD {
	private DataFlowDiagram dfd;
	private DataDictionary dd;
	private LegalAssessmentFacts laf;
	private TraceModel dfd2gdprTrace;
	private TraceModel gdpr2dfdTrace;
	
	private dataflowdiagramFactory dfdFactory;
	private datadictionaryFactory ddFactory;
	private TracemodelFactory tmFactory;
	
	private LabelType dataLabelType;
	private LabelType personalDataLabelType;
	
	private LabelType consentLabelType;
	private LabelType obligationLabelType;
	private LabelType contractLabelType;
	private LabelType authorityLabelType;
	
	private LabelType personLabelType;
	private LabelType controllerLabelType;
	
	
	private LabelType purposeLabelType;
	
	private Map<Processing, Node> processingToNodeMap = new HashMap<>();		
	private Map<Entity, Label> entityToLabelMap = new HashMap<>();
	
	private ResourceSet rs;
	
	private Resource ddResource;
	
	/**
	 * Creates Transformation with tracemodel to recreate DFD instance
	 * @param gdprFile Location of the GDPR instance
	 * @param ddFile Location of the Data Dictionary instance
	 * @param traceModelFile Location of the TraceModel instance
	 */
	public GDPR2DFD(String gdprFile, String ddFile, String traceModelFile) {		
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(GDPRPackage.eNS_URI, GDPRPackage.eINSTANCE);
		rs.getPackageRegistry().put(TracemodelPackage.eNS_URI, TracemodelPackage.eINSTANCE);
		
		dfdFactory = dataflowdiagramFactory.eINSTANCE;
		ddFactory = datadictionaryFactory.eINSTANCE;
		tmFactory = TracemodelFactory.eINSTANCE;
		
		dfd = dfdFactory.createDataFlowDiagram();		
		gdpr2dfdTrace = tmFactory.createTraceModel();
		
		Resource gdprResource = rs.getResource(URI.createFileURI(gdprFile), true);
		Resource ddResource = rs.getResource(URI.createFileURI(ddFile), true);
		Resource tmResource = rs.getResource(URI.createFileURI(traceModelFile), true);
		
		laf = (LegalAssessmentFacts) gdprResource.getContents().get(0);
		dd = (DataDictionary) ddResource.getContents().get(0);		
		dfd2gdprTrace = (TraceModel) tmResource.getContents().get(0);
	}
	
	/**
	 * Creates Transformation with tracemodel to recreate DFD instance
	 * @param gdprFile GDPR instance
	 * @param ddFile Data Dictionary instance
	 * @param traceModelFile TraceModel instance
	 */
	public GDPR2DFD(LegalAssessmentFacts laf, DataDictionary dd, TraceModel tm) {				
		dfdFactory = dataflowdiagramFactory.eINSTANCE;
		ddFactory = datadictionaryFactory.eINSTANCE;
		tmFactory = TracemodelFactory.eINSTANCE;
		
		dfd = dfdFactory.createDataFlowDiagram();		
		gdpr2dfdTrace = tmFactory.createTraceModel();

		this.laf = laf;
		this.dd = dd;
		this.dfd2gdprTrace = tm;
	}
	
	/**
	 * Creates Transformation for initial transformations from the GDPR into the DFD metamodel
	 * @param gdprFile Location of the gdpr instance
	 */
	public GDPR2DFD(String gdprFile) {		
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(GDPRPackage.eNS_URI, GDPRPackage.eINSTANCE);
		
		dfdFactory = dataflowdiagramFactory.eINSTANCE;
		ddFactory = datadictionaryFactory.eINSTANCE;
		tmFactory = TracemodelFactory.eINSTANCE;
		dfd = dfdFactory.createDataFlowDiagram();
		dd = ddFactory.createDataDictionary();	
		gdpr2dfdTrace = tmFactory.createTraceModel();
		
		Resource gdprResource = rs.getResource(URI.createFileURI(gdprFile), true);
		
		laf = (LegalAssessmentFacts) gdprResource.getContents().get(0);					
	}
	
	/**
	 * Saves the created model instances at the provided locations
	 * @param dfdFile Location of where to save the new DFD instance
	 * @param ddFile Location of where to save the new DD instance
	 * @param traceModelFile Location of where to save the new TM instance
	 */
	public void save(String dfdFile, String ddFile, String traceModelFile) {
		Resource dfdResource = createAndAddResource(dfdFile, new String[] {"dataflowdiagram"} ,rs);
		Resource gdpr2dfdTraceResource = createAndAddResource(traceModelFile, new String[] {"tracemodel"} ,rs);
		if (ddResource == null)  {
			ddResource = createAndAddResource(ddFile, new String[] {"datadictionary"} ,rs);
			ddResource.getContents().add(dd);
		}
		
		dfdResource.getContents().add(dfd);		
		gdpr2dfdTraceResource.getContents().add(gdpr2dfdTrace);
		
		saveResource(gdpr2dfdTraceResource);
		saveResource(dfdResource);
		saveResource(ddResource);
	}
	
	/**
	 * Performs the transformation with the information provided in the constructor
	 */
	public void transform() {
		dfd.setId(laf.getId());
		createLabelTypes();
		createLabels();
		
		if (dfd2gdprTrace != null) handleTraceModel();
				
		laf.getProcessing().stream().forEach(p -> {
			Node node = convertProcessing(p);
			processingToNodeMap.put(p, node);
			dfd.getNodes().add(node);
		});
		
		laf.getProcessing().stream().forEach(p -> {
			dfd.getFlows().addAll(createFlows(p));
		});
		
		laf.getProcessing().stream().forEach(p -> {
			annotateBehaviour(p);
		});
	}
	
	
	/**
	 * Creates the labeltypes that hold the GDPR instance specific information
	 */
	private void createLabelTypes() {
		dataLabelType = ddFactory.createLabelType();
		personalDataLabelType = ddFactory.createLabelType();
		
		obligationLabelType  = ddFactory.createLabelType();
		consentLabelType = ddFactory.createLabelType();
		contractLabelType = ddFactory.createLabelType();
		authorityLabelType = ddFactory.createLabelType();
		
		personLabelType = ddFactory.createLabelType();
		controllerLabelType = ddFactory.createLabelType();
		
		purposeLabelType = ddFactory.createLabelType();
		
		
		dataLabelType.setEntityName("Data");
		personalDataLabelType.setEntityName("PersonalData");
		
		obligationLabelType.setEntityName("Obligation");
		consentLabelType.setEntityName("Consent");
		contractLabelType.setEntityName("Contract");
		authorityLabelType.setEntityName("PublicAuthority");
		
		personLabelType.setEntityName("NaturalPerson");
		controllerLabelType.setEntityName("Controller");
		
		purposeLabelType.setEntityName("Purposes");


		dd.getLabelTypes().add(dataLabelType);
		dd.getLabelTypes().add(personalDataLabelType);
		
		dd.getLabelTypes().add(obligationLabelType);
		dd.getLabelTypes().add(consentLabelType);
		dd.getLabelTypes().add(contractLabelType);
		dd.getLabelTypes().add(authorityLabelType);
		
		dd.getLabelTypes().add(personLabelType);
		dd.getLabelTypes().add(controllerLabelType);
		
		dd.getLabelTypes().add(purposeLabelType);
	}
	
	/**
	 * Creates all Labels to hold GDPR specific information
	 */
	private void createLabels() {
		laf.getInvolvedParties().forEach(role -> {
			Label label = ddFactory.createLabel();
			label.setEntityName(role.getEntityName());
			
			if (role instanceof NaturalPerson) personLabelType.getLabel().add(label);
			else if (role instanceof Controller) contractLabelType.getLabel().add(label);
					
			entityToLabelMap.put(role, label);
		});
		
		laf.getLegalBases().forEach(legalBasis -> {
			Label label = ddFactory.createLabel();
			label.setEntityName(legalBasis.getEntityName());
			
			if (legalBasis instanceof Consent) consentLabelType.getLabel().add(label);
			else if (legalBasis instanceof Obligation) obligationLabelType.getLabel().add(label);
			else if (legalBasis instanceof PerformanceOfContract) contractLabelType.getLabel().add(label);
			else if (legalBasis instanceof ExerciseOfPublicAuthority) authorityLabelType.getLabel().add(label);
						
			entityToLabelMap.put(legalBasis, label);
		});
		
		laf.getPurposes().forEach(purpose -> {
			Label label = ddFactory.createLabel();
			label.setEntityName(purpose.getEntityName());
			purposeLabelType.getLabel().add(label);
			entityToLabelMap.put(purpose, label);
		});
		
		laf.getData().forEach(data -> {
			Label label = ddFactory.createLabel();
			label.setEntityName(data.getEntityName());
			
			if (data instanceof PersonalData personalData) personalDataLabelType.getLabel().add(label);
			else dataLabelType.getLabel().add(label);
			
			entityToLabelMap.put(data, label);
		});
		
		
	}
	
	
	
	/**
	 * Creates all flows outgoing from a processing from following processing or pulls them from the tracemodel if present
	 * @param processing Source for the created Nodes
	 * @return All flows going out from the source node
	 */
	private List<Flow> createFlows(Processing processing) {
		if (dfd2gdprTrace != null) {
			return dfd2gdprTrace.getFlowList()
					.stream()
					.filter(fe -> fe.getSourceID().equals(processing.getId()))
					.map(fe -> fe.getFlow())
					.toList();
		}
		
		List<Flow> flows = new ArrayList<>();
		
		Node sourceNode = processingToNodeMap.get(processing);
		for (Processing followingProcessing : processing.getFollowingProcessing()) {
			Node destinationNode = processingToNodeMap.get(followingProcessing);
			
			List<Data> dataSent = intersection(processing.getOutputData(), followingProcessing.getInputData());
			dataSent.forEach(data -> {
				String dataName = data.getEntityName();
				
				Flow flow = dfdFactory.createFlow();
				flow.setEntityName(dataName);
				
				Pin outPin = sourceNode.getBehaviour().getOutPin().stream().filter(pin -> pin.getEntityName().equals(dataName)).findAny().orElse(null);
				if (outPin == null) {
					outPin = ddFactory.createPin();
					outPin.setEntityName(dataName);
					sourceNode.getBehaviour().getOutPin().add(outPin);
				}
				
				Pin inPin = destinationNode.getBehaviour().getInPin().stream().filter(pin -> pin.getEntityName().equals(dataName)).findAny().orElse(null);
				if (inPin == null) {
					inPin = ddFactory.createPin();
					inPin.setEntityName(dataName);
					destinationNode.getBehaviour().getInPin().add(inPin);
				}
				
				flow.setDestinationNode(destinationNode);
				flow.setSourceNode(sourceNode);
				flow.setDestinationPin(inPin);
				flow.setSourcePin(outPin);
				
				flows.add(flow);
			});
			
		}
		
		return flows;
	}
	
	/**
	 * Fills the processingToNode Map in case the tracemodel is present
	 */
	private void handleTraceModel() {
		dfd2gdprTrace.getTracesList().forEach(t -> {
			processingToNodeMap.put(t.getProcessing(), t.getNode());
		});
	}
	
	/**
	 * Transforms a processing into a node or pulls the node from the tracemodel if present
	 * @param processing Processing to be transformed
	 * @return Node that was created or pulled from the tracemodel
	 */
	private Node convertProcessing(Processing processing) {
		Node node;
		
		String name = processing.getEntityName();
		
		if (processingToNodeMap.containsKey(processing)) {
			node = processingToNodeMap.get(processing);
		} else if (processing instanceof Collecting) {
			node = dfdFactory.createExternal();;
		} else if (processing instanceof Storing) {
			node = dfdFactory.createStore();
		} else if (processing instanceof Usage) {
			node = dfdFactory.createProcess();
		} else if (processing instanceof Transferring) {
			node = dfdFactory.createProcess();
		} else {
			node = dfdFactory.createProcess();
		}
		

		node.setId(processing.getId());
		node.setEntityName(name);
		
		node.getProperties().addAll(createTypeLabel(processing));
		
		if (node.getBehaviour() == null) {
			Behaviour behaviour = ddFactory.createBehaviour();
			behaviour.setEntityName(name + " Behaviour");
			node.setBehaviour(behaviour);
			dd.getBehaviour().add(behaviour);
		}
				
		
		var trace = tmFactory.createTrace();
		trace.setNode(node);
		trace.setProcessing(processing);
		gdpr2dfdTrace.getTracesList().add(trace);
		
		return node;		
	}
	
	/**
	 * Annotates the behaviour of the node associated with processing with the corresponding assignments
	 * @param processing processing whichs corresponding node needs to be annotated
	 */
	private void annotateBehaviour(Processing processing) {
		Node node = processingToNodeMap.get(processing);
		
		processing.getOutputData().forEach(data -> {
			if (data instanceof PersonalData personalData) {
				if (processing.getInputData().contains(personalData)) {
					var assignment = ddFactory.createForwardingAssignment();
					assignment.getInputPins().add(node.getBehaviour().getInPin().stream().filter(pin -> pin.getEntityName().equals(personalData.getEntityName())).findAny().orElseThrow());
					assignment.setOutputPin(node.getBehaviour().getOutPin().stream().filter(pin -> pin.getEntityName().equals(personalData.getEntityName())).findAny().orElseThrow());
					
					assignment.setEntityName("Forward " + personalData.getEntityName());
					node.getBehaviour().getAssignment().add(assignment);
				} else {
					var assignment = ddFactory.createAssignment();
					assignment.setOutputPin(node.getBehaviour().getOutPin().stream().filter(pin -> pin.getEntityName().equals(personalData.getEntityName())).findAny().orElseThrow());
					assignment.setTerm(ddFactory.createTRUE());
					personalData.getDataReferences().forEach(person -> {
						assignment.getOutputLabels().add(entityToLabelMap.get(person));
					});
					
					assignment.setEntityName("Send " + personalData.getEntityName());
					node.getBehaviour().getAssignment().add(assignment);
				}
			}
		});
	}
	
	
	
	/**
	 * Creates the label storing the processing type
	 * @param processing Processing whichs type is stored
	 * @return Label storing the type
	 */
	private List<Label> createTypeLabel(Processing processing) {
		var labels = new ArrayList<Label>();
		
		processing.getOnTheBasisOf().forEach(legalBasis -> {
			Label label = entityToLabelMap.get(legalBasis);
			labels.add(label);
		});
		
		processing.getPurpose().forEach(purpose -> {
			Label label = entityToLabelMap.get(purpose);
			labels.add(label);
		});
		
		if (processing.getResponsible() != null) labels.add(entityToLabelMap.get(processing.getResponsible()));		
		
		return labels;
	}
	
	/**
	 * Returns a label representing the Entity and its relationship to the processing element and creates it if necessary
	 * @param entity Entity to be represented by the label
	 * @param reference Reference representing the entiy and its relationship
	 * @return Creatd Label
	 */
	
	
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

	public TraceModel getGDPR2DFDTraceModel() {
		return gdpr2dfdTrace;
	}	
	
	public List<Data> intersection(List<Data> list1, List<Data> list2) {
		return list1.stream()
				  .distinct()
				  .filter(list2::contains)
				  .toList();
	}
}
