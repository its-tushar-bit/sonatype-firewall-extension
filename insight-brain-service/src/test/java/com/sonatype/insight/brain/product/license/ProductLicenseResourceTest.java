package com.sonatype.insight.brain.product.license;

import org.junit.Test;

import com.sonatype.insight.brain.service.AbstractResourceTest;

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
}
