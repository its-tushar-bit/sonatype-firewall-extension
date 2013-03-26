/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadRequest;
import com.sonatype.insight.scan.upload.DefaultScanUploader;
import com.sonatype.insight.scan.upload.ScanUploader;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path( ApplicationResource.SERVICE_PATH )
public class ApplicationResource
{
    public static final String SERVICE_PATH = "rest/application";

    public static final String GET_APPLICATION_PATH = "{applicationPublicId}";

    public static final String GET_APPLICATION_ICON_PATH = "icon/{applicationPublicId}";

    public static final String ADD_APPLICATION_SYNC_PATH = "sync";

    public static final String VALIDATE_PATH = "validate/{applicationPublicId}";

    private static final Logger log = LoggerFactory.getLogger( ApplicationResource.class );

    private static final ScanUploader uploader =
        new DefaultScanUploader( LoggerFactory.getLogger( DefaultScanUploader.class ), false /* failOnLogErrors */ );

    private static final ApplicationDAO applicationDAO = new ApplicationDAO();

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    @Context
    private BaseUrl baseUrl;

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
        throws IOException
    {
        final List<ApplicationManagementSummary> applicationManagements = new ArrayList<ApplicationManagementSummary>();
        final List<Application> applications = applicationDAO.getAll();
        for ( Application application : applications )
        {
            log.debug( "Found application with public id {}", application.getPublicId() );

            final PolicyEvaluation policyEvaluation = work.getPolicyEvaluation( application.getId() );
            final ApplicationManagementSummary applicationManagement = new ApplicationManagementSummary();
            applicationManagement.setId( application.getId() );
            applicationManagement.setPublicId( application.getPublicId() );
            applicationManagement.setPolicyEvaluation( policyEvaluation );

            applicationManagements.add( applicationManagement );
        }

        return applicationManagements;
    }

    @GET
    @Path( GET_APPLICATION_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationManagementSummary getApplication(
        @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws IOException
    {
        return getApplicationManagementSummary( applicationPublicId );
    }

    @GET
    @Path( GET_APPLICATION_ICON_PATH )
    @Produces( "image/png" )
    public Response getApplicationIcon( @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        byte[] imageBytes = applicationDAO.getIcon( application.getId(), work.getIconDir() );
        return Response.ok( imageBytes ).build();
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationManagementSummary addApplication( String applicationPublicId )
        throws IOException
    {
        if ( applicationDAO.getByPublicId( applicationPublicId ) != null )
        {
            throw new BadRequestException( "An application with id " + applicationPublicId + " already exists" );
        }

        String result = validateApplicationPublicId( applicationPublicId, proxy );
        if ( "OK".equals( result ) )
        {
            Application application = applicationDAO.getByPublicId( applicationPublicId );
            ApplicationManagementSummary applicationManagement = new ApplicationManagementSummary();
            applicationManagement.setId( application.getId() );
            applicationManagement.setPublicId( application.getPublicId() );
            return applicationManagement;
        }
        throw new BadRequestException( "Invalid application id " + applicationPublicId );
    }

    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationManagementSummary addApplication( @FormDataParam( "applicationId" ) String applicationId,
                                                        @FormDataParam( "applicationName" ) String applicationPublicId,
                                                        @FormDataParam( "file" ) InputStream uploadedInputStream,
                                                        @FormDataParam( "file" ) FormDataContentDisposition fileDetail )
        throws IOException
    {
        return addApplicationInternal( applicationId, applicationPublicId, uploadedInputStream, fileDetail );
    }

    @POST
    @Path( ADD_APPLICATION_SYNC_PATH )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response addApplicationSync( @FormDataParam( "applicationId" ) String applicationId,
                                        @FormDataParam( "applicationName" ) String applicationPublicId,
                                        @FormDataParam( "file" ) InputStream uploadedInputStream,
                                        @FormDataParam( "file" ) FormDataContentDisposition fileDetail )
        throws IOException
    {
        addApplicationInternal( applicationId, applicationPublicId, uploadedInputStream, fileDetail );
        return Response.seeOther( URI.create(
            baseUrl.get() + InsightBrainService.APPLICATION_ASSET_PATH.substring( 1 ) + "index.html" ) ).build();
    }

    public ApplicationManagementSummary addApplicationInternal( String applicationPublicId, String applicationName,
                                                                InputStream uploadedInputStream,
                                                                FormDataContentDisposition fileDetail )
        throws IOException
    {
        if ( applicationPublicId == null || applicationPublicId.trim().isEmpty() )
        {
            throw new BadRequestException( "ID is required." );
        }

        Application application = applicationDAO.getByPublicId( applicationPublicId );
        if ( application != null )
        {
            throw new BadRequestException( applicationPublicId + " is already used as an ID." );
        }

        if ( applicationName == null || applicationName.trim().isEmpty() )
        {
            throw new BadRequestException( "Name is required." );
        }
        // Use explicit international characters to ensure the server response mimics the client
        if ( applicationName.toLowerCase().matches( ".*[^-a-zàèìòùáéíóúýâêîôûãñõäëïöüçßøåæÞþÐð0-9 ].*" ) )
        {
            throw new BadRequestException( "Name must be alpha numeric." );
        }
        if ( applicationName.matches( "^ |.* {2,}.*| $" ) )
        {
            throw new BadRequestException(
                "Name must not have leading or trailing spaces, or have two spaces in a row" );
        }

        application = applicationDAO.getByName( applicationName );
        if ( application != null )
        {
            throw new BadRequestException( applicationName + " is already used as a name." );
        }

        application = new Application();
        application.setPublicId( applicationName );
        applicationDAO.insert( application );

        final long fileSize = fileDetail.getSize();
        if ( fileSize > 0 )
        {
            if ( fileSize > 5242880 )
            {
                throw new BadRequestException( "Icon file size must be smaller than 5 MB." );
            }
            try
            {
                applicationDAO.setIcon( application.getId(), work.getIconDir(), uploadedInputStream );
            }
            catch ( IllegalArgumentException e )
            {
                log.debug( "Invalid icon uploaded for new application " );
                applicationDAO.delete( application );
                throw new BadRequestException( fileDetail.getFileName() + " is not a valid image." );
            }
            catch ( IOException e )
            {
                log.debug( "Invalid icon uploaded for new application " );
                applicationDAO.delete( application );
                throw new BadRequestException( fileDetail.getFileName() + " is not a valid image." );
            }
        }

        return getApplicationManagementSummary( applicationName );
    }

    private ApplicationManagementSummary getApplicationManagementSummary( String applicationPublicId )
        throws IOException
    {
        final Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        log.debug( "Found application with public id {}", application.getPublicId() );

        final PolicyEvaluation policyEvaluation = work.getPolicyEvaluation( application.getId() );
        final ApplicationManagementSummary applicationManagement = new ApplicationManagementSummary();
        applicationManagement.setId( application.getId() );
        applicationManagement.setPublicId( application.getPublicId() );
        applicationManagement.setPolicyEvaluation( policyEvaluation );

        return applicationManagement;
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
