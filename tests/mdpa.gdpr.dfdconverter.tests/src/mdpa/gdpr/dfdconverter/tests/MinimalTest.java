package mdpa.gdpr.dfdconverter.tests;

import mdpa.gdpr.dfdconverter.GDPR2DFD;
import mdpa.gdpr.metamodel.GDPR.LegalAssessmentFacts;
import tools.mdsd.modelingfoundations.identifier.Entity;
import mdpa.gdpr.dfdconverter.DFD2GDPR;

import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;
import org.eclipse.emf.ecore.util.EcoreUtil;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.dataflowanalysis.dfd.datadictionary.Behaviour;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MinimalTest {
	private static final String DFD_MODEL_PATH = "minimal.dataflowdiagram";
	private static final String DD_MODEL_PATH = "minimal.datadictionary";
	private static final String TM_MODEL_PATH = "minimal.tracemodel";
	private static final String GDPR_MODEL_PATH = "minimal.gdpr";
	
	private ResourceSet rs = new ResourceSetImpl();
	private EqualityHelper eh = new EqualityHelper();
	
	private Map<Pin, Pin> pinCompareMap;
	private Map<Behaviour, Behaviour> behaviourCompareMap;
	
	@BeforeAll
	public static void setUpBeforeAll() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("dataflowdiagram", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("datadictionary", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("tracemodel", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("gdpr", new XMIResourceFactoryImpl());
	}
	
	@BeforeEach
	public void cleanUp() {
		pinCompareMap = new HashMap<>();
		behaviourCompareMap = new HashMap<>();
	}
	
	@Test
	public void testGDPR2DFDForFirstIteration() {
		String gdprFile = Paths.get("models", "Minimal.gdpr").toString();
		String dfdFile = Paths.get("models", "MinimalResult.dataflowdiagram").toString();
		String ddFile = Paths.get("models", "MinimalResult.datadictionary").toString();
		
		GDPR2DFD trans = new GDPR2DFD(gdprFile, dfdFile, ddFile);
		trans.transform();
		
		Resource dfdResultResource = rs.getResource(URI.createFileURI(dfdFile), true);
		Resource ddResultResource = rs.getResource(URI.createFileURI(ddFile), true);
		Resource dfdGoldStandardResource = rs.getResource(URI.createFileURI(Paths.get("models","GoldStandards", "MinimalGoldStandard.dataflowdiagram").toString()), true);
		Resource ddGoldStandardResource = rs.getResource(URI.createFileURI(Paths.get("models","GoldStandards", "MinimalGoldStandard.datadictionary").toString()), true);
		
		DataDictionary ddResult = (DataDictionary) ddResultResource.getContents().get(0);
		DataFlowDiagram dfdResult = (DataFlowDiagram) dfdResultResource.getContents().get(0);	
		DataDictionary ddGoldStandard = (DataDictionary) ddGoldStandardResource.getContents().get(0);
		DataFlowDiagram dfdGoldStandard = (DataFlowDiagram) dfdGoldStandardResource.getContents().get(0);	
		
		assert(equals(dfdResult, dfdGoldStandard));
		assert(equals(ddResult, ddGoldStandard));
		
		File fileDFDResult = new File(Paths.get("models", "MinimalResult.dataflowdiagram").toString());
		File fileDDResult = new File(Paths.get("models", "MinimalResult.datadictionary").toString());
		
		fileDFDResult.delete();
		fileDDResult.delete();		
	}
	
	
	
	@Test
	public void testDFD2GDPR() {
		String gdprFile = Paths.get("models", "MinimalResult.gdpr").toString();
		String dfdFile = Paths.get("models", "Minimal.dataflowdiagram").toString();
		String ddFile = Paths.get("models", "Minimal.datadictionary").toString();
		String tmFile = Paths.get("models", "MinimalResult.tracemodel").toString();
		
		DFD2GDPR trans = new DFD2GDPR(dfdFile, ddFile, gdprFile, tmFile);
		trans.transform();
		
		Resource gdprResultResource = rs.getResource(URI.createFileURI(gdprFile), true);
		Resource gdprGoldStandardResource = rs.getResource(URI.createFileURI(Paths.get("models", "GoldStandards", "MinimalGoldStandard.gdpr").toString()), true);
		
		LegalAssessmentFacts lafResult = (LegalAssessmentFacts) gdprResultResource.getContents().get(0);
		LegalAssessmentFacts lafGoldStandard = (LegalAssessmentFacts) gdprGoldStandardResource.getContents().get(0);
		
		assert(equals(lafResult, lafGoldStandard));
		
		File fileDfd = new File(dfdFile);
		File fileDd = new File(dfdFile);
		File gdpr = new File(gdprFile);
		File tm = new File(tmFile);
		
		fileDfd.delete();
		fileDd.delete();
		gdpr.delete();
		tm.delete();
		
		GDPR2DFD trans2 = new GDPR2DFD(Paths.get("models", "Minimal.gdpr").toString(), dfdFile, ddFile);
		trans2.transform();
	}
				
	
	 private boolean equals(EObject obj1, EObject obj2) {
		    if (obj1 == null || obj2 == null) {
		    	return false;
		    }
		    
		    if (!obj1.eClass().equals(obj2.eClass())) {
			   return false;
		    }
		    
		    if (obj1 instanceof DataDictionary) return true;
		    
		    if (obj1 instanceof Pin) {
		    	if (pinCompareMap.containsKey(obj1)) {
		    		if (!pinCompareMap.getOrDefault(obj1, null).equals(obj2)) return false;		    		
		    	} else pinCompareMap.put((Pin) obj1,(Pin) obj2);
		    } else if (obj1 instanceof Behaviour) {		    
	    		if (behaviourCompareMap.containsKey(obj1)) {
		    		if (!behaviourCompareMap.getOrDefault(obj1, null).equals(obj2)) return false;		    		
		    	} else behaviourCompareMap.put((Behaviour) obj1,(Behaviour) obj2);		    	
		    } else if (obj1 instanceof Label) {
		    	return ((Label)obj1).getEntityName().equals(((Label)obj2).getEntityName());
		    }		    
		    else if (!(obj1 instanceof Flow)){
		    // Compare attributes
			    for (EAttribute attribute : obj1.eClass().getEAllAttributes()) {
			      Object value1 = obj1.eGet(attribute);
			      Object value2 = obj2.eGet(attribute);
			      if (value1 == null) {
			    	  if (value2 != null) return false;
			      } else if (value2 == null) {
			    	  if (value1 != null) return false;
			      }
			      else if (!value1.equals(value2)) {
			    	  System.out.println();
			    	return false;
			      }
			    }
		    }
		    
		    // Compare references
		    for (EReference reference : obj1.eClass().getEAllReferences()) {
		      if (!compareReferences(obj1, obj2, reference)) {
		    	  return false;
		      }
		    }
		    
		    return true;
		  }
	 
	 private boolean compareReferences(EObject obj1, EObject obj2, EReference reference) {
		    Object values1Object =  obj1.eGet(reference);
		    Object values2Object =  obj2.eGet(reference);
		    
		    if (values1Object instanceof EObject) return equals((EObject)values1Object, (EObject)values2Object);
		    
		    List<EObject> values1 = (List<EObject>) values1Object;
		    List<EObject> values2 = (List<EObject>) values2Object;
		   
		    if (values1 == null && values2 == null) return true;
		    
		    if (values1.size() != values2.size()) {
		    	return false;
		    }
		    
		    
		    for (var value1 : values1) {
		    	if (values2.stream().noneMatch(v2 -> equals(v2, value1))) {
		    		System.out.println();
		    		return false;
		    	}
		    		
		    }
		    
		    return true;
		  }
}
