package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.LicensingException;

import com.sun.jersey.multipart.FormDataParam;

@Path( ProductLicenseResource.SERVICE_PATH )
@Named
public class ProductLicenseResource
{
    public static final String SERVICE_PATH = "rest/product/license";

    private final CLMLicenseManager licenseManager;

    private final Logger log = LoggerFactory.getLogger( ProductLicenseResource.class );

    @Inject
    public ProductLicenseResource( CLMLicenseManager licenseManager )
    {
        this.licenseManager = licenseManager;
    }

    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_PLAIN )
    @UnlicensedPath
    public String installLicense( @FormDataParam( "file" ) InputStream is ) 
        throws IOException, LicensingException
    {
        licenseManager.installLicense( is );
        log.info( "CLM License successfully installed" );
        //Note an empty string triggers success in the UI
        return "";
    }

    @DELETE
    @UnlicensedPath
    public void uninstallLicense()
    {
        try
        {
            licenseManager.uninstallLicense();
            log.info( "CLM License successfully uninstalled" );
        }
        catch ( LicensingException e )
        {
            log.error( "Unable to uninstall license", e );
        }
    }

    @GET
    @UnlicensedPath
    @Produces( MediaType.TEXT_PLAIN )
    public String validate()
    {
        licenseManager.validate();
        return "OK";
    }

}
