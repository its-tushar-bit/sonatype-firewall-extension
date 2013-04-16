/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.product.license.ProductLicenseResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

public abstract class AbstractResourceTest
    extends AbstractLicenseTest
{
    protected void installLicense()
        throws Exception
    {
        File file = new File( getClass().getResource( "/productlicense/license.lic" ).toURI() );
        FormDataMultiPart form = new FormDataMultiPart();
        form.bodyPart( new FormDataBodyPart( "file", new ByteArrayInputStream( FileUtils.readFileToByteArray( file ) ),
                                             MediaType.APPLICATION_OCTET_STREAM_TYPE ) );

        WebResource resource = Client.create().resource( getServiceURL() );

        resource.type( MediaType.MULTIPART_FORM_DATA ).post( form );

        Assert.assertTrue( getLicenseManager().isValid() );
    }

    protected void uninstallLicense()
        throws Exception
    {
        WebResource resource = Client.create().resource( getServiceURL() );

        resource.delete();

        Assert.assertFalse( getLicenseManager().isValid() );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ProductLicenseResource.SERVICE_PATH;
    }
                
    protected static void assertResponseStatus( final int expectedStatus, final Response response )
        throws IOException
    {
        final int actualStatus = response.getStatusCode();
        Assert.assertEquals( "URI:" + response.getUri() + ", StatusText:" + response.getStatusText()
            + ", ResponseBody:" + response.getResponseBody(), expectedStatus, actualStatus );
    }
}
