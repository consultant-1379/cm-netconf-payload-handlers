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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.ejb.*;

import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.itpf.sdk.resources.Resources;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class FileResource {

    private Resource init(final String absoluteFileURI) {
        return Resources.getFileSystemResource(absoluteFileURI);
    }

    public String getFileContentAsString(final String absoluteFileURI) throws FileNotFoundException {
        final Resource resource = init(absoluteFileURI);
        if (resource != null && resource.exists()) {
            return new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)).lines().map(String::trim).collect(Collectors.joining());
        } else {
            throw new FileNotFoundException("Failed to read node get configuration response from file system");
        }
    }

    public boolean deleteFile(final String filePath) {
        return Resources.getFileSystemResource(filePath).delete();
    }
}
