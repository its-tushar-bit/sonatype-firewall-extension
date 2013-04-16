package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;
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

@Named
@Singleton
public class CLMLicenseManager
{
    private final class CachedLicenseData
    {
        private final String fingerprint;

        private final Integer applicationCount;

        private final Set<CLMEnforcementPoint> enforcementPoints = new HashSet<CLMEnforcementPoint>();

        public CachedLicenseData( String fingerprint, Integer applicationCount,
                                  Set<CLMEnforcementPoint> enforcementPoints )
        {
            this.fingerprint = fingerprint;
            this.applicationCount = applicationCount;
            this.enforcementPoints.addAll( enforcementPoints );
        }
    }

    public final static String PROPERTY_APPLICATION_COUNT = "licensedApplications";

    public final static String PROPERTY_ENFORCEMENT_POINTS = "enforcementPoints";

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
            applicationCount = Integer.decode( key.getProperties().getProperty( PROPERTY_APPLICATION_COUNT ) );

            String[] enforcementPointIds = key.getProperties().getProperty( PROPERTY_ENFORCEMENT_POINTS ).split( "," );

            for ( String enforcementPointId : enforcementPointIds )
            {
                enforcementPoints.add( CLMEnforcementPoint.valueOf( enforcementPointId.trim() ) );
            }
        }
        catch ( Exception e )
        {
            log.debug( "Attempted to retrieve license details and failed", e );
            licenseFingerprint = null;
            applicationCount = null;
            enforcementPoints.clear();
        }

        licenseCache = new CachedLicenseData( licenseFingerprint, applicationCount, enforcementPoints );
    }

    public synchronized void installLicense( InputStream is )
        throws IOException, LicensingException
    {
        licenseManager.installLicense( is );
        log.info( "License installed successfully" );
        licenseCache = null;
    }

    public synchronized void uninstallLicense()
        throws LicensingException
    {
        licenseManager.uninstallLicense();
        log.info( "License uninstalled successfully" );
        licenseCache = null;
    }

    /**
     * Get a license fingerprint, if there is no license, null will be returned
     */
    public String getLicenseFingerprint()
    {
        if ( licenseCache != null )
        {
            return licenseCache.fingerprint;
        }

        populateLicenseCache();

        return licenseCache.fingerprint;
    }

    public int getApplicationCountLimit()
    {
        if ( licenseCache != null )
        {
            return licenseCache.applicationCount;
        }

        populateLicenseCache();

        return licenseCache.applicationCount;
    }

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

    public void validateEnforcementPoint( CLMEnforcementPoint enforcementPoint )
        throws InvalidLicenseException
    {
        if ( licenseCache == null )
        {
            populateLicenseCache();
        }

        if ( !licenseCache.enforcementPoints.contains( enforcementPoint ) )
        {
            String msg = "Enforcement Point " + enforcementPoint.name() + " is not licensed!";
            log.error( msg );
            throw new InvalidLicenseException( msg );
        }
    }
}
