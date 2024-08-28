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

import static java.lang.String.join;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.TypedEventInputHandler;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.cm.exceptions.NetconfConfigFileWritingException;
import com.ericsson.oss.mediation.cm.exceptions.NetconfExecutionException;
import com.ericsson.oss.mediation.cm.persistence.NetconfPayloadRequestPersistenceService;
import com.ericsson.oss.mediation.cm.util.AdditionalInfoBuilder;
import com.ericsson.oss.mediation.cm.util.FileResource;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@EventHandler
public final class ApplyNetconfPayloadHandler implements TypedEventInputHandler { // NOSONAR

    private static Logger logger = LoggerFactory.getLogger(ApplyNetconfPayloadHandler.class);
    private static final String GET_CONFIG_RESPONSES_FOLDER = "/ericsson/tor/data/mscmce/";
    private static final String CM_NBI_REST_NETCONF_PAYLOAD ="CM_NBI_REST_NETCONF_PAYLOAD.APPLIED";
    private double aggregatedEditConfigResponseTime;
    private double getConfigResponseTime;

    @Inject
    private NetconfHandlerInputManager handlerInputManager;

    @Inject
    private NetconfPayloadRequestPersistenceService persistenceService;

    @Inject
    private GetNetconfConfiguration getNetconfConfiguration;

    @Inject
    private NetconfCandidateDatastoreHandler netconfCandidateDatastoreHandler;

    @Inject
    private MoActionExecutionHandler moActionExecutionHandler;

    @Inject
    private FileResource fileResource;

    @Inject
    private SystemRecorder systemRecorder;

    Map<String, Object> additionalInfo = new HashMap<>();

    /**
     * Callback method, will be called once the handler is loaded and used to
     * initialize attributes.
     */
    @Override
    public void init(final EventHandlerContext context) {// Added unimplemented
                                                         // method

    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent inputEvent) {

        boolean isAnError = true;
        Long netconfPayloadRequestPoId = null;
        String requestId = null;

        try {
            handlerInputManager.init(inputEvent.getHeaders());
            String nodeAddress = handlerInputManager.getNodeAddress();
            requestId = handlerInputManager.getRequestId();
            netconfPayloadRequestPoId = handlerInputManager.getNetconfPayloadRequestPoId();
            StringBuilder netconfConfiguration = new StringBuilder();
            if (isParsingOk()) {
                logger.info("Starting applying netconf payload on node : {} with requestId : {} ", nodeAddress,
                        requestId);

                final NetconfManager netconfManager = handlerInputManager.getNetconfManager();
                validateNetconfManagerStatus(netconfManager);

                persistenceService.updateInprogressStatus(netconfPayloadRequestPoId);

                executeEditConfigs(nodeAddress, requestId, netconfManager);

                executeMoAction(netconfManager, netconfConfiguration);

                executeGetConfigWithFilter(netconfConfiguration);

                executeGetConfig(netconfManager, netconfConfiguration);


                if (netconfConfiguration.length() > 0) {
                    writeNetconfConfigToFile(netconfConfiguration.toString());
                }

                isAnError = false;
                logger.info("Completed applying netconf payload on node : {} with requestId : {} ", nodeAddress,
                        requestId);

            } else {
                logger.info("Updating CM NBI request PO with requestId : {} with parser errors", requestId);
                additionalInfo = AdditionalInfoBuilder.buildParserError(
                        fileResource.getFileContentAsString(handlerInputManager.getNetconfPayloadFileName()));
            }
        } catch (NetconfManagerException netconfException) {
            logger.error("Failed to execute request for request Id : {}.", requestId, netconfException);
            additionalInfo = AdditionalInfoBuilder.buildNetconfConnectionError(netconfException.getMessage());
        } catch (NetconfExecutionException netconfExecutionException) {
            logger.error("Failed to execute request for request Id : {}.", requestId, netconfExecutionException);
            additionalInfo = AdditionalInfoBuilder.buildNetconfExecutionError(netconfExecutionException.getMessage());
        } catch (NetconfConfigFileWritingException netconfConfigFileWritingException) {
            logger.error("Failed to execute request for request Id : {}.", requestId,
                    netconfConfigFileWritingException);
            additionalInfo = AdditionalInfoBuilder.buildFileWriteError(netconfConfigFileWritingException.getMessage());
        } catch (Exception exception) {
            logger.error("Failed to execute request for request Id : {}.",
                    requestId != null ? requestId : inputEvent.getHeaders().get("netconfPayloadRequestId"), exception);
            final String exceptionDetails = exception.getMessage();
            final String netconfResponse = "NetconfResponse";
            if (exceptionDetails != null && exceptionDetails.contains(netconfResponse)) {
                additionalInfo = AdditionalInfoBuilder.buildNetconfExecutionError(exceptionDetails
                        .substring(exceptionDetails.indexOf(netconfResponse), exceptionDetails.length()));
            } else {
                additionalInfo = AdditionalInfoBuilder.buildExecutionError(exceptionDetails);
            }
        } finally {
            updateResult(
                    netconfPayloadRequestPoId != null ? netconfPayloadRequestPoId
                            : (Long) inputEvent.getHeaders().get("netconfPayloadRequestPoId"),
                    isAnError, additionalInfo);
            Instant payloadExecutionEndTime = Instant.now();
            fileResource.deleteFile(handlerInputManager.getNetconfPayloadFileName());
            recordPayloadExecutionStatistics(isAnError, payloadExecutionEndTime);
        }

        return inputEvent;
    }


