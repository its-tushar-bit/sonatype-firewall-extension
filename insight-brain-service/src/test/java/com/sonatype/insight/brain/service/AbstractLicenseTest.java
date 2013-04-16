package com.sonatype.insight.brain.service;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.google.inject.AbstractModule;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;

public abstract class AbstractLicenseTest
    extends AbstractBrainServiceTest
{
    // by default license is always valid, to override, simply uninstall the license
    private final TestProductLicenseManager licenseManager = new TestProductLicenseManager( true );

    private final TestLicenseFingerprinter licenseFingerprinter = new TestLicenseFingerprinter();

    @Override
    protected void configureBrain( TestInsightBrainService brain )
    {
        brain.addModule( new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind( ProductLicenseManager.class ).toInstance( licenseManager );
                bind( LicenseFingerprinter.class ).toInstance( licenseFingerprinter );
            }
        } );
    }

    protected TestProductLicenseManager getLicenseManager()
    {
        return licenseManager;
    }

    protected TestLicenseFingerprinter getLicenseFingerprinter()
    {
        return licenseFingerprinter;
    }
}
