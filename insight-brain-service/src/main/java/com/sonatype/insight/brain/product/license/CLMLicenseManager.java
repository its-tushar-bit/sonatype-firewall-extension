package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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
        
        populateLicenseCache();
    }

    private void populateLicenseCache()
    {
        String licenseFingerprint = null;
        Integer applicationCount = null;
        Set<CLMEnforcementPoint> enforcementPoints = new HashSet<CLMEnforcementPoint>();

        try
        {
            ProductLicenseKey key = licenseManager.getLicenseDetails();

            licenseFingerprint = licenseFingerprinter.calculate( key );
            applicationCount =
                Integer.decode( key.getProperties().getProperty( ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT ) );

            String[] enforcementPointIds =
                key.getProperties().getProperty( ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS ).split( "," );

            for ( String enforcementPointId : enforcementPointIds )
            {
                enforcementPoints.add( CLMEnforcementPoint.valueOf( enforcementPointId.trim() ) );
            }
        }
        catch ( Exception e )
        {
            log.debug( "Attempted to retrieve license details and failed", e );
            licenseFingerprint = null;
            applicationCount = 0;
            enforcementPoints.clear();
        }

        licenseCache = new CachedLicenseData( licenseFingerprint, applicationCount, enforcementPoints );
    }

    public synchronized void installLicense( InputStream is )
        throws IOException, LicensingException
    {
        licenseManager.installLicense( is );
        log.info( "License installed successfully" );
        populateLicenseCache();
    }

    public synchronized void uninstallLicense()
        throws LicensingException
    {
        licenseManager.uninstallLicense();
        log.info( "License uninstalled successfully" );
        populateLicenseCache();
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
     * Validate that the license is installed and contains the requested enforcement point
     * 
     * @throws InvalidLicenseException when enforcement point is not licensed
     */
    public void validateEnforcementPoint( CLMEnforcementPoint enforcementPoint )
        throws InvalidLicenseException
    {
        if ( !Arrays.asList( licenseCache.getEnforcementPoints() ).contains( enforcementPoint ) )
        {
            String msg = "Enforcement Point " + enforcementPoint.name() + " is not licensed!";
            log.error( msg );
            throw new InvalidLicenseException( msg );
        }
    }
}