    private void recordPayloadExecutionStatistics(boolean isAnError, Instant payLoadExecutionStopTime) {
        Instant payLoadExecutionStartTime;
        
        final Map<String, Object> eventData = new HashMap<>();

        payLoadExecutionStartTime = persistenceService.getPayLoadExecutionStartTime(handlerInputManager.getNetconfPayloadRequestPoId());

        double nodeResponseTimeValue=aggregatedEditConfigResponseTime + moActionExecutionHandler.getResponseTime() + (handlerInputManager.hasGetConfig() ? getConfigResponseTime : 0) + getNetconfConfiguration.getResponseTime();

        eventData.put("NetworkElement", handlerInputManager.getNodeAddress());
        eventData.put("Platform", handlerInputManager.getNetconfModel());
        eventData.put("Duration", Duration.between(payLoadExecutionStartTime, payLoadExecutionStopTime).toMillis());
        eventData.put("Result", (!isAnError) ? "Success" : "Failure");        
        eventData.put("NodeResponseTime", nodeResponseTimeValue/1000);
        eventData.put("Edit-Config Requests", handlerInputManager.getRpcConfigEditBodys().size());
        eventData.put("Get-Config Requests", handlerInputManager.getRpcGetConfigBodys().size());
        eventData.put("Get Requests", handlerInputManager.hasGetConfig() ? 1 : 0);
        eventData.put("Action Requests", handlerInputManager.getActionBodys().size());
        eventData.put("Number of RPCs", handlerInputManager.getActionBodys().size() + handlerInputManager.getRpcConfigEditBodys().size() + handlerInputManager.getRpcGetConfigBodys().size() + (handlerInputManager.hasGetConfig() ? 1 : 0));

        logger.info("Netconf Payload execution statistics for request Id : {}, are : {}", handlerInputManager.getRequestId(), eventData);

        systemRecorder.recordEventData(CM_NBI_REST_NETCONF_PAYLOAD, eventData);
    }


    private boolean isParsingOk() {
        return handlerInputManager.isParseOk() && (!handlerInputManager.getRpcConfigEditBodys().isEmpty()
                || !handlerInputManager.getActionBodys().isEmpty()
                || !handlerInputManager.getRpcGetConfigBodys().isEmpty() || handlerInputManager.hasGetConfig());
    }

    private void executeEditConfigs(String nodeAddress, String requestId, final NetconfManager netconfManager)
            throws NetconfManagerException {

        if (!handlerInputManager.getRpcConfigEditBodys().isEmpty()) {
            String messageId = "";
            NetconfResponse editConfigResponse = null;
            final List<String> editConfigMessageIds = handlerInputManager.getEditConfigMessageIds();
            final List<String> rpcEditConfigBodys = handlerInputManager.getRpcConfigEditBodys();
            final List<Datastore> rpcEditConfigDatatores = handlerInputManager.getRpcEditConfigDataStores();

            for (int i = 0; i < rpcEditConfigBodys.size(); i++) {
                messageId = editConfigMessageIds.get(i);
                String editConfigBody = rpcEditConfigBodys.get(i);
                Datastore datastore = rpcEditConfigDatatores.get(i);
                logger.debug(
                        "Applying edit-config with messageId : {}, requestId : {}, Datastore : {}, on node : {}. Edit config body : {} ",
                        messageId, requestId, datastore, nodeAddress, editConfigBody);
                if (datastore.equals(Datastore.RUNNING)) {
                    Instant editConfigStartTime = Instant.now();
                    editConfigResponse = netconfManager.editConfig(datastore, editConfigBody);
                    Duration editConfigResponseDuration = Duration.between(editConfigStartTime, Instant.now());
                    aggregatedEditConfigResponseTime = aggregatedEditConfigResponseTime + editConfigResponseDuration.toMillis();
                    logger.debug("Edit config response received for messageId : {}, requestId : {} is : {} ", messageId,
                            requestId, editConfigResponse);
                } else {
                    editConfigResponse = netconfCandidateDatastoreHandler.write(netconfManager, editConfigBody);
                }
                if (editConfigResponse != null && editConfigResponse.isError()) {
                    throw new NetconfExecutionException(editConfigResponse.toString());
                }
            }
        } else {
            logger.info(
                    "Skipping edit configs execution as no edit configs part of netconf payload for request Id : {}",
                    handlerInputManager.getRequestId());
        }

    }

