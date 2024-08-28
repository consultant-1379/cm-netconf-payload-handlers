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
 * ParseYangNetconfFileHandler is used to parse the Netconf payload and build the RPC configs.
 */
@EventHandler
public class ParseYangNetconfPayloadHandler extends AbstractNetconfHandler {

    private static Logger logger = LoggerFactory.getLogger(ParseYangNetconfPayloadHandler.class);
    private static final String RPC_EDIT_CONFIG_REGEX_YANG = "(<rpc.*?>)\\s*<edit-config.*?<config.*?>\\s*(.*?)\\s*</config\\s*>\\s*</edit-config\\s*>\\s*</rpc\\s*>";
    private static final String RPC_GET_CONFIG_REGEX_YANG = "(<rpc.*?>)\\s*<get-config.*?<filter.*?>\\s*(.*?)\\s*</filter\\s*>\\s*</get-config\\s*>\\s*</rpc\\s*>";
    private static final String RPC_MESSAGE_ID_REGEX = "<rpc.*?message-id\\s*=\\s*\"(.*?)\".*?>";
    private static final String RPC_ACTION_REGEX = "(<rpc.*?>)\\s*<action.*?>\\s*(<target.*?>\\s*.*?\\s*</target\\s*>)?\\s*(.*?)\\s*</action\\s*>\\s*</rpc\\s*>";

    @Override
    public void readEditConfigRpcs() {
        Pattern yangEditConfigPattern = null;
        Pattern editConfigDatastorePattern = null;

        logger.debug("Reading yang edit configs from the netconf payload with request Id : {}.", requestId);
        yangEditConfigPattern = Pattern.compile(RPC_EDIT_CONFIG_REGEX_YANG);
        editConfigDatastorePattern = Pattern.compile(EDIT_CONFIG_DATASTORE_REGEX);

        final Matcher yangEditConfigMatcher = yangEditConfigPattern.matcher(formattedNetconPayload);
        int messageId = 1;
        while (yangEditConfigMatcher.find()) {
            final Pattern rpcMessageIdPattern = Pattern.compile(RPC_MESSAGE_ID_REGEX);
            final Matcher rpcMessageIdMatcher = rpcMessageIdPattern.matcher(yangEditConfigMatcher.group(1).trim());
            final Matcher editConfigDatastoreMatcher = editConfigDatastorePattern.matcher(yangEditConfigMatcher.group(0).trim());
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

            rpcEditConfigBodys.add(yangEditConfigMatcher.group(2).trim());
            messageId++;
        }
    }

    @Override
    public void readGetConfigRpcs() {

        Pattern yangGetConfigPattern = Pattern.compile(RPC_GET_CONFIG_REGEX_YANG);
        final Matcher yangGetConfigMatcher = yangGetConfigPattern.matcher(formattedNetconPayload);
        int messageId = 1;

        while (yangGetConfigMatcher.find()) {
            final Pattern yangGetConfigMessageIdPattern = Pattern.compile(RPC_MESSAGE_ID_REGEX);
            final Matcher yangGetConfigMessageIdMatcher = yangGetConfigMessageIdPattern.matcher(yangGetConfigMatcher.group(1).trim());
            if (yangGetConfigMessageIdMatcher.find()) {
                rpcGetConfigMessageIds.add(yangGetConfigMessageIdMatcher.group(1).trim());
            } else {
                rpcGetConfigMessageIds.add(UNKNOWN_GET_MESSAGE_ID_PREFIX + messageId);
            }
            rpcGetConfigBodys.add(yangGetConfigMatcher.group(2).trim());

            messageId++;
        }
    }

    @Override
    public void readActionRpcs() {
        Pattern actionPattern = Pattern.compile(RPC_ACTION_REGEX);
        final Matcher actionMatcher = actionPattern.matcher(formattedNetconPayload);
        int messageId = 1;

        while (actionMatcher.find()) {
            final Pattern messageIdPattern = Pattern.compile(RPC_MESSAGE_ID_REGEX);
            final Matcher messageIdMatcher = messageIdPattern.matcher(actionMatcher.group(1).trim());
            if (messageIdMatcher.find()) {
                rpcActionMessageIds.add(messageIdMatcher.group(1).trim());
            } else {
                rpcActionMessageIds.add(UNKNOWN_GET_MESSAGE_ID_PREFIX + messageId);
            }
            rpcActionBodys.add(actionMatcher.group(3).trim());

            messageId++;
        }
    }

    @Override
    public String getNetconfModel() {
        return "YANG";
    }
}
