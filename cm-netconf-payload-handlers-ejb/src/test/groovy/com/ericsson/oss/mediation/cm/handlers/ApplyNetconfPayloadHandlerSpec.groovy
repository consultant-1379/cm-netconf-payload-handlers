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

import java.nio.file.Paths

import javax.inject.Inject
import java.time.Instant;


import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.mediation.cm.exceptions.AttributeNotFoundException
import com.ericsson.oss.mediation.cm.persistence.NetconfPayloadRequestPersistenceService;
import com.ericsson.oss.mediation.cm.util.AdditionalInfoBuilder
import com.ericsson.oss.mediation.cm.util.FileResource
import com.ericsson.oss.mediation.util.netconf.api.Datastore
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException
import com.ericsson.oss.mediation.util.netconf.filter.SubTreeFilter

public class ApplyNetconfPayloadHandlerSpec extends CdiSpecification {

    private static final String RPC_CONFIG_1 = "<ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>LTE01dg2ERBS00008</managedElementId><SystemFunctions><systemFunctionsId>1</systemFunctionsId><Lm xmlns=\"urn:com:ericsson:ecim:RcsLM\"><lmId>1</lmId><fingerprint>Site1</fingerprint></Lm></SystemFunctions></ManagedElement>"
    private static final String RPC_CONFIG_2 = "<ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>LTE01dg2ERBS00008</managedElementId><SystemFunctions><systemFunctionsId>1</systemFunctionsId><Lm xmlns=\"urn:com:ericsson:ecim:RcsLM\"><lmId>1</lmId><fingerprint>Site2</fingerprint></Lm></SystemFunctions></ManagedElement>"
    private static final String ACTION_CONFIG_1 = "<data>  <ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>NR01gNodeBRadio00001</managedElementId><SystemFunctions><systemFunctionsId>1</systemFunctionsId><Lm xmlns=\"urn:com:ericsson:ecim:RcsLM\"><lmId>1</lmId><EmergencyUnlock><emergencyUnlockId>1</emergencyUnlockId><activate></activate></EmergencyUnlock></Lm></SystemFunctions></ManagedElement> </data>"
    private static final String ACTION_CONFIG_2 = "<data>  <ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>NR01gNodeBRadio00001</managedElementId><SystemFunctions><systemFunctionsId>1</systemFunctionsId><Lm xmlns=\"urn:com:ericsson:ecim:RcsLM\"><lmId>1</lmId><EmergencyUnlock><emergencyUnlockId>1</emergencyUnlockId><activate></activate></EmergencyUnlock></Lm></SystemFunctions></ManagedElement> </data>"
    private static final String GET_CONFIG_1 = "<rpc message-id=\"91\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">    <get-config> <source><running/> </source>       </get-config></rpc>"
    private static final String GET_CONFIG_2 = "<rpc message-id=\"91\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">    <get-config> <source><running/> </source>       </get-config></rpc>"
    private static final String GET_CONFIG_WITH_FILTER_1 = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><get-config><source><running /></source><filter type=\"subtree\"><ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>1</managedElementId> <SystemFunctions> <systemFunctionsId>1</systemFunctionsId> <Fm xmlns=\"urn:com:ericsson:ecim:RcsFm\"> <fmId>1</fmId></Fm> </SystemFunctions> </ManagedElement></filter></get-config></rpc> ]]>]]>"
    private static final String GET_CONFIG_WITH_FILTER_2 = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><get-config><source><running /></source><filter type=\"subtree\"><ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>1</managedElementId> <SystemFunctions> <systemFunctionsId>1</systemFunctionsId> <Fm xmlns=\"urn:com:ericsson:ecim:RcsFm\"> <fmId>1</fmId></Fm> </SystemFunctions> </ManagedElement></filter></get-config></rpc> ]]>]]>"

