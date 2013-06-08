/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
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

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.application.ApplicationResource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

@Path( CIResource.SERVICE_PATH )
@ProductLicenseEnforcementPoint( { CLMEnforcementPoint.Build } )
@Named
public class CIResource
{
    public static final String SERVICE_PATH = "rest/ci";

    private final InsightWork work;
    
    private final SaasClient client;
    
    private final ScanUploader uploader;
    
    private ApplicationDAO applicationDAO = new ApplicationDAO();
    
    @Inject
    public CIResource( final InsightWork work, final SaasClient client, final ScanUploader uploader )
    {
        this.work = work;
        this.client = client;
        this.uploader = uploader;
    }
    
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
    public ScanReceipt putScan( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                @Context HttpServletRequest req )
        throws IOException
    {
        return uploader.upload( req, applicationPublicId, "rest/ci/scan" );
    }

    @GET
    @Path( "report/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    public StreamingOutput getReport( @PathParam( "applicationPublicId" )
    final String applicationPublicId, @Context
    final HttpServletRequest req )
        throws IOException
    {
        applicationDAO.getByPublicIdNotNull( applicationPublicId );
        
        return StreamingOutput.class.cast( client.doProxy( req, "rest/ci/report" ).getEntity() );
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
