package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;
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

        private final long expirationTimestamp;

        public CachedLicenseData( final String fingerprint, Integer applicationLimit,
                                  final Set<CLMEnforcementPoint> enforcementPoints, final long expirationTimestamp )
        {
            this.fingerprint = fingerprint;
            this.expirationTimestamp = expirationTimestamp;
            super.setApplicationLimit( applicationLimit );
            super.setEnforcementPoints( enforcementPoints.toArray( new CLMEnforcementPoint[0] ) );
        }

        public String getFingerprint()
        {
            return fingerprint;
        }
    }

    public final class LicenseSummary
    {

        public final long expiryTimestamp;

        public LicenseSummary( long timestamp )
        {
            this.expiryTimestamp = timestamp;
        }
    }

    private final ProductLicenseManager licenseManager;

    private final LicenseFingerprinter licenseFingerprinter;

    private static final Logger log = LoggerFactory.getLogger( CLMLicenseManager.class );

    private volatile CachedLicenseData licenseCache;

    @Inject
    public CLMLicenseManager( final ProductLicenseManager licenseManager,
                              final LicenseFingerprinter licenseFingerprinter )
    {
        this.licenseManager = licenseManager;
        this.licenseFingerprinter = licenseFingerprinter;
        try
        {
            populateLicenseCache();
        }
        catch ( LicensingException e )
        {
            log.debug( "Unable to load license details", e );
            clearLicenseCache();
        }
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
        return licenseCache.getFingerprint();
    }

    /**
     * Get the application limit in the license, if no license, 0 will be returned
     */
    public int getApplicationCountLimit()
    {
        return licenseCache.getApplicationLimit();
    }

    /**
     * Get whether the license is currently valid
     * 
     * @return the validity
     */
    public boolean isValid()
    {
        return getLicenseFingerprint() != null && licenseCache.expirationTimestamp > System.currentTimeMillis();
    }

    /**
     * Validate that a license is installed
     * 
     * @throws InvalidLicenseException when no license is installed or the installed license is not valid
     */
    public void validate()
        throws InvalidLicenseException
    {
        if ( !isValid() )
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

        Set<CLMEnforcementPoint> licensed = EnumSet.noneOf( CLMEnforcementPoint.class );
        Collections.addAll( licensed, licenseCache.getEnforcementPoints() );
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

    public LicenseSummary getLicenseSummary()
    {
        return new LicenseSummary( this.licenseCache.expirationTimestamp );
    }

    private void populateLicenseCache()
        throws LicensingException
    {
        ProductLicenseKey key = licenseManager.getLicenseDetails();

        licenseManager.verifyFeature( key, new CLMFeature() );

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

        licenseCache =
            new CachedLicenseData( licenseFingerprint, applicationCount, enforcementPoints,
                                   key.getExpirationDate().getTime() );
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
        licenseCache = new CachedLicenseData( null, 0, Collections.<CLMEnforcementPoint> emptySet(), 0 );
    }
}
