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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.mediation.cm.exceptions.AttributeNotFoundException;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;

/**
 * The Class NetconfHandlerInputManager is used to handle the input event headers.
 */
public class NetconfHandlerInputManager {

    protected static final String NODE_ADDRESS_ATTR = "nodeAddress";
    protected static final String EDIT_CONFIG_MESSAGE_IDS_ATTR = "editConfigMessageIds";
    protected static final String GET_CONFIG_MESSAGE_IDS_ATTR = "getConfigMessageIds";
    protected static final String NETCONF_CAPABILITIES_ATTR = "netconfCapabilities";
    protected static final String NETCONF_MANAGER_ATTR = "netconfManager";
    protected static final String IS_PARSE_OK_ATTR = "isParseOk";
    protected static final String RPC_EDIT_CONFIG_BODYS_ATTR = "rpcEditConfigBodys";
    protected static final String RPC_GET_CONFIG_BODYS_ATTR = "rpcGetConfigBodys";
    protected static final String RPC_EDIT_CONFIG_DATASTORE_ATTR = "rpcEditConfigDataStore";
    protected static final String RPC_GET_CONFIG_DATASTORE_ATTR = "rpcGetConfigDataStore";
    protected static final String RPC_ACTION_BODYS_ATTR = "rpcActionBodys";
    protected static final String ACTION_MESSAGE_IDS_ATTR = "actionMessageIds";

    protected static final String REQUEST_ID = "netconfPayloadRequestId";
    protected static final String NETCONF_SESSION_OPERATIONS_STATUS_ATTR = "netconfSessionOperationsStatus";
    protected static final String NETCONF_PAYLOAD = "cmNbiNetconfPayload";
    protected static final String NETCONF_PAYLOAD_REQUEST_PO_ID = "netconfPayloadRequestPoId";
    protected static final String NETCONF_MODEL = "netconfModelType";
    protected static final String HAS_GET_CONFIG_ATTR = "hasGetConfig";

    private String nodeAddress;
    private String nodeName;
    private String requestId;

    private Map<String, Object> headers;
    private List<String> editMessageIds;
    private List<String> getMessageIds;
    private List<String> rpcEditBodys;
    private List<String> rpcGetBodys;
    private List<String> rpcActionBodys;
    private List<String> actionMessageIds;
    private List<Datastore> rpcEditDataStore;

    private NetconfManager netconfManager;
    private boolean isParseOk = false;
    private String netconfPayload;
    private Long netconfPayloadRequestPoId;
    private String netconfModel;
    private boolean hasGetConfig = false;

    /**
     * Initialize method used to retrieve input attributes in the input event headers , needs to be invoked first before getting any attribute
     *
     * @param inputEventHeaders
     * @param operationType
     *            netconf operation type
     */
    public void init(final Map<String, Object> inputEventHeaders) {
        this.headers = inputEventHeaders;
        nodeAddress = getAttribute(NODE_ADDRESS_ATTR, true);
        netconfPayload = getAttribute(NETCONF_PAYLOAD, true);
        netconfPayloadRequestPoId = getAttribute(NETCONF_PAYLOAD_REQUEST_PO_ID, true);
        nodeName = retrieveNodeName(nodeAddress);
        netconfManager = getAttribute(NETCONF_MANAGER_ATTR, false);
        requestId = getAttribute(REQUEST_ID, false);
        rpcEditBodys = getAttribute(RPC_EDIT_CONFIG_BODYS_ATTR, false);
        rpcGetBodys = getAttribute(RPC_GET_CONFIG_BODYS_ATTR, false);
        editMessageIds = getAttribute(EDIT_CONFIG_MESSAGE_IDS_ATTR, false);
        getMessageIds = getAttribute(GET_CONFIG_MESSAGE_IDS_ATTR, false);
        rpcEditDataStore = getAttribute(RPC_EDIT_CONFIG_DATASTORE_ATTR, false);
        actionMessageIds = getAttribute(ACTION_MESSAGE_IDS_ATTR, false);
        rpcActionBodys = getAttribute(RPC_ACTION_BODYS_ATTR, false);
        netconfModel = getAttribute(NETCONF_MODEL, false);
        hasGetConfig = (getAttribute(HAS_GET_CONFIG_ATTR, false) != null ? getAttribute(HAS_GET_CONFIG_ATTR, false) : hasGetConfig);

        isParseOk = (getAttribute(IS_PARSE_OK_ATTR, false) != null ? getAttribute(IS_PARSE_OK_ATTR, false) : isParseOk);
    }

    public String getNetconfPayloadFileName() {
        return netconfPayload;
    }

    public String getNetconfModel() {
        return netconfModel;
    }

    public Long getNetconfPayloadRequestPoId() {
        return netconfPayloadRequestPoId;
    }

    /**
     * @return the node address
     */
    public String getNodeAddress() {
        return nodeAddress;
    }

    /**
     * @return the node name
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @return the requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * @return the RPC message IDs
     */
    public List<String> getEditConfigMessageIds() {
        final List<String> editMessageIdsCopy = new ArrayList<>();
        editMessageIdsCopy.addAll(editMessageIds);
        return editMessageIdsCopy;
    }

    /**
     * @return the RPC message IDs
     */
    public List<String> getGetConfigMessageIds() {
        final List<String> getConfigMessageIdsCopy = new ArrayList<>();
        getConfigMessageIdsCopy.addAll(getMessageIds);
        return getConfigMessageIdsCopy;
    }

