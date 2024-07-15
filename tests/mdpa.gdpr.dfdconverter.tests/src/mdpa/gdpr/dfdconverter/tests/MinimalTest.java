package mdpa.gdpr.dfdconverter.tests;

import mdpa.gdpr.dfdconverter.GDPR2DFD;
import mdpa.gdpr.metamodel.GDPR.LegalAssessmentFacts;
import mdpa.gdpr.dfdconverter.DFD2GDPR;

import java.nio.file.Paths;

import java.util.List;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MinimalTest {
	
	private ResourceSet rs = new ResourceSetImpl();
	
	
	/**
	 * Register relevant factories
	 */
	@BeforeAll
	public static void setUpBeforeAll() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("dataflowdiagram", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("datadictionary", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("tracemodel", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("gdpr", new XMIResourceFactoryImpl());
	}
	
		
	/**
	 * Transforms an example GDPR model instance into a DFD one and test whether all information is restored on the transformation back
	 */
	@Test
	public void testRoundTrip() {
		String gdprFileString = Paths.get("models", "Minimal.gdpr").toString();
		
		GDPR2DFD transformation = new GDPR2DFD(gdprFileString);
		transformation.transform();
		
		DFD2GDPR transformationBack = new DFD2GDPR(transformation.getDataFlowDiagram(), transformation.getDataDictionary(),	transformation.getGDPR2DFDTraceModel());
		transformationBack.transform();
		
		Resource gdprResource = rs.getResource(URI.createFileURI(gdprFileString), true);
		LegalAssessmentFacts laf = (LegalAssessmentFacts) gdprResource.getContents().get(0);
		
		equals(laf, transformationBack.getLegalAssessmentFacts());
		
	}
	
	
	
	
	/**
	 * Compares two EObjects on functional Equality
	 * @param obj1
	 * @param obj2
	 * @return Whether the objects are functionally equal
	 */
	 private boolean equals(EObject obj1, EObject obj2) {
		    if (obj1 == null || obj2 == null) {
		    	return false;
		    }
		    
		    if (!obj1.eClass().equals(obj2.eClass())) {
			   return false;//Flows and DD are only compared on References not attributes
		    }
			   
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
		    
		    
		    // Compare references
		    for (EReference reference : obj1.eClass().getEAllReferences()) {
		      if (!compareReferences(obj1, obj2, reference)) {
		    	  return false;
		      }
		    }
		    
		    return true;
		  }
	 
	 /**
		 * Compares the reference of two EObjects on equality
		 * @param obj1
		 * @param obj2
		 * @param reference To be compared for
		 * @return Whether the values for the named reference are equal for both EObjects
		 */
	 @SuppressWarnings("unchecked")
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
