package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;

public class ProductLicenseResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testInstallLicense()
        throws Exception
    {
        installLicense();
    }

    @Test
    public void testUninstallLicense()
        throws Exception
    {
        installLicense();
        uninstallLicense();
    }

    @Test
    @Ignore("This test is no longer valid, as the UI will now reload if a license is not installed, the server no longer does redirecting here")
    public void testProperHtmlRetrieved()
        throws Exception
    {
        installLicense();
        assertRequest( getApplicationIndexUrl(), 200 );
        uninstallLicense();
        assertRequest( getApplicationIndexUrl(), 303 );
        installLicense();
        assertRequest( getPolicyIndexUrl(), 200 );
        uninstallLicense();
        assertRequest( getPolicyIndexUrl(), 303 );
    }

    private void assertRequest( String path, int status )
        throws IOException
    {
        try
        {
            Client.create().resource( path ).get( InputStream.class );
            Assert.assertTrue( status == 200 );
        }
        catch ( UniformInterfaceException e )
        {
            Assert.assertEquals( status, e.getResponse().getStatus() );
        }
    }

    private String getApplicationIndexUrl()
    {
        return getRestBaseUrl() + InsightBrainService.APPLICATION_ASSET_PATH.substring( 1 ) + "index.html";
    }

    private String getPolicyIndexUrl()
    {
        return getRestBaseUrl() + InsightBrainService.POLICY_ASSET_PATH.substring( 1 ) + "index.html";
    }
}
