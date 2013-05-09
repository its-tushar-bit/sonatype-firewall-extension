package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.product.ProductLicenseManager;

import com.sonatype.insight.license.model.CLMEnforcementPoint;

@Named
@Singleton
public class CLMLicenseManager
{
    private final ProductLicenseManager licenseManager;

    private final LicenseCacheManager licenseCache;

    private static final Logger log = LoggerFactory.getLogger( CLMLicenseManager.class );

    @Inject
    public CLMLicenseManager( final ProductLicenseManager licenseManager, final LicenseCacheManager licenseCache )
    {
        this.licenseManager = licenseManager;
        this.licenseCache = licenseCache;
    }

    public synchronized void installLicense( InputStream is )
        throws IOException, LicensingException
    {
        licenseManager.installLicense( is );
        licenseCache.reset();
        log.info( "License installed successfully" );
    }

    public synchronized void uninstallLicense()
        throws LicensingException
    {
        licenseManager.uninstallLicense();
        licenseCache.clear();
        log.info( "License uninstalled successfully" );
    }

    /**
     * Get a license fingerprint, if there is no license, null will be returned
     */
    public String getLicenseFingerprint()
    {
        try
        {
            return licenseCache.get().getFingerprint();
        }
        catch ( LicensingException e )
        {
            log.debug( "Failed to retrieve license fingerprint", e );
            return null;
        }
    }

    /**
     * Get the application limit in the license, if no license, 0 will be returned
     */
    public int getApplicationCountLimit()
    {
        try
        {
            return licenseCache.get().getApplicationLimit();
        }
        catch ( LicensingException e )
        {
            log.debug( "Failed to retrieve application limit", e );
            return 0;
        }
    }

    /**
     * Validate that a license is installed
     * 
     * @throws InvalidLicenseException when no license is installed
     */
    public void validate()
        throws InvalidLicenseException
    {
        if ( getLicenseFingerprint() == null )
        {
            String msg = "CLM is not licensed!";
            log.error( msg );
            throw new InvalidLicenseException( msg );
        }
    }

    /**
     * Validates that the license is installed and contains any of the requested enforcement points.
     * 
     * @throws InvalidLicenseException If none of the enforcement points is licensed.
     */
    public void validateAnyEnforcementPoint( Set<CLMEnforcementPoint> enforcementPoints )
    {
        if ( enforcementPoints.isEmpty() )
        {
            return;
        }

        try
        {
            Set<CLMEnforcementPoint> licensed =
                EnumSet.copyOf( Arrays.asList( licenseCache.get().getEnforcementPoints() ) );
            for ( CLMEnforcementPoint requested : enforcementPoints )
            {
                if ( licensed.contains( requested ) )
                {
                    return;
                }
            }
        }
        catch ( LicensingException e )
        {
            log.debug( "Failed to retrieve enforcement points", e );
        }

        if ( enforcementPoints.size() == 1 )
        {
            throw new InvalidLicenseException( "The enforcement point " + enforcementPoints.iterator().next()
                + " is not licensed!" );
        }
        throw new InvalidLicenseException( "None of the enforcement points " + enforcementPoints + " is licensed!" );
    }
}
