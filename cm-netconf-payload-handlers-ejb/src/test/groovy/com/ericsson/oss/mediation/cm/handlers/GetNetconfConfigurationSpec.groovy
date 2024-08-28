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

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.resources.Resource
import com.ericsson.oss.itpf.sdk.resources.Resources
import com.ericsson.oss.itpf.sdk.resources.ResourcesException
import com.ericsson.oss.mediation.cm.exceptions.AttributeNotFoundException
import com.ericsson.oss.mediation.cm.exceptions.NetconfExecutionException
import com.ericsson.oss.mediation.cm.persistence.NetconfPayloadRequestPersistenceService;
import com.ericsson.oss.mediation.util.netconf.api.Datastore
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException
import com.ericsson.oss.mediation.util.netconf.filter.SubTreeFilter

import spock.lang.Unroll

/*import org.junit.runner.RunWith
 import org.powermock.api.mockito.PowerMockito
 import org.powermock.core.classloader.annotations.PowerMockIgnore
 import org.powermock.core.classloader.annotations.PrepareForTest
 import org.powermock.modules.junit4.PowerMockRunnerDelegate
 import org.powermock.modules.junit4.PowerMockRunner
 import org.spockframework.runtime.Sputnik
 @RunWith(PowerMockRunner.class)
 @PowerMockRunnerDelegate(Sputnik.class)
 @PrepareForTest([Resources.class])
 @PowerMockIgnore("javax.management.*")*/
public class GetNetconfConfigurationSpec extends CdiSpecification {

    private static final String GET_CONFIG_WITH_FILTER_1 = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><get-config><source><running /></source><filter type=\"subtree\"><ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>1</managedElementId> <SystemFunctions> <systemFunctionsId>1</systemFunctionsId> <Fm xmlns=\"urn:com:ericsson:ecim:RcsFm\"> <fmId>1</fmId></Fm> </SystemFunctions> </ManagedElement></filter></get-config></rpc> ]]>]]>"
    private static final String GET_CONFIG_WITH_FILTER_2 = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><get-config><source><running /></source><filter type=\"subtree\"><ManagedElement xmlns=\"urn:com:ericsson:ecim:ComTop\"><managedElementId>1</managedElementId> <SystemFunctions> <systemFunctionsId>1</systemFunctionsId> <Fm xmlns=\"urn:com:ericsson:ecim:RcsFm\"> <fmId>1</fmId></Fm> </SystemFunctions> </ManagedElement></filter></get-config></rpc> ]]>]]>"

    private static final String MESSAGE_ID_1 = "NO_MESSAGE_ID, RPC Message Sequence: 1"
    private static final String MESSAGE_ID_2 = "NO_MESSAGE_ID, RPC Message Sequence: 2"
    private static final List<String> MESSAGE_IDS_1 = Arrays.asList(MESSAGE_ID_1, MESSAGE_ID_2)
    private static final List<String> GET_CONFIG_BODYS = Arrays.asList(GET_CONFIG_WITH_FILTER_1, GET_CONFIG_WITH_FILTER_2)
    private static final String AP_NODE_FDN = "Project=RadioNodeECTValidSEValidSB,Node=LTE01dg2ERBS00008"
    private static final String NOADE_ADDRESS = "SubNetwork=LTE01dg2ERBS00008,ManagedElement=LTE01dg2ERBS00008"
    private static final String NETCONF_PAYLOAD = "cmNbiNetconfPayload";

    @MockedImplementation
    private NetconfManager netconfManager

    @ObjectUnderTest
    private GetNetconfConfiguration getNetconfConfiguration;

    @MockedImplementation
    Resource resource

    @Inject
    private NetconfHandlerInputManager handlerInputManager

    private NetconfResponse rpcResponse = new NetconfResponse()

    private Map<String, Object> inputEventHeaders = new HashMap()

    private Map<String, Object> resultMap = new HashMap()

    @Unroll
    public void "execute edit config with filter for ECIM without any error"() {
	given: "Init handler"

	initInputEventHeadersWithFullAttributes(inputEventHeaders)
	handlerInputManager.init(inputEventHeaders)
	initRpcResponse(false, "")
	netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
	netconfManager.getConfig(Datastore.RUNNING, _ as SubTreeFilter) >> rpcResponse

	when:
	getNetconfConfiguration.getNetconfConfiguration(handlerInputManager)

	then:
	notThrown(Exception)
    }

    @Unroll
    public void "throw exception if there is an error with execution of edit config with filter"() {
	given: "Init handler"

	initInputEventHeadersWithFullAttributes(inputEventHeaders)
	handlerInputManager.init(inputEventHeaders)
	initRpcResponse(true, "")
	netconfManager.getStatus() >> NetconfConnectionStatus.CONNECTED
	netconfManager.getConfig(Datastore.RUNNING, _ as SubTreeFilter) >> rpcResponse

	when:
	getNetconfConfiguration.getNetconfConfiguration(handlerInputManager)

	then:
	thrown(NetconfExecutionException)
    }

    @Unroll
    public void "write netconf config without any error"() {
	given: "Init handler"

	//PowerMockito.mockStatic(Resources.class)
	//PowerMockito.when(Resources.getFileSystemResource(_ as String)).thenReturn(resource)
	Resources.getFileSystemResource(Paths.get(getClass().getResource("/getConfigResponse.txt").toURI()).toString()) >> resource
	resource.exists() >> true
	resource.createFile() >> true
	resource.write("Test Data".getBytes(StandardCharsets.UTF_8), true) >> 10
	when:
	getNetconfConfiguration.writeNetconfConfigurationFile(Paths.get(getClass().getResource("/getConfigResponse.txt").toURI()).toString(),_ as String,_ as String)

	then:
	notThrown(Exception)
    }

    @Unroll
    public void "write netconf config without any error with creating file"() {
	given: "Init handler"

	Resources.getFileSystemResource(Paths.get(getClass().getResource("/getConfigResponse.txt").toURI()).toString()) >> resource
	resource.exists() >> false
	resource.createFile() >> true
	resource.write("Test Data".getBytes(StandardCharsets.UTF_8), true) >> 10
	when:
	boolean fileIsWritten = getNetconfConfiguration.writeNetconfConfigurationFile(Paths.get(getClass().getResource("/getConfigResponse.txt").toURI()).toString(),"Test Data",_ as String)

	then:
	notThrown(Exception)
    }

    @Unroll
    public void "write netconf config without any error with empty data to file"() {
	given: "Init handler"

	Resources.getFileSystemResource(Paths.get(getClass().getResource("/getConfigResponse.txt").toURI()).toString()) >> resource
	resource.exists() >> false
	resource.createFile() >> true
	resource.write("Test Data".getBytes(StandardCharsets.UTF_8), true) >> 10
	when:
	boolean fileIsWritten = getNetconfConfiguration.writeNetconfConfigurationFile(Paths.get(getClass().getResource("/getConfigResponse.txt").toURI()).toString(),"",_ as String)

	then:
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
	inputEventHeaders.put("rpcGetConfigBodys", GET_CONFIG_BODYS);
	inputEventHeaders.put("getConfigMessageIds",MESSAGE_IDS_1);
	inputEventHeaders.put("netconfModelType","ECIM");
    }
}