    /**
     * @return the RPC edit-config bodys
     */
    public List<String> getRpcConfigEditBodys() {
        final List<String> rpcEditBodysCopy = new ArrayList<>();
        rpcEditBodysCopy.addAll(rpcEditBodys);
        return rpcEditBodysCopy;
    }

    /**
     * @return the RPC get-config bodys
     */
    public List<String> getRpcGetConfigBodys() {
        final List<String> rpcGetConfigBodysCopy = new ArrayList<>();
        rpcGetConfigBodysCopy.addAll(rpcGetBodys);
        return rpcGetConfigBodysCopy;
    }

    /**
     * @return the RPC edit-config datastores
     */
    public List<Datastore> getRpcEditConfigDataStores() {
        final List<Datastore> rpcEditConfigDataStoresCopy = new ArrayList<>();
        rpcEditConfigDataStoresCopy.addAll(rpcEditDataStore);
        return rpcEditConfigDataStoresCopy;
    }

    public List<String> getActionBodys() {
        final List<String> rpcActionBodysCopy = new ArrayList<>();
        rpcActionBodysCopy.addAll(rpcActionBodys);
        return rpcActionBodysCopy;
    }

    public List<String> getActionMessageIds() {
        final List<String> actionMessageIdsCopy = new ArrayList<>();
        actionMessageIdsCopy.addAll(actionMessageIds);
        return actionMessageIdsCopy;
    }

    public boolean hasGetConfig() {
        return hasGetConfig;
    }

    /**
     * @return the netconfManager
     */
    public NetconfManager getNetconfManager() {
        return netconfManager;
    }

    /**
     * @return is the Netconf file parsing OK
     */
    public boolean isParseOk() {
        return isParseOk;
    }

    /**
     * Add the required capabilities into the event headers
     *
     * @param requiredCapabilities
     */
    public void addRequiredCapabilitiesToHeader(final String[] requiredCapabilities) {
        headers.put(NETCONF_CAPABILITIES_ATTR, requiredCapabilities);
    }

    /**
     * Put the Get Message IDs into the event headers
     *
     * @param messageIds
     */
    public void addGetConfigMessageIdsToHeader(final List<String> getMessageIds) {
        headers.put(GET_CONFIG_MESSAGE_IDS_ATTR, getMessageIds);
    }

    /**
     * Put the Edit Message IDs into the event headers
     *
     * @param messageIds
     */
    public void addEditConfigMessageIdsToHeader(final List<String> editMessageIds) {
        headers.put(EDIT_CONFIG_MESSAGE_IDS_ATTR, editMessageIds);
    }

    /**
     * Put the RPC edit-config bodys into the event headers
     *
     * @param rpcBodys
     */
    public void addRpcEditConfigBodysToHeader(final List<String> rpcEditBodys) {
        headers.put(RPC_EDIT_CONFIG_BODYS_ATTR, rpcEditBodys);
    }

    /**
     * Put the RPC edit-config bodys into the event headers
     *
     * @param rpcBodys
     */
    public void addRpcGetConfigBodysToHeader(final List<String> rpcGetBodys) {
        headers.put(RPC_GET_CONFIG_BODYS_ATTR, rpcGetBodys);
    }

    /**
     * Put the RPC edit-config datastores into the event headers
     *
     * @param rpcDatatores
     */
    public void addRpcEditConfigDataStoresToHeader(final List<Datastore> editConfigDataStores) {
        headers.put(RPC_EDIT_CONFIG_DATASTORE_ATTR, editConfigDataStores);
    }

    /**
     * Put the Netconf file parsing result into the event headers
     *
     * @param isParseOk
     *            true if finish parsing the Netconf file
     */
    public void addParseResultToHeader(final boolean isParseOk) {
        headers.put(IS_PARSE_OK_ATTR, isParseOk);
    }

    public void addActionBodysToHeader(final List<String> rpcActionBodys) {
        headers.put(RPC_ACTION_BODYS_ATTR, rpcActionBodys);
    }

    public void addActionMessageIdsToHeader(final List<String> actionMessageIds) {
        headers.put(ACTION_MESSAGE_IDS_ATTR, actionMessageIds);
    }

    public void addNetconfModel(final String netconfModel) {
        headers.put(NETCONF_MODEL, netconfModel);
    }

    public void addGetConfigPresence(final boolean hasGetConfig) {
        headers.put(HAS_GET_CONFIG_ATTR, hasGetConfig);
    }

    /**
     * @return the node Netconf configuration file with full path
     */

    /**
     * Helper method used to retrieve the input attributes
     *
     * @param attributeName
     * @param isMandatory
     * @return attributeValue
     */
    private <T> T getAttribute(final String attributeName, final boolean isMandatory) {
        @SuppressWarnings("unchecked")
        final T attributeValue = (T) headers.get(attributeName);
        if (isMandatory && (attributeValue == null || (attributeValue instanceof String && ((String) attributeValue).trim().isEmpty()))) {
            throw new AttributeNotFoundException(String.format("Attribute %s is invalid: %s", attributeName, attributeValue));
        }

        return attributeValue;
    }

    private String retrieveNodeName(final String nodeAddress) {
        final String[] tokens = nodeAddress.split(",");

        return tokens[tokens.length - 1].split("=")[1];
    }
}
