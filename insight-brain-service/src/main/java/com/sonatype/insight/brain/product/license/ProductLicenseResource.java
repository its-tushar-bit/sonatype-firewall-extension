package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.product.ProductLicenseManager;

import com.sun.jersey.multipart.FormDataParam;

@Path( ProductLicenseResource.SERVICE_PATH )
@Named
public class ProductLicenseResource
{
    public static final String SERVICE_PATH = "rest/product/license";

    private final ProductLicenseManager licenseManager;

    @Inject
    public ProductLicenseResource( ProductLicenseManager licenseManager )
    {
        this.licenseManager = licenseManager;
    }

    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public void installLicense( @FormDataParam( "file" )
    InputStream is )
    {
        try
        {
            licenseManager.installLicense( is );
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( LicensingException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @DELETE
    public void uninstallLicense()
    {
        try
        {
            licenseManager.uninstallLicense();
        }
        catch ( LicensingException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
