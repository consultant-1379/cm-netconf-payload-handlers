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
package com.ericsson.oss.mediation.cm.persistence;

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject

public class NetconfPayloadRequestPersistenceServiceSpec extends CdiSpecification {

    @ObjectUnderTest
    private NetconfPayloadRequestPersistenceService resultHandler

    @MockedImplementation
    private PersistenceObject persistenceObject

    @MockedImplementation
    private DataPersistenceService dataPersistenceService

    @MockedImplementation
    private DataBucket dataBucket

    @MockedImplementation
    private ComponentEvent mockComponentEvent


    @MockedImplementation
    private EventHandlerContext mockEct

    private static final long  NETCONF_PAYLOAD_REQUEST_PO_ID = 1003L;
    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_TITILE = "errorTitle";
    private static final String ERROR_DETAILS = "errorDetails";
    private static final Integer UNPROCESSABLE_ENTITY = 422;
    private static final Integer EXECUTION_ERROR_CODE = 500;

    private final Map additionalInfo = new HashMap();

    def "Test success execution with poid and additionalInfo" (){
        given: "Required MOs exist"
        dataPersistenceService.getLiveBucket()>> dataBucket
        dataBucket.findPoById(NETCONF_PAYLOAD_REQUEST_PO_ID) >> persistenceObject
        when: "Set result is executed"
        resultHandler.markExecutionSuccess(NETCONF_PAYLOAD_REQUEST_PO_ID, null)
        then:
        notThrown(Exception)
    }

    def "Test success execution with Invalid poid and additionalInfo" (){
        given: "Required MOs exist"
        dataPersistenceService.getLiveBucket()>> dataBucket
        dataBucket.findPoById(102l) >> persistenceObject
        when: "Set result is executed"
        resultHandler.markExecutionSuccess(NETCONF_PAYLOAD_REQUEST_PO_ID, null)
        then:
        notThrown(Exception)
    }

    def "Test Failure execution with poid and additionalInfo" (){
        given: "Required MOs exist"
        prepareFailureAdditionalInfo(additionalInfo)
        dataPersistenceService.getLiveBucket()>> dataBucket
        dataBucket.findPoById(NETCONF_PAYLOAD_REQUEST_PO_ID) >> persistenceObject
        when: "Set result is executed"
        resultHandler.markExecutionFailure(NETCONF_PAYLOAD_REQUEST_PO_ID, additionalInfo)
        then:
        notThrown(Exception)
    }

    def "Test Failure execution with invalid poid and additionalInfo" (){
        given: "Required MOs exist"
        prepareFailureAdditionalInfo(additionalInfo)
        dataPersistenceService.getLiveBucket()>> dataBucket
        dataBucket.findPoById(102l) >> persistenceObject
        when: "Set result is executed"
        resultHandler.markExecutionFailure(NETCONF_PAYLOAD_REQUEST_PO_ID, additionalInfo)
        then:
        notThrown(Exception)
    }

    private void prepareFailureAdditionalInfo(final Map additionalInfo) {
        additionalInfo.put(ERROR_CODE, EXECUTION_ERROR_CODE);
        additionalInfo.put(ERROR_TITILE, "Netconf Connection Error");
        additionalInfo.put(ERROR_DETAILS, "Connection Error Message");
    }

    def "Verify Execution of netconfpayload inprogress status " (){
        given: "Required MOs exist"
        dataPersistenceService.getLiveBucket()>> dataBucket
        dataBucket.findPoById(NETCONF_PAYLOAD_REQUEST_PO_ID) >> persistenceObject
        when: "Set result is executed"
        resultHandler.updateInprogressStatus(NETCONF_PAYLOAD_REQUEST_PO_ID)
        then:
        notThrown(Exception)
    }
    def "Verify updating parsing status during execution of netconf payload " (){
        given: "Required MOs exist"
        dataPersistenceService.getLiveBucket()>> dataBucket
        dataBucket.findPoById(NETCONF_PAYLOAD_REQUEST_PO_ID) >> persistenceObject
        when: "Set result is executed"
        resultHandler.updateParsingStatus(NETCONF_PAYLOAD_REQUEST_PO_ID)
        then:
        notThrown(Exception)
    }
    def "Verify updating parsing status during execution of netconf payload with invalid poid" (){
        given: "Required MOs exist"
        dataPersistenceService.getLiveBucket()>> dataBucket
        dataBucket.findPoById(100) >> persistenceObject
        when: "Set result is executed"
        resultHandler.updateParsingStatus(NETCONF_PAYLOAD_REQUEST_PO_ID)
        then:
        notThrown(Exception)
    }
}