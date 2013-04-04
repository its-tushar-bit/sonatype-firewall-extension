/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.ErrorResponseGenerator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path( ApplicationResource.SERVICE_PATH )
public class ApplicationResource
{
    public static final String SERVICE_PATH = "rest/application";

    public static final String GET_APPLICATION_NAMES = "services/names";

    public static final String GET_APPLICATION_PATH = "{applicationPublicId}";

    public static final String GET_APPLICATION_ICON_PATH = "icon/{applicationPublicId}";

    public static final String GET_CAN_ACCESS_ROBOHASH_PATH = "services/canGetHashIcon";

    public static final String ADD_APPLICATION_SYNC_PATH = "services/sync";

    public static final String VALIDATE_PATH = "validate/{applicationPublicId}";

    private static final Logger log = LoggerFactory.getLogger( ApplicationResource.class );

    private static final ApplicationDAO applicationDAO = new ApplicationDAO();

    @Context
    private InsightWork work;

    @Context
    private BaseUrl baseUrl;

    private ErrorResponseGenerator errorResponseGenerator = new ErrorResponseGenerator( false );

    @GET
    @Path( VALIDATE_PATH )
    @Produces( MediaType.TEXT_PLAIN )
    public String validateApplicationPublicId( @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws IOException
    {
        return validateApplicationPublicIdInternal( applicationPublicId );
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
            final ApplicationManagementSummary applicationManagement =
                ApplicationManagementSummary.fromApplication( application );
            applicationManagement.setPolicyEvaluation( policyEvaluation );

            applicationManagements.add( applicationManagement );
        }

        return applicationManagements;
    }

    @GET
    @Path( GET_APPLICATION_NAMES )
    @Produces( MediaType.APPLICATION_JSON )
    public Map<String, String> getApplicationNames()
    {
        final List<Application> applications = applicationDAO.getAll();
        HashMap<String, String> applicationPublicIDNamePairs = new HashMap<String, String>();

        for ( Application application : applications )
        {
            log.debug( "Found application with public id {}", application.getPublicId() );
            applicationPublicIDNamePairs.put( application.getPublicId(), application.getName() );
        }

        return applicationPublicIDNamePairs;
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

        String result = validateApplicationPublicIdInternal( applicationPublicId );
        if ( "OK".equals( result ) )
        {
            Application application = applicationDAO.getByPublicId( applicationPublicId );
            return ApplicationManagementSummary.fromApplication( application );
        }
        throw new BadRequestException( "Invalid application id " + applicationPublicId );
    }

    /**
     * This is one of two service methods used for editing and adding applications. This method is used for AJAX
     * calls since its return type is a JSON object.
     *
     * @return ApplicationManagementSummary object of the application data which was posted for editing or adding.
     * @throws IOException
     */
    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationManagementSummary addEditApplication( @FormDataParam( "applicationId" ) String applicationId,
                                                            @FormDataParam(
                                                                "applicationPublicId" ) String applicationPublicId,
                                                            @FormDataParam( "applicationName" ) String applicationName,
                                                            @FormDataParam( "hasRobotSource" ) boolean hasRobotSource,
                                                            @FormDataParam( "robotHash" ) String robotHash,
                                                            @FormDataParam( "file" ) InputStream uploadedInputStream,
                                                            @FormDataParam(
                                                                "file" ) FormDataContentDisposition fileDetail )
        throws IOException
    {
        return addEditApplicationInternal( applicationId, applicationPublicId, applicationName, hasRobotSource,
                                           robotHash, uploadedInputStream, fileDetail );
    }

    /**
     * This is one of two service methods used for editing and adding applications. This method is used for synchronous
     * calls since it returns a HTTP Response.
     *
     * @return HTTP Response redirect to the application management page.
     * @throws IOException
     */
    @POST
    @Path( ADD_APPLICATION_SYNC_PATH )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response addEditApplicationSync( @FormDataParam( "applicationId" ) String applicationId,
                                            @FormDataParam( "applicationPublicId" ) String applicationPublicId,
                                            @FormDataParam( "applicationName" ) String applicationName,
                                            @FormDataParam( "hasRobotSource" ) boolean hasRobotSource,
                                            @FormDataParam( "robotHash" ) String robotHash,
                                            @FormDataParam( "file" ) InputStream uploadedInputStream,
                                            @FormDataParam( "file" ) FormDataContentDisposition fileDetail )
        throws IOException
    {
        String errorMessage = null;
        try
        {
            addEditApplicationInternal( applicationId, applicationPublicId, applicationName, hasRobotSource, robotHash,
                                        uploadedInputStream, fileDetail );
        }
        catch ( Exception e )
        {
            errorMessage = errorResponseGenerator.mapException( e ).getMessageBody();
        }

        UriBuilder uriBuilder =
            baseUrl.redirect().path( InsightBrainService.APPLICATION_ASSET_PATH ).path( "index.html" );
        if ( errorMessage != null )
        {
            uriBuilder = uriBuilder.queryParam( "errorMessage", errorMessage );
        }

        return Response.seeOther( uriBuilder.build() ).build();
    }

