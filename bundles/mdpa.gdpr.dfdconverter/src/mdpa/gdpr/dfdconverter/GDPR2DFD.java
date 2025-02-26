package mdpa.gdpr.dfdconverter;

import org.dataflowanalysis.dfd.datadictionary.*;
import org.dataflowanalysis.dfd.dataflowdiagram.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.URIHandlerImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

import mdpa.gdpr.metamodel.GDPR.*;
import tools.mdsd.modelingfoundations.identifier.Entity;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.*;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

public class GDPR2DFD {
	private DataFlowDiagram dfd;
	private DataDictionary dd;
	private LegalAssessmentFacts laf;
	private TraceModel inTrace;
	private TraceModel outTrace;
	
	private dataflowdiagramFactory dfdFactory;
	private datadictionaryFactory ddFactory;	
	
	private LabelType processingTypeLabelType;
	private LabelType dataLabelType;
	private LabelType personalDataLabelType;
	
	private LabelType consentLabelType;
	private LabelType obligationLabelType;
	private LabelType contractLabelType;
	private LabelType authorityLabelType;
	
	private LabelType personLabelType;
	private LabelType controllerLabelType;
	private LabelType thirdPartyLabelType;
	
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
		
		Resource gdprResource = rs.getResource(URI.createFileURI(gdprFile), true);
		Resource ddResource = rs.getResource(URI.createFileURI(ddFile), true);
		Resource tmResource = rs.getResource(URI.createFileURI(traceModelFile), true);
		
		EcoreUtil.resolveAll(rs);
		EcoreUtil.resolveAll(ddResource);
		EcoreUtil.resolveAll(gdprResource);
		EcoreUtil.resolveAll(tmResource);
		
