package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

@Named
@Singleton
public class CLMLicenseManager
{
    private final class CachedLicenseData
        extends ProductLicenseDetails
    {
        private final String fingerprint;

        public CachedLicenseData( String fingerprint, Integer applicationLimit,
                                  Set<CLMEnforcementPoint> enforcementPoints )
        {
            this.fingerprint = fingerprint;
            super.setApplicationLimit( applicationLimit );
            super.setEnforcementPoints( enforcementPoints.toArray( new CLMEnforcementPoint[0] ) );
        }
    }

    private final ProductLicenseManager licenseManager;

    private final LicenseFingerprinter licenseFingerprinter;

    private volatile CachedLicenseData licenseCache = null;

    private static final Logger log = LoggerFactory.getLogger( CLMLicenseManager.class );

    @Inject
    public CLMLicenseManager( final LicenseFingerprinter licenseFingerprinter,
                              final ProductLicenseManager licenseManager )
    {
        this.licenseFingerprinter = licenseFingerprinter;
        this.licenseManager = licenseManager;

        try
        {
            populateLicenseCache();
        }
        catch ( LicensingException e )
        {
            log.debug( "Attempted to retrieve license details and failed", e );
        }
    }

    private void populateLicenseCache()
        throws LicensingException
    {
        clearLicenseCache();

        ProductLicenseKey key = licenseManager.getLicenseDetails();

        String licenseFingerprint = licenseFingerprinter.calculate( key );

        Integer applicationCount =
            Integer.decode( getPropertyNotNull( key, ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT ) );

        Set<CLMEnforcementPoint> enforcementPoints = new HashSet<CLMEnforcementPoint>();
        String[] enforcementPointIds =
            getPropertyNotNull( key, ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS ).split( "," );
        for ( String enforcementPointId : enforcementPointIds )
        {
            enforcementPointId = enforcementPointId.trim();
            try
            {
                enforcementPoints.add( CLMEnforcementPoint.valueOf( enforcementPointId ) );
            }
            catch ( IllegalArgumentException e )
            {
                log.warn( "License enables unknown enforcement point {}, ignored", enforcementPointId );
            }
        }

        licenseCache = new CachedLicenseData( licenseFingerprint, applicationCount, enforcementPoints );
    }

    private String getPropertyNotNull( ProductLicenseKey key, String property )
        throws LicensingException
    {
        String value = key.getProperties().getProperty( property );
        if ( value == null )
        {
            throw new LicensingException( key, "License lacks property " + property, null );
        }
        return value;
    }

    private void clearLicenseCache()
    {
        licenseCache = new CachedLicenseData( null, 0, Collections.<CLMEnforcementPoint> emptySet() );
    }

    public synchronized void installLicense( InputStream is )
        throws IOException, LicensingException
    {
        licenseManager.installLicense( is );
        populateLicenseCache();
        log.info( "License installed successfully" );
    }

    public synchronized void uninstallLicense()
        throws LicensingException
    {
        licenseManager.uninstallLicense();
        clearLicenseCache();
        log.info( "License uninstalled successfully" );
    }

    /**
     * Get a license fingerprint, if there is no license, null will be returned
     */
    public String getLicenseFingerprint()
    {
        return licenseCache.fingerprint;
    }

    /**
     * Get the application limit in the license, if no license, 0 will be returned
     */
    public int getApplicationCountLimit()
    {
        return licenseCache.getApplicationLimit();
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
        Set<CLMEnforcementPoint> licensed = EnumSet.copyOf( Arrays.asList( licenseCache.getEnforcementPoints() ) );
        for ( CLMEnforcementPoint requested : enforcementPoints )
        {
            if ( licensed.contains( requested ) )
            {
                return;
            }
        }
        if ( enforcementPoints.size() == 1 )
        {
            throw new InvalidLicenseException( "The enforcement point " + enforcementPoints.iterator().next()
                + " is not licensed!" );
        }
        throw new InvalidLicenseException( "None of the enforcement points " + enforcementPoints + " is licensed!" );
    }
}
