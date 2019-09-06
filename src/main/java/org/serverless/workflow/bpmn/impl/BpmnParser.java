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

package org.serverless.workflow.bpmn.impl;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNPlane;
import org.eclipse.bpmn2.di.BpmnDiFactory;
import org.serverless.workflow.api.Workflow;
import org.serverless.workflow.api.WorkflowController;
import org.serverless.workflow.api.events.TriggerEvent;
import org.serverless.workflow.api.states.EventState;
import org.serverless.workflow.bpmn.util.ParserUtils;
import org.serverless.workflow.bpmn.util.WorkflowBpmn2ResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

public class BpmnParser {

    private static final Logger logger = LoggerFactory.getLogger(BpmnParser.class);
    private static final String exporterName = "serverless-workflow";
    private static final String exporterVersion = "1.0";

    private WorkflowController workflowController;
    private boolean genDefaultOnWorkflowErrors = true;

    private boolean zOrderEnabled = true;
    private List<StartEvent> startMessageEvents = new ArrayList<>();

    public BpmnParser(String workflowJSON) {
        this.workflowController = new WorkflowController().forJson(workflowJSON);
    }

    public BpmnParser(Workflow workflow) {
        this.workflowController = new WorkflowController().forWorkflow(workflow);
    }

    public BpmnParser(WorkflowController workflowController) {
        this.workflowController = workflowController;
    }

    public String toBpmn2String() throws Exception {
        WorkflowBpmn2ResourceImpl resource = parse();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        resource.save(outputStream,
                      new HashMap());

        return unescapeHtml4(outputStream.toString("UTF-8"));
    }

    public Definitions toBpmn2Definitions() {
        WorkflowBpmn2ResourceImpl resource = parse();
        DocumentRoot documentRoot = (DocumentRoot) resource.getContents().get(0);
        return documentRoot.getDefinitions();
    }

    public WorkflowBpmn2ResourceImpl toBpmn2Resource() {
        return parse();
    }

    private WorkflowBpmn2ResourceImpl parse() {
        if (workflowController != null && workflowController.isValid()) {
            return genBpmnResource();
        } else {
            if (genDefaultOnWorkflowErrors) {
                logger.info("Generating default process");
                return genDefaultResource();
            } else {
                logger.info("Can't parse workflow with validation errors: " + workflowController.displayValidationErrors());
                throw new RuntimeException("Can't parse workflow with validation errors: " + workflowController.displayValidationErrors());
            }
        }
    }

    public WorkflowController getWorkflowController() {
        return workflowController;
    }

    public void setGenDefaultOnWorkflowErrors(boolean genDefaultOnWorkflowErrors) {
        this.genDefaultOnWorkflowErrors = genDefaultOnWorkflowErrors;
    }

    private WorkflowBpmn2ResourceImpl genBpmnResource() {
        WorkflowBpmn2ResourceImpl resource = ParserUtils.createNewResource();
        genDefinitions(resource);

        return resource;
    }

    private WorkflowBpmn2ResourceImpl genDefaultResource() {
        WorkflowBpmn2ResourceImpl resource = ParserUtils.createNewResource();
        genDefaultDefinitions(resource);

        return resource;
    }

