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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.TypedEventInputHandler;
import com.ericsson.oss.mediation.cm.exceptions.AttributeNotFoundException;
import com.ericsson.oss.mediation.cm.persistence.NetconfPayloadRequestPersistenceService;
import com.ericsson.oss.mediation.cm.util.AdditionalInfoBuilder;
import com.ericsson.oss.mediation.cm.util.FileResource;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;

public abstract class AbstractNetconfHandler implements TypedEventInputHandler { // NOSONAR

    private static Logger logger = LoggerFactory.getLogger(AbstractNetconfHandler.class);

    private static final String CAPABILITY_REGEX = "<capability\\s*>(.*?)</capability\\s*>";
    private static final String HELLO_MESSAGE_REGEX = "<hello.*?>\\s*<capabilities\\s*>(.*?)</capabilities\\s*>\\s*</hello\\s*>\\s*]]>]]>";
    private static final String COMMENT_REGEX = "<!--.*?-->";
    public static final String UNKNOWN_EDIT_MESSAGE_ID_PREFIX = "message-id is unknown, the rpc edit-config message sequence is #";
    public static final String UNKNOWN_GET_MESSAGE_ID_PREFIX = "message-id is unknown, the rpc get-config message sequence is #";
    public static final String EDIT_CONFIG_DATASTORE_REGEX = "(<rpc.*?>)\\s*<edit-config.*?<target.*?>\\s*(.*?)\\s*</target\\s*>";
    private static final String RPC_GET_CONFIG_REGEX_ECIM = "(<rpc.*?>)\\s*<get-config.*?<source.*?>\\s*(.*?)\\s*</source\\s*>\\s*</get-config\\s*>";

    final List<String> rpcEditConfigMessageIds = new ArrayList<>();
    final List<String> rpcGetConfigMessageIds = new ArrayList<>();
    final List<String> rpcEditConfigBodys = new ArrayList<>();
    final List<String> rpcGetConfigBodys = new ArrayList<>();
    final List<Datastore> rpcEditConfigDatastores = new ArrayList<>();

    final List<String> rpcActionMessageIds = new ArrayList<>();
    final List<String> rpcActionBodys = new ArrayList<>();

    String formattedNetconPayload = null;
    String[] allRequiredCapabilities = null;
    String nodeName = null;
    String errorMessage = null;
    boolean isParseOk = false;
    String requestId = null;
    boolean hasGetConfig = false;

    @Inject
    private NetconfHandlerInputManager handlerInputManager;

    @Inject
    private NetconfPayloadRequestPersistenceService persistenceService;

    @Inject
    private FileResource fileResource;

    /**
     * Callback method, will be called once the handler is loaded and used to initialise attributes.
     *
     * @param context
     *            the event context
     */
    @Override
    public void init(final EventHandlerContext context) {
    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent inputEvent) {

        try {
            handlerInputManager.init(inputEvent.getHeaders());

            nodeName = handlerInputManager.getNodeName();
            requestId = handlerInputManager.getRequestId();
            Long netconfPayloadRequestPoId = handlerInputManager.getNetconfPayloadRequestPoId();
            logger.info("Started parsing the netconf payload with requestId : {} : for node : {} ", requestId, nodeName);

            final String netconPayloadFileName = handlerInputManager.getNetconfPayloadFileName();

            persistenceService.updateParsingStatus(netconfPayloadRequestPoId);
            String netconfPayload = fileResource.getFileContentAsString(netconPayloadFileName);
            formattedNetconPayload = netconfPayLoadCleanUp(netconfPayload);
            allRequiredCapabilities = getAllRequiredCapabilities(readCapabilities(formattedNetconPayload));
            readEditConfigRpcs();
            readGetConfigRpcs();
            readActionRpcs();
            readGetConfigWithoutFilter();
            isParseOk = true;
            logger.info("Completed parsing the netconf payload with requestId : {} : for node : {} ", requestId, nodeName);
        } catch (final AttributeNotFoundException e) {
            logger.error("Failed to initialize Handler Input Manager.", e);
            persistenceService.markExecutionFailure(handlerInputManager.getNetconfPayloadRequestPoId(), AdditionalInfoBuilder.buildExecutionError(e.getMessage()));
        } catch (final Exception e) {
            errorMessage = e.getMessage();
            logger.error("Failed to read the Netconf payload with exception.", e);
            persistenceService.markExecutionFailure(handlerInputManager.getNetconfPayloadRequestPoId(), AdditionalInfoBuilder.buildExecutionError(e.getMessage()));
        }

        logger.debug("Updating headers for requestId : {}, messageIds :{}, rpcEditConfigBodys : {}, rpcgetConfigBodys : {}, rpcEditConfigDatastores : {}, rpcActionBodys : {}  ", requestId,
                rpcEditConfigMessageIds, rpcEditConfigBodys, rpcGetConfigBodys, rpcEditConfigDatastores, rpcActionBodys);
        uploadResultIntoHeaders(isParseOk, allRequiredCapabilities, rpcEditConfigMessageIds, rpcEditConfigBodys, rpcGetConfigMessageIds, rpcGetConfigBodys, rpcEditConfigDatastores);

        return inputEvent;
    }

    public abstract void readEditConfigRpcs();

    public abstract void readGetConfigRpcs();

    public abstract void readActionRpcs();

    public abstract String getNetconfModel();

    protected void readGetConfigWithoutFilter() {
        Pattern getConfigPattern = Pattern.compile(RPC_GET_CONFIG_REGEX_ECIM);
        final Matcher getConfigMatcher = getConfigPattern.matcher(formattedNetconPayload);
        hasGetConfig = getConfigMatcher.find();
    }

