package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sun.jersey.api.client.Client;

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
    public void testProperHtmlRetrieved()
        throws Exception
    {
        installLicense();
        assertHtmlContents( getApplicationIndexUrl(), "<!-- unlicensed -->", false );
        uninstallLicense();
        assertHtmlContents( getApplicationIndexUrl(), "<!-- unlicensed -->", true );
        installLicense();
        assertHtmlContents( getPolicyIndexUrl(), "<!-- unlicensed -->", false );
        uninstallLicense();
        assertHtmlContents( getPolicyIndexUrl(), "<!-- unlicensed -->", true );
    }

    private void assertHtmlContents( String path, String contents, boolean match )
        throws IOException
    {
        InputStream is = Client.create().resource( path ).get( InputStream.class );

        try
        {
            String html = IOUtils.toString( is );

            Assert.assertEquals( match, html.contains( contents ) );
        }
        finally
        {
            is.close();
        }
    }

    private String getApplicationIndexUrl()
    {
        return getRestBaseUrl() + "application-assets/index.html";
    }

    private String getPolicyIndexUrl()
    {
        return getRestBaseUrl() + "policy-assets/index.html";
    }
}
