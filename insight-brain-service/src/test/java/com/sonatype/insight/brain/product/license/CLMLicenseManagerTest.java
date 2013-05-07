package com.sonatype.insight.brain.product.license;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;
import org.sonatype.licensing.LicenseKey;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.FeatureValidator;
import org.sonatype.licensing.internal.DefaultFeatureValidator;

import com.google.inject.AbstractModule;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.TestInsightBrainService;

public class CLMLicenseManagerTest
    extends AbstractBrainServiceTest
{
    private static class NegativeFeatureValidator
        extends DefaultFeatureValidator
    {
        @Override
        public boolean isValid( Feature feature, LicenseKey licenseKey )
        {
            return false;
        }
    }
    
    @Override
    protected void configureBrain( TestInsightBrainService brain )
    {
        super.configureBrain( brain );

        if ( "testLicenseLacksClmFeature".equals( testName.getMethodName() ) )
        {
            brain.addModule( new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind( FeatureValidator.class ).toInstance( new NegativeFeatureValidator() );
                }
            } );
        }
    }

    @Test
    public void testLicenseLacksClmFeature()
        throws Exception
    {
        CLMLicenseManager clmLicenseManager = brain.getInjector().getInstance( CLMLicenseManager.class );
        InputStream licenseStream = this.getClass().getResourceAsStream( "/productlicense/license.lic" );
        try
        {
            clmLicenseManager.installLicense( licenseStream );
            fail( "Expected LicensingException" );
        }
        catch ( LicensingException expected )
        {
            assertEquals( "License does not permit use of feature 'SonatypeCLM'", expected.getMessage() );
        }
        finally
        {
            IOUtil.close( licenseStream );
        }

        assertNull( clmLicenseManager.getLicenseFingerprint() );
    }
}
