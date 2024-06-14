package mdpa.gdpr.dfdconverter;

import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;

import mdpa.gdpr.dfdconverter.tracemodel.tracemodel.TraceModel;

public record DFDAndTracemodel(DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary, TraceModel tracemodel) {

}
