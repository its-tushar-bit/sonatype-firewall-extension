package com.sonatype.insight.brain.productlicense;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.licensing.product.ProductLicenseManager;

import com.google.inject.AbstractModule;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.TestInsightBrainService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

public class ProductLicenseResourceTest
    extends AbstractResourceTest
{
    private final TestProductLicenseManager licenseManager = new TestProductLicenseManager();

    @Override
    protected void configureBrain( TestInsightBrainService brain )
    {
        brain.addModule( new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind( ProductLicenseManager.class ).toInstance( licenseManager );
            }
        } );
    }

    @Test
    public void testInstallLicense()
        throws Exception
    {
        File file = new File( getClass().getResource( "license.lic" ).toURI() );
        FormDataMultiPart form = new FormDataMultiPart();
        form.bodyPart( new FormDataBodyPart( "file", new ByteArrayInputStream( FileUtils.readFileToByteArray( file ) ),
                                             MediaType.APPLICATION_OCTET_STREAM_TYPE ) );

        WebResource resource = Client.create().resource( getServiceURL() );

        resource.type( MediaType.MULTIPART_FORM_DATA ).put( form );

        Assert.assertTrue( licenseManager.isValid() );
    }

    @Test
    public void testUninstallLicense()
        throws Exception
    {
        testInstallLicense();

        WebResource resource = Client.create().resource( getServiceURL() );

        resource.delete();

        Assert.assertFalse( licenseManager.isValid() );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ProductLicenseResource.SERVICE_PATH;
    }
}
