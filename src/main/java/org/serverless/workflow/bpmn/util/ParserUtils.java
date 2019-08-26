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
import java.util.List;
import java.util.Map;

import org.eclipse.bpmn2.Auditing;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.Documentation;
import org.eclipse.bpmn2.ExtensionAttributeValue;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.OutputSet;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.ProcessType;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.StartEvent;
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

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;

public class ParserUtils {

    private static final String exporterName = "serverless-workflow";
    private static final String exporterVersion = "1.0";

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
        if (properties.get("adhoc") != null) {
            ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;
            EAttributeImpl extensionAttribute = (EAttributeImpl) metadata.demandFeature(
                    "http://www.jboss.org/drools",
                    "adHoc",
                    false,
                    false);
            EStructuralFeatureImpl.SimpleFeatureMapEntry extensionEntry = new EStructuralFeatureImpl.SimpleFeatureMapEntry(extensionAttribute,
                                                                                                                           properties.get("adhocprocess"));
            process.getAnyAttribute().add(extensionEntry);
        } else {
            ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;
            EAttributeImpl extensionAttribute = (EAttributeImpl) metadata.demandFeature(
                    "http://www.jboss.org/drools",
                    "adHoc",
                    false,
                    false);
            EStructuralFeatureImpl.SimpleFeatureMapEntry extensionEntry = new EStructuralFeatureImpl.SimpleFeatureMapEntry(extensionAttribute,
                                                                                                                           "true");
            process.getAnyAttribute().add(extensionEntry);
        }

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
            ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;
            EAttributeImpl extensionAttribute = (EAttributeImpl) metadata.demandFeature(
                    "http://www.jboss.org/drools",
                    "packageName",
                    false,
                    false);
            EStructuralFeatureImpl.SimpleFeatureMapEntry extensionEntry = new EStructuralFeatureImpl.SimpleFeatureMapEntry(extensionAttribute,
                                                                                                                           properties.get("package"));
            process.getAnyAttribute().add(extensionEntry);
        }

        // add version attrbute to process
        if (properties.get("version") != null && properties.get("version").length() > 0) {
            ExtendedMetaData metadata = ExtendedMetaData.INSTANCE;
            EAttributeImpl extensionAttribute = (EAttributeImpl) metadata.demandFeature(
                    "http://www.jboss.org/drools",
                    "version",
                    false,
                    false);
            EStructuralFeatureImpl.SimpleFeatureMapEntry extensionEntry = new EStructuralFeatureImpl.SimpleFeatureMapEntry(extensionAttribute,
                                                                                                                           properties.get("version"));
            process.getAnyAttribute().add(extensionEntry);
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
        itemDefinition.setStructureRef("java.lang.String");
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
        bounds.setX(120 + (eventCounter * 120));
        bounds.setY(100);
        bpmnShape.setBounds(bounds);
        plane.getPlaneElement().add(bpmnShape);
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
