package com.sonatype.insight.brain;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;

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
    }

    @Override
    public void installLicense( final InputStream licenseFile )
        throws IOException, LicensingException
    {
        valid = true;
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
