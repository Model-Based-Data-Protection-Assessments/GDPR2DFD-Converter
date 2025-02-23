package mdpa.gdpr.dfdconverter;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
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
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.FlowTrace;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.LabelTrace;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.NodeTrace;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.TraceModel;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.TracemodelFactory;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.TracemodelPackage;
import mdpa.gdpr.metamodel.GDPR.AbstractGDPRElement;
import mdpa.gdpr.metamodel.GDPR.Collecting;
import mdpa.gdpr.metamodel.GDPR.Consent;
import mdpa.gdpr.metamodel.GDPR.Controller;
import mdpa.gdpr.metamodel.GDPR.Data;
import mdpa.gdpr.metamodel.GDPR.ExerciseOfPublicAuthority;
import mdpa.gdpr.metamodel.GDPR.GDPRFactory;
import mdpa.gdpr.metamodel.GDPR.Processing;
import mdpa.gdpr.metamodel.GDPR.Purpose;
import mdpa.gdpr.metamodel.GDPR.Role;
import mdpa.gdpr.metamodel.GDPR.Storing;
import mdpa.gdpr.metamodel.GDPR.ThirdParty;
import mdpa.gdpr.metamodel.GDPR.Transferring;
import mdpa.gdpr.metamodel.GDPR.Usage;
import mdpa.gdpr.metamodel.GDPR.LegalBasis;
import mdpa.gdpr.metamodel.GDPR.NaturalPerson;
import mdpa.gdpr.metamodel.GDPR.Obligation;
import mdpa.gdpr.metamodel.GDPR.PerformanceOfContract;
import mdpa.gdpr.metamodel.GDPR.PersonalData;
import mdpa.gdpr.metamodel.GDPR.LegalAssessmentFacts;
import tools.mdsd.modelingfoundations.identifier.Entity;

public class DFD2GDPR {	

	private DataFlowDiagram dfd;
	private DataDictionary dd;
	private LegalAssessmentFacts laf;
	
	private TraceModel inTrace;
	private TraceModel outTrace;

	private GDPRFactory gdprFactory;
	
	private Map<Node, Processing> mapNodeToProcessing = new HashMap<>();	
	private Map<Label, Entity> labelToEntityMap = new HashMap<>();
	private Map<Entity, Entity> gdprElementClonesMap = new HashMap<>();
	
	private List<Node> collectingNodes = new ArrayList<>();
	private Map<Node, Long> mapNodeToAnalyzedIncomingFlows = new HashMap<>();
		
	private ResourceSet rs;	
	
	public DFD2GDPR(String dfdFile, String ddFile, String traceModelFile) {
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(dataflowdiagramPackage.eNS_URI, dataflowdiagramPackage.eINSTANCE);
		rs.getPackageRegistry().put(datadictionaryPackage.eNS_URI, datadictionaryPackage.eINSTANCE);
		rs.getPackageRegistry().put(TracemodelPackage.eNS_URI, TracemodelPackage.eINSTANCE);
		
		Resource dfdResource = rs.getResource(URI.createFileURI(dfdFile), true);		
		Resource ddResource = rs.getResource(URI.createFileURI(ddFile), true);
		Resource tmResource = rs.getResource(URI.createFileURI(traceModelFile), true);
		
		EcoreUtil.resolveAll(rs);
		EcoreUtil.resolveAll(ddResource);
		EcoreUtil.resolveAll(dfdResource);
		EcoreUtil.resolveAll(tmResource);
		
		setup((DataFlowDiagram) dfdResource.getContents().get(0), (DataDictionary) ddResource.getContents().get(0), (TraceModel) tmResource.getContents().get(0));
	}
	
	public DFD2GDPR(String dfdFile, String ddFile) {
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(dataflowdiagramPackage.eNS_URI, dataflowdiagramPackage.eINSTANCE);
		rs.getPackageRegistry().put(datadictionaryPackage.eNS_URI, datadictionaryPackage.eINSTANCE);
		rs.getPackageRegistry().put(TracemodelPackage.eNS_URI, TracemodelPackage.eINSTANCE);
		
		Resource dfdResource = rs.getResource(URI.createFileURI(dfdFile), true);
		Resource ddResource = rs.getResource(URI.createFileURI(ddFile), true);
		
		EcoreUtil.resolveAll(rs);
		EcoreUtil.resolveAll(ddResource);
		EcoreUtil.resolveAll(dfdResource);
		
		setup((DataFlowDiagram) dfdResource.getContents().get(0), (DataDictionary) ddResource.getContents().get(0));

	}
	
