/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadRequest;
import com.sonatype.insight.scan.upload.DefaultScanUploader;
import com.sonatype.insight.scan.upload.ScanUploader;

@Path( ApplicationResource.SERVICE_PATH )
public class ApplicationResource
{
    public static final String SERVICE_PATH = "rest/application";

    public static final String VALIDATE_PATH = "validate/{applicationPublicId}";

    private static final Logger log = LoggerFactory.getLogger( ApplicationResource.class );

    private static final ScanUploader uploader =
        new DefaultScanUploader( LoggerFactory.getLogger( DefaultScanUploader.class ), false /* failOnLogErrors */ );

    private static final ApplicationDAO applicationDAO = new ApplicationDAO();

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    @GET
    @Path( VALIDATE_PATH )
    @Produces( MediaType.TEXT_PLAIN )
    public String validateApplicationPublicId( @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws IOException
    {
        return validateApplicationPublicId( applicationPublicId, proxy );
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<ApplicationManagementSummary> getApplications()
    {
        final List<ApplicationManagementSummary> applicationManagements = new ArrayList<ApplicationManagementSummary>();
        final List<Application> applications = applicationDAO.getAll();
        for ( Application application : applications )
        {
            log.debug( "Found application with public id {}", application.getPublicId() );

            final File latestReport = work.getLatestReport( application.getId() );
            final ApplicationManagementSummary applicationManagement = new ApplicationManagementSummary();
            applicationManagement.setId( application.getId() );
            if ( latestReport != null )
            {
                applicationManagement.setLastModified( latestReport.lastModified() );
                applicationManagement.setReportName( latestReport.getName() );
            }

            applicationManagements.add( applicationManagement );
        }

        return applicationManagements;
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationManagementSummary addApplication( String applicationPublicId )
        throws IOException
    {
        String result = validateApplicationPublicId( applicationPublicId, proxy );
        if ( "OK".equals( result ) )
        {
            Application application = applicationDAO.getByPublicId( applicationPublicId );
            ApplicationManagementSummary applicationManagement = new ApplicationManagementSummary();
            applicationManagement.setId( application.getId() );
            return applicationManagement;
        }
        throw new BadRequestException( "Invalid application id" );
    }

    public static String validateApplicationPublicId( String applicationPublicId, InsightProxy proxy )
        throws IOException
    {
        final BOMCheckScanUploadRequest request = new BOMCheckScanUploadRequest( applicationPublicId, null, null );

        String result = uploader.validateToken( proxy.contextualize( request ) );
        log.debug( "validateApplicationPublicId({}) result:{}", applicationPublicId, result );

        if ( "OK".equals( result ) )
        {
            // The token is valid. Create an application object for it if it doesn't exist already.
            if ( applicationDAO.getByPublicId( applicationPublicId ) == null )
            {
                Application application = new Application();
                application.setPublicId( applicationPublicId );
                applicationDAO.insert( application );
            }
        }

        return result;
    }
}
