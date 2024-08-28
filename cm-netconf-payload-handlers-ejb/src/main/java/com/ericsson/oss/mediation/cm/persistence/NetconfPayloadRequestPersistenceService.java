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

import java.time.Instant;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.cm.nbi.netconf.NetconfPayloadRequestResult;
import com.ericsson.oss.services.cm.nbi.netconf.NetconfPayloadRequestStatus;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class NetconfPayloadRequestPersistenceService {

    private static Logger logger = LoggerFactory.getLogger(NetconfPayloadRequestPersistenceService.class);
    private static final String STATUS_ATTRIBUTE = "status";
    private static final String RESULT_ATTRIBUTE = "result";
    private static final String ADDITIONAL_ATTRIBUTE = "additionalInfo";
    private static final String INVALID_POID = "Invalid poId : {}, unable to retrieve persistence object.";

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    public DataPersistenceService getDataPersistenceService() {

        if (dataPersistenceService == null) {
            dataPersistenceService = new ServiceFinderBean().find(DataPersistenceService.class);
        }

        return dataPersistenceService;

    }

    public void markExecutionSuccess(Long poId, Map<String, Object> additionalInfo) {

        logger.debug("Updating Netconf Payload request for poId : {} with Success result and additionalInfo : {}", poId, additionalInfo);

        PersistenceObject persistenceObject = getDataPersistenceService().getLiveBucket().findPoById(poId);
        if (persistenceObject != null) {
            persistenceObject.setAttribute(STATUS_ATTRIBUTE, NetconfPayloadRequestStatus.COMPLETED.toString());
            persistenceObject.setAttribute(RESULT_ATTRIBUTE, NetconfPayloadRequestResult.SUCCESS.toString());
            persistenceObject.setAttribute(ADDITIONAL_ATTRIBUTE, additionalInfo);
        } else {

            logger.warn(INVALID_POID, poId);
        }
    }

    public void markExecutionFailure(Long poId, Map<String, Object> additionalInfo) {

        logger.debug("Updating Netconf Payload request for poId : {} with Failure result and additionalInfo : {}", poId, additionalInfo);

        PersistenceObject persistenceObject = getDataPersistenceService().getLiveBucket().findPoById(poId);
        if (persistenceObject != null) {
            persistenceObject.setAttribute(STATUS_ATTRIBUTE, NetconfPayloadRequestStatus.COMPLETED.toString());
            persistenceObject.setAttribute(RESULT_ATTRIBUTE, NetconfPayloadRequestResult.FAILURE.toString());
            persistenceObject.setAttribute(ADDITIONAL_ATTRIBUTE, additionalInfo);
        } else {
            logger.warn(INVALID_POID, poId);
        }
    }

    public void updateInprogressStatus(Long poId) {
        updateStatus(poId, NetconfPayloadRequestStatus.IN_PROGRESS);
    }

    public void updateParsingStatus(Long poId) {
        updateStatus(poId, NetconfPayloadRequestStatus.PARSING);
    }

    public Instant getPayLoadExecutionStartTime(Long poId){
        PersistenceObject persistenceObject = getDataPersistenceService().getLiveBucket().findPoById(poId);
        return persistenceObject.getCreatedTime().toInstant();
    }

    private void updateStatus(Long poId, NetconfPayloadRequestStatus status) {

        logger.debug("Updating Netconf Payload request for poId : {} with {} status", poId, status);

        PersistenceObject persistenceObject = getDataPersistenceService().getLiveBucket().findPoById(poId);
        if (persistenceObject != null) {
            persistenceObject.setAttribute(STATUS_ATTRIBUTE, status.toString());
        } else {
            logger.warn(INVALID_POID, poId);
        }
    }
}