	public DFD2GDPR(DataFlowDiagram dfd, DataDictionary dd, TraceModel inTrace) {
		setup(dfd, dd, inTrace);
	}
	
	public DFD2GDPR(DataFlowDiagram dfd, DataDictionary dd) {
		setup(dfd, dd);
	}
	
	private void setup(DataFlowDiagram dfd, DataDictionary dd, TraceModel inTrace) {
		gdprFactory = GDPRFactory.eINSTANCE;
		laf = gdprFactory.createLegalAssessmentFacts();
		outTrace = TracemodelFactory.eINSTANCE.createTraceModel();
		this.dfd = dfd;
		this.dd = dd;
		this.inTrace = inTrace;
		if (rs == null) {
			rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		}
		
		if (inTrace == null) {
			dfd.getNodes().stream().forEach(node -> {
				long count = dfd.getFlows().stream().filter(flow -> flow.getDestinationNode().equals(node)).count();
				mapNodeToAnalyzedIncomingFlows.put(node, count);
			});
		}
	}
	
	private void setup(DataFlowDiagram dfd, DataDictionary dd) {
		setup(dfd, dd, null);
	}
	
	/**
	 * Performs the transformation on the models provided in the Constructor
	 */
	public void transform() {
		laf.setId(dfd.getId());
		
		if (inTrace == null) {
			collectingNodes = identifyCollectingNodes();
		}
		
				
		// convert labels
		parseLabels();
		
		// convert nodes
		convertNodes();
		dfd.getNodes().forEach(this::createProcessingReferences);
		
		// convert flows				
		transformFlowsInOrder();
		
		if (laf.getInvolvedParties().stream().noneMatch(Controller.class::isInstance)) {
			var controller = gdprFactory.createController();
			laf.getInvolvedParties().add(controller);
			laf.getProcessing().forEach(processing -> processing.setResponsible(controller));
		}
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
		tmResource.getContents().add(outTrace);
		
		saveResource(gdprResource);
		saveResource(tmResource);
	}
	
	/**
	 * Creates the GDPR specific objects from the labels holding the information
	 */
	private void parseLabels() {		
		dd.getLabelTypes().forEach(labelType -> {
			labelType.getLabel().forEach(label -> convertLabel(labelType, label));
		});
	}
	
	private void convertLabel(LabelType type, Label label) {
		var optLt = labelTraceLookup(type, label);
		
		if (optLt.isPresent()) {
			LabelTrace lt = optLt.get();
			// Put element in map
			AbstractGDPRElement gdprElement = cloneGDPRElement(lt.getGdprElement());
			//AbstractGDPRElement gdprElement = lt.getGdprElement();
	        labelToEntityMap.put(label, gdprElement);
	        lt.setGdprElement(gdprElement);
	        outTrace.getLabelTraces().add(lt);
		} else {
			switch(type.getEntityName()) {
			case "Controller":
				createNewGPDRElement(label, type, gdprFactory.createController(), laf.getInvolvedParties());
				break;
			case "NaturalPerson":
	        	createNewGPDRElement(label, type, gdprFactory.createNaturalPerson(), laf.getInvolvedParties());
				break;
			case "ThirdParty":
	        	createNewGPDRElement(label, type, gdprFactory.createThirdParty(), laf.getInvolvedParties());
				break;
			case "Consent":
	        	createNewGPDRElement(label, type, gdprFactory.createConsent(), laf.getLegalBases());
				break;
			case "Obligation":
				createNewGPDRElement(label, type, gdprFactory.createObligation(), laf.getLegalBases());
				break;
			case "Contract":
	        	createNewGPDRElement(label, type, gdprFactory.createPerformanceOfContract(), laf.getLegalBases());
				break;
			case "PublicAuthority":
	        	createNewGPDRElement(label, type, gdprFactory.createExerciseOfPublicAuthority(), laf.getLegalBases());
				break;
			case "Purposes":
	        	createNewGPDRElement(label, type, gdprFactory.createPurpose(), laf.getPurposes());
				break;
			case "Data":
	        	createNewGPDRElement(label, type, gdprFactory.createData(), laf.getData());
				break;
			case "PersonalData":
	        	createNewGPDRElement(label, type, gdprFactory.createPersonalData(), laf.getData());
				break;
			}
		}
	}
		
	private <T extends AbstractGDPRElement> T createNewGPDRElement(Label label, LabelType type, T gdprElement, List<T> lafElementCollection) {
		gdprElement.setEntityName(label.getEntityName());
		gdprElement.setId(label.getId());
        labelToEntityMap.put(label, gdprElement);
        lafElementCollection.add(gdprElement);
        LabelTrace lt = TracemodelFactory.eINSTANCE.createLabelTrace();
        lt.setGdprElement(gdprElement);
        lt.setLabel(label);
        lt.setLabelType(type);
        outTrace.getLabelTraces().add(lt);
        return gdprElement;
	}
	