    private static final String MESSAGE_ID_1 = "NO_MESSAGE_ID, RPC Message Sequence: 1"
    private static final String MESSAGE_ID_2 = "NO_MESSAGE_ID, RPC Message Sequence: 2"
    private static final List<String> RPC_BODYS_1 = Arrays.asList(RPC_CONFIG_1, RPC_CONFIG_2)
    private static final List<String> MESSAGE_IDS_1 = Arrays.asList(MESSAGE_ID_1, MESSAGE_ID_2)
    private static final List<Datastore> datastores = Arrays.asList(Datastore.RUNNING, Datastore.CANDIDATE)
    private static final List<String> ACTION_BODYS = Arrays.asList(ACTION_CONFIG_1, ACTION_CONFIG_2)
    private static final List<String> GET_CONFIG_BODYS = Arrays.asList(GET_CONFIG_WITH_FILTER_1, GET_CONFIG_WITH_FILTER_2)

    private static final String AP_NODE_FDN = "Project=RadioNodeECTValidSEValidSB,Node=LTE01dg2ERBS00008"
    private static final String NOADE_ADDRESS = "SubNetwork=LTE01dg2ERBS00008,ManagedElement=LTE01dg2ERBS00008"
    private static final String NETCONF_FILE_PATH = "/ericsson/tor/data/autoprovisioning/artifacts/raw/RadioNodeECTValidSEValidSB/LTE01dg2ERBS00008/optionalFeature.xml"
    private static final String ERROR_MESSAGE = "Node Reject"
    private static final String NODE_NAME = "LTE01dg2ERBS00008"
    private static final String COM_TOP_NAMESPACE_VERSION = "10.10.1"
    private static final String NODE_TYPE = "RadioNode"
    private static final String PROJECT_FDN = "Project=RadioNodeECTValidSEValidSB"
    private static final String SECURE_USER = "secureUserName"
    private static final String SECURE_PASSWORD = "secureUserPassword"
    private static final String ADDRESS = "address"
    private static final String NETCONF_PAYLOAD = "cmNbiNetconfPayload";
    private static final Instant START_TIME_OF_DURATION = Instant.now();


    @ObjectUnderTest
    private ApplyNetconfPayloadHandler handler

    @MockedImplementation
    private ComponentEvent mockComponentEvent

    @MockedImplementation
    private EventHandlerContext mockEct

    @MockedImplementation
    private NetconfPayloadRequestPersistenceService service

    @MockedImplementation
    private NetconfManager netconfManager

    @MockedImplementation
    private NetconfCandidateDatastoreHandler netconfCandidateDatastoreHandler;

    @MockedImplementation
    private MoActionExecutionHandler moActionExecutionHandler;

    @MockedImplementation
    private GetNetconfConfiguration getNetconfConfiguration;

    @MockedImplementation
    private FileResource fileResource;

    @Inject
    private NetconfHandlerInputManager handlerInputManager

    private NetconfResponse rpcResponse = new NetconfResponse()

    private Map<String, Object> inputEventHeaders = new HashMap()

    private Map<String, Object> resultMap = new HashMap()

    public void "Build parse error when parseOk is set to false"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("isParseOk", false)
        inputEventHeaders.put("rpcActionBodys", Collections.emptyList());
        inputEventHeaders.put("actionMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcGetConfigBodys", Collections.emptyList());
        initRpcResponse(false, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.editConfig(Datastore.RUNNING, _ as String) >> rpcResponse
        netconfManager.editConfig(Datastore.CANDIDATE, _ as String) >> rpcResponse
        handlerInputManager.getRpcEditConfigDataStores() >> datastores
        netconfCandidateDatastoreHandler.write(netconfManager, _ as String) >> rpcResponse

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionFailure(_ as Long, _ as Map)
        notThrown(Exception)
    }

    public void "Build parse error when parseOk is set to true and all types of rpc bodies are empty"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("isParseOk", true)
        inputEventHeaders.put("editConfigMessageIds", Collections.emptyList())
        inputEventHeaders.put("rpcEditConfigBodys", Collections.emptyList())
        inputEventHeaders.put("rpcActionBodys", Collections.emptyList());
        inputEventHeaders.put("actionMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcGetConfigBodys", Collections.emptyList());
        initRpcResponse(false, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.editConfig(Datastore.RUNNING, _ as String) >> rpcResponse
        netconfManager.editConfig(Datastore.CANDIDATE, _ as String) >> rpcResponse
        handlerInputManager.getRpcEditConfigDataStores() >> datastores
        netconfCandidateDatastoreHandler.write(netconfManager, _ as String) >> rpcResponse

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionFailure(_ as Long, _ as Map)
        notThrown(Exception)
    }

