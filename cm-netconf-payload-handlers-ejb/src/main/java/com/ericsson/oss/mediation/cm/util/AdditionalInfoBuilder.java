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

package com.ericsson.oss.mediation.cm.util;

import java.util.HashMap;
import java.util.Map;

public class AdditionalInfoBuilder {

    private AdditionalInfoBuilder() {

    }

    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_TITILE = "errorTitle";
    private static final String ERROR_DETAILS = "errorDetails";
    private static final String FILE_PATH = "filePath";

    private static final Integer UNPROCESSABLE_ENTITY = 422;
    private static final Integer EXECUTION_ERROR_CODE = 500;

    public static Map<String, Object> buildParserError(final String netconfPayload) {
        Map<String, Object> additionalInfo = new HashMap<>();

        additionalInfo.put(ERROR_CODE, UNPROCESSABLE_ENTITY);
        additionalInfo.put(ERROR_TITILE, "Unparsable Netconf Payload");
        additionalInfo.put(ERROR_DETAILS, netconfPayload);

        return additionalInfo;
    }

    public static Map<String, Object> buildNetconfExecutionError(final String editConfigResponse) {
        Map<String, Object> additionalInfo = new HashMap<>();

        additionalInfo.put(ERROR_CODE, UNPROCESSABLE_ENTITY);
        additionalInfo.put(ERROR_TITILE, "Netconf Execution Error");
        additionalInfo.put(ERROR_DETAILS, editConfigResponse);

        return additionalInfo;
    }

    public static Map<String, Object> buildExecutionError(final String exceptionDetails) {
        Map<String, Object> additionalInfo = new HashMap<>();

        additionalInfo.put(ERROR_CODE, EXECUTION_ERROR_CODE);
        additionalInfo.put(ERROR_TITILE, "Internal Execution Error");
        additionalInfo.put(ERROR_DETAILS, exceptionDetails);

        return additionalInfo;
    }

    public static Map<String, Object> buildNetconfConnectionError(final String netconfConnectionException) {
        Map<String, Object> additionalInfo = new HashMap<>();

        additionalInfo.put(ERROR_CODE, EXECUTION_ERROR_CODE);
        additionalInfo.put(ERROR_TITILE, "Netconf Connection Error");
        additionalInfo.put(ERROR_DETAILS, netconfConnectionException);

        return additionalInfo;
    }

    public static Map<String, Object> buildFileWriteError(final String errorDetails) {
        Map<String, Object> additionalInfo = new HashMap<>();

        additionalInfo.put(ERROR_CODE, EXECUTION_ERROR_CODE);
        additionalInfo.put(ERROR_TITILE, "Failed to store node configuration data");
        additionalInfo.put(ERROR_DETAILS, errorDetails);

        return additionalInfo;
    }

    public static Map<String, Object> buildFilePath(final String nodeConfigFilePath) {
        Map<String, Object> additionalInfo = new HashMap<>();

        additionalInfo.put(FILE_PATH, nodeConfigFilePath);

        return additionalInfo;

    }

}