	private Optional<LabelTrace> labelTraceLookup(LabelType type, Label label) {
		if (inTrace == null) {
			return Optional.empty();
		}
		return inTrace.getLabelTraces().stream().filter(lt -> lt.getLabelType().getEntityName().equals(type.getEntityName()) &&
				lt.getLabel().getEntityName().equals(label.getEntityName())).findAny();
	}
	
	private void convertNodes() {
		dfd.getNodes().forEach(node -> convertNode(node));
	}
	
	private void convertNode(Node node) {
		Processing gdprProcessing;
		
		var optNt = nodeTraceLookup(node);
		if (optNt.isPresent()) {
			NodeTrace nt = optNt.get();
			gdprProcessing = cloneProcessing(nt.getGdprProcessing());
			System.out.println(gdprProcessing.getEntityName() + "    " + gdprProcessing.getInputData().size());
			addNodeTrace(node, gdprProcessing);
		} else {
			gdprProcessing = createNewGDPRProcessing(node);
			addNodeTrace(node, gdprProcessing);
		}
		laf.getProcessing().add(gdprProcessing);
		mapNodeToProcessing.put(node, gdprProcessing);
	}
	
	private Optional<NodeTrace> nodeTraceLookup(Node node) {
		if (inTrace == null) {
			return Optional.empty();
		}
		return inTrace.getNodeTraces().stream().filter(
				nt -> nt.getDfdNode().getId().equals(node.getId())).findAny();
	}
	
	private void addNodeTrace(Node node, Processing processing) {
		var nt = TracemodelFactory.eINSTANCE.createNodeTrace();
		nt.setDfdNode(node);
		nt.setGdprProcessing(processing);
		outTrace.getNodeTraces().add(nt);
	}
	
	private Processing createNewGDPRProcessing(Node node) {
		Processing processing;
		Label processingTypeLabel = node.getProperties().stream().filter(label -> getLabelTypeOfLabel(label).getEntityName().equals("ProcessingType")).findFirst().orElse(null);
		
		if (processingTypeLabel == null) {
			if (collectingNodes.contains(node)) processing = gdprFactory.createCollecting();
			else processing = gdprFactory.createUsage();
		} else {
			switch (processingTypeLabel.getEntityName()) {
			case "Collecting":
				processing = gdprFactory.createCollecting();
				break;
			case "Storing":
				processing = gdprFactory.createStoring();
				break;
			case "Usage":
				processing = gdprFactory.createUsage();
				break;
			case "Transferring":
				processing = gdprFactory.createTransferring();
				break;
			default: // this should not occur processings without any additional information should be modelled as "Usage"
				processing = gdprFactory.createProcessing();
				break;
			}
		}
		
		processing.setEntityName(node.getEntityName());
		processing.setId(node.getId());
		
		return processing;
	}
	
	private LabelType getLabelTypeOfLabel(Label label) {
		var labelContainer = label.eContainer();
		if(labelContainer instanceof LabelType type) {
			return type;
		} else {
			// I know it is ugly but it is just a fallback and should never be the case with validated models.
			//throw new Exception("Containment of label " + label.getEntityName() + " is not of type LabelType!");
			return null;
		}
	}
		
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
	private void createProcessingReferences(Node node) {
		var processing = mapNodeToProcessing.get(node);
		
		/*for (Pin pin : node.getBehavior().getInPin()) {
			Data data = getDataFromPin(pin);
			if(processing.getInputData().stream().noneMatch(d -> d.getId().equals(data.getId())) && data != null ) {
				processing.getInputData().add(data);
			}
		}
		for (Pin pin : node.getBehavior().getOutPin()) {
			Data data = getDataFromPin(pin);
			if(processing.getOutputData().stream().noneMatch(d -> d.getId().equals(data.getId())) && data != null) {
				processing.getOutputData().add(data);
			}
		}*/
			
		processing.getOnTheBasisOf().clear();
		processing.getOnTheBasisOf().addAll(getLegalBasesFromNode(node));
		
		processing.getPurpose().clear();
		processing.getPurpose().addAll(getPurposesFromNode(node));
		
		processing.setResponsible(getControllerFromNode(node));		
	}
	
	private Data getDataFromPin(Pin pin) {
		return laf.getData().stream().filter(it -> it.getEntityName().equals(pin.getEntityName())).findAny().orElse(null);
	}
	
