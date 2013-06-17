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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.ErrorResponseGenerator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
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

    private final InsightWork work;

    private final BaseUrl baseUrl;
    
    private final CLMLicenseManager licenseManager;

    private final SaasClient client;

    private final PolicyEvaluationUtils policyEvaluationUtils;

    @Inject
    public ApplicationResource( final InsightWork work, final BaseUrl baseUrl, final CLMLicenseManager licenseManager,
                                final SaasClient client, final PolicyEvaluationUtils policyEvaluationUtils )
    {
        this.work = work;
        this.baseUrl = baseUrl;
        this.licenseManager = licenseManager;
        this.client = client;
        this.policyEvaluationUtils = policyEvaluationUtils;
    }

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
    public Response getApplicationIcon( @PathParam( "applicationPublicId" )
    final String applicationPublicId )
        throws IOException
    {
        byte[] imageBytes = null;
        Application application = applicationDAO.getByPublicId( applicationPublicId );
        if ( application != null )
        {
            imageBytes = applicationDAO.getIcon( application.getId(), work.getIconDir() );
        }
        if ( imageBytes == null )
        {
            UriBuilder defaultIconUriBuilder =
                baseUrl.redirect().path( InsightBrainService.BRAIN_ASSET_PATH ).path( "img/defaulticon_application.png" );
            return Response.temporaryRedirect( defaultIconUriBuilder.build() ).build();
        }
        final byte[] imageOutputBytes = imageBytes;
        StreamingOutput stream = new StreamingOutput()
        {
            @Override
            public void write( OutputStream output )
                throws IOException, WebApplicationException
            {
                output.write( imageOutputBytes );
            }
        };
        return Response.ok( stream ).build();
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
            throw new NotFoundException( "Null or empty hashcode." );
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
                catch ( BadRequestException e )
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
            throw new PaymentRequiredException( "You have exceeded the licensed limit of " + appLimit
                + " applications." );
        }

        applicationDAO.insert( application );

        return getApplicationManagementSummary( application );
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationManagementSummary editApplication( Application application )
        throws IOException
    {
        applicationDAO.update( application );

        return getApplicationManagementSummary( application );
    }

    @DELETE
    @Path( GET_APPLICATION_PATH )
    public void deleteApplication( @PathParam( "applicationPublicId" ) final String applicationPublicId )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );

        PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );
        policyDAO.deleteByOwnerId( application.getId() );
        
        FileUtils.deleteDirectory( work.getScanDir( application.getId() ) );
        FileUtils.deleteDirectory( work.getAuditDir( application.getId() ) );
        FileUtils.deleteDirectory( work.getReportDir( application.getId() ) );

        // delete application last, this way the operation can be retried later if anything goes wrong
        applicationDAO.deleteWithIcon( application, work.getIconDir() );
    }

    private ApplicationManagementSummary getApplicationManagementSummary( final Application application )
        throws IOException
    {
        final String applicationPublicId = application.getPublicId();
        final String applicationId = application.getId();
        log.debug( "Found application with public id {}", applicationPublicId );

        final ApplicationManagementSummary applicationManagement =
            ApplicationManagementSummary.fromApplication( application );

        final List<PolicyEvaluation> policyEvaluationList =
            work.getMostRecentPolicyEvaluations( application.getId() );
        Map<String, PolicyEvaluation> policyEvaluations = new HashMap<String, PolicyEvaluation>();
        Map<String, PolicyEvaluationResult> policyEvaluationResults = new HashMap<String, PolicyEvaluationResult>();
        for ( PolicyEvaluation policyEvaluation : policyEvaluationList )
        {
            final Stage stage = policyEvaluation.getStage();
            policyEvaluations.put( stage.getStageTypeId(), policyEvaluation );

            List<PolicyAlert> alerts =
                policyEvaluationUtils.findOldPolicyAlerts( applicationPublicId, applicationId,
                                                           policyEvaluation.getScanId(), stage );
            final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
            policyEvaluationResult.setAlerts( alerts );
            policyEvaluationUtils.calculateCounters( policyEvaluationResult );

            // Alerts are not needed by the Application Management UI and greatly bloat the JSON response
            policyEvaluationResult.setAlerts( null );

            policyEvaluationResults.put( stage.getStageTypeId(), policyEvaluationResult );
        }

        applicationManagement.setPolicyEvaluations( policyEvaluations );
        applicationManagement.setPolicyEvaluationsResults( policyEvaluationResults );

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
