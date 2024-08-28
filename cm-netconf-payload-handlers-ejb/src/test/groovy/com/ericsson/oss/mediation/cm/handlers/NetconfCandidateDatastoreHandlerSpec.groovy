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
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager
import com.ericsson.oss.mediation.cm.exceptions.AttributeNotFoundException
import com.ericsson.oss.mediation.cm.exceptions.NetconfExecutionException
import com.ericsson.oss.mediation.cm.persistence.NetconfPayloadRequestPersistenceService;
import com.ericsson.oss.mediation.util.netconf.api.Datastore
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException
import com.ericsson.oss.mediation.util.netconf.filter.SubTreeFilter

public class NetconfCandidateDatastoreHandlerSpec extends CdiSpecification {


    @ObjectUnderTest
    private NetconfCandidateDatastoreHandler handler

    @MockedImplementation
    private NetconfManager netconfManager

    @MockedImplementation
    private RetryManager retryManager;

    @Inject
    private NetconfHandlerInputManager handlerInputManager

    private NetconfResponse rpcResponse = new NetconfResponse()

    private Map<String, Object> inputEventHeaders = new HashMap()

    public void "lock datastore without any error"() {
	given: "Init handler"

	initRpcResponse(false, "")
	netconfManager.lock(Datastore.CANDIDATE) >> rpcResponse
	netconfManager.editConfig(Datastore.CANDIDATE, _ as String) >> rpcResponse
	netconfManager.validate(Datastore.CANDIDATE) >> rpcResponse
	netconfManager.commit() >> rpcResponse
	netconfManager.unlock(Datastore.CANDIDATE) >> rpcResponse

	when:
	handler.write(netconfManager, _ as String)

	then:
	notThrown(Exception)
    }

    public void "return proper error when lock datastore with an error"() {
	given: "Init handler"

	initRpcResponse(true, "")
	netconfManager.lock(Datastore.CANDIDATE) >> rpcResponse
	netconfManager.editConfig(Datastore.CANDIDATE, _ as String) >> rpcResponse
	netconfManager.validate(Datastore.CANDIDATE) >> rpcResponse
	netconfManager.commit() >> rpcResponse
	netconfManager.unlock(Datastore.CANDIDATE) >> rpcResponse

	when:
	handler.write(netconfManager, _ as String)

	then:
	notThrown(Exception)
    }

    public void "return proper error when validate datastore with an error"() {
	given: "Init handler"

	initRpcResponse(false, "")
	netconfManager.lock(Datastore.CANDIDATE) >> rpcResponse
	netconfManager.editConfig(Datastore.CANDIDATE, _ as String) >> rpcResponse
	initRpcResponse(true, "")
	netconfManager.validate(Datastore.CANDIDATE) >> rpcResponse
	netconfManager.commit() >> rpcResponse
	netconfManager.unlock(Datastore.CANDIDATE) >> rpcResponse

	when:
	handler.write(netconfManager, _ as String)

	then:
	notThrown(Exception)
    }

    public void "return proper error when commit datastore with an error"() {
	given: "Init handler"

	initRpcResponse(false, "")
	netconfManager.lock(Datastore.CANDIDATE) >> {throw new NetconfManagerException("")}
	netconfManager.editConfig(Datastore.CANDIDATE, _ as String) >> rpcResponse
	netconfManager.validate(Datastore.CANDIDATE) >> rpcResponse
	initRpcResponse(true, "")
	netconfManager.commit() >> rpcResponse
	netconfManager.unlock(Datastore.CANDIDATE) >> rpcResponse

	when:
	handler.write(netconfManager, _ as String)

	then:
	notThrown(Exception)
    }

    public void "return a response with error details when lock datastore has an error"() {
	given: "Init handler"

	initRpcResponse(true, "")
	netconfManager.lock(Datastore.CANDIDATE) >> rpcResponse
	netconfManager.editConfig(Datastore.CANDIDATE, _ as String) >> rpcResponse
	netconfManager.validate(Datastore.CANDIDATE) >> rpcResponse
	netconfManager.commit() >> rpcResponse
	netconfManager.unlock(Datastore.CANDIDATE) >> rpcResponse

	when:
	handler.executeEditConfig(netconfManager, _ as String)

	then:
	notThrown(Exception)
    }

    private void initRpcResponse(final boolean isError, final String errorMessage) {
	rpcResponse.setError(isError)
	rpcResponse.setErrorMessage(errorMessage)
	rpcResponse.setData("test data")
    }
}