package com.ericsson.oss.mediation.cm.handlers;

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;

public class NetconfCandidateDatastoreHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetconfCandidateDatastoreHandler.class);

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int WAIT_INTERVAL = 1000;
    private static final double EXPONENTIAL_BACKOFF = 1.1;

    @Inject
    private RetryManager retryManager;

    public NetconfResponse write(final NetconfManager netconfManager, final String body) {
        try {
            NetconfResponse response = null;
            response = getLock(netconfManager);
            if (response != null && response.isError()) {
                LOGGER.error("Netconf lock operation failed with response: {}", response);
                return response;
            }

            response = executeEditConfig(netconfManager, body);
            if (response.isError()) {
                unlockOnError(netconfManager, response);
                return response;
            }

            response = validate(netconfManager);
            if (response.isError()) {
                unlockOnError(netconfManager, response);
                return response;
            }

            response = commit(netconfManager);
            if (response.isError()) {
                unlockOnError(netconfManager, response);
                return response;
            }

            response = netconfManager.unlock(Datastore.CANDIDATE);
            if (response.isError()) {
                LOGGER.error("Netconf unlock operation failed with response: {}", response.getErrorMessage());
                return response;
            }

            LOGGER.info("Write operation completed successfully");
            return response;

        } catch (final NetconfManagerException e) {
            LOGGER.error(e.getMessage(), e);
            NetconfResponse response = new NetconfResponse();
            response.setError(true);
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    private NetconfResponse commit(final NetconfManager netconfManager) throws NetconfManagerException {
        NetconfResponse response;
        response = netconfManager.commit();
        return response;
    }

    private NetconfResponse validate(final NetconfManager netconfManager) throws NetconfManagerException {
        NetconfResponse response;
        response = netconfManager.validate(Datastore.CANDIDATE);
        return response;
    }

    private NetconfResponse executeEditConfig(final NetconfManager netconfManager, final String body) throws NetconfManagerException {
        NetconfResponse response;
        response = netconfManager.editConfig(Datastore.CANDIDATE, body);
        return response;
    }

    private NetconfResponse unlockOnError(final NetconfManager netconfManager, NetconfResponse response) throws NetconfManagerException {
        if (response.isError()) {
            response = unlock(netconfManager, response);
        }
        return response;
    }

    private NetconfResponse unlock(final NetconfManager netconfManager, NetconfResponse response) throws NetconfManagerException {
        LOGGER.error("Netconf operation failed with response: {}", response.getErrorMessage());
        response = netconfManager.unlock(Datastore.CANDIDATE);
        if (response.isError()) {
            LOGGER.error("Netconf commit-unlock operation failed with response: {}", response.getErrorMessage());
            return response;
        }
        return response;
    }

    private NetconfResponse getLock(final NetconfManager netconfManager) {
        NetconfResponse response;
        try {
            response = retryManager.executeCommand(getRetryPolicy(), new RetriableCommand<NetconfResponse>() {
                @Override
                public NetconfResponse execute(final RetryContext retryContext) throws Exception {
                    final NetconfResponse response = netconfManager.lock(Datastore.CANDIDATE);
                    if (response != null && response.isError()) {
                        throw new Exception(); // NOSONAR
                    }
                    return response;
                }
            });
        } catch (final RetriableCommandException rce) {
            LOGGER.error("retry manager has exceeded max number of retries : {}", rce.getMessage());
            response = new NetconfResponse();
            response.setError(true);
            response.setErrorMessage("Netconf lock operation failed on Candidate Datastore.");
            return response;
        }

        return response;
    }

    private RetryPolicy getRetryPolicy() {
        return RetryPolicy.builder().attempts(MAX_RETRY_ATTEMPTS).waitInterval(WAIT_INTERVAL, TimeUnit.MILLISECONDS).exponentialBackoff(EXPONENTIAL_BACKOFF).retryOn(Exception.class).build();
    }

}