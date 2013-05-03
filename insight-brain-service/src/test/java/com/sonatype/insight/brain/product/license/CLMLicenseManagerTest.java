package com.sonatype.insight.brain.product.license;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Properties;

import org.junit.Test;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

public class CLMLicenseManagerTest
{

    @Test
    public void testLicenseLacksClmFeature()
        throws Exception
    {
        Properties props = new Properties();
        props.setProperty( ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT, "100" );
        props.setProperty( ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, CLMEnforcementPoint.Build.name() );
        ProductLicenseKey licenseKey = mock( ProductLicenseKey.class );
        when( licenseKey.getProperties() ).thenReturn( props );

        ProductLicenseManager licenseManager = mock( ProductLicenseManager.class );
        when( licenseManager.getLicenseDetails() ).thenReturn( licenseKey );
        doThrow( new LicensingException( "no CLM" ) ).when( licenseManager ).verifyFeature( any( ProductLicenseKey.class ),
                                                                                            any( Feature.class ) );

        LicenseFingerprinter licenseFingerprinter = mock( LicenseFingerprinter.class );
        when( licenseFingerprinter.calculate( any( ProductLicenseKey.class ) ) ).thenReturn( "fingerprint" );

        CLMLicenseManager clmManager = new CLMLicenseManager( licenseFingerprinter, licenseManager );
        assertSame( null, clmManager.getLicenseFingerprint() );
    }

}