	private void transformFlowsInOrder() {
		if (inTrace != null) dfd.getFlows().forEach(flow -> transformFlow(flow));
		else {
			var startingNodes = collectingNodes;
			while (startingNodes != null && startingNodes.size() > 0) {		
				List<Node> followingNodes = new ArrayList<>();
				for (Node node : startingNodes) {
					List<Flow> outgoingFlows = dfd.getFlows().stream().filter(flow -> flow.getSourceNode().equals(node)).toList();
					var processing = mapNodeToProcessing.get(node);
					if (!outgoingFlows.isEmpty()) {
						processing.getOutputData().addAll(processing.getInputData().stream().filter(it -> !processing.getOutputData().contains(it)).toList());
					}
					
					List<Processing> followingProcessing = outgoingFlows.stream().map(it -> mapNodeToProcessing.get(it.getDestinationNode())).toList(); 
					List<Processing> followingProcessingWithNewInputData = new ArrayList<>();
					
					if (collectingNodes.contains(node) && processing.getOutputData().isEmpty()) {
						var dataCollecting = gdprFactory.createData();
						var purposeCollecting = gdprFactory.createPurpose();
						laf.getData().add(dataCollecting);
						laf.getPurposes().add(purposeCollecting);
						
						processing.getOutputData().add(dataCollecting);
						processing.getPurpose().add(purposeCollecting);
					}
					
					List<Data> outgoingData = processing.getOutputData();
					outgoingFlows.forEach(flow -> {
						outgoingData.forEach(data -> {
							if (outTrace.getFlowTraces().stream().filter(flowTrace -> flowTrace.getDataFlow().equals(flow) && flowTrace.getData().equals(data)).count() == 0) {
								var ft = TracemodelFactory.eINSTANCE.createFlowTrace();
								ft.setDataFlow(flow);
								ft.setDest(mapNodeToProcessing.get(flow.getDestinationNode()));
								ft.setSource(mapNodeToProcessing.get(flow.getSourceNode()));
								ft.setData(data);
								outTrace.getFlowTraces().add(ft);
							}
						});
					});
					
					followingProcessing.forEach(it -> {
						if (!it.getInputData().containsAll(outgoingData)) {
							followingProcessingWithNewInputData.add(it);
							it.getInputData().addAll(outgoingData.stream().filter(data -> !it.getInputData().contains(data)).toList());
							it.getPurpose().addAll(processing.getPurpose().stream().filter(purpose -> !it.getPurpose().contains(purpose)).toList());
						}
						
						if (!processing.getFollowingProcessing().contains(it)) processing.getFollowingProcessing().add(it);
					});					
					followingNodes.addAll(outgoingFlows.stream().map(it -> it.getDestinationNode()).filter(it -> followingProcessingWithNewInputData.contains(mapNodeToProcessing.get(it))).toList());
				}
				startingNodes = followingNodes;
			}
		}
	}
	
	/**
	 * Transforms a flow into the following processing attribute of processing
	 * @param flow
	 */
	private void transformFlow(Flow flow) {		
		Processing source;
		Processing dest;
		
		var optFt = flowTraceLookup(flow);
		if (optFt.isPresent()) {
			var ft = optFt.get();
			outTrace.getFlowTraces().addAll(ft);
			if (ft.isEmpty()) return;
			else {
				source = cloneProcessing(ft.get(0).getSource());
				dest = cloneProcessing(ft.get(0).getDest());
				System.out.println("test");
			}
		} else {
			source = mapNodeToProcessing.get(flow.getSourceNode());
			dest = mapNodeToProcessing.get(flow.getDestinationNode());
			
			var ft = TracemodelFactory.eINSTANCE.createFlowTrace();
			ft.setDataFlow(flow);
			ft.setDest(dest);
			ft.setSource(source);
			ft.setData(getDataFromPin(flow.getSourcePin()));
			outTrace.getFlowTraces().add(ft);
		}
		
		// this might not be necessary if a trace exists but checking does no harm.
		if(!source.getFollowingProcessing().contains(dest)) source.getFollowingProcessing().add(dest);
	}
	
	private Optional<List<FlowTrace>> flowTraceLookup(Flow flow) {
		if (inTrace == null) {
			return Optional.empty();
		}
		return Optional.of(inTrace.getFlowTraces().stream().filter(ft -> ft.getDataFlow().getId().equals(flow.getId())).toList());
	}
	
