package com.sonatype.insight.brain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.Features;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.internal.DefaultLicenseKey;

import com.sonatype.insight.brain.product.license.CLMFeature;

/**
 * Simple replacement for a ProductLicenseManager.
 */
public class TestProductLicenseManager
    implements ProductLicenseManager
{
    private boolean valid;

    private ProductLicenseKey key;

    public TestProductLicenseManager()
    {
        this( false );
    }

    public TestProductLicenseManager( boolean valid )
    {
        this.valid = valid;

        if ( this.valid )
        {
            createKey();
        }
    }

    @Override
    public void installLicense( final InputStream licenseFile )
        throws IOException, LicensingException
    {
        valid = true;
        createKey();
    }

    private void createKey()
    {
        Map<String, Feature> featureMap = new HashMap<String, Feature>();
        featureMap.put( CLMFeature.ID, new CLMFeature() );
        Properties properties = new Properties();
        properties.put( "enforcementPoints", "Procure, Develop, Build, StageRelease, Release" );
        properties.put( "licensedApplications", "100" );
        key = new DefaultLicenseKey( new Features( featureMap ) );
        key.setEffectiveDate( new Date( System.currentTimeMillis() - 10000 ) );
        key.setExpirationDate( new Date( System.currentTimeMillis() + 10000 ) );
        key.setProperties( properties );
    }

    @Override
    public void uninstallLicense()
        throws LicensingException
    {
        valid = false;
        key = null;
    }

    @Override
    public ProductLicenseKey getLicenseDetails()
        throws LicensingException
    {
        if ( !valid )
        {
            throw new LicensingException( "Not licensed" );
        }
        return key;
    }

    @Override
    public ProductLicenseKey getLicenseDetails( final InputStream licenseFile )
        throws IOException, LicensingException
    {
        if ( !valid )
        {
            throw new LicensingException( "Not licensed" );
        }
        return key;
    }

    @Override
    public void verifyLicenseAndFeature( final Feature feature )
        throws LicensingException
    {
        // TODO
    }

    @Override
    public void verifyFeature( final ProductLicenseKey key, final Feature feature )
        throws LicensingException
    {
        // TODO
    }

    public boolean isValid()
    {
        return valid;
    }

    public ProductLicenseKey getKey()
    {
        return key;
    }

    public void setKey( final ProductLicenseKey key )
    {
        this.key = key;
    }
}