    @DELETE
    @Path( GET_APPLICATION_PATH )
    public void deleteApplication( @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws IOException
    {
        ApplicationManagementSummary applicationManagementSummary = getApplication( applicationPublicId );
        if ( applicationManagementSummary.getPolicyEvaluation() != null )
        {
            throw new BadRequestException( "Cannot delete " + applicationPublicId + " because it has been used." );
        }
        Application application = applicationDAO.getByPublicId( applicationPublicId );
        applicationDAO.deleteWithIcon( application, work.getIconDir() );
    }

    @GET
    @Path( GET_CAN_ACCESS_ROBOHASH_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public boolean canAccessRobohash()
    {
        boolean canAccess = false;
        Socket socket = null;
        try
        {
            socket = new Socket( "robohash.org", 80 );
            canAccess = true;
        }
        catch ( UnknownHostException e )
        {
            canAccess = false;
        }
        catch ( IOException e )
        {
            canAccess = false;
        }
        finally
        {
            if ( socket != null )
            {
                try
                {
                    socket.close();
                }
                catch ( IOException e )
                {
                    canAccess = false;
                }
            }
        }
        return canAccess;
    }

    private ApplicationManagementSummary addEditApplicationInternal( String applicationId, String applicationPublicId,
                                                                     String applicationName, boolean hasRobotSource,
                                                                     String robotHash, InputStream uploadedInputStream,
                                                                     FormDataContentDisposition fileDetail )
        throws IOException
    {
        if ( hasRobotSource )
        {
            try
            {
                URL robotURL = new URL( "http://robohash.org/" + robotHash );
                uploadedInputStream = robotURL.openStream();
            }
            catch ( Exception ex )
            {
                if ( uploadedInputStream != null )
                {
                    uploadedInputStream.close();
                    uploadedInputStream = null;
                }
            }
        }

        byte[] imageByteArray = null;
        if ( uploadedInputStream != null )
        {
            // Copy the uploadInputStream to bytes to enforce size limitation (5 MB)
            ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
            try
            {
                for ( int b = 0; ( b = uploadedInputStream.read() ) != -1; )
                {
                    if ( imageOutputStream.size() > 5242880 )
                    {
                        throw new BadRequestException( "Icon file size must be smaller than 5 MB." );
                    }
                    imageOutputStream.write( b );
                }
                imageByteArray = imageOutputStream.toByteArray();
            }
            finally
            {
                imageOutputStream.close();
                uploadedInputStream.close();
            }
        }

        Application application;
        if ( applicationId == null || applicationId.isEmpty() )
        {
            application = new Application();
        }
        else
        {
            application = applicationDAO.getByIdNotNull( applicationId );
        }
        application.setPublicId( applicationPublicId );
        application.setName( applicationName );

        if ( applicationId == null || applicationId.isEmpty() )
        {
            applicationDAO.insert( application );
        }
        else
        {
            applicationDAO.update( application );
        }

        if ( imageByteArray != null && imageByteArray.length > 0 )
        {
            InputStream sizeCheckedInputStream = new ByteArrayInputStream( imageByteArray );
            try
            {
                applicationDAO.setIcon( application.getId(), work.getIconDir(), sizeCheckedInputStream );
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
            finally
            {
                sizeCheckedInputStream.close();
            }
        }

        return getApplicationManagementSummary( applicationPublicId );
    }

    private ApplicationManagementSummary getApplicationManagementSummary( String applicationPublicId )
        throws IOException
    {
        final Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        log.debug( "Found application with public id {}", application.getPublicId() );

        final PolicyEvaluation policyEvaluation = work.getPolicyEvaluation( application.getId() );
        final ApplicationManagementSummary applicationManagement =
            ApplicationManagementSummary.fromApplication( application );
        applicationManagement.setPolicyEvaluation( policyEvaluation );

        return applicationManagement;
    }

    public static String validateApplicationPublicIdInternal( String applicationPublicId )
        throws IOException
    {
        if ( applicationDAO.getByPublicId( applicationPublicId ) == null )
        {
            return "Invalid application id " + applicationPublicId;
        }

        log.debug( "Found application with public id {}", applicationPublicId );
        return "OK";
    }
}
