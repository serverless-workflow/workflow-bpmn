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

import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.junit.jupiter.api.Test;
import org.serverless.workflow.bpmn.impl.BpmnParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BpmnParserTest {

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
    public void testWorkflowMetadata() throws Exception {
        String workflowString = IOUtils
                .toString(this.getClass().getResourceAsStream("/basic/eventstatewithtrigger.json"),
                          "UTF-8");

        BpmnParser parser = new BpmnParser(workflowString);
        assertTrue(parser.getWorkflowController().isValid());

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

        Iterator<FeatureMap.Entry> iter = process.getAnyAttribute().iterator();
        while (iter.hasNext()) {
            FeatureMap.Entry entry = iter.next();
            if (entry.getEStructuralFeature().getName().equals("packageName")) {
                assertEquals("com.test.process",
                             entry.getValue());
            }

            if (entry.getEStructuralFeature().getName().equals("version")) {
                assertEquals("1.0",
                             entry.getValue());
            }
        }

        assertNotNull(process.getFlowElements());
        assertEquals(1, process.getFlowElements().size());
        assertTrue(process.getFlowElements().get(0) instanceof StartEvent);

        StartEvent startEvent = (StartEvent) process.getFlowElements().get(0);
        assertNotNull(startEvent.getEventDefinitions());
        assertEquals(1, startEvent.getEventDefinitions().size());
        assertTrue(startEvent.getEventDefinitions().get(0) instanceof MessageEventDefinition);
    }
}
