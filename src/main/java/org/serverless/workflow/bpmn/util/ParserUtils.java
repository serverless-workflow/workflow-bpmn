/*
 *
 *   Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.serverless.workflow.bpmn.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.bpmn2.Assignment;
import org.eclipse.bpmn2.Auditing;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.Documentation;
import org.eclipse.bpmn2.ExtensionAttributeValue;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.FormalExpression;
import org.eclipse.bpmn2.InputOutputSpecification;
import org.eclipse.bpmn2.InputSet;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.OutputSet;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.ProcessType;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNPlane;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.di.BpmnDiFactory;
import org.eclipse.dd.dc.Bounds;
import org.eclipse.dd.dc.DcFactory;
import org.eclipse.dd.di.DiagramElement;
import org.eclipse.dd.di.Plane;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EAttributeImpl;
import org.eclipse.emf.ecore.impl.EStructuralFeatureImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.jboss.drools.DroolsFactory;
import org.jboss.drools.DroolsPackage;
import org.jboss.drools.MetaDataType;
import org.jboss.drools.impl.DroolsFactoryImpl;
import org.serverless.workflow.api.events.TriggerEvent;
import org.serverless.workflow.api.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;

public class ParserUtils {

    private static final String exporterName = "serverless-workflow";
    private static final String exporterVersion = "1.0";

    private static final Logger logger = LoggerFactory.getLogger(ParserUtils.class);

    public static WorkflowBpmn2ResourceImpl createNewResource() {
        DroolsFactoryImpl.init();
        DroolsFactoryImpl.init();
        Resource resource = new WorkflowBpmn2ResourceFactoryImpl().createResource(URI.createURI("virtual.bpmn2"));
        Bpmn2Factory factory = Bpmn2Factory.eINSTANCE;
        Definitions definitions = factory.createDefinitions();
        definitions.setExporter(exporterName);
        definitions.setExporterVersion(exporterVersion);
        DocumentRoot docummentRoot = factory.createDocumentRoot();
        docummentRoot.setDefinitions(definitions);
        resource.getContents().add(docummentRoot);
        return (WorkflowBpmn2ResourceImpl) resource;
    }

    public static void applyDefinitionProperties(Definitions def,
                                                 Map<String, String> properties) {
        def.setTypeLanguage(properties.get("typelanguage"));
        def.setTargetNamespace(properties.get("targetnamespace"));
        def.setExpressionLanguage(properties.get("expressionlanguage"));

        ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;
        EAttributeImpl extensionAttribute = (EAttributeImpl) metadata.demandFeature(
                "xsi",
                "schemaLocation",
                false,
                false);
        EStructuralFeatureImpl.SimpleFeatureMapEntry extensionEntry =
                new EStructuralFeatureImpl.SimpleFeatureMapEntry(extensionAttribute,
                                                                 "http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd http://www.jboss.org/drools drools.xsd");
        def.getAnyAttribute().add(extensionEntry);
    }

    public static void applyProcessProperties(Process process,
                                              Map<String, String> properties) {
        process.setId(properties.get("id"));

        // unless defined  make process adHoc
        // Note : ad-hoc processes not supported in kogito yet
//        if (properties.get("adhoc") != null) {
//            ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;
//            EAttributeImpl extensionAttribute = (EAttributeImpl) metadata.demandFeature(
//                    "http://www.jboss.org/drools",
//                    "adHoc",
//                    false,
//                    false);
//            EStructuralFeatureImpl.SimpleFeatureMapEntry extensionEntry = new EStructuralFeatureImpl.SimpleFeatureMapEntry(extensionAttribute,
//                                                                                                                           properties.get("adhocprocess"));
//            process.getAnyAttribute().add(extensionEntry);
//        } else {
//            ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;
//            EAttributeImpl extensionAttribute = (EAttributeImpl) metadata.demandFeature(
//                    "http://www.jboss.org/drools",
//                    "adHoc",
//                    false,
//                    false);
//            EStructuralFeatureImpl.SimpleFeatureMapEntry extensionEntry = new EStructuralFeatureImpl.SimpleFeatureMapEntry(extensionAttribute,
//                                                                                                                           "true");
//            process.getAnyAttribute().add(extensionEntry);
//        }

        if (properties.get("processn") != null) {
            process.setName(escapeXml(properties.get("processn")));
        } else {
            process.setName("");
        }
        if (properties.get("auditing") != null && !"".equals(properties.get("auditing"))) {
            Auditing audit = Bpmn2Factory.eINSTANCE.createAuditing();
            audit.getDocumentation().add(createDocumentation(properties.get("auditing")));
            process.setAuditing(audit);
        }
        process.setProcessType(ProcessType.getByName(properties.get("processtype")));
        process.setIsClosed(Boolean.parseBoolean(properties.get("isclosed")));
        process.setIsExecutable(Boolean.parseBoolean(properties.get("executable")));
        // get the drools-specific extension packageName attribute to Process if defined
        if (properties.get("package") != null && properties.get("package").length() > 0) {
            addDroolsExtensionToBaseElement(process,
                                            "packageName",
                                            properties.get("package"));
        }

        // add version attrbute to process
        if (properties.get("version") != null && properties.get("version").length() > 0) {
            addDroolsExtensionToBaseElement(process,
                                            "version",
                                            properties.get("version"));
        }
    }

    public static Documentation createDocumentation(String text) {
        Documentation doc = Bpmn2Factory.eINSTANCE.createDocumentation();
        doc.setText(text);
        return doc;
    }

    public static void addStartMessageEvent(TriggerEvent triggerEvent,
                                            Definitions definitions,
                                            Process process,
                                            int eventCounter) {

        // first create the item definition
        ItemDefinition itemDefinition = Bpmn2Factory.eINSTANCE.createItemDefinition();
        itemDefinition.setIsCollection(false);
        itemDefinition.setStructureRef("String");
        definitions.getRootElements().add(itemDefinition);
        // then the message
        Message message = Bpmn2Factory.eINSTANCE.createMessage();
        message.setId(triggerEvent.getEventID());
        message.setItemRef(itemDefinition);
        message.setName(triggerEvent.getMessage());
        definitions.getRootElements().add(message);
        // add the process property
        Property property = Bpmn2Factory.eINSTANCE.createProperty();
        property.setId(triggerEvent.getName());
        property.setItemSubjectRef(itemDefinition);
        property.setName(triggerEvent.getName());
        process.getProperties().add(property);
        // now the actual start event
        StartEvent startEvent = (StartEvent) Bpmn20Stencil.createElement("StartMessageEvent",
                                                                         null,
                                                                         false);
        startEvent.setName(triggerEvent.getName());
        // add the data mapping for start event
        DataOutput dataOutput = Bpmn2Factory.eINSTANCE.createDataOutput();
        dataOutput.setName("event");
        dataOutput.setItemSubjectRef(itemDefinition);
        addDroolsExtensionToBaseElement(dataOutput,
                                        "dtype",
                                        "String");

        startEvent.getDataOutputs().add(dataOutput);
        DataOutputAssociation dataOutputAssociation = Bpmn2Factory.eINSTANCE.createDataOutputAssociation();
        dataOutputAssociation.getSourceRef().add(dataOutput);
        dataOutputAssociation.setTargetRef(property);
        startEvent.getDataOutputAssociation().add(dataOutputAssociation);
        // add the data output
        OutputSet outputSet = Bpmn2Factory.eINSTANCE.createOutputSet();
        outputSet.getDataOutputRefs().add(dataOutput);
        startEvent.setOutputSet(outputSet);
        // last the message event definition
        MessageEventDefinition messageEventDefinition = (MessageEventDefinition) startEvent.getEventDefinitions().get(0);
        messageEventDefinition.setMessageRef(message);

        // add the start even to process
        process.getFlowElements().add(startEvent);

        // add bpmndi info for the start event
        Plane plane = definitions.getDiagrams().get(0).getPlane();
        BPMNShape bpmnShape = BpmnDiFactory.eINSTANCE.createBPMNShape();
        bpmnShape.setBpmnElement(startEvent);
        Bounds bounds = DcFactory.eINSTANCE.createBounds();
        bounds.setWidth(36);
        bounds.setHeight(36);
        bounds.setX(120);
        bounds.setY(100 + (eventCounter * 120));
        bpmnShape.setBounds(bounds);
        plane.getPlaneElement().add(bpmnShape);
    }

    public static void generateWorkitems(List<Function> functions,
                                         String triggerName,
                                         Definitions definitions,
                                         Process process,
                                         int triggerCounter) {

        // find the start event for which we are bounding the new workitem(s) to
        StartEvent workingStartEvent = null;
        for (FlowElement flowElement : process.getFlowElements()) {
            if (flowElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) flowElement;
                if (startEvent.getEventDefinitions() != null && startEvent.getEventDefinitions().get(0) instanceof MessageEventDefinition) {
                    if (startEvent.getName().equals(triggerName)) {
                        // found our start event...
                        workingStartEvent = startEvent;
                    }
                }
            }
        }

        if (workingStartEvent != null) {
            // create rest workitem from function (TODO: currently we just grab first function...will implement multiple next version!)
            Task restWorkitem = Bpmn2Factory.eINSTANCE.createTask();
            restWorkitem.setId(triggerName + "RestWorkitem");
            process.getFlowElements().add(restWorkitem);

            setRestWorkitemInfo(restWorkitem,
                                functions.get(0),
                                triggerName,
                                definitions,
                                process,
                                triggerCounter);
            // add the start even to process
            // conect start event to workitem
            //connectNodes(workingStartEvent, workitemForFunction, process);
            // create end event
            // connect end event to workitem
            //connectNodes(workitemForFunction, endEvent, process);

        } else {
            logger.error("Unable to find message start event for trigger name: " + triggerName);
        }
    }

    public static void setRestWorkitemInfo(Task task,
                                           Function function,
                                           String triggerName,
                                           Definitions definitions,
                                           Process process,
                                           int triggerCounter) {
        // add selectable to task
        addDroolsExtensionToBaseElement(task,
                                        "selectable",
                                        "true");
        // add taskName to task
        addDroolsExtensionToBaseElement(task,
                                        "taskName",
                                        function.getName());
        task.setName(function.getName());
        // create item definitions for taskName
        ItemDefinition taskNameItemDefinition = Bpmn2Factory.eINSTANCE.createItemDefinition();
        taskNameItemDefinition.setStructureRef("String");
        taskNameItemDefinition.setId(task.getId() + "_TaskNameInput");
        definitions.getRootElements().add(taskNameItemDefinition);

        Map<String, ItemDefinition> metadataInputDefinitionsMap = new HashMap<>();
        // create item definitions for each of the metadata entries of the function
        function.getMetadata().entrySet().stream().forEach(entry -> {
            ItemDefinition itemDefinition = Bpmn2Factory.eINSTANCE.createItemDefinition();
            itemDefinition.setStructureRef("String");
            itemDefinition.setId(task.getId() + "_" + entry.getKey() + "Input");
            metadataInputDefinitionsMap.put(entry.getKey(),
                                            itemDefinition);
            definitions.getRootElements().add(itemDefinition);
        });
        // and one for "Result"
        ItemDefinition resultItemDefinition = Bpmn2Factory.eINSTANCE.createItemDefinition();
        resultItemDefinition.setStructureRef("String");
        resultItemDefinition.setId(task.getId() + "_" + "ResultOutput");
        metadataInputDefinitionsMap.put("Result",
                                        resultItemDefinition);
        definitions.getRootElements().add(resultItemDefinition);
        // and one for "ContentData"
        ItemDefinition contentDataItemDefinition = Bpmn2Factory.eINSTANCE.createItemDefinition();
        contentDataItemDefinition.setStructureRef("java.lang.Object");
        contentDataItemDefinition.setId(task.getId() + "_" + "ContentDataInput");
        metadataInputDefinitionsMap.put("ContentData",
                                        contentDataItemDefinition);
        definitions.getRootElements().add(contentDataItemDefinition);

        InputOutputSpecification ioSpec = Bpmn2Factory.eINSTANCE.createInputOutputSpecification();
        InputSet ioSpecInputSet = Bpmn2Factory.eINSTANCE.createInputSet();
        // add data inputs to io specification of task
        DataInput taskNameDataInput = Bpmn2Factory.eINSTANCE.createDataInput();
        taskNameDataInput.setId(task.getId() + "_" + "TaskName");
        addDroolsExtensionToBaseElement(taskNameDataInput,
                                        "dtype",
                                        "String");
        taskNameDataInput.setName("TaskName");
        taskNameDataInput.setItemSubjectRef(taskNameItemDefinition);
        ioSpec.getDataInputs().add(taskNameDataInput);
        ioSpecInputSet.getDataInputRefs().add(taskNameDataInput);
        // content data now
        DataInput contentDataDataInput = Bpmn2Factory.eINSTANCE.createDataInput();
        contentDataDataInput.setId(task.getId() + "_" + "ContentData");
        addDroolsExtensionToBaseElement(contentDataDataInput,
                                        "dtype",
                                        "String");
        contentDataDataInput.setName("ContentData");
        contentDataDataInput.setItemSubjectRef(contentDataItemDefinition);
        ioSpec.getDataInputs().add(contentDataDataInput);
        ioSpecInputSet.getDataInputRefs().add(contentDataDataInput);

        Map<String, DataInput> metadataDataInputsMap = new HashMap<>();
        function.getMetadata().entrySet().stream().forEach(entry -> {
            DataInput dataInput = Bpmn2Factory.eINSTANCE.createDataInput();
            dataInput.setId(task.getId() + "_" + entry.getKey());
            addDroolsExtensionToBaseElement(dataInput,
                                            "dtype",
                                            "String");
            dataInput.setName(entry.getKey());
            dataInput.setItemSubjectRef(metadataInputDefinitionsMap.get(entry.getKey()));
            ioSpec.getDataInputs().add(dataInput);
            ioSpecInputSet.getDataInputRefs().add(dataInput);
            metadataDataInputsMap.put(entry.getKey(),
                                      dataInput);
        });
        ioSpec.getInputSets().add(ioSpecInputSet);

        // now data outputs
        DataOutput resultDataOutput = Bpmn2Factory.eINSTANCE.createDataOutput();
        resultDataOutput.setId(task.getId() + "_Result");
        resultDataOutput.setName("Result");
        addDroolsExtensionToBaseElement(resultDataOutput,
                                        "dtype",
                                        "java.lang.Object");
        resultDataOutput.setItemSubjectRef(metadataInputDefinitionsMap.get("Result"));
        ioSpec.getDataOutputs().add(resultDataOutput);

        OutputSet resultOutputSet = Bpmn2Factory.eINSTANCE.createOutputSet();
        resultOutputSet.getDataOutputRefs().add(resultDataOutput);
        ioSpec.getOutputSets().add(resultOutputSet);

        task.setIoSpecification(ioSpec);

        // now data input associations
        // first TaskName
        DataInputAssociation taskNameInputAssociation = Bpmn2Factory.eINSTANCE.createDataInputAssociation();
        taskNameInputAssociation.setTargetRef(taskNameDataInput);
        Assignment taskNameAssignment = Bpmn2Factory.eINSTANCE.createAssignment();
        FormalExpression taskNameFromExpression = Bpmn2Factory.eINSTANCE.createFormalExpression();
        taskNameFromExpression.setBody(function.getName());
        taskNameAssignment.setFrom(taskNameFromExpression);
        FormalExpression taskNameToExpression = Bpmn2Factory.eINSTANCE.createFormalExpression();
        taskNameToExpression.setBody(taskNameDataInput.getId());
        taskNameAssignment.setTo(taskNameToExpression);
        taskNameInputAssociation.getAssignment().add(taskNameAssignment);
        task.getDataInputAssociations().add(taskNameInputAssociation);
        // second contentData
        DataInputAssociation contentDataInputAssociation = Bpmn2Factory.eINSTANCE.createDataInputAssociation();
        contentDataInputAssociation.setTargetRef(contentDataDataInput);
        contentDataInputAssociation.getSourceRef().add(getProcessPropertyFor(triggerName,
                                                                             process));
        task.getDataInputAssociations().add(contentDataInputAssociation);

        // then others in metadata
        function.getMetadata().entrySet().stream().forEach(entry -> {
            DataInputAssociation newInputAssociation = Bpmn2Factory.eINSTANCE.createDataInputAssociation();
            newInputAssociation.setTargetRef(metadataDataInputsMap.get(entry.getKey()));
            Assignment newAssignment = Bpmn2Factory.eINSTANCE.createAssignment();
            FormalExpression newFromExpression = Bpmn2Factory.eINSTANCE.createFormalExpression();
            newFromExpression.setBody(entry.getValue());
            newAssignment.setFrom(newFromExpression);
            FormalExpression newToExpression = Bpmn2Factory.eINSTANCE.createFormalExpression();
            newToExpression.setBody(metadataDataInputsMap.get(entry.getKey()).getId());
            newAssignment.setTo(newToExpression);
            newInputAssociation.getAssignment().add(newAssignment);
            task.getDataInputAssociations().add(newInputAssociation);
        });

        // now data output associations
        DataOutputAssociation resultDataOutputAssociation = Bpmn2Factory.eINSTANCE.createDataOutputAssociation();
        resultDataOutputAssociation.getSourceRef().add(resultDataOutput);
        resultDataOutputAssociation.setTargetRef(getProcessPropertyFor(triggerName,
                                                                       process));
        task.getDataOutputAssociations().add(resultDataOutputAssociation);

        // finally .... bpmndi info for the task
        // add bpmndi info for the start event
        Plane plane = definitions.getDiagrams().get(0).getPlane();
        BPMNShape bpmnShape = BpmnDiFactory.eINSTANCE.createBPMNShape();
        bpmnShape.setBpmnElement(task);
        Bounds bounds = DcFactory.eINSTANCE.createBounds();
        bounds.setWidth(100);
        bounds.setHeight(80);
        bounds.setX(100 + 200 + (triggerCounter * 200));
        bounds.setY(80 + (triggerCounter * 80));
        bpmnShape.setBounds(bounds);
        plane.getPlaneElement().add(bpmnShape);
    }

    public static Property getProcessPropertyFor(String triggerName,
                                                 Process process) {
        for (Property p : process.getProperties()) {
            if (p.getName().equals(triggerName)) {
                return p;
            }
        }

        return null;
    }

    private static void addDroolsExtensionToBaseElement(BaseElement baseElement,
                                                        String metaName,
                                                        String value) {
        ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;
        EAttributeImpl extensionAttribute = (EAttributeImpl) metadata.demandFeature(
                "http://www.jboss.org/drools",
                metaName,
                false,
                false);
        EStructuralFeatureImpl.SimpleFeatureMapEntry extensionEntry = new EStructuralFeatureImpl.SimpleFeatureMapEntry(extensionAttribute,

                                                                                                                       value);
        baseElement.getAnyAttribute().add(extensionEntry);
    }

    private String toBPMNIdentifier(String str) {

        str = str.replaceAll("\\s+",
                             "");
        StringBuilder sb = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (i == 0) {
                if (isNCNameStart(c)) {
                    sb.append(c);
                } else {
                    sb.append(convertNonNCNameChar(c));
                }
            } else {
                if (isNCNamePart(c)) {
                    sb.append(c);
                } else {
                    sb.append(convertNonNCNameChar(c));
                }
            }
        }
        // return and strip leading digits
        return sb.toString().replaceFirst("^\\d*",
                                          "");
    }

    private boolean isNCNameStart(char c) {
        return (Character.isDigit(c) || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_');
    }

    private boolean isNCNamePart(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || Character.isDigit(c) || c == '-' || c == '_' || c == '.');
    }

    private static String convertNonNCNameChar(char c) {
        String str = "" + c;
        byte[] bytes = str.getBytes();
        StringBuilder sb = new StringBuilder(4);

        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%x",
                                    bytes[i]));
        }
        return sb.toString().toUpperCase();
    }

    private String getMetaDataValue(List<ExtensionAttributeValue> extensionValues,
                                    String metaDataName) {
        if (extensionValues != null && extensionValues.size() > 0) {
            for (ExtensionAttributeValue extattrval : extensionValues) {
                FeatureMap extensionElements = extattrval.getValue();

                List<MetaDataType> metadataExtensions = (List<MetaDataType>) extensionElements
                        .get(DroolsPackage.Literals.DOCUMENT_ROOT__META_DATA,
                             true);

                for (MetaDataType metaType : metadataExtensions) {
                    if (metaType.getName() != null && metaType.getName().equals(metaDataName) && metaType.getMetaValue() != null && metaType.getMetaValue().length() > 0) {
                        return metaType.getMetaValue();
                    }
                }
            }
        }

        return null;
    }

    private void setMetaDataExtensionValue(BaseElement element,
                                           String metaDataName,
                                           String metaDataValue) {
        if (element != null) {
            MetaDataType eleMetadata = DroolsFactory.eINSTANCE.createMetaDataType();
            eleMetadata.setName(metaDataName);
            eleMetadata.setMetaValue(metaDataValue);

            if (element.getExtensionValues() == null || element.getExtensionValues().isEmpty()) {
                ExtensionAttributeValue extensionElement = Bpmn2Factory.eINSTANCE.createExtensionAttributeValue();
                element.getExtensionValues().add(extensionElement);
            }
            FeatureMap.Entry eleExtensionElementEntry = new EStructuralFeatureImpl.SimpleFeatureMapEntry(
                    (EStructuralFeature.Internal) DroolsPackage.Literals.DOCUMENT_ROOT__META_DATA,
                    eleMetadata);
            element.getExtensionValues().get(0).getValue().add(eleExtensionElementEntry);
        }
    }

    private static void orderDiagramElements(Definitions def,
                                             boolean zOrderEnabled) {
        if (zOrderEnabled) {
            if (def.getDiagrams() != null) {
                for (BPMNDiagram diagram : def.getDiagrams()) {
                    if (diagram != null) {
                        BPMNPlane plane = diagram.getPlane();
                        List<DiagramElement> unsortedElements = new ArrayList<DiagramElement>(plane.getPlaneElement());
                        plane.getPlaneElement().clear();
                        Collections.sort(unsortedElements,
                                         new DIZorderComparator());
                        plane.getPlaneElement().addAll(unsortedElements);
                        diagram.setPlane(plane);
                    }
                }
            }
        }
    }
}
