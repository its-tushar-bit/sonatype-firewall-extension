package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

@Named
@Singleton
public class CLMLicenseManager
{
    private final ProductLicenseManager licenseManager;

    private final LicenseFingerprinter licenseFingerprinter;

    private volatile String licenseFingerprint = null;

    private static final Logger log = LoggerFactory.getLogger( CLMLicenseManager.class );

    @Inject
    public CLMLicenseManager( final LicenseFingerprinter licenseFingerprinter,
                              final ProductLicenseManager licenseManager )
    {
        this.licenseFingerprinter = licenseFingerprinter;
        this.licenseManager = licenseManager;
    }

    /**
     * Get a license fingerprint, if there is no license, null will be returned
     * 
     * @return
     */
    public String getLicenseFingerprint()
    {
        if ( licenseFingerprint != null )
        {
            return licenseFingerprint;
        }

        try
        {
            licenseFingerprint = licenseFingerprinter.calculate( licenseManager.getLicenseDetails() );
        }
        catch ( Throwable t )
        {
            log.debug( "Attempted to retrieve a license fingerprint and failed", t );
            licenseFingerprint = null;
        }

        return licenseFingerprint;
    }

    public synchronized void installLicense( InputStream is )
        throws IOException, LicensingException
    {
        licenseManager.installLicense( is );
        log.info( "License installed successfully" );
        licenseFingerprint = null;
    }

    public synchronized void uninstallLicense()
        throws LicensingException
    {
        licenseManager.uninstallLicense();
        log.info( "License uninstalled successfully" );
        licenseFingerprint = null;
    }

    public boolean isLicensedInstalled()
    {
        return getLicenseFingerprint() != null;
    }
}
