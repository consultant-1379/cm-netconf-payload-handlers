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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;

/**
 * The Class ParseEcimNetconfFileHandler is used to parse the Netconf paylod and build the RPC configs.
 */
@EventHandler
public class ParseEcimNetconfPayloadHandler extends AbstractNetconfHandler {

    private static Logger logger = LoggerFactory.getLogger(ParseEcimNetconfPayloadHandler.class);

    private static final String MANAGED_ELEMENT_ID_REGEX = "<managedElementId\\s*>\\s*1\\s*</managedElementId\\s*>";
    private static final String MANAGED_ELEMENT_REGEX = "ManagedElement=1";
    private static final String RPC_EDIT_CONFIG_REGEX_ECIM = "(<rpc.*?>)\\s*<edit-config.*?<config.*?>\\s*(<ManagedElement.*?</ManagedElement\\s*>)\\s*</config\\s*>\\s*</edit-config\\s*>\\s*</rpc\\s*>";
    private static final String RPC_MESSAGE_ID_REGEX = "<rpc.*?message-id\\s*=\\s*\"(.*?)\".*?>";
    private static final String RPC_GET_CONFIG_WITH_FILTER_REGEX_ECIM = "(<rpc.*?>)\\s*<get-config.*?<filter.*?>\\s*(<ManagedElement.*?</ManagedElement\\s*>)\\s*</filter\\s*>\\s*</get-config\\s*>\\s*</rpc\\s*>";
    private static final String RPC_ACTION_REGEX_ECIM = "(<rpc.*?>)\\s*<action.*?(<data>\\s*<ManagedElement.*?</ManagedElement\\s*>\\s*</data\\s*>)\\s*</action\\s*>";

    @Override
    public void readEditConfigRpcs() {
        Pattern ecimEditConfigPattern = null;
        Pattern editConfigDatastorePattern = null;

        logger.debug("Reading Ecim edit configs from the netconf payload with request Id : {}.", requestId);
        ecimEditConfigPattern = Pattern.compile(RPC_EDIT_CONFIG_REGEX_ECIM);
        editConfigDatastorePattern = Pattern.compile(EDIT_CONFIG_DATASTORE_REGEX);

        final Matcher ecimEditConfigMatcher = ecimEditConfigPattern.matcher(formattedNetconPayload);
        int messageId = 1;
        final String managedElementId = "<managedElementId>" + nodeName + "</managedElementId>";
        final String managedElement = "ManagedElement=" + nodeName;
        while (ecimEditConfigMatcher.find()) {
            final Pattern rpcMessageIdPattern = Pattern.compile(RPC_MESSAGE_ID_REGEX);
            final Matcher rpcMessageIdMatcher = rpcMessageIdPattern.matcher(ecimEditConfigMatcher.group(1).trim());
            final Matcher editConfigDatastoreMatcher = editConfigDatastorePattern.matcher(ecimEditConfigMatcher.group(0).trim());
            Datastore editConfigDataStore = null;
            if (editConfigDatastoreMatcher.find()) {
                editConfigDataStore = getDataStore(editConfigDatastoreMatcher.group(2).replaceAll("[</>,^\\s]*", ""));
            }

            if (rpcMessageIdMatcher.find()) {
                rpcEditConfigMessageIds.add(rpcMessageIdMatcher.group(1).trim());
            } else {
                rpcEditConfigMessageIds.add(UNKNOWN_EDIT_MESSAGE_ID_PREFIX + messageId);
            }
            rpcEditConfigDatastores.add(editConfigDataStore != null ? editConfigDataStore : getDataStore("default"));
            rpcEditConfigBodys.add(ecimEditConfigMatcher.group(2).trim().replaceAll(MANAGED_ELEMENT_ID_REGEX, managedElementId).replaceAll(MANAGED_ELEMENT_REGEX, managedElement));
            messageId++;
        }
    }

    @Override
    public void readGetConfigRpcs() {

        Pattern ecimGetConfigPattern = Pattern.compile(RPC_GET_CONFIG_WITH_FILTER_REGEX_ECIM);
        final Matcher ecimGetConfigMatcher = ecimGetConfigPattern.matcher(formattedNetconPayload);

        int messageId = 1;
        final String managedElementId = "<managedElementId>" + nodeName + "</managedElementId>";
        final String managedElement = "ManagedElement=" + nodeName;
        while (ecimGetConfigMatcher.find()) {
            final Pattern ecimGetMessageIdPattern = Pattern.compile(RPC_MESSAGE_ID_REGEX);
            final Matcher ecimGetMessageIdMatcher = ecimGetMessageIdPattern.matcher(ecimGetConfigMatcher.group(1).trim());
            if (ecimGetMessageIdMatcher.find()) {
                rpcGetConfigMessageIds.add(ecimGetMessageIdMatcher.group(1).trim());
            } else {
                rpcGetConfigMessageIds.add(UNKNOWN_GET_MESSAGE_ID_PREFIX + messageId);
            }
            rpcGetConfigBodys.add(ecimGetConfigMatcher.group(2).trim().replaceAll(MANAGED_ELEMENT_ID_REGEX, managedElementId).replaceAll(MANAGED_ELEMENT_REGEX, managedElement));
            messageId++;
        }
    }

    @Override
    public void readActionRpcs() {

        Pattern actionPattern = Pattern.compile(RPC_ACTION_REGEX_ECIM);
        final Matcher actionMatcher = actionPattern.matcher(formattedNetconPayload);

        int messageId = 1;
        final String managedElementId = "<managedElementId>" + nodeName + "</managedElementId>";
        final String managedElement = "ManagedElement=" + nodeName;
        while (actionMatcher.find()) {
            final Pattern messageIdPattern = Pattern.compile(RPC_MESSAGE_ID_REGEX);
            final Matcher messageIdMatcher = messageIdPattern.matcher(actionMatcher.group(1).trim());
            if (messageIdMatcher.find()) {
                rpcActionMessageIds.add(messageIdMatcher.group(1).trim());
            } else {
                rpcActionMessageIds.add(UNKNOWN_GET_MESSAGE_ID_PREFIX + messageId);
            }
            rpcActionBodys.add(actionMatcher.group(2).trim().replaceAll(MANAGED_ELEMENT_ID_REGEX, managedElementId).replaceAll(MANAGED_ELEMENT_REGEX, managedElement));
            messageId++;
        }
    }

    @Override
    public String getNetconfModel() {
        return "ECIM";
    }
}
