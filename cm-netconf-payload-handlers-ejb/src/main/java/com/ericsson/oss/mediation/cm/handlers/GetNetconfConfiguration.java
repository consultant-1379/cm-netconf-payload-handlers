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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.itpf.sdk.resources.Resources;
import com.ericsson.oss.mediation.cm.exceptions.NetconfExecutionException;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;
import com.ericsson.oss.mediation.util.netconf.filter.SubTreeFilter;

/**
 * The Class GetNetconfConfiguration is used to retrieve netconf configuration from a node, and save the configuration data to file system
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class GetNetconfConfiguration {

    private static Logger logger = LoggerFactory.getLogger(GetNetconfConfiguration.class);
    private long aggregatedResponseTime;

    public long getResponseTime() {
        return aggregatedResponseTime;
    }

    public String getNetconfConfiguration(final NetconfHandlerInputManager handlerInputManager) throws NetconfManagerException {
        String messageId = "";
        final List<String> getConfigMessageIds = handlerInputManager.getGetConfigMessageIds();
        final List<String> rpcGetConfigBodys = handlerInputManager.getRpcGetConfigBodys();

        final String nodeAddress = handlerInputManager.getNodeAddress();
        final String requestId = handlerInputManager.getRequestId();
        final StringBuilder nodeConfigData = new StringBuilder();
        NetconfResponse nodeConfigResponse;
        final NetconfManager netconfManager = handlerInputManager.getNetconfManager();

        logger.info("Executing get config for request Id : {} on node : {}", requestId, nodeAddress);

        for (int i = 0; i < rpcGetConfigBodys.size(); i++) {
            messageId = getConfigMessageIds.get(i);
            String getConfigBody = rpcGetConfigBodys.get(i);
            logger.debug("Executing get-config with messageId : {}, requestId : {}, on node : {}. get config body : {} ", messageId, requestId, nodeAddress, getConfigBody);
            Instant startTime = Instant.now();
            nodeConfigResponse = netconfManager.getConfig(Datastore.RUNNING, new SubTreeFilter(getConfigBody));
            Duration responseTime = Duration.between(startTime, Instant.now());
            aggregatedResponseTime = aggregatedResponseTime + responseTime.toMillis();
            logger.debug("Get-Config command successfully executed for request Id : {} and response is {}", requestId, nodeConfigResponse.getData());
            if (nodeConfigResponse.isError()) {
                logger.debug("Netconf execution failure for request Id : {}, netconf response : {}", requestId, nodeConfigResponse);
                throw new NetconfExecutionException(nodeConfigResponse.toString());
            } else {
                nodeConfigData.append(nodeConfigResponse.getData());
            }
        }
        return nodeConfigData.toString();

    }

    public boolean writeNetconfConfigurationFile(final String nodeConfigUri, final String configData, final String requestId) {
        logger.debug("Writing get configuration data : {} in to file {} for request Id : {}", configData, nodeConfigUri, requestId);

        final Resource resource = Resources.getFileSystemResource(nodeConfigUri);
        if (!resource.exists()) {
            logger.trace("Resource not exist creating new file {}", resource.getName());
            resource.createFile();
        }

        final int writeBytes = resource.write(configData.getBytes(StandardCharsets.UTF_8), true);
        if (writeBytes > 0) {
            logger.debug("Successfully wrote get configuration file with size {} to {}", writeBytes, nodeConfigUri);
            return true;
        }
        logger.error("Failed to write get configuration data to file {}", nodeConfigUri);
        return false;
    }
}
