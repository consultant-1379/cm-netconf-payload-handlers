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

import java.nio.file.Paths

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.event.ComponentEvent
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.sdk.resources.Resource
import com.ericsson.oss.itpf.sdk.resources.Resources
import com.ericsson.oss.mediation.cm.exceptions.AttributeNotFoundException
import com.ericsson.oss.mediation.cm.persistence.NetconfPayloadRequestPersistenceService

public class ParseEcimNetconfPayloadHandlerSpec extends CdiSpecification {

    @ObjectUnderTest
    private ParseEcimNetconfPayloadHandler handler

    @MockedImplementation
    private ComponentEvent mockComponentEvent

    @MockedImplementation
    private EventHandlerContext mockEct

    @MockedImplementation
    private NetconfPayloadRequestPersistenceService service

    @Inject
    private NetconfHandlerInputManager handlerInputManager

    private static final String MOCK_NETCONF_FILE_1 = "/ecimNetconfConfigurationFile1.xml"
    private static final String MOCK_NETCONF_FILE_2 = "/ecimNetconfConfigurationFile2.xml"
    private static final String MOCK_NETCONF_FILE_3 = "/ecimNetconfConfigurationFile3.xml"
    private static final String MOCK_NETCONF_FILE_4 = "/ecimNetconfConfigurationFile4.xml"
    private static final String MOCK_NETCONF_FILE_GET_CONFIG = "/ecimNetconfGetConfigurationFile.xml"
    private static final String NON_EXIST_NETCONF_FILE = "/ecimNetconfConfigurationFileNonExist.xml"
    private static final String NODE_ADDRESS = "SubNetwork=LTE01dg2ERBS00008,ManagedElement=LTE01dg2ERBS00008"
    private static final long   NETCONF_PAYLOAD_REQUEST_PO_ID = 1003L;
    private static final String EXPECTED_MESSAGE_ID = "1"
    private static final String EXPECTED_AUTO_GEN_MESSAGE_ID = "message-id is unknown, the rpc edit-config message sequence is #1"
    private static final String NODE_FDN_ATTR = "ecimNodeFdn";
    private static final String NODE_ADDRESS_ATTR = "nodeAddress";
    private static final String CM_NBI_NETCONF_PAYLOAD = "cmNbiNetconfPayload";
    private static final String NODE_FDN = "Project=RadioNodeECTValidSEValidSB,Node=LTE01dg2ERBS00008"

    private static final String eventHeaderFileContent = "<!--This xml is automatically generated from the ECT Basic Template generator. --> <!--It will contains the RPC's required to unlock the LTE cells. --> <!--It's generated for MOM version: 18.Q4 MTR CXP2020013/1-R57A20 (UP: CXP9024418/6-R54A162). --> <hello xmlns='urn:ietf:params:xml:ns:netconf:base:1.0'> <capabilities> <capability>urn:ietf:params:netconf:base:1.0</capability> <capability>urn:com:ericsson:ebase:2.1.0</capability> </capabilities> </hello> ]]>]]> <rpc xmlns='urn:ietf:params:xml:ns:netconf:base:1.0' message-id='1'> <edit-config> <target> <running /> </target> <config> <ManagedElement xmlns='urn:com:ericsson:ecim:ComTop'> <managedElementId>1</managedElementId> <SystemFunctions> <systemFunctionsId>1</systemFunctionsId> <Lm xmlns='urn:com:ericsson:ecim:RcsLM'> <lmId>1</lmId> <fingerprint>Site1</fingerprint> </Lm> </SystemFunctions> </ManagedElement> </config> </edit-config> </rpc> ]]>]]> <rpc xmlns='urn:ietf:params:xml:ns:netconf:base:1.0' message-id='Close Session'> <close-session /> </rpc> ]]>]]> ";

    private final Map<String, Object> inputEventHeaders = new HashMap()

    def setup() {
        handler.init(mockEct)
    }

