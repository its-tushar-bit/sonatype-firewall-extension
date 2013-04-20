/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.application.ApplicationResource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.product.license.CLMEnforcementPoint;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.upload.BOMCheckReportDownloadRequestWithLicense;
import com.sonatype.insight.scan.upload.DefaultReportDownloader;
import com.sonatype.insight.scan.upload.ReportDownloader;

@Path( CIResource.SERVICE_PATH )
@ProductLicenseEnforcementPoint( { CLMEnforcementPoint.Build } )
@Named
public class CIResource
    extends AbstractSaasResource
{
    public static final String SERVICE_PATH = "rest/ci";

    private static final Logger log = LoggerFactory.getLogger( CIResource.class );

    final ReportDownloader downloader = new DefaultReportDownloader( log );

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;
    
    @Inject
    private CLMLicenseManager licenseManager;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    /**
     * @deprecated Use ApplicationResource.validateApplicationPublicId() instead.
     */
    @GET
    @Path( "validate/{applicationPublicId}" )
    @Produces( MediaType.TEXT_PLAIN )
    public String validateToken( @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws Exception
    {
        return ApplicationResource.validateApplicationPublicIdInternal( applicationPublicId );
    }

    @PUT
    @Path( "scan/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response putScan( @PathParam( "applicationPublicId" )
    final String applicationPublicId, @QueryParam( "instanceId" )
    final String instanceId, @QueryParam( "jobId" )
    final String jobId, @Context
    HttpServletRequest req )
        throws Exception
    {
        return Response.ok( doUpload( req, applicationPublicId, "rest/ci/scan", instanceId, jobId ) ).build();
    }

    @GET
    @Path( "report/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    public StreamingOutput getReport( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                      @QueryParam( "scanId" ) final String scanId )
    {
        applicationDAO.getByPublicIdNotNull( applicationPublicId );

        final BOMCheckReportDownloadRequestWithLicense request =
            new BOMCheckReportDownloadRequestWithLicense( licenseManager.getLicenseFingerprint(), scanId, null );

        return new StreamingOutput()
        {
            @Override
            public void write( final OutputStream os )
                throws IOException
            {
                try
                {
                    downloader.download( proxy.contextualize( request ), os );
                }
                catch ( final InterruptedException e )
                {
                    throw new IOException( e.getMessage() );
                }
            }
        };
    }

    @GET
    @Path( "artifact/{scanId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getArtifactInfo( @PathParam( "scanId" ) final String scanId,
                                     @QueryParam( "groupId" ) final String groupId,
                                     @QueryParam( "artifactId" ) final String artifactId,
                                     @QueryParam( "version" ) final String version )
    {
        String applicationPublicId = "unknown";
        String appId = work.findOwningAppId( scanId );
        if ( appId != null )
        {
            applicationPublicId = applicationDAO.getByIdNotNull( appId ).getPublicId();
        }
        return Response.temporaryRedirect( URI.create( "rest/report/" + applicationPublicId + '/' + scanId //
                                                           + "/artifactDetails" + "?groupId=" + groupId + "&artifactId="
                                                           + artifactId + "&version=" + version ) ).build();
    }
}
