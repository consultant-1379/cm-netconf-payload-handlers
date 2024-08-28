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

import java.nio.file.Paths

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.resources.Resource
import com.ericsson.oss.itpf.sdk.resources.Resources

public class FileResourceSpec extends CdiSpecification {

    @ObjectUnderTest
    private FileResource fileResource

    @MockedImplementation
    private Resource resource

    @MockedImplementation
    private Resources resources

    def "Test reading from a unknown file" (){
        given: "text and file"
        print "===="+getClass().getResource("/").toURI()
        when: "get file content is executed"
        fileResource.getFileContentAsString(Paths.get(getClass().getResource("/").toURI()).toString()+"/dummy.txt")
        then:
        thrown(Exception)
    }


    def "Test deleting file" (){
        given: "text and file"
        print "===="+getClass().getResource("/").toURI()
        new File(Paths.get(getClass().getResource("/").toURI()).toString()+"/Dummy.txt").createNewFile()
        when: "delete file is executed"
        fileResource.deleteFile(Paths.get(getClass().getResource("/Dummy.txt").toURI()).toString())
        then:
        notThrown(Exception)
    }
}