    public void "Read capabilities within default capability list from input event headers"() {
        given: "Init handler"
        def localFilePath = Paths.get(getClass().getResource(MOCK_NETCONF_FILE_1).toURI()).toString()
        String fileContents = new File(localFilePath).text
        initHeaders(fileContents, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        handlerInputManager.init(inputEventHeaders)

        when:
        def cleanText = handler.netconfPayLoadCleanUp(fileContents)
        def capabilitySize = handler.getAllRequiredCapabilities(handler.readCapabilities(cleanText)).size()
        handler.destroy()

        then:
        capabilitySize == DefaultCapabilities.getDefaultCapabilityUrns().size()
        notThrown(Exception)
    }

    public void "Verify the ApplyNetconfFileEvent could be handled correctly"() {
        given: "Init handler"
        def localFilePath = Paths.get(getClass().getResource(MOCK_NETCONF_FILE_1).toURI()).toString()
        String fileContents = new File(localFilePath).text
        initHeaders(fileContents, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        handler.onEvent(mockComponentEvent)
        handler.destroy()

        then:
        notThrown(Exception)
    }

    public void "Get extended capabilities beyond default capability list from input file path"() {
        given: "Init handler"
        def localFilePath = Paths.get(getClass().getResource(MOCK_NETCONF_FILE_2).toURI()).toString()
        String fileContents = new File(localFilePath).text
        initHeaders(fileContents, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        handlerInputManager.init(inputEventHeaders)

        when:
        def cleanText = handler.netconfPayLoadCleanUp(readFile(localFilePath))
        def configCapabilitySize = handler.readCapabilities(cleanText).size()
        def requiredCapabilitySize = handler.getAllRequiredCapabilities(handler.readCapabilities(cleanText)).size()
        handler.destroy()

        then:
        configCapabilitySize == 2
        requiredCapabilitySize == DefaultCapabilities.getDefaultCapabilityUrns().size() + 1
        notThrown(Exception)
    }

    public void "Get extended capabilities beyond default capability list from event headers"() {
        given: "Init handler"
        initHeaders(eventHeaderFileContent, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        handlerInputManager.init(inputEventHeaders)

        when:
        def cleanText = handler.netconfPayLoadCleanUp(eventHeaderFileContent)
        def configCapabilitySize = handler.readCapabilities(cleanText).size()
        def requiredCapabilitySize = handler.getAllRequiredCapabilities(handler.readCapabilities(cleanText)).size()
        handler.destroy()

        then:
        configCapabilitySize == 2
        requiredCapabilitySize == DefaultCapabilities.getDefaultCapabilityUrns().size() + 1
        notThrown(Exception)
    }


    public void "AttributeNotFoundException is captured and handled correctly"() {
        given: "Init handler"
        initHeaders(null, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        handler.onEvent(mockComponentEvent)

        then:
        0 * service.updateParsingStatus(_ as Long)
        notThrown(AttributeNotFoundException)
    }

    public void "Read RPCs and generate message IDs automatically from the Netconf configuration file"() {
        given: "Init handler"
        def localFilePath = Paths.get(getClass().getResource(MOCK_NETCONF_FILE_1).toURI()).toString()
        initHeaders(localFilePath, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        ComponentEvent outputFromOnEvent = handler.onEvent(mockComponentEvent)
        Map<String, Object> eventHeaders = outputFromOnEvent.getHeaders()
        handlerInputManager.init(eventHeaders)
        println "----------------rpcGetConfigBodys"+eventHeaders.get("rpcGetConfigBodys")
        println "----------------rpcEditConfigDataStore"+eventHeaders.get("rpcEditConfigDataStore")
        println "----------------getConfigMessageIds:"+eventHeaders.get("getConfigMessageIds")
        println "----------------actionMessageIds:"+eventHeaders.get("actionMessageIds")
        println "----------------editConfigMessageIds:"+eventHeaders.get("editConfigMessageIds")

        then:
        eventHeaders.get("editConfigMessageIds").size() == 3
        eventHeaders.get("editConfigMessageIds")[0] == EXPECTED_AUTO_GEN_MESSAGE_ID
        eventHeaders.get("rpcEditConfigBodys").size() == 3
        eventHeaders.get("rpcActionBodys").size() == 2
        eventHeaders.get("getConfigMessageIds").size() == 2
        eventHeaders.get("rpcGetConfigBodys").size() == 2
        eventHeaders.get("hasGetConfig") == true
        handlerInputManager.getActionBodys() != null
        handlerInputManager.getActionMessageIds() != null
        handlerInputManager.getConfigMessageIds != null
        handlerInputManager.getNetconfModel() == "ECIM"
        notThrown(Exception)
    }

    public void "Read RPCs and generate message IDs automatically from the Netconf Get configuration file"() {
        given: "Init handler"
        def localFilePath = Paths.get(getClass().getResource(MOCK_NETCONF_FILE_GET_CONFIG).toURI()).toString()
        initHeaders(localFilePath, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        ComponentEvent outputFromOnEvent = handler.onEvent(mockComponentEvent)
        Map<String, Object> eventHeaders = outputFromOnEvent.getHeaders()
        println "----------------rpcGetConfigBodys"+eventHeaders.get("rpcGetConfigBodys")
        println "----------------getConfigMessageIds:"+eventHeaders.get("getConfigMessageIds")

        then:
        eventHeaders.get("getConfigMessageIds").size() == 2
        eventHeaders.get("rpcGetConfigBodys").size() == 2
        eventHeaders.get("hasGetConfig") == true
        notThrown(Exception)
    }

    public void "Parse non-hello and non-well-formated Netconf Configuration File correctly, and ignore non-editConfig RPCs"() {
        given: "Init handler"
        def localFilePath = Paths.get(getClass().getResource(MOCK_NETCONF_FILE_1).toURI()).toString()
        initHeaders(localFilePath, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        ComponentEvent outputFromOnEvent = handler.onEvent(mockComponentEvent)
        Map<String, Object> eventHeaders = outputFromOnEvent.getHeaders()

        then:
        eventHeaders.get("editConfigMessageIds").size() == 3
        eventHeaders.get("editConfigMessageIds")[0] == EXPECTED_AUTO_GEN_MESSAGE_ID
        eventHeaders.get("rpcEditConfigBodys").size() == 3
        eventHeaders.get("netconfCapabilities").size() == DefaultCapabilities.getDefaultCapabilityUrns().size()
        notThrown(Exception)
    }

    public void "Parse multiple sessions Netconf Configuration File correctly"() {
        given: "Init handler"
        def localFilePath = Paths.get(getClass().getResource(MOCK_NETCONF_FILE_4).toURI()).toString()
        initHeaders(localFilePath, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        ComponentEvent outputFromOnEvent = handler.onEvent(mockComponentEvent)
        Map<String, Object> eventHeaders = outputFromOnEvent.getHeaders()

        then:
        eventHeaders.get("editConfigMessageIds").size() == 2
        eventHeaders.get("rpcEditConfigBodys").size() == 2
        eventHeaders.get("netconfCapabilities").size() == DefaultCapabilities.getDefaultCapabilityUrns().size() + 2
        notThrown(Exception)
    }

    public void "FileNotFoundException is captured and handled correctly"() {
        given: "Init handler"
        initHeaders(NON_EXIST_NETCONF_FILE, NODE_ADDRESS, NODE_FDN, NETCONF_PAYLOAD_REQUEST_PO_ID)
        mockComponentEvent.getHeaders() >> inputEventHeaders

        when:
        handler.onEvent(mockComponentEvent)

        then:
        notThrown(FileNotFoundException)
    }

    private void initHeaders(final String fileText, final String nodeAddress, final String apNodeFdn, final long netconfPayloadRequestPoId) {
        inputEventHeaders.put("cmNbiNetconfPayload", fileText)
        inputEventHeaders.put("nodeAddress", nodeAddress)
        inputEventHeaders.put(NODE_FDN_ATTR, apNodeFdn)
        inputEventHeaders.put("netconfPayloadRequestPoId", netconfPayloadRequestPoId)
    }

    private enum DefaultCapabilities {
        NETCONF_BASE("urn:ietf:params:netconf:base:1.0"),
        NETCONF_CANDIDATE("urn:ietf:params:netconf:capability:candidate:1.0"),
        NETCONF_NOTIFICATION("urn:ietf:params:netconf:capability:notification:1.0"),
        NETCONF_ROLLBACK("urn:ietf:params:netconf:capability:rollback-on-error:1.0"),
        NETCONF_VALIDATION("urn:ietf:params:netconf:capability:validate:1.0"),
        NETCONF_WRITABLE_RUNNING("urn:ietf:params:netconf:capability:writable-running:1.0"),
        XML_BASE("urn:ietf:params:xml:ns:netconf:base:1.0"),
        XML_CANDIDATE("urn:ietf:params:xml:ns:netconf:capability:candidate:1.0"),
        XML_ROLLBACK("urn:ietf:params:xml:ns:netconf:capability:rollback-on-error:1.0"),
        XML_VALIDATION("urn:ietf:params:xml:ns:netconf:capability:validate:1.0"),
        XML_WRITABLE_RUNNING("urn:ietf:params:xml:ns:netconf:capability:writable-running:1.0"),
        ERICSSON_ACTION("urn:ericsson:com:netconf:action:1.0"),
        ERICSSON_EBASE_1("urn:com:ericsson:ebase:0.1.0"),
        ERICSSON_EBASE_2("urn:com:ericsson:ebase:1.1.0"),
        ERICSSON_EBASE_3("urn:com:ericsson:ebase:1.2.0"),
        ERICSSON_EBASE_4("urn:com:ericsson:ebase:2.0.0"),
        ERICSSON_HEARTBRAT("urn:ericsson:com:netconf:heartbeat:1.0"),
        ERICSSON_NOTIFICATION("urn:ericsson:com:netconf:notification:1.1"),
        ERICSSON_OPERATION("urn:com:ericsson:netconf:operation:1.0");

        private String urn;
        private static List<DefaultCapabilities> defaultCapabilityList = Collections.unmodifiableList(Arrays.asList(values()));

        private DefaultCapabilities(String urn) {
            this.urn = urn;
        }

        public String getUrn() {
            return urn;
        }

        public static List<String> getDefaultCapabilityUrns() {
            List<String> urns = new ArrayList();
            for (DefaultCapabilities capability : defaultCapabilityList) {
                urns.add(capability.urn);
            }
            return urns;
        }
    }

    private String readFile(final String filePath) throws IOException {
        final Resource resource = Resources.getFileSystemResource(filePath);
        final StringBuilder stringBuilder = new StringBuilder();
        if ((resource != null) && resource.exists()) {
            final InputStreamReader isReader = new InputStreamReader(resource.getInputStream());
            final BufferedReader bufferedReader = new BufferedReader(isReader);
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line.trim()).append(" ");
                line = bufferedReader.readLine();
            }
            isReader.close();
            return stringBuilder.toString();
        } else {
            throw new FileNotFoundException("The netconf file does NOT exist: " + filePath);
        }
    }
}
