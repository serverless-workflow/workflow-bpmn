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

package org.serverless.workflow.bpmn;

import org.apache.commons.io.IOUtils;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.Task;
import org.junit.jupiter.api.Test;
import org.serverless.workflow.bpmn.impl.BpmnParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BpmnParserTest extends BaseParserTest {

    @Test
    public void testGetDefaultProcess() throws Exception {
        BpmnParser parser = new BpmnParser("{}");

        String defaultProcess = parser.toBpmn2String();
        assertNotNull(defaultProcess);

        Definitions def = parser.toBpmn2Definitions();
        assertNotNull(def);

        assertNotNull(def.getRootElements().get(0));
        assertTrue(def.getRootElements().get(0) instanceof Process);
        assertEquals("defaultProcess",
                     def.getRootElements().get(0).getId());
    }

    @Test
    public void testSingleEventStateWithSingleTrigger() throws Exception {
        String workflowString = IOUtils
                .toString(this.getClass().getResourceAsStream("/basic/eventstatewithtrigger.json"),
                          "UTF-8");

        BpmnParser parser = new BpmnParser(workflowString);
        assertTrue(parser.getWorkflowManager().getWorkflowValidator().validate().size() < 1);

        Definitions def = parser.toBpmn2Definitions();
        assertNotNull(def.getRootElements().get(0));
        assertTrue(def.getRootElements().get(0) instanceof Process);

        Process process = (Process) def.getRootElements().get(0);
        assertEquals("testprocess",
                     process.getId());
        assertEquals("testprocessname",
                     process.getName());
        assertTrue(process.isIsExecutable());
        assertFalse(process.isIsClosed());

        assertNotNull(process.getProperties());
        assertEquals(1,
                     process.getProperties().size());
        Property processVar = process.getProperties().get(0);
        assertEquals("testtrigger",
                     processVar.getName());
        assertEquals("String",
                     processVar.getItemSubjectRef().getStructureRef());

        assertEquals("com.test.process",
                     getExtensionValueFor(process,
                                          "packageName"));
        assertEquals("1.0",
                     getExtensionValueFor(process,
                                          "version"));

        assertNotNull(process.getFlowElements());
        assertEquals(5,
                     process.getFlowElements().size());
        assertTrue(process.getFlowElements().get(0) instanceof StartEvent);
        assertTrue(process.getFlowElements().get(1) instanceof Task);
        assertTrue(process.getFlowElements().get(2) instanceof EndEvent);
        assertTrue(process.getFlowElements().get(3) instanceof SequenceFlow);
        assertTrue(process.getFlowElements().get(4) instanceof SequenceFlow);

        StartEvent startEvent = (StartEvent) process.getFlowElements().get(0);
        assertEquals("testtrigger",
                     startEvent.getName());
        assertNotNull(startEvent.getEventDefinitions());
        assertEquals(1,
                     startEvent.getEventDefinitions().size());
        assertTrue(startEvent.getEventDefinitions().get(0) instanceof MessageEventDefinition);

        Task restWorkItem = (Task) process.getFlowElements().get(1);
        assertNotNull(restWorkItem);
        assertEquals("testtrigger-Rest",
                     restWorkItem.getName());
        assertEquals("Rest",
                     getExtensionValueFor(restWorkItem,
                                          "taskName"));
    }

    @Test
    public void testMultipleEventStatesWithMultipleTriggers() throws Exception {
        String workflowString = IOUtils
                .toString(this.getClass().getResourceAsStream("/basic/multipleeventstateswithtriggers.json"),
                          "UTF-8");

        BpmnParser parser = new BpmnParser(workflowString);

        assertTrue(parser.getWorkflowManager().getWorkflowValidator().validate().size() < 1);

        Definitions def = parser.toBpmn2Definitions();
        assertNotNull(def.getRootElements().get(0));
        assertTrue(def.getRootElements().get(0) instanceof Process);

        Process process = (Process) def.getRootElements().get(0);
        assertEquals("testprocess",
                     process.getId());
        assertEquals("testprocessname",
                     process.getName());
        assertTrue(process.isIsExecutable());
        assertFalse(process.isIsClosed());

        assertNotNull(process.getProperties());
        assertEquals(2,
                     process.getProperties().size());
        assertEquals("testtrigger",
                     process.getProperties().get(0).getName());
        assertEquals("String",
                     process.getProperties().get(0).getItemSubjectRef().getStructureRef());

        assertEquals("testtrigger2",
                     process.getProperties().get(1).getName());
        assertEquals("String",
                     process.getProperties().get(1).getItemSubjectRef().getStructureRef());

        assertEquals("com.test.process",
                     getExtensionValueFor(process,
                                          "packageName"));
        assertEquals("1.0",
                     getExtensionValueFor(process,
                                          "version"));

        assertNotNull(process.getFlowElements());
        assertEquals(10,
                     process.getFlowElements().size());
        assertTrue(process.getFlowElements().get(0) instanceof StartEvent);
        assertTrue(process.getFlowElements().get(1) instanceof StartEvent);
        assertTrue(process.getFlowElements().get(2) instanceof Task);
        assertTrue(process.getFlowElements().get(3) instanceof Task);
        assertTrue(process.getFlowElements().get(4) instanceof EndEvent);
        assertTrue(process.getFlowElements().get(5) instanceof EndEvent);
        assertTrue(process.getFlowElements().get(6) instanceof SequenceFlow);
        assertTrue(process.getFlowElements().get(7) instanceof SequenceFlow);
        assertTrue(process.getFlowElements().get(8) instanceof SequenceFlow);
        assertTrue(process.getFlowElements().get(9) instanceof SequenceFlow);

        StartEvent startEvent1 = (StartEvent) process.getFlowElements().get(0);
        assertNotNull(startEvent1.getEventDefinitions());
        assertEquals("testtrigger",
                     startEvent1.getName());
        assertEquals(1,
                     startEvent1.getEventDefinitions().size());
        assertTrue(startEvent1.getEventDefinitions().get(0) instanceof MessageEventDefinition);

        StartEvent startEvent2 = (StartEvent) process.getFlowElements().get(1);
        assertNotNull(startEvent2.getEventDefinitions());
        assertEquals("testtrigger2",
                     startEvent2.getName());
        assertEquals(1,
                     startEvent2.getEventDefinitions().size());
        assertTrue(startEvent2.getEventDefinitions().get(0) instanceof MessageEventDefinition);

        Task restWorkItem1 = (Task) process.getFlowElements().get(2);
        assertNotNull(restWorkItem1);
        assertEquals("testtrigger-Rest",
                     restWorkItem1.getName());
        assertEquals("Rest",
                     getExtensionValueFor(restWorkItem1,
                                          "taskName"));

        Task restWorkItem2 = (Task) process.getFlowElements().get(3);
        assertNotNull(restWorkItem2);
        assertEquals("testtrigger2-Rest",
                     restWorkItem2.getName());
        assertEquals("Rest",
                     getExtensionValueFor(restWorkItem2,
                                          "taskName"));
    }
}