    private void executeMoAction(NetconfManager netconfManager, StringBuilder netconfConfiguration)
            throws NetconfManagerException {

        String actionResults = moActionExecutionHandler.executeMoAction(handlerInputManager, netconfManager);
        if (actionResults != null && actionResults.length() > 0) {
            netconfConfiguration.append(actionResults);
        }
    }

    private void executeGetConfigWithFilter(StringBuilder netconfConfiguration) throws NetconfManagerException {

        if (!handlerInputManager.getRpcGetConfigBodys().isEmpty()) {

            final String nodeConfigData = getNetconfConfiguration.getNetconfConfiguration(handlerInputManager);
            if (nodeConfigData != null) {
                netconfConfiguration.append(nodeConfigData);
            }
        } else {
            logger.info(
                    "Skipping get configs execution, may be error in edit configs/actions execution or no get configs in payload. request Id : {}",
                    handlerInputManager.getRequestId());
        }
    }

    private void executeGetConfig(final NetconfManager netconfManager, StringBuilder netconfConfiguration)
            throws NetconfManagerException {

        if (handlerInputManager.hasGetConfig()) {
            logger.debug("Applying get-config without filter , requestId : {}, on node : {}. ",
                    handlerInputManager.getRequestId(), handlerInputManager.getNodeAddress());
            Instant getConfigStartTime = Instant.now();
            NetconfResponse netconfResponse = netconfManager.getConfig(Datastore.RUNNING);
            getConfigResponseTime = Duration.between(getConfigStartTime, Instant.now()).toMillis();
            logger.debug("Completed executing get-config without filter , requestId : {}, on node : {}. ",
                    handlerInputManager.getRequestId(), handlerInputManager.getNodeAddress());
            if (netconfResponse != null && !netconfResponse.isError()) {
                netconfConfiguration.append(netconfResponse.getData());
            } else {
                logger.debug("Netconf execution failure for request Id : {}, netconf response : {}",
                        handlerInputManager.getRequestId(), netconfResponse);
                throw new NetconfExecutionException(netconfResponse != null ? netconfResponse.toString()
                        : "Netconf execution failure for request Id : " + handlerInputManager.getRequestId());
            }

        } else {
            logger.info(
                    "Skipping get config without filter execution, may be error in edit configs/actions/getconfigs execution or no get config in payload. request Id : {}",
                    handlerInputManager.getRequestId());
        }
    }

    private void writeNetconfConfigToFile(String nodeConfigData) {
        final String getConfigFilePath = join("/", GET_CONFIG_RESPONSES_FOLDER,
                handlerInputManager.getRequestId() + ".txt");

        if (getNetconfConfiguration.writeNetconfConfigurationFile(getConfigFilePath, nodeConfigData,
                handlerInputManager.getRequestId())) {
            additionalInfo = AdditionalInfoBuilder.buildFilePath(getConfigFilePath);
            logger.debug("updating additional info with path {} for request Id : {}", additionalInfo,
                    handlerInputManager.getRequestId());
        } else {
            throw new NetconfConfigFileWritingException(
                    "Failed to write get configuration data to file : " + getConfigFilePath);
        }

    }

    private void updateResult(final Long netconfPayloadRequestPoId, final boolean isAnError,
            final Map<String, Object> additionalInfo) {
        if (isAnError) {
            persistenceService.markExecutionFailure(netconfPayloadRequestPoId, additionalInfo);
        } else {
            persistenceService.markExecutionSuccess(netconfPayloadRequestPoId, additionalInfo);
        }
    }

    private void validateNetconfManagerStatus(final NetconfManager netconfManager) throws NetconfManagerException {
        if (netconfManager.getStatus() != NetconfConnectionStatus.CONNECTED) {
            final String errMessage = String.format("The Netconf Manager is not connected, current status: %s",
                    netconfManager.getStatus().toString());
            throw new NetconfManagerException(errMessage);
        }
    }

    /**
     * Callback method, will be called once handler is unloaded.
     */
    @Override
    public void destroy() {// Added Unimplemented method
    }
}
