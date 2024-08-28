/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.cm.handlers;

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.mediation.cm.exceptions.AttributeNotFoundException
import com.ericsson.oss.mediation.cm.exceptions.NetconfExecutionException
import com.ericsson.oss.mediation.cm.persistence.NetconfPayloadRequestPersistenceService;
import com.ericsson.oss.mediation.util.netconf.api.Datastore
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException
import com.ericsson.oss.mediation.util.netconf.filter.SubTreeFilter

public class MoActionExecutionHandlerSpec extends CdiSpecification {

    private static final String ACTION_CONFIG_1 = "<data>  <ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>NR01gNodeBRadio00001</managedElementId><SystemFunctions><systemFunctionsId>1</systemFunctionsId><Lm xmlns=\"urn:com:ericsson:ecim:RcsLM\"><lmId>1</lmId><EmergencyUnlock><emergencyUnlockId>1</emergencyUnlockId><activate></activate></EmergencyUnlock></Lm></SystemFunctions></ManagedElement> </data>"
    private static final String ACTION_CONFIG_2 = "<data>  <ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>NR01gNodeBRadio00001</managedElementId><SystemFunctions><systemFunctionsId>1</systemFunctionsId><Lm xmlns=\"urn:com:ericsson:ecim:RcsLM\"><lmId>1</lmId><EmergencyUnlock><emergencyUnlockId>1</emergencyUnlockId><activate></activate></EmergencyUnlock></Lm></SystemFunctions></ManagedElement> </data>"

    private static final String MESSAGE_ID_1 = "NO_MESSAGE_ID, RPC Message Sequence: 1"
    private static final String MESSAGE_ID_2 = "NO_MESSAGE_ID, RPC Message Sequence: 2"
    private static final List<String> MESSAGE_IDS_1 = Arrays.asList(MESSAGE_ID_1, MESSAGE_ID_2)
    private static final List<String> ACTION_BODYS = Arrays.asList(ACTION_CONFIG_1, ACTION_CONFIG_2)

    private static final String AP_NODE_FDN = "Project=RadioNodeECTValidSEValidSB,Node=LTE01dg2ERBS00008"
    private static final String NOADE_ADDRESS = "SubNetwork=LTE01dg2ERBS00008,ManagedElement=LTE01dg2ERBS00008"
    private static final String NETCONF_PAYLOAD = "cmNbiNetconfPayload";

    @ObjectUnderTest
    private MoActionExecutionHandler handler

    @MockedImplementation
    private NetconfManager netconfManager

    @Inject
    private NetconfHandlerInputManager handlerInputManager

    private NetconfResponse rpcResponse = new NetconfResponse()

    private Map<String, Object> inputEventHeaders = new HashMap()

    private Map<String, Object> resultMap = new HashMap()

    String returnString

    public void "execute mo action for ECIM without any error"() {
	given: "Init handler"

	initInputEventHeadersWithFullAttributes(inputEventHeaders)
	handlerInputManager.init(inputEventHeaders)
	initRpcResponse(false, "")
	netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
	netconfManager.action("urn:com:ericsson:ecim:1.0", _ as String, "data") >> rpcResponse

	when:
	returnString = handler.executeMoAction(handlerInputManager, netconfManager)

	then:
	notThrown(Exception)
    }

    public void "execute mo action for ECIM with error"() {
	given: "Init handler"

	initInputEventHeadersWithFullAttributes(inputEventHeaders)
	handlerInputManager.init(inputEventHeaders)
	initRpcResponse(true, "")
	netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
	netconfManager.action("urn:com:ericsson:ecim:1.0", _ as String, "data") >> rpcResponse

	when:
	returnString = handler.executeMoAction(handlerInputManager, netconfManager)

	then:
	thrown(NetconfExecutionException)
    }

    public void "execute mo action for Yang without any error"() {
	given: "Init handler"

	initInputEventHeadersWithFullAttributes(inputEventHeaders)
	inputEventHeaders.put("netconfModelType","YANG");
	handlerInputManager.init(inputEventHeaders)
	initRpcResponse(false, "")
	netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
	netconfManager.action("urn:ietf:params:xml:ns:yang:1", _ as String, "result", "return-value", "certificate-signing-request") >> rpcResponse

	when:
	returnString = handler.executeMoAction(handlerInputManager, netconfManager)

	then:
	notThrown(Exception)
    }

    public void "execute mo action is not invoked if header doesn't have action bodys"() {
	given: "Init handler"

	initInputEventHeadersWithFullAttributes(inputEventHeaders)
	inputEventHeaders.put("rpcActionBodys",Collections.emptyList());
	handlerInputManager.init(inputEventHeaders)

	when:
	returnString = handler.executeMoAction(handlerInputManager, netconfManager)

	then:
	0 * netconfManager.action("urn:com:ericsson:ecim:1.0", _ as String, "data")
	notThrown(Exception)
    }

    private void initRpcResponse(final boolean isError, final String errorMessage) {
	rpcResponse.setError(isError)
	rpcResponse.setErrorMessage(errorMessage)
	rpcResponse.setData("test data")
    }

    private void initInputEventHeadersWithFullAttributes(final Map<String, Object> inputEventHeaders) {
	inputEventHeaders.put("apNodeFdn", AP_NODE_FDN)
	inputEventHeaders.put("nodeAddress", NOADE_ADDRESS)
	inputEventHeaders.put("cmNbiNetconfPayload", NETCONF_PAYLOAD)
	inputEventHeaders.put("netconfManager", netconfManager)
	inputEventHeaders.put("isParseOk", true)
	inputEventHeaders.put("editConfigMessageIds", MESSAGE_IDS_1)
	inputEventHeaders.put("netconfPayloadRequestPoId", 1003L)
	inputEventHeaders.put("rpcActionBodys", ACTION_BODYS);
	inputEventHeaders.put("actionMessageIds",MESSAGE_IDS_1);
	inputEventHeaders.put("netconfModelType","ECIM");
    }
}