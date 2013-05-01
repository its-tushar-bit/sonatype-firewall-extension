/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.ErrorResponseGenerator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Named
@Path( ApplicationResource.SERVICE_PATH )
public class ApplicationResource
{
    public static final String SERVICE_PATH = "rest/application";

    public static final String GET_APPLICATION_NAMES = "services/names";

    public static final String GET_APPLICATION_PATH = "{applicationPublicId}";

    public static final String APPLICATION_ICON_PATH = "icon/";

    public static final String GENERATE_ICON_PATH = "services/generateIcon/{hashcode}";

    public static final String APPLICATION_ICON_PATH_SYNC = APPLICATION_ICON_PATH + "sync";

    public static final String GET_APPLICATION_ICON_PATH = APPLICATION_ICON_PATH + "{applicationPublicId}";

    public static final String VALIDATE_PATH = "validate/{applicationPublicId}";

    private static final Logger log = LoggerFactory.getLogger( ApplicationResource.class );

    private static final ApplicationDAO applicationDAO = new ApplicationDAO();

    @Context
    private InsightWork work;

    @Context
    private BaseUrl baseUrl;
    
    @Inject
    private CLMLicenseManager licenseManager;

    @Inject
    private SaasClient client;

    private ErrorResponseGenerator errorResponseGenerator = new ErrorResponseGenerator( false );

    @GET
    @Path( VALIDATE_PATH )
    @Produces( MediaType.TEXT_PLAIN )
    public String validateApplicationPublicId( @PathParam( "applicationPublicId" ) final String applicationPublicId )
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
            applicationManagements.add( getApplicationManagementSummary( application ) );
        }

        return applicationManagements;
    }

    @GET
    @Path( GET_APPLICATION_NAMES )
    @Produces( MediaType.APPLICATION_JSON )
    public Map<String, String> getApplicationNames()
    {
        final List<Application> applications = applicationDAO.getAll();
        Map<String, String> applicationPublicIDNamePairs = new LinkedHashMap<String, String>();

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
    public ApplicationManagementSummary getApplication( @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws IOException
    {
        final Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        return getApplicationManagementSummary( application );
    }

    @GET
    @Path( GET_APPLICATION_ICON_PATH )
    @Produces( "image/png" )
    public StreamingOutput getApplicationIcon( @PathParam( "applicationPublicId" )
    final String applicationPublicId )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        final byte[] imageBytes = applicationDAO.getIcon( application.getId(), work.getIconDir() );
        if ( imageBytes == null )
        {
            throw new WebApplicationException( Response.Status.NOT_FOUND );
        }
        return new StreamingOutput()
        {
            @Override
            public void write( OutputStream output )
                throws IOException, WebApplicationException
            {
                output.write( imageBytes );
            }
        };
    }

    @GET
    @Path( GENERATE_ICON_PATH )
    @Produces( "image/png" )
    public StreamingOutput generateApplicationIcon( @PathParam( "hashcode" )
    final String hashcode, @Context
    final HttpServletRequest req )
        throws IOException
    {
        if ( hashcode == null || hashcode.isEmpty() )
        {
            throw new WebApplicationException( Response.Status.NOT_FOUND );
        }
        return StreamingOutput.class.cast( client.doProxy( req, "rest/application/icon/generate/" + hashcode ).getEntity() );
    }

    /**
     * This is one of two service methods used for editing and adding icons. This method is used for AJAX calls since
     * its return type is a JSON object.
     * 
     * @throws IOException
     */
    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Path( APPLICATION_ICON_PATH )
    public void addEditIcon( @FormDataParam( "applicationId" ) String applicationId,
                             @FormDataParam( "hasRobotSource" ) boolean hasRobotSource,
                             @FormDataParam( "robotHash" ) String robotHash,
                             @FormDataParam( "file" ) InputStream uploadedInputStream,
                             @FormDataParam( "file" ) FormDataContentDisposition fileDetail )
        throws IOException
    {
        addIconInternal( applicationId, hasRobotSource, robotHash, uploadedInputStream, fileDetail );
    }

    /**
     * This is one of two service methods used for editing and adding icons. This method is used for synchronous calls
     * since it returns a HTTP Response.
     * 
     * @return HTTP Response redirect to the application management page.
     * @throws IOException
     */
    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Path( APPLICATION_ICON_PATH_SYNC )
    public Response addEditIconSync( @FormDataParam( "applicationId" ) String applicationId,
                                     @FormDataParam( "hasRobotSource" ) boolean hasRobotSource,
                                     @FormDataParam( "robotHash" ) String robotHash,
                                     @FormDataParam( "file" ) InputStream uploadedInputStream,
                                     @FormDataParam( "file" ) FormDataContentDisposition fileDetail )
    {
        String errorMessage = null;
        try
        {
            addIconInternal( applicationId, hasRobotSource, robotHash, uploadedInputStream, fileDetail );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
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

    public void addIconInternal( String applicationId, boolean hasRobotSource, String robotHash,
                                 InputStream uploadedInputStream, FormDataContentDisposition fileDetail )
        throws IOException
    {
        if ( hasRobotSource )
        {
            try
            {
                HttpResponse iconResponse =
                    client.getResponse( null, "rest/application/icon/generate/" + robotHash, null, (String) null );
                uploadedInputStream = iconResponse.getEntity().getContent();
            }
            catch ( Exception e )
            {
                log.error( e.getMessage(), e );
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

            if ( imageByteArray != null && imageByteArray.length > 0 )
            {
                InputStream sizeCheckedInputStream = new ByteArrayInputStream( imageByteArray );
                try
                {
                    applicationDAO.setIcon( applicationId, work.getIconDir(), sizeCheckedInputStream );
                }
                catch ( IllegalArgumentException e )
                {
                    throw new BadRequestException( fileDetail.getFileName() + " is not a valid image.", e );
                }
                catch ( IOException e )
                {
                    throw new BadRequestException( fileDetail.getFileName() + " is not a valid image.", e );
                }
                finally
                {
                    sizeCheckedInputStream.close();
                }
            }
        }
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationManagementSummary addApplication( Application application )
        throws IOException
    {
        int appLimit = licenseManager.getApplicationCountLimit();
        
        if ( applicationDAO.getAll().size() >= appLimit )
        {
            throw new BadRequestException( "You have exceeded the licensed limit of " + appLimit + " applications." );
        }

        applicationDAO.insert( application );

        return getApplication( application.getPublicId() );
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationManagementSummary editApplication( Application application )
        throws IOException
    {
        applicationDAO.update( application );

        return getApplication( application.getPublicId() );
    }

    @DELETE
    @Path( GET_APPLICATION_PATH )
    public void deleteApplication( @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws IOException
    {
        if ( isApplicationInUse( applicationPublicId ) )
        {
            throw new BadRequestException( "Cannot delete " + applicationPublicId + " because it has been used." );
        }

        Application application = applicationDAO.getByPublicId( applicationPublicId );
        applicationDAO.deleteWithIcon( application, work.getIconDir() );
        PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );
        policyDAO.deleteByOwnerId( application.getId() );
    }

    private boolean isApplicationInUse( final String applicationPublicId )
        throws IOException
    {
        ApplicationManagementSummary applicationManagementSummary = getApplication( applicationPublicId );
        if ( !applicationManagementSummary.getPolicyEvaluations().isEmpty() )
        {
            return true;
        }
        if ( applicationManagementSummary.getScansCount() != 0 )
        {
            return true;
        }
        return false;
    }

    private ApplicationManagementSummary getApplicationManagementSummary( final Application application )
        throws IOException
    {
        log.debug( "Found application with public id {}", application.getPublicId() );

        final ApplicationManagementSummary applicationManagement =
            ApplicationManagementSummary.fromApplication( application );
        applicationManagement.setPolicyEvaluations( work.getMostRecentPolicyEvaluations( application.getId() ) );
        File[] scans = work.getScanDir( applicationManagement.getId() ).listFiles();
        applicationManagement.setScansCount( scans != null ? scans.length : 0 );

        return applicationManagement;
    }

    public static String validateApplicationPublicIdInternal( String applicationPublicId )
    {
        if ( applicationDAO.getByPublicId( applicationPublicId ) == null )
        {
            return "Invalid application id " + applicationPublicId;
        }

        log.debug( "Found application with public id {}", applicationPublicId );
        return "OK";
    }
}