    private Definitions genDefinitions(WorkflowBpmn2ResourceImpl resource) {
        DocumentRoot documentRoot = (DocumentRoot) resource.getContents().get(0);
        Definitions definitions = documentRoot.getDefinitions();
        definitions.setExporter(exporterName);
        definitions.setExporterVersion(exporterVersion);

        Map<String, String> definitionProps = Stream.of(new String[][]{
                {"typelanguage", "http://www.java.com/javaTypes"},
                {"targetnamespace", "http://www.omg.org/bpmn2"},
                {"expressionlanguage", "http://www.mvel.org/2.0"}
        }).collect(Collectors.toMap(data -> data[0],
                                    data -> data[1]));
        ParserUtils.applyDefinitionProperties(definitions,
                                              definitionProps);

        Workflow workflow = workflowController.getWorkflow();
        Map<String, String> processProps = new HashMap<>();
        processProps.put("id",
                         workflow.getMetadata().get("id") != null ? workflow.getMetadata().get("id") : "defaultProcess");
        processProps.put("processn",
                         workflow.getMetadata().get("processn") != null ? workflow.getMetadata().get("processn") : "Default Process");
        processProps.put("processtype",
                         workflow.getMetadata().get("processtype") != null ? workflow.getMetadata().get("processtype") : "Public");
        processProps.put("isclosed",
                         workflow.getMetadata().get("isclosed") != null ? workflow.getMetadata().get("isclosed") : "false");
        processProps.put("executable",
                         workflow.getMetadata().get("executable") != null ? workflow.getMetadata().get("executable") : "true");
        processProps.put("package",
                         workflow.getMetadata().get("package") != null ? workflow.getMetadata().get("package") : "org.serverless.workflow");
        processProps.put("version",
                         workflow.getMetadata().get("version") != null ? workflow.getMetadata().get("version") : "1.0");

        Process process = Bpmn2Factory.eINSTANCE.createProcess();
        ParserUtils.applyProcessProperties(process,
                                           processProps);

        // generate empty di and add process so we have it
        BpmnDiFactory factory = BpmnDiFactory.eINSTANCE;
        BPMNDiagram diagram = factory.createBPMNDiagram();
        diagram.setResolution(0);
        BPMNPlane plane = factory.createBPMNPlane();
        plane.setBpmnElement(process);
        diagram.setPlane(plane);

        definitions.getDiagrams().add(diagram);
        definitions.getRootElements().add(process);

        // generate message start events
        // trigger events become start message events
        List<TriggerEvent> triggerEventList = workflowController.getAllTriggerEventsAssociatedWithEventStates();
        if (triggerEventList != null && triggerEventList.size() > 0) {
            AtomicInteger starEventCounter = new AtomicInteger();
            triggerEventList.stream().forEach(trigger -> {
                ParserUtils.addStartMessageEvent(trigger,
                                                 definitions,
                                                 process,
                                                 starEventCounter.getAndIncrement());
            });
        } else {
            // no trigger events...cant generate!
            logger.error("No trigger events found. Returning default process!");
            return genDefaultDefinitions(resource);
        }

        // next generate workitem for each of the event functions
        // Note - currently we only support event states.
        // Next versions will support more serverless workflow states
        AtomicInteger triggerEventCounter = new AtomicInteger();
        triggerEventList.stream().forEach(trigger -> {
            List<EventState> eventStatesForTrigger = workflowController.getEventStatesForTriggerEvent(trigger);
            ParserUtils.generateWorkitems(workflowController.getAllFunctionsForEventStates(eventStatesForTrigger),
                                          trigger.getName(),
                                          definitions,
                                          process,
                                          triggerEventCounter.getAndIncrement());
        });

        ParserUtils.generateEndEvents(definitions,
                                      process);
        ParserUtils.connectNodes(definitions,
                                 process);

        return definitions;
    }

    private Definitions genDefaultDefinitions(WorkflowBpmn2ResourceImpl resource) {
        DocumentRoot documentRoot = (DocumentRoot) resource.getContents().get(0);
        Definitions definitions = documentRoot.getDefinitions();
        definitions.setExporter(exporterName);
        definitions.setExporterVersion(exporterVersion);

        Map<String, String> definitionProps = Stream.of(new String[][]{
                {"typelanguage", "http://www.java.com/javaTypes"},
                {"targetnamespace", "http://www.omg.org/bpmn2"},
                {"expressionlanguage", "http://www.mvel.org/2.0"}
        }).collect(Collectors.toMap(data -> data[0],
                                    data -> data[1]));
        ParserUtils.applyDefinitionProperties(definitions,
                                              definitionProps);

        Process process = Bpmn2Factory.eINSTANCE.createProcess();
        Map<String, String> processProps = Stream.of(new String[][]{
                {"id", "defaultProcess"},
                {"processn", "Default Process"},
                {"processtype", "Public"},
                {"isclosed", "false"},
                {"executable", "true"},
                {"package", "org.serverless.workflow"},
                {"version", "1.0"},
        }).collect(Collectors.toMap(data -> data[0],
                                    data -> data[1]));

        ParserUtils.applyProcessProperties(process,
                                           processProps);

        // add empty diagram info
        BpmnDiFactory factory = BpmnDiFactory.eINSTANCE;
        BPMNDiagram diagram = factory.createBPMNDiagram();
        diagram.setResolution(0);
        BPMNPlane plane = factory.createBPMNPlane();
        plane.setBpmnElement(process);
        diagram.setPlane(plane);

        definitions.getDiagrams().add(diagram);

        definitions.getRootElements().add(process);

        return definitions;
    }
}