		setup((LegalAssessmentFacts) gdprResource.getContents().get(0), (TraceModel) tmResource.getContents().get(0), (DataDictionary) ddResource.getContents().get(0));
	}
	
	/**
	 * Creates Transformation with tracemodel to recreate DFD instance
	 * @param gdprFile GDPR instance
	 * @param ddFile Data Dictionary instance
	 * @param traceModelFile TraceModel instance
	 */
	public GDPR2DFD(LegalAssessmentFacts laf, DataDictionary dd, TraceModel inTrace) {				
		setup(laf, inTrace, dd);
	}
	
	public GDPR2DFD(LegalAssessmentFacts laf, TraceModel inTrace) {				
		setup(laf, inTrace, null);
	}
	
	/**
	 * Creates Transformation for initial transformations from the GDPR into the DFD metamodel
	 * @param gdprFile Location of the gdpr instance
	 */
	public GDPR2DFD(String gdprFile) {		
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(GDPRPackage.eNS_URI, GDPRPackage.eINSTANCE);
				
		Resource gdprResource = rs.getResource(URI.createFileURI(gdprFile), true);
		
		EcoreUtil.resolveAll(gdprResource);
		
		setup((LegalAssessmentFacts) gdprResource.getContents().get(0), null, null);
	}
	
	public GDPR2DFD(LegalAssessmentFacts laf) {
		setup(laf, null, null);
	}
	
	private void setup(LegalAssessmentFacts laf, TraceModel inTrace, DataDictionary dd) {
		this.laf = laf;
		this.inTrace = inTrace;
		
		outTrace = TracemodelFactory.eINSTANCE.createTraceModel();

		dfdFactory = dataflowdiagramFactory.eINSTANCE;
		ddFactory = datadictionaryFactory.eINSTANCE;
		dfd = dfdFactory.createDataFlowDiagram();
		if(dd == null) {
			this.dd = ddFactory.createDataDictionary();
		} else {
			this.dd = dd;
		}
		
		if (rs == null) {
			rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		}
	}
	
	/**
	 * Saves the created model instances at the provided locations
	 * @param dfdFile Location of where to save the new DFD instance
	 * @param ddFile Location of where to save the new DD instance
	 * @param traceModelFile Location of where to save the new TM instance
	 */
	public void save(String dfdFile, String ddFile, String traceModelFile) {
		Resource dfdResource = createAndAddResource(dfdFile, new String[] {"dataflowdiagram"} ,rs);
		Resource outTraceResource = createAndAddResource(traceModelFile, new String[] {"tracemodel"} ,rs);
		if (ddResource == null)  {
			ddResource = createAndAddResource(ddFile, new String[] {"datadictionary"} ,rs);
			ddResource.getContents().add(dd);
		}
		if (!ddResource.getURI().toString().equals(URI.createFileURI(ddFile).toFileString())) {			

			ddResource = createAndAddResource(ddFile, new String[] {"datadictionary"} ,rs);			
			ddResource.getContents().add(dd);	
			
			dd = (DataDictionary) ddResource.getContents().get(0);

			EcoreUtil.resolveAll(ddResource);
			
			
			
			Map<String, Pin> idToPinMap = new HashMap<>();
			dd.getBehavior().stream().map(it -> {
				List<Pin> pins = new ArrayList<>();
				pins.addAll(it.getInPin());
				pins.addAll(it.getOutPin());
				return pins;
			}).flatMap(it -> it.stream()).forEach(pin -> idToPinMap.put(pin.getId(), pin));
			
			List<Flow> newFlows = dfd.getFlows().stream().map(flow -> {
				Flow newFlow = dfdFactory.createFlow();
				newFlow.setId(flow.getId());
				newFlow.setEntityName(flow.getEntityName());
				newFlow.setDestinationNode(flow.getDestinationNode());
				newFlow.setSourceNode(flow.getSourceNode());
				
				newFlow.setDestinationPin(idToPinMap.get(flow.getDestinationPin().getId()));
				newFlow.setSourcePin(idToPinMap.get(flow.getSourcePin().getId()));
				return newFlow;
			}).toList();
			
			dfd.getFlows().removeAll(dfd.getFlows());
			dfd.getFlows().addAll(newFlows);
			
			List<FlowTrace> newFlowTraces = outTrace.getFlowTraces().stream().map(trace -> {
				var ft = TracemodelFactory.eINSTANCE.createFlowTrace();
				ft.setDataFlow(newFlows.stream().filter(it -> trace.getDataFlow().getId().equals(it.getId())).findAny().orElseThrow());
				ft.setSource(trace.getSource());
				ft.setDest(trace.getDest());
				ft.setData(trace.getData());
				return ft;
			}).toList();
			
			outTrace.getFlowTraces().clear();
			outTrace.getFlowTraces().addAll(newFlowTraces);
			
			Map<String, Label> ifToLabelMap = new HashMap<>();
			dd.getLabelTypes().forEach(labelType -> labelType.getLabel().forEach(label -> ifToLabelMap.put(label.getId(), label)));
			
			Map<String, Behavior> idToBehaviorMap = new HashMap<>();
			dd.getBehavior().forEach(behavior -> idToBehaviorMap.put(behavior.getId(), behavior));
			
			dfd.getNodes().forEach(node -> {
				node.setBehavior(idToBehaviorMap.get(node.getBehavior().getId()));
				var newProperties = node.getProperties().stream().map(label -> ifToLabelMap.get(label.getId())).toList();
				node.getProperties().removeAll(node.getProperties());
				node.getProperties().addAll(newProperties);
			});
			
			
			
		}
		
		dfdResource.getContents().add(dfd);		
		outTraceResource.getContents().add(outTrace);
		

		saveResource(ddResource);
		EcoreUtil.resolveAll(outTraceResource);
		saveResource(outTraceResource);
		saveResource(dfdResource);
	}
	
	/**
	 * Performs the transformation with the information provided in the constructor
	 */
	public void transform() {
		dfd.setId(laf.getId());
		
		//Create Nodes 
		createNodes();
		
		//Create DD
		if(dd == null) {
			ddFactory = datadictionaryFactory.eINSTANCE;
			dd = ddFactory.createDataDictionary();	
		}
		
		//Create LabelTypes
		createLabelTypes();
		
		//Create Labels
		createLabels();
		
		//Annotate Labels to Nodes (LegalBasis,Purpose,ProcessingType,Role)
		annotateNodeLabels();
		
		//Create Flows
		laf.getProcessing().stream().forEach(p -> {
			dfd.getFlows().addAll(createFlows(p));
		});
		
		//Sorry for that but otherwise containment causes issues
		laf.getProcessing().forEach(p -> {
			p.getFollowingProcessing().forEach(fp -> {
				p.getOutputData().forEach(data -> {
					dfd.getFlows().stream().filter(flow -> flow.getSourceNode().equals(processingToNodeMap.get(p)) && flow.getDestinationNode().equals(processingToNodeMap.get(fp))).forEach(flow -> {
						addFlowTrace(flow, p, fp, data);
					});
				});
			});
		});
		
		// Create/Annotate Behaviors to Nodes
		laf.getProcessing().stream().forEach(p -> {
			annotateBehaviour(p);
		});
	}
	
	/**
	 * Creates the labeltypes that hold the GDPR instance specific information
	 */
	private void createLabelTypes() {
		processingTypeLabelType = getOrCreateLabelType("ProcessingType");
		dataLabelType = getOrCreateLabelType("Data");
		personalDataLabelType = getOrCreateLabelType("PersonalData");
		
		obligationLabelType = getOrCreateLabelType("Obligation");
		consentLabelType = getOrCreateLabelType("Consent");
		contractLabelType = getOrCreateLabelType("Contract");
		authorityLabelType = getOrCreateLabelType("PublicAuthority");
		
		personLabelType = getOrCreateLabelType("NaturalPerson");
		controllerLabelType = getOrCreateLabelType("Controller");
		thirdPartyLabelType = getOrCreateLabelType("ThirdParty");
		
		purposeLabelType = getOrCreateLabelType("Purposes");
	}
	
	private Optional<LabelType> labelTypeDDLookup(String typeName) {
		return dd.getLabelTypes().stream().filter(lt -> lt.getEntityName().equals(typeName)).findAny();
	}
	
	private LabelType getOrCreateLabelType(String typeName) {
		var optLT = labelTypeDDLookup(typeName);
		if (optLT.isPresent()) {
			return optLT.get();
		} else {
			LabelType type = ddFactory.createLabelType();
			type.setEntityName(typeName);
			dd.getLabelTypes().add(type);
			return type;
		}
	}
	
	/**
	 * Creates all Labels to hold GDPR specific information
	 */
	private void createLabels() {
		laf.getInvolvedParties().forEach(role -> {
			if (role instanceof NaturalPerson) convertElementToLabel(role,this.personLabelType);
			else if (role instanceof ThirdParty) convertElementToLabel(role, this.thirdPartyLabelType);
			else if (role instanceof Controller) convertElementToLabel(role, this.controllerLabelType);		
		});
		
		laf.getLegalBases().forEach(legalBasis -> {
			if (legalBasis instanceof Consent) convertElementToLabel(legalBasis, this.consentLabelType);
			else if (legalBasis instanceof Obligation) convertElementToLabel(legalBasis, this.obligationLabelType);
			else if (legalBasis instanceof PerformanceOfContract) convertElementToLabel(legalBasis, this.contractLabelType);
			else if (legalBasis instanceof ExerciseOfPublicAuthority) convertElementToLabel(legalBasis, this.authorityLabelType);
		});
		
		laf.getPurposes().forEach(purpose -> {
			convertElementToLabel(purpose, this.purposeLabelType);
		});
		
		laf.getData().forEach(data -> {
			if (data instanceof PersonalData personalData) convertElementToLabel(data, this.personalDataLabelType);
			else convertElementToLabel(data, dataLabelType);
		});
		
		createProcessingTypeLabels();
	}
	
	private void convertElementToLabel(AbstractGDPRElement gdprElement, LabelType type) {
		if(!entityToLabelMap.containsKey(gdprElement)) {
			Label label;
			var optLt = labelTraceLookup(gdprElement, type);
			if (optLt.isPresent()) {
				LabelTrace lt = optLt.get();
				label = lt.getLabel();
				type.getLabel().add(label);
				outTrace.getLabelTraces().add(lt);
			} else {
				label = createLabel(gdprElement, type);
				addLabelTrace(gdprElement, label, type);
			}
			entityToLabelMap.put(gdprElement, label);
		}
	}
	
	private Label createLabel(AbstractGDPRElement gdprElement, LabelType type) {
		Label label;
		label = ddFactory.createLabel();
		label.setEntityName(gdprElement.getEntityName());
		label.setId(gdprElement.getId());
		type.getLabel().add(label);
		return label;
	}
	
	private Optional<LabelTrace> labelTraceLookup(AbstractGDPRElement gdprElement, LabelType type) {
		if (inTrace == null) {
			return Optional.empty();
		}
		return inTrace.getLabelTraces().stream()
				.filter( // same id ensures same element, same labeltype also ensures same gdpr class
						lt -> lt.getLabelType().getEntityName().equals(type.getEntityName()) && 
							  lt.getGdprElement().getId().equals(gdprElement.getId()))
				.findAny();
	}
	
	private void addLabelTrace(AbstractGDPRElement gdprElement, Label label, LabelType type) {
		var lt = TracemodelFactory.eINSTANCE.createLabelTrace();
		lt.setGdprElement(gdprElement);
		lt.setLabel(label);
		lt.setLabelType(type);
		outTrace.getLabelTraces().add(lt);
	}
	
	private void createProcessingTypeLabels() {
		var processingTypeLabelNames = List.of("Collecting", "Storing", "Usage", "Transferring");
		for(String name : processingTypeLabelNames) {
			if (processingTypeLabelType.getLabel().stream().noneMatch(label -> label.getEntityName().equals(name))) {
				Label newProcessingTypeLabel;
				newProcessingTypeLabel = ddFactory.createLabel();
				newProcessingTypeLabel.setEntityName(name);
				processingTypeLabelType.getLabel().add(newProcessingTypeLabel);
			}
		}
	}
	
	private void createNodes() {
		laf.getProcessing().stream().forEach(p -> {
			Node node = convertProcessing(p);
			dfd.getNodes().add(node);
		});
	}
	
	private Node convertProcessing(Processing processing) {
		// Map lookup
		if (processingToNodeMap.containsKey(processing)) {
			return processingToNodeMap.get(processing);
		} else { 
			// Trace lookup
			Node node;
			var optNt = nodeTraceLookup(processing);
			if (optNt.isPresent()) {
				NodeTrace nt = optNt.get();
				node = nt.getDfdNode();
				if (dd.getBehavior().stream().noneMatch(behavior -> behavior.getId().equals(node.getBehavior().getId()))) dd.getBehavior().add(node.getBehavior());
				outTrace.getNodeTraces().add(nt);
			} else {
				// Create
				node = createNewNode(processing);
				addNodeTrace(processing, node);
			}
			
			processingToNodeMap.put(processing, node);
			return node;
		}
	}
	
	private Node createNewNode(Processing processing) {
		Node node;
		if (processing instanceof Collecting) {
			node = dfdFactory.createExternal();
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
		node.setEntityName(processing.getEntityName());
		
		Behavior behavior = ddFactory.createBehavior();
		behavior.setEntityName(node.getEntityName() + " Behavior");
		node.setBehavior(behavior);
		dd.getBehavior().add(behavior);
		
		return node;
	}
	
	private Optional<NodeTrace> nodeTraceLookup(Processing processing) {
		if (inTrace == null) {
			return Optional.empty();
		}
		return inTrace.getNodeTraces().stream()
				.filter(
						nt -> nt.getGdprProcessing().getId().equals(processing.getId()) &&
							  nt.getGdprProcessing().getClass().equals(processing.getClass()))
				.findAny();
	}
	
	private void addNodeTrace(Processing processing, Node node) {
		var nt = TracemodelFactory.eINSTANCE.createNodeTrace();
		nt.setDfdNode(node);
		nt.setGdprProcessing(processing);
		outTrace.getNodeTraces().add(nt);
	}

	private void annotateNodeLabels() {
		laf.getProcessing().stream().forEach(p -> {
			addNodeLabels(p);
		});
	}
	
	private Label getProcessingTypeLabel(String labelName) {
		return processingTypeLabelType.getLabel().stream().filter(l -> l.getEntityName().equals(labelName)).findAny().orElseThrow();
	}
	
	private void addNodeLabels(Processing processing) {
		Node node = processingToNodeMap.get(processing);
		
		processing.getOnTheBasisOf().forEach(legalBasis -> {
			Label label = entityToLabelMap.get(legalBasis);
			node.getProperties().add(label);

		});
		
		processing.getPurpose().forEach(purpose -> {
			Label label = entityToLabelMap.get(purpose);
			node.getProperties().add(label);
		});
		
		if (processing.getResponsible() != null) {
			Label label = entityToLabelMap.get(processing.getResponsible());
			node.getProperties().add(label);
		}
		
		String processingTypeName;
		if (processing instanceof Collecting) {
			processingTypeName = "Collecting";
		} else if (processing instanceof Storing) {
			processingTypeName = "Storing";
		} else if (processing instanceof Usage) {
			processingTypeName = "Usage";
		} else if (processing instanceof Transferring) {
			processingTypeName = "Transferring";
		} else {
			processingTypeName = "Usage";
			//TODO: Error needs handling. Processing instances should not be allowed and should be of type Usage.
		}
		
		node.getProperties().add(getProcessingTypeLabel(processingTypeName));
	}
	
	/**
	 * Creates all flows outgoing from a processing from following processing or pulls them from the tracemodel if present
	 * @param processing Source for the created Nodes
	 * @return All flows going out from the source node
	 */
	private List<Flow> createFlows(Processing processing) {
		List<Flow> flows = new ArrayList<>();
		
		Node sourceNode = processingToNodeMap.get(processing);
		for (Processing followingProcessing : processing.getFollowingProcessing()) {
			Node destinationNode = processingToNodeMap.get(followingProcessing);
			
			List<Data> dataSent = intersection(processing.getOutputData(), followingProcessing.getInputData());
			dataSent.forEach(data -> {
				Optional<FlowTrace> optFt = flowTraceLookup(processing, followingProcessing, data);
				
				if(optFt.isPresent()) {
					FlowTrace ft = optFt.get();
					flows.add(ft.getDataFlow());
				} else {
					String dataName = data.getEntityName();
					
					Pin outPin = sourceNode.getBehavior().getOutPin().stream().filter(pin -> pin.getEntityName().equals(dataName)).findAny().orElse(null);
					if (outPin == null) {
						outPin = ddFactory.createPin();
						outPin.setEntityName(dataName);
						sourceNode.getBehavior().getOutPin().add(outPin);
					}
					
					Pin inPin = destinationNode.getBehavior().getInPin().stream().filter(pin -> pin.getEntityName().equals(dataName)).findAny().orElse(null);
					if (inPin == null) {
						inPin = ddFactory.createPin();
						inPin.setEntityName(dataName);
						destinationNode.getBehavior().getInPin().add(inPin);
					}
					
					Flow flow = createNewFlow(dataName, sourceNode, outPin, destinationNode, inPin);
					flows.add(flow);
				}
			});
		}
		
		return flows;
	}
	
	private Optional<FlowTrace> flowTraceLookup(Processing source, Processing dest, Data data) {
		if (inTrace == null) {
			return Optional.empty();
		}
		return inTrace.getFlowTraces().stream()
				.filter(
						ft -> ft.getSource().getId().equals(source.getId()) &&
							  ft.getDest().getId().equals(dest.getId()) &&
							  ft.getData().getId().equals(data.getId()))
				.findAny();
	}
	
	private Flow createNewFlow(String name, Node source, Pin sourcePin, Node dest, Pin destPin) {
		Flow flow = dfdFactory.createFlow();
		flow.setEntityName(name);
		
		flow.setDestinationNode(dest);
		flow.setSourceNode(source);
		flow.setDestinationPin(destPin);
		flow.setSourcePin(sourcePin);

		return flow;
	}
	
	private void addFlowTrace(Flow dataflow, Processing source, Processing dest, Data data) {
		var ft = TracemodelFactory.eINSTANCE.createFlowTrace();
		ft.setDataFlow(dataflow);
		ft.setSource(source);
		ft.setDest(dest);
		ft.setData(data);
		outTrace.getFlowTraces().add(ft);
	}
	
	/**
	 * Annotates the behaviour of the node associated with processing with the corresponding assignments
	 * @param processing processing whichs corresponding node needs to be annotated
	 */
	private void annotateBehaviour(Processing processing) {
		Node node = processingToNodeMap.get(processing);
		
		processing.getOutputData().forEach(data -> {
			Optional<AssignmentTrace> optAt = assignmentTraceLookup(processing, data, node);
			
			if (optAt.isPresent()) {
				AssignmentTrace at = optAt.get();
//				node.getBehaviour().getAssignment().add(at.getAssignment());
				outTrace.getAssignmentTraces().add(at);
			} else {
				if (processing.getFollowingProcessing().size() == 0) return;
				if (processing.getInputData().contains(data)) {
					if (!containsAssignment(node.getBehavior(), data.getEntityName(), data)) {
						// the same data is set as input and output the labels are simply forwarded.
							node.getBehavior().getOutPin().forEach(pin -> {
								var assignment = ddFactory.createForwardingAssignment();
							assignment.getInputPins().addAll(node.getBehavior().getInPin());
						
							assignment.setOutputPin(pin);
							
							assignment.setEntityName("Forward " + data.getEntityName());
							node.getBehavior().getAssignment().add(assignment);
						});						
					}
				} else {
					// if data is not forwarded AND the data is of type PersonalData, the label of the corresponding natural person is set.
					if (data instanceof PersonalData personalData) {
						if (!containsAssignment(node.getBehavior(), data) && processing.getFollowingProcessing().size() > 0) {
							var assignment = ddFactory.createAssignment();
							assignment.setOutputPin(node.getBehavior().getOutPin().stream().filter(pin -> pin.getEntityName().equals(personalData.getEntityName())).findAny().orElseThrow());
							assignment.setTerm(ddFactory.createTRUE());
							personalData.getDataReferences().forEach(person -> {
								assignment.getOutputLabels().add(entityToLabelMap.get(person));
							});
							
							assignment.setEntityName("Send " + personalData.getEntityName());
							node.getBehavior().getAssignment().add(assignment);
						}
					} else {
						// Special case, here no assignment is set. It is up to the developer in the DFD/DD to decide what happens in this node.
						// Once this is done in the DFD/DD, the trace keeps it up to date.
					}
				}
			}
		});
	}
	
	
	private boolean containsAssignment(Behavior behaviour, String inputPinName, Data data) {
		return behaviour.getAssignment().stream().anyMatch(
				ass ->  {			
					if (ass.getOutputPin() == null) return false;
					if (!ass.getOutputPin().getEntityName().equals(data.getEntityName())) return false; 
					if (ass instanceof ForwardingAssignment forwardingAssignment) {
						if (forwardingAssignment.getInputPins().stream().anyMatch(pin -> pin.getEntityName().equals(inputPinName))) return true;
					} else if (ass instanceof Assignment assignment) {
						if (assignment.getInputPins().stream().anyMatch(pin -> pin.getEntityName().equals(inputPinName))) return true;
					}
					return false;
				});
	}
	
	private boolean containsAssignment(Behavior behaviour, Data data) {
		return behaviour.getAssignment().stream().anyMatch(ass -> ass.getOutputPin().getEntityName().equals(data.getEntityName()));
	}
	
	private Optional<AssignmentTrace> assignmentTraceLookup(Processing processing, Data outputData, Node node) {
		if (inTrace == null) {
			return Optional.empty();
		}
		return inTrace.getAssignmentTraces().stream()
				.filter(
						at -> at.getProcessing().getId().equals(processing.getId()) &&
							  at.getOutputData().getId().equals(outputData.getId()) && 
							  node.getBehavior().getAssignment().stream()
							  	.anyMatch(
							  			ass -> ass.getEntityName().equals(at.getAssignment().getEntityName())))
				.findAny();
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

	public TraceModel getGDPR2DFDTrace() {
		return outTrace;
	}	
	
	public List<Data> intersection(List<Data> list1, List<Data> list2) {
		return list1.stream()
				  .distinct()
				  .filter(data -> list2.contains(data))
				  .toList();
	}
}