    protected String netconfPayLoadCleanUp(final String fileText) {
        return fileText.replaceAll(COMMENT_REGEX, "").replaceAll("\r", "").replaceAll("\n", "");
    }

    protected String[] getAllRequiredCapabilities(final List<String> capabilities) {
        final Set<String> requiredCapabilities = new HashSet<>(DefaultCapabilities.getDefaultCapabilityUrns());
        requiredCapabilities.addAll(capabilities);
        return requiredCapabilities.toArray(new String[0]);
    }

    protected List<String> readCapabilities(final String content) {
        final List<String> capabilities = new ArrayList<>();
        final Pattern helloMessagePattern = Pattern.compile(HELLO_MESSAGE_REGEX);
        final Matcher helloMessageMatcher = helloMessagePattern.matcher(content);
        while (helloMessageMatcher.find()) {
            final Pattern capabelityPattern = Pattern.compile(CAPABILITY_REGEX);
            final Matcher capabelityMatcher = capabelityPattern.matcher(helloMessageMatcher.group(1));
            while (capabelityMatcher.find()) {
                capabilities.add(capabelityMatcher.group(1).trim());
            }
        }
        logger.debug("Capabilities read for requestId :{} from the netconf payload are : {}", requestId, capabilities);
        return capabilities;
    }

    protected void uploadResultIntoHeaders(final boolean isParseOk, final String[] allRequiredCapabilities, final List<String> rpcEditConfigMessageIds, final List<String> rpcEditConfigBodys,
            final List<String> rpcGetConfigMessageIds, final List<String> rpcGetConfigBodys, final List<Datastore> rpcEditConfigDataStores) {
        handlerInputManager.addParseResultToHeader(isParseOk);
        handlerInputManager.addRequiredCapabilitiesToHeader(allRequiredCapabilities);
        handlerInputManager.addEditConfigMessageIdsToHeader(rpcEditConfigMessageIds);
        handlerInputManager.addRpcEditConfigBodysToHeader(rpcEditConfigBodys);
        handlerInputManager.addGetConfigMessageIdsToHeader(rpcGetConfigMessageIds);
        handlerInputManager.addRpcGetConfigBodysToHeader(rpcGetConfigBodys);
        handlerInputManager.addRpcEditConfigDataStoresToHeader(rpcEditConfigDataStores);
        handlerInputManager.addActionBodysToHeader(rpcActionBodys);
        handlerInputManager.addActionMessageIdsToHeader(rpcActionMessageIds);
        handlerInputManager.addNetconfModel(getNetconfModel());
        handlerInputManager.addGetConfigPresence(hasGetConfig);

    }

    private enum DefaultCapabilities {
        NETCONF_BASE("urn:ietf:params:netconf:base:1.0"), NETCONF_CANDIDATE("urn:ietf:params:netconf:capability:candidate:1.0"), NETCONF_NOTIFICATION(
                "urn:ietf:params:netconf:capability:notification:1.0"), NETCONF_ROLLBACK("urn:ietf:params:netconf:capability:rollback-on-error:1.0"), NETCONF_VALIDATION(
                        "urn:ietf:params:netconf:capability:validate:1.0"), NETCONF_WRITABLE_RUNNING("urn:ietf:params:netconf:capability:writable-running:1.0"), XML_BASE(
                                "urn:ietf:params:xml:ns:netconf:base:1.0"), XML_CANDIDATE(
                                        "urn:ietf:params:xml:ns:netconf:capability:candidate:1.0"), XML_ROLLBACK("urn:ietf:params:xml:ns:netconf:capability:rollback-on-error:1.0"), XML_VALIDATION(
                                                "urn:ietf:params:xml:ns:netconf:capability:validate:1.0"), XML_WRITABLE_RUNNING(
                                                        "urn:ietf:params:xml:ns:netconf:capability:writable-running:1.0"), ERICSSON_ACTION("urn:ericsson:com:netconf:action:1.0"), ERICSSON_EBASE_1(
                                                                "urn:com:ericsson:ebase:0.1.0"), ERICSSON_EBASE_2("urn:com:ericsson:ebase:1.1.0"), ERICSSON_EBASE_3(
                                                                        "urn:com:ericsson:ebase:1.2.0"), ERICSSON_EBASE_4("urn:com:ericsson:ebase:2.0.0"), ERICSSON_HEARTBRAT(
                                                                                "urn:ericsson:com:netconf:heartbeat:1.0"), ERICSSON_NOTIFICATION(
                                                                                        "urn:ericsson:com:netconf:notification:1.1"), ERICSSON_OPERATION("urn:com:ericsson:netconf:operation:1.0");

        private String urn;
        private static List<DefaultCapabilities> defaultCapabilityList = Collections.unmodifiableList(Arrays.asList(values()));

        private DefaultCapabilities(final String urn) {
            this.urn = urn;
        }

        public String getUrn() {
            return urn;
        }

        public static List<String> getDefaultCapabilityUrns() {
            final List<String> urns = new ArrayList<>();
            for (final DefaultCapabilities capability : defaultCapabilityList) {
                urns.add(capability.getUrn());
            }
            return urns;
        }
    }

    /**
     * Callback method, will be called once handler is unloaded.
     */
    @Override
    public void destroy() {
    }

    protected static Datastore getDataStore(final String dataStore) {
        switch (dataStore) {
        case "running":
            return Datastore.RUNNING;
        case "candidate":
            return Datastore.CANDIDATE;
        default:
            return Datastore.RUNNING;
        }

    }

}
