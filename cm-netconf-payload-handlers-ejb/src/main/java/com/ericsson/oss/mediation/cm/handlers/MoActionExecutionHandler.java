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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.cm.exceptions.NetconfExecutionException;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;

@Stateless
public class MoActionExecutionHandler {

    private static Logger logger = LoggerFactory.getLogger(MoActionExecutionHandler.class);
    private long aggregatedResponseTime;

    public String executeMoAction(final NetconfHandlerInputManager handlerInputManager, final NetconfManager netconfManager) throws NetconfManagerException {

        if (!handlerInputManager.getActionBodys().isEmpty()) {

            String messageId = "";
            NetconfResponse actionResponse = null;
            final List<String> actionMessageIds = handlerInputManager.getActionMessageIds();
            final List<String> rpcActionBodys = handlerInputManager.getActionBodys();
            StringBuilder actionResults = new StringBuilder();
            String netconfModel = handlerInputManager.getNetconfModel();
            String nodeNamespace = getNameSpace(netconfModel);

            for (int i = 0; i < rpcActionBodys.size(); i++) {
                messageId = actionMessageIds.get(i);
                String actionBody = rpcActionBodys.get(i);

                logger.debug("Applying action with messageId : {}, requestId : {},namespace : {} on node : {}. action body : {} ", messageId, handlerInputManager.getRequestId(), nodeNamespace,
                        handlerInputManager.getNodeAddress(), actionBody);
                Instant startTime = Instant.now();
                if ("YANG".equalsIgnoreCase(netconfModel)) {
                    actionResponse = netconfManager.action(nodeNamespace, actionBody, "result", "return-value", "certificate-signing-request");
                } else {
                    actionResponse = netconfManager.action(nodeNamespace, actionBody, "data");
                }
                Duration responseTime = Duration.between(startTime, Instant.now());
                aggregatedResponseTime = aggregatedResponseTime + responseTime.toMillis();
                logger.debug("MO Action response received for messageId : {}, requestId : {} is : {} ", messageId, handlerInputManager.getRequestId(), actionResponse);
                actionResults.append(actionResponse.getData());

                if (actionResponse.isError()) {
                    throw new NetconfExecutionException(actionResponse.toString());
                }
            }
            return actionResults.toString();

        } else {
            logger.info("Skipping MO Actions execution, may be error in edit configs execution or no edit configs in payload. request Id : {}", handlerInputManager.getRequestId());
        }
        return null;
    }

    public long getResponseTime() {
        return aggregatedResponseTime;
    }

    private String getNameSpace(String netconfModel) {
        if (netconfModel.equalsIgnoreCase("ECIM")) {
            return "urn:com:ericsson:ecim:1.0";
        } else {
            return "urn:ietf:params:xml:ns:yang:1";
        }
    }

}
