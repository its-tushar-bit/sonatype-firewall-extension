/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;


public class HashGAVResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testSetHashGAV()
        throws Exception
    {
        String hash = "ab1234ab1234ab";
        String groupId = "HashGAVResourceTest_G";
        String artifactId = "HashGAVResourceTest_A";
        String version = "HashGAVResourceTest_V";
        String extension = "HashGAVResourceTest_E";
        String classifier = "HashGAVResourceTest_C";

        HashGAV hashGAV = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        Response response = RestAccess.post( getServiceURL(), JsonHelpers.asJson( hashGAV ) );
        assertResponseStatus( 200, response );
        hashGAV = JsonHelpers.fromJson( response.getResponseBody(), HashGAV.class );
        assertHashGAV( hash, groupId, artifactId, version, extension, classifier, hashGAV );

        new HashGAVDAO().delete( hashGAV );
    }

    private void assertHashGAV( String hash, String groupId, String artifactId, String version, String extension,
                                String classifier, HashGAV hashGAV )
    {
        assertEquals( hash, hashGAV.getHash() );
        assertEquals( groupId, hashGAV.getGroupId() );
        assertEquals( artifactId, hashGAV.getArtifactId() );
        assertEquals( version, hashGAV.getVersion() );
        assertEquals( extension, hashGAV.getExtension() );
        assertEquals( classifier, hashGAV.getClassifier() );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + HashGAVResource.SERVICE_PATH;
    }
}