	//Cloning because of ECore Containment
	private AbstractGDPRElement cloneGDPRElement(AbstractGDPRElement element) {
		if (element instanceof Data data) {
        	return cloneData(data);
        } else if (element instanceof LegalBasis legalBasis) {
        	return cloneLegalBasis(legalBasis);
        } else if (element instanceof Processing processing) {
        	return cloneProcessing(processing);
        } else if (element instanceof Purpose purpose) {
        	return clonePurpose(purpose);
        } else if (element instanceof Role role) {
			return cloneRole(role);
        } else {
        	return null;
        }
	}
	
	private Data cloneData(Data data) {
		if (gdprElementClonesMap.containsKey(data)) return (Data) gdprElementClonesMap.get(data);

		
		Data clone;
		if (data instanceof PersonalData personalData) {
			clone = GDPRFactory.eINSTANCE.createPersonalData();
			personalData.getDataReferences().forEach(reference -> {
				((PersonalData)clone).getDataReferences().add((NaturalPerson)cloneRole(reference));
			});
		} else clone = GDPRFactory.eINSTANCE.createData();
		clone.setEntityName(data.getEntityName());
		clone.setId(data.getId());
		
		laf.getData().add(clone);
		gdprElementClonesMap.put(data, clone);

		return clone;
	}
	
	private Role cloneRole(Role role) {
		if (role == null) {
			return null;
		}
		if (gdprElementClonesMap.containsKey(role)) return (Role) gdprElementClonesMap.get(role);
		
		Role clone;
		if (role instanceof Controller) {
			clone = GDPRFactory.eINSTANCE.createController();
		} else if (role instanceof ThirdParty) {
			clone = GDPRFactory.eINSTANCE.createThirdParty();
		} else {
			clone = GDPRFactory.eINSTANCE.createNaturalPerson();
		}
		
		if (role.getName() != null) {
			clone.setName(role.getName());
		}
		clone.setEntityName(role.getEntityName());
		clone.setId(role.getId());
		
		laf.getInvolvedParties().add(clone);
		gdprElementClonesMap.put(role, clone);

				
		return clone;
	}
	
	private Purpose clonePurpose(Purpose purpose) {
		if (gdprElementClonesMap.containsKey(purpose)) return (Purpose) gdprElementClonesMap.get(purpose);
	
		Purpose clone = GDPRFactory.eINSTANCE.createPurpose();
		clone.setEntityName(purpose.getEntityName());
		clone.setId(purpose.getId());
		
		laf.getPurposes().add(clone);
		gdprElementClonesMap.put(purpose, clone);
		
		return clone;
	}
	
	private LegalBasis cloneLegalBasis(LegalBasis legalBasis) {
		if(gdprElementClonesMap.containsKey(legalBasis)) return (LegalBasis) gdprElementClonesMap.get(legalBasis);
		
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
		
		
		laf.getLegalBases().add(clone);
		gdprElementClonesMap.put(legalBasis, clone);
		
		return clone;
	}
	
	private Processing cloneProcessing(Processing processing) {
		if (gdprElementClonesMap.containsKey(processing)) return (Processing) gdprElementClonesMap.get(processing);
	
		Processing clone;
		if ( processing instanceof Collecting) clone= GDPRFactory.eINSTANCE.createCollecting();
		else if (processing instanceof Storing) clone = GDPRFactory.eINSTANCE.createStoring();
		else if (processing instanceof Transferring) clone = GDPRFactory.eINSTANCE.createTransferring();
		else if (processing instanceof Usage) clone = GDPRFactory.eINSTANCE.createUsage();
		else clone = GDPRFactory.eINSTANCE.createProcessing();
		
		
		if(processing.getResponsible() != null) clone.setResponsible((Role)cloneRole(processing.getResponsible()));
		
		clone.setEntityName(processing.getEntityName());
		clone.setId(processing.getId());
		processing.getInputData().forEach(data -> clone.getInputData().add(cloneData(data)));
		processing.getOutputData().forEach(data -> clone.getOutputData().add(cloneData(data)));
		processing.getOnTheBasisOf().forEach(legalBasis -> clone.getOnTheBasisOf().add(cloneLegalBasis(legalBasis)));
		processing.getPurpose().forEach(purpose -> clone.getPurpose().add(clonePurpose(purpose)));
		
		laf.getProcessing().add(clone);
		gdprElementClonesMap.put(processing, clone);
		
		return clone;
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

	public TraceModel getDFD2GDPRTrace() {
		return outTrace;
	}
	
	private List<Node> identifyCollectingNodes() {
		return dfd.getNodes().stream().filter(node -> dfd.getFlows().stream().noneMatch(flow -> flow.getDestinationNode().equals(node))).toList();
	}
}
