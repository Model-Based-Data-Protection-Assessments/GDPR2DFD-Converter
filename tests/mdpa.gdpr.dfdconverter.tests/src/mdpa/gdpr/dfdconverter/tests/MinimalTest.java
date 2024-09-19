package mdpa.gdpr.dfdconverter.tests;

import mdpa.gdpr.dfdconverter.GDPR2DFD;
import mdpa.gdpr.metamodel.GDPR.LegalAssessmentFacts;
import mdpa.gdpr.dfdconverter.DFD2GDPR;
import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.TraceModel;


import java.nio.file.Paths;

import java.util.List;

import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("dfd", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("dd", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("tracemodel", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("gdpr", new XMIResourceFactoryImpl());
	}
	
	@Test
	public void testDFDStart() {
		String dfdFilePath = Paths.get("models", "DFD", "minimal.dfd").toString();
		String ddFilePath = Paths.get("models", "DFD", "minimal.dd").toString();
		String gdprFilePath = Paths.get("models", "DFD", "minimal.gdpr").toString();
		String traceFilePath = Paths.get("models", "DFD", "minimal.tracemodel").toString();
		
		// First Pass, DFD->GDPR->DFD, check DFD and DD
		DFD2GDPR dfd2gdpr = new DFD2GDPR(dfdFilePath, ddFilePath);
		dfd2gdpr.transform();
		dfd2gdpr.save(gdprFilePath, traceFilePath);

		GDPR2DFD gdpr2dfd = new GDPR2DFD(gdprFilePath, ddFilePath, traceFilePath);
		gdpr2dfd.transform();
		
		Resource dfdResource = rs.getResource(URI.createFileURI(dfdFilePath), true);
		DataFlowDiagram dfd = (DataFlowDiagram) dfdResource.getContents().get(0);
		Resource ddResource = rs.getResource(URI.createFileURI(ddFilePath), true);
		DataDictionary dd = (DataDictionary) ddResource.getContents().get(0);
		
		EcoreUtil.resolveAll(ddResource);
		EcoreUtil.resolveAll(dfdResource);

		equals(dd, gdpr2dfd.getDataDictionary());
		equals(dfd, gdpr2dfd.getDataFlowDiagram());
		
		gdpr2dfd.save(dfdFilePath, ddFilePath, traceFilePath);

		// Second pass, DFD->GDPR, check GDPR and TraceModel
		dfd2gdpr = new DFD2GDPR(dfdFilePath, ddFilePath, traceFilePath);
		dfd2gdpr.transform();
		
		Resource gdprResource = rs.getResource(URI.createFileURI(gdprFilePath), true);
		LegalAssessmentFacts laf = (LegalAssessmentFacts) gdprResource.getContents().get(0);
		Resource tmResource = rs.getResource(URI.createFileURI(traceFilePath), true);
		TraceModel traceModel = (TraceModel) tmResource.getContents().get(0);
		
		EcoreUtil.resolveAll(gdprResource);
		EcoreUtil.resolveAll(tmResource);
		
		equals(laf, dfd2gdpr.getLegalAssessmentFacts());
		equals(traceModel, dfd2gdpr.getDFD2GDPRTrace());

		dfd2gdpr.save(gdprFilePath, traceFilePath);
		
		// Third pass, GDPR->DFD, check DFD, DD and TraceModel
		gdpr2dfd = new GDPR2DFD(gdprFilePath, ddFilePath, traceFilePath);
		gdpr2dfd.transform();
		
		dfdResource = rs.getResource(URI.createFileURI(dfdFilePath), true);
		dfd = (DataFlowDiagram) dfdResource.getContents().get(0);
		ddResource = rs.getResource(URI.createFileURI(ddFilePath), true);
		dd = (DataDictionary) ddResource.getContents().get(0);
		tmResource = rs.getResource(URI.createFileURI(traceFilePath), true);
		traceModel = (TraceModel) tmResource.getContents().get(0);
		
		EcoreUtil.resolveAll(ddResource);
		EcoreUtil.resolveAll(dfdResource);
		EcoreUtil.resolveAll(tmResource);

		equals(dd, gdpr2dfd.getDataDictionary());
		equals(dfd, gdpr2dfd.getDataFlowDiagram());
		equals(traceModel, gdpr2dfd.getGDPR2DFDTrace());

		gdpr2dfd.save(dfdFilePath, ddFilePath, traceFilePath);
	}
	
	@Test
	public void testGDPRStart() {
		String dfdFilePath = Paths.get("models", "GDPR", "minimal.dfd").toString();
		String ddFilePath = Paths.get("models", "GDPR", "minimal.dd").toString();
		String gdprFilePath = Paths.get("models", "GDPR", "minimal.gdpr").toString();
		String traceFilePath = Paths.get("models", "GDPR", "minimal.tracemodel").toString();
		
		// First Pass, GDPR->DFD->GDPR, check GDPR and trace <- trace might be wrong
		GDPR2DFD gdpr2dfd = new GDPR2DFD(gdprFilePath);
		gdpr2dfd.transform();
		gdpr2dfd.save(dfdFilePath, ddFilePath, traceFilePath);
		
		DFD2GDPR dfd2gdpr = new DFD2GDPR(dfdFilePath, ddFilePath, traceFilePath);
		dfd2gdpr.transform();
		
		Resource gdprResource = rs.getResource(URI.createFileURI(gdprFilePath), true);
		LegalAssessmentFacts laf = (LegalAssessmentFacts) gdprResource.getContents().get(0);
		Resource tmResource = rs.getResource(URI.createFileURI(traceFilePath), true);
		TraceModel traceModel = (TraceModel) tmResource.getContents().get(0);
		
		EcoreUtil.resolveAll(gdprResource);
		EcoreUtil.resolveAll(tmResource);
		
		equals(laf, dfd2gdpr.getLegalAssessmentFacts());
		equals(traceModel, dfd2gdpr.getDFD2GDPRTrace());
		
		dfd2gdpr.save(gdprFilePath, traceFilePath);

		// Second pass, GDPR->DFD, with trace and input DD, check DFD, DD and TraceModel
		gdpr2dfd = new GDPR2DFD(gdprFilePath, ddFilePath, traceFilePath);
		gdpr2dfd.transform();
		
		Resource dfdResource = rs.getResource(URI.createFileURI(dfdFilePath), true);
		DataFlowDiagram dfd = (DataFlowDiagram) dfdResource.getContents().get(0);
		Resource ddResource = rs.getResource(URI.createFileURI(ddFilePath), true);
		DataDictionary dd = (DataDictionary) ddResource.getContents().get(0);
		tmResource = rs.getResource(URI.createFileURI(traceFilePath), true);
		traceModel = (TraceModel) tmResource.getContents().get(0);
		
		EcoreUtil.resolveAll(ddResource);
		EcoreUtil.resolveAll(dfdResource);
		EcoreUtil.resolveAll(tmResource);

		equals(dd, gdpr2dfd.getDataDictionary());
		equals(dfd, gdpr2dfd.getDataFlowDiagram());
		equals(traceModel, gdpr2dfd.getGDPR2DFDTrace());

		gdpr2dfd.save(dfdFilePath, ddFilePath, traceFilePath);

		// Third pass, DFD->GDPR, with trace, check GDPR and TraceModel
		dfd2gdpr = new DFD2GDPR(dfdFilePath, ddFilePath, traceFilePath);
		dfd2gdpr.transform();
		
		gdprResource = rs.getResource(URI.createFileURI(gdprFilePath), true);
		laf = (LegalAssessmentFacts) gdprResource.getContents().get(0);
		tmResource = rs.getResource(URI.createFileURI(traceFilePath), true);
		traceModel = (TraceModel) tmResource.getContents().get(0);
		
		EcoreUtil.resolveAll(gdprResource);
		EcoreUtil.resolveAll(tmResource);
		
		equals(laf, dfd2gdpr.getLegalAssessmentFacts());
		equals(traceModel, dfd2gdpr.getDFD2GDPRTrace());

		dfd2gdpr.save(gdprFilePath, traceFilePath);
	}
	
	@Test
	public void missingTests() {
		//Missing Tests:
		// 1. GDPR -> DFD with trace (trace contains previously modified/added assignments as node behavior)
		// 2. DFD -> GDPR -> DFD with or without trace (DFD contains a changed or unrelated assignment, that is kept after transformation)
		// 3. DFD -> GDPR with trace (trace contains correct GDPR LegalBases that can not directly be derived from the DFD)
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
		    
		    if (values1Object instanceof EObject) {
		    	return equals((EObject)values1Object, (EObject)values2Object);
		    }
		    
		    List<EObject> values1 = (List<EObject>) values1Object;
		    List<EObject> values2 = (List<EObject>) values2Object;
		   
		    if (values1 == null && values2 == null) {
		    	return true;
		    }
		    
		    if (values1.size() != values2.size()) {
		    	return false;
		    }
		    
		    for (var value1 : values1) {
		    	if (values2.stream().noneMatch(v2 -> equals(v2, value1))) {
		    		return false;
		    	}
		    }
		    
		    return true;
		  }
}
