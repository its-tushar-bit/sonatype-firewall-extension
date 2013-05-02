package com.sonatype.insight.brain.product.license;

import org.junit.Test;

import com.sonatype.insight.brain.service.AbstractResourceTest;

public class ProductLicenseResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testInstallUninstallLicense()
        throws Exception
    {
        installLicense();
        uninstallLicense();
    }
}
