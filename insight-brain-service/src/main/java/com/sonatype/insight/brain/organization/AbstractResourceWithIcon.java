/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.IconDAO;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.ErrorResponseGenerator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sun.jersey.core.header.FormDataContentDisposition;

abstract class AbstractResourceWithIcon
{
    public static final String GENERATE_ICON_PATH = "services/generateIcon/{hashcode}";

    private static final Logger log = LoggerFactory.getLogger( AbstractResourceWithIcon.class );

    @Context
    private InsightWork work;

    @Inject
    private SaasClient client;

    @Context
    private BaseUrl baseUrl;

    private ErrorResponseGenerator errorResponseGenerator = new ErrorResponseGenerator( false );

    protected void addIconInternal( String ownerId, boolean hasRobotSource, String robotHash,
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
                    new IconDAO().setIcon( ownerId, work.getIconDir(), sizeCheckedInputStream );
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

    protected Response addEditIconSync( String ownerId, boolean hasRobotSource, String robotHash,
                                        InputStream uploadedInputStream, FormDataContentDisposition fileDetail )
    {
        String errorMessage = null;
        try
        {
            addIconInternal( ownerId, hasRobotSource, robotHash, uploadedInputStream, fileDetail );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            errorMessage = errorResponseGenerator.mapException( e ).getMessageBody();
        }

        UriBuilder uriBuilder = baseUrl.redirect().path( InsightBrainService.BRAIN_ASSET_PATH ).path( "index.html" );
        if ( errorMessage != null )
        {
            uriBuilder = uriBuilder.queryParam( "errorMessage", errorMessage );
        }

        return Response.seeOther( uriBuilder.build() ).build();
    }

    @GET
    @Path( GENERATE_ICON_PATH )
    @Produces( "image/png" )
    public StreamingOutput generateIcon( @PathParam( "hashcode" ) final String hashcode,
                                         @Context final HttpServletRequest req )
        throws IOException
    {
        if ( hashcode == null || hashcode.isEmpty() )
        {
            throw new NotFoundException( "Null or empty hashcode." );
        }
        return StreamingOutput.class.cast( client.doProxy( req, "rest/application/icon/generate/" + hashcode ).getEntity() );
    }

    protected Response getIcon( final String ownerId )
        throws IOException
    {
        byte[] imageBytes = null;
        if ( ownerId != null )
        {
            imageBytes = new IconDAO().getIcon( ownerId, work.getIconDir() );
        }
        if ( imageBytes == null )
        {
            UriBuilder defaultIconUriBuilder =
                baseUrl.redirect().path( InsightBrainService.BRAIN_ASSET_PATH ).path( "img/" + getDefaultIconFilename() );
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

    protected abstract String getDefaultIconFilename();
}