    public void "Test result is updated without error message while all edit config RPCs are executed correctly"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("rpcActionBodys", Collections.emptyList());
        inputEventHeaders.put("actionMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcGetConfigBodys", Collections.emptyList());
        initRpcResponse(false, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.editConfig(Datastore.RUNNING, _ as String) >> rpcResponse
        netconfManager.editConfig(Datastore.CANDIDATE, _ as String) >> rpcResponse
        handlerInputManager.getRpcEditConfigDataStores() >> datastores
        netconfCandidateDatastoreHandler.write(netconfManager, _ as String) >> rpcResponse

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionSuccess(_ as Long, _ as Map)
        notThrown(Exception)
    }

    public void "Test result is updated without error message while all action RPCs are executed correctly"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("rpcActionBodys", ACTION_BODYS);
        inputEventHeaders.put("actionMessageIds", MESSAGE_IDS_1);
        inputEventHeaders.put("rpcEditConfigBodys", Collections.emptyList());
        inputEventHeaders.put("editConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcGetConfigBodys", Collections.emptyList());
        initRpcResponse(false, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.action(Datastore.RUNNING, _ as String) >> rpcResponse
        moActionExecutionHandler.executeMoAction(handlerInputManager, netconfManager) >> "test data"
        getNetconfConfiguration.writeNetconfConfigurationFile(_ as String, _ as String, _ as String) >> true

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionSuccess(_ as Long, _ as Map)
        1 * moActionExecutionHandler.executeMoAction(handlerInputManager, netconfManager);
        0 * handler.executeEditConfigs(ADDRESS, NETCONF_PAYLOAD, netconfManager);
        notThrown(Exception)
    }

    public void "Test result is updated without error message while all get config RPCs are executed correctly"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("rpcActionBodys", Collections.emptyList());
        inputEventHeaders.put("actionMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcEditConfigBodys", Collections.emptyList());
        inputEventHeaders.put("editConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcGetConfigBodys", Collections.emptyList());
        inputEventHeaders.put("hasGetConfig", true);
        initRpcResponse(false, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.getConfig(Datastore.RUNNING) >> rpcResponse
        StringBuilder netconfConfiguration = new StringBuilder();

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionSuccess(_ as Long, _ as Map)
        notThrown(Exception)
    }

    public void "Test result is updated with error message when error is seen while get config RPCs are executed"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("rpcActionBodys", Collections.emptyList());
        inputEventHeaders.put("actionMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcEditConfigBodys", Collections.emptyList());
        inputEventHeaders.put("editConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcGetConfigBodys", Collections.emptyList());
        inputEventHeaders.put("hasGetConfig", true);
        initRpcResponse(true, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.getConfig(Datastore.RUNNING) >> rpcResponse
        StringBuilder netconfConfiguration = new StringBuilder();

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionFailure(_ as Long, _ as Map)
        notThrown(Exception)
    }

    public void "Test result is updated without error message while all get config with filter RPCs are executed correctly"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("rpcActionBodys", Collections.emptyList());
        inputEventHeaders.put("actionMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcEditConfigBodys", Collections.emptyList());
        inputEventHeaders.put("editConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", MESSAGE_IDS_1);
        inputEventHeaders.put("rpcGetConfigBodys", GET_CONFIG_BODYS);
        initRpcResponse(false, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.getConfig(Datastore.RUNNING, new SubTreeFilter(_ as String)) >> rpcResponse
        getNetconfConfiguration.getNetconfConfiguration(handlerInputManager) >> "test data"
        getNetconfConfiguration.writeNetconfConfigurationFile('/ericsson/tor/data/mscmce//null.txt', 'test data', null) >> true

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionSuccess(_ as Long, _ as Map)
        notThrown(Exception)
    }

