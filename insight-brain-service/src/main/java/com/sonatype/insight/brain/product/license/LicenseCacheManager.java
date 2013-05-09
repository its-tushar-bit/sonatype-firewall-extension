package com.sonatype.insight.brain.product.license;

import java.util.Collections;
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

import de.schlichtherle.license.NoLicenseInstalledException;

@Named
@Singleton
public class LicenseCacheManager
{
    public final class CachedLicenseData
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
        
        public String getFingerprint()
        {
            return fingerprint;
        }
    }

    private volatile CachedLicenseData licenseCache = null;

    private volatile long nextCacheReset = 0;

    private final ProductLicenseManager licenseManager;

    private final LicenseFingerprinter licenseFingerprinter;

    private static final Logger log = LoggerFactory.getLogger( LicenseCacheManager.class );
    
    //1 day
    private static final long CACHE_DURATION = 1000 * 60 * 60 * 24;

    @Inject
    public LicenseCacheManager( ProductLicenseManager licenseManager, LicenseFingerprinter licenseFingerprinter )
    {
        this.licenseManager = licenseManager;
        this.licenseFingerprinter = licenseFingerprinter;
        
        try
        {
            reset();
        }
        catch ( LicensingException e )
        {
            if ( e.getCause() instanceof NoLicenseInstalledException )
            {
                String msg = "No license installed";
                if ( log.isDebugEnabled() )
                {
                    log.warn( msg, e );
                }
                else
                {
                    log.warn( msg );
                }
            }
            else
            {
                log.error( "Installed license is invalid", e );
            }
        }
    }

    public CachedLicenseData get()
        throws LicensingException
    {
        if ( System.currentTimeMillis() > nextCacheReset )
        {
            reset();
        }

        return this.licenseCache;
    }
    
    public void reset()
        throws LicensingException
    {
        populateLicenseCache();
        nextCacheReset = System.currentTimeMillis() + CACHE_DURATION;
    }
    
    public void clear()
    {
        clearLicenseCache();
    }

    private void populateLicenseCache()
        throws LicensingException
    {
        clearLicenseCache();

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
}
