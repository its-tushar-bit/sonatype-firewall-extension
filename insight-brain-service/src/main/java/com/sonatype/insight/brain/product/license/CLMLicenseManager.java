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

    private String licenseFingerprint = null;

    private Boolean licenseInstalled = null;

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
    public synchronized String getLicenseFingerprint()
    {
        if ( licenseFingerprint != null )
        {
            return licenseFingerprint;
        }

        try
        {
            licenseFingerprint = licenseFingerprinter.calculate( licenseManager.getLicenseDetails() );
        }
        catch ( LicensingException e )
        {
            log.debug( "Attempted to retrieve a license fingerprint with no license installed", e );
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
        licenseInstalled = Boolean.TRUE;
    }

    public synchronized void uninstallLicense()
        throws LicensingException
    {
        licenseManager.uninstallLicense();
        log.info( "License uninstalled successfully" );
        licenseFingerprint = null;
        licenseInstalled = Boolean.FALSE;
    }

    public synchronized boolean isLicensedInstalled()
    {
        if ( licenseInstalled != null )
        {
            return licenseInstalled;
        }

        try
        {
            licenseManager.getLicenseDetails();
            licenseInstalled = true;
        }
        catch ( LicensingException e )
        {
            licenseInstalled = false;
        }

        return licenseInstalled;
    }
}