    public void "Test result is updated with file write error message if error seen while get config with filter response is written"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("rpcActionBodys", Collections.emptyList());
        inputEventHeaders.put("actionMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcEditConfigBodys", Collections.emptyList());
        inputEventHeaders.put("editConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", MESSAGE_IDS_1);
        inputEventHeaders.put("rpcGetConfigBodys", GET_CONFIG_BODYS);
        initRpcResponse(false, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.getConfig(Datastore.RUNNING, new SubTreeFilter(_ as String)) >> rpcResponse
        StringBuilder netconfConfiguration = new StringBuilder();
        netconfConfiguration.append("test data");
        getNetconfConfiguration.getNetconfConfiguration(handlerInputManager) >> "test data"
        getNetconfConfiguration.writeNetconfConfigurationFile(_ as String, _ as String, _ as String) >> false

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionFailure(_ as Long, _ as Map)
        notThrown(Exception)
    }

    public void "Test result is updated with error message while all RPCs are not executed correctly"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        initRpcResponse(true, "")
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders
        netconfManager.editConfig(Datastore.RUNNING, _ as String) >> rpcResponse

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * service.markExecutionFailure(_ as Long, _ as Map)
        notThrown(Exception)
    }


    public void "Test result is not updated again while the parsing of Netconf file is failed"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("isParseOk", false)
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        handler.onEvent(mockComponentEvent)

        then:
        0 * service.markExecutionSuccess(_ as String,_ as Map)
        notThrown(Exception)
    }

    public void "Test RPC ERROR could be handled correctly"() {
        given: "Init handler"

        handler.init(mockEct)
        initRpcResponse(true, ERROR_MESSAGE)
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
        netconfManager.editConfig(Datastore.RUNNING, _ as String) >> rpcResponse
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        handler.onEvent(mockComponentEvent)

        then:
        1 * netconfManager.editConfig(Datastore.RUNNING, RPC_CONFIG_1)
        notThrown(Exception)
    }

    public void "Test NetconfManagerException is handled while the status of Netconf Manager is not CONNECTED"() {
        given: "Init handler"
        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        netconfManager.getStatus() >> NetconfConnectionStatus.NOT_CONNECTED
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        handler.onEvent(mockComponentEvent);
        handler.destroy()

        then:
        notThrown(NetconfManagerException)
    }

    public void "Test NullPointerException is handled while the Netconf Manager is not able to retrieve from the input event header"() {
        given: "Init handler"
        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("netconfManager", null)
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        handler.onEvent(mockComponentEvent);
        handler.destroy()

        then:
        notThrown(NullPointerException)
    }

    public void "Test AttributeNotFoundException is handled while apNodeFdn in the input event header is missing for some unexcepted scenario"() {
        given: "Init handler"

        handler.init(mockEct)
        initInputEventHeadersWithFullAttributes(inputEventHeaders)
        inputEventHeaders.put("apNodeFdn", null)
        service.getPayLoadExecutionStartTime(1003L) >> START_TIME_OF_DURATION
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        handler.onEvent(mockComponentEvent);

        then:
        notThrown(AttributeNotFoundException)
    }

    private void initRpcResponse(final boolean isError, final String errorMessage) {
        rpcResponse.setError(isError)
        rpcResponse.setErrorMessage(errorMessage)
    }

    private void initInputEventHeadersWithFullAttributes(final Map<String, Object> inputEventHeaders) {
        inputEventHeaders.put("apNodeFdn", AP_NODE_FDN)
        inputEventHeaders.put("nodeAddress", NOADE_ADDRESS)
        inputEventHeaders.put("cmNbiNetconfPayload", Paths.get(getClass().getResource("/getConfigResponse.txt").toURI()).toString())
        inputEventHeaders.put("netconfManager", netconfManager)
        inputEventHeaders.put("isParseOk", true)
        inputEventHeaders.put("editConfigMessageIds", MESSAGE_IDS_1)
        inputEventHeaders.put("rpcEditConfigBodys", RPC_BODYS_1)
        inputEventHeaders.put("netconfPayloadRequestPoId", 1003L)
        inputEventHeaders.put("rpcEditConfigDataStore", datastores)
        inputEventHeaders.put("rpcActionBodys", Collections.emptyList());
        inputEventHeaders.put("actionMessageIds", Collections.emptyList());
        inputEventHeaders.put("getConfigMessageIds", Collections.emptyList());
        inputEventHeaders.put("rpcGetConfigBodys", Collections.emptyList());
    }
}
