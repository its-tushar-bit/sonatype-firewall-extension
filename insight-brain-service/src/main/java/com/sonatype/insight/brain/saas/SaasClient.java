/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionManagerFactory;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractInjectable;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionResource;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotAuthenticatedException;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;

@Named
@Singleton
public class SaasClient
    extends AbstractInjectable<SaasClient>
{
    private final Logger log = LoggerFactory.getLogger( SaasClient.class );

    private final Configuration config;

    private final HttpClient client;
    
    private final CLMLicenseManager licenseManager;

    private static volatile String version;
    
    public static final String UPLOAD_FILE_ATTRIBUTE = "saas.upload.file";

    @Inject
    public SaasClient( final InsightProxy proxy, final CLMLicenseManager licenseManager )
    {
        this.licenseManager = licenseManager;
        config = proxy.contextualize( new Configuration() );
        client = HttpClientUtils.createConfig( config );
        client.getParams().setParameter( ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME,
                                         PoolingClientConnectionManagerFactory.class.getName() );
        // TODO Need to determine if there is additional information we should be sending to the SaaS
        loadVersion();
    }
    
    public HttpResponse getResponse( HttpServletRequest request, String path, Map<String, String> queryParams,
                                     String... uriParams )
        throws IOException
    {
        return execute( request, path, queryParams, uriParams );
    }

    public <T> T get( Class<T> clazz, String path, Map<String,String> queryParams, String... uriParams )
        throws IOException
    {
        return get( null, clazz, path, queryParams, uriParams );
    }

    public <T> T get( HttpServletRequest request, Class<T> clazz, String path, String... uriParams )
        throws IOException
    {
        return get( request, clazz, path, null, uriParams );
    }

    public <T> T get( HttpServletRequest request, Class<T> clazz, String path, Map<String,String> queryParams, String... uriParams )
        throws IOException
    {
        boolean usingStream = false;
        HttpResponse response = getResponse( request, path, queryParams, uriParams );
        try
        {
            switch ( response.getStatusLine().getStatusCode() )
            {
                case 200:
                case 202:
                    HttpEntity entity = response.getEntity();
                    if ( entity == null )
                    {
                        return null;
                    }
                    else if ( String.class.equals( clazz ) )
                    {
                        return clazz.cast( EntityUtils.toString( entity, "UTF-8" ) );
                    }
                    else if ( InputStream.class.equals( clazz ) )
                    {
                        usingStream = true;
                        return clazz.cast( entity.getContent() );
                    }
                    else
                    {
                        InputStream in = null;
                        try
                        {
                            in = entity.getContent();
                            return JsonUtils.parse( IOUtil.toByteArray( in ), clazz );
                        }
                        finally
                        {
                            IOUtil.close( in );
                        }
                    }
                case 400:
                    throw new BadRequestException( getErrorMessage( response ) );
                case 401:
                    throw new NotAuthenticatedException( getErrorMessage( response ) );
                case 402:
                    throw new PaymentRequiredException( getErrorMessage( response ) );
                case 403:
                    throw new NotAuthorizedException( getErrorMessage( response ) );
                case 404:
                    throw new NotFoundException( getErrorMessage( response ) );
                case 409:
                    throw new ConflictException( getErrorMessage( response ) );
                default:
                    throw new InternalServerException( "SAAS Error: " + getErrorMessage( response ) );
            }
        }
        finally
        {
            if ( !usingStream )
            {
                try
                {
                    EntityUtils.consume( response.getEntity() );
                }
                catch ( IOException e )
                {
                    log.error( "Failed to consume response entity", e );
                }
            }
        }
    }

    private String getErrorMessage( HttpResponse response )
        throws IOException
    {
        Header hdr = response.getFirstHeader( HttpHeaders.CONTENT_TYPE );
        if ( hdr != null && hdr.getValue() != null && hdr.getValue().contains( HTTP.PLAIN_TEXT_TYPE )
            && response.getEntity() != null )
        {
            return EntityUtils.toString( response.getEntity(), "UTF-8" );
        }
        return response.getStatusLine().getReasonPhrase();
    }

    public Response doProxy( HttpServletRequest request, String path, String... uriParams )
        throws IOException
    {
        return doProxy( request, path, null, uriParams );
    }

    public Response doProxy( HttpServletRequest request, String path, Map<String,String> queryParams, String... uriParams )
        throws IOException
    {
        HttpResponse response = getResponse( request, path, queryParams, uriParams );
        return buildResponse( response );
    }

    private HttpResponse execute( HttpServletRequest request, String path, Map<String,String> queryParams, String... uriParams )
        throws IOException
    {
        HttpUriRequest cloudReq;
        if ( request == null || "GET".equals( request.getMethod() ) )
        {
            cloudReq = new HttpGet( buildUri( request, path, queryParams, uriParams ) );
        }
        else if ( "POST".equals( request.getMethod() ) )
        {
            cloudReq = new HttpPost( buildUri( request, path, queryParams, uriParams ) );
            
            ( (HttpPost) cloudReq ).setEntity( new BufferedHttpEntity( buildEntity( request ) ) );
        }
        else if ( "PUT".equals( request.getMethod() ) )
        {
            cloudReq = new HttpPut( buildUri( request, path, queryParams, uriParams ) );
            
            ( (HttpPut) cloudReq ).setEntity( new BufferedHttpEntity( buildEntity( request ) ) );
        }
        else if ( "DELETE".equals( request.getMethod() ) )
        {
            cloudReq = new HttpDelete( buildUri( request, path, queryParams, uriParams ) );
        }
        else
        {
            throw new IllegalArgumentException( "Unknown request method " + request.getMethod() );
        }
        populateRequest( request, cloudReq );
        try
        {
            return client.execute( cloudReq );
        }
        catch ( HttpHostConnectException e )
        {
            throw new GatewayTimeoutException( e.getMessage(), e );
        }
    }
    
    //note i hide this warning because the http client should be handling closing the new stream i create below
    @SuppressWarnings( "resource" )
    private HttpEntity buildEntity( HttpServletRequest request )
        throws IOException
    {
        File uploadFile = ( File ) request.getAttribute( UPLOAD_FILE_ATTRIBUTE );
        
        InputStream is = null;
        long length = request.getContentLength();
        
        if ( uploadFile != null )
        {
            is = new FileInputStream( uploadFile );
            length = uploadFile.length();
        }
        else
        {
            is = request.getInputStream();
        }
        
        return new InputStreamEntity( is, length );
    }

    private void populateRequest( final HttpServletRequest orig, HttpUriRequest req )
    {
        if ( orig != null )
        {
            for ( Enumeration<String> e = orig.getHeaderNames(); e.hasMoreElements(); )
            {
                String headerName = e.nextElement();
                if ( !HttpHeaders.CONNECTION.equals( headerName ) && !HttpHeaders.HOST.equals( headerName )
                    && !HttpHeaders.ACCEPT_ENCODING.equals( headerName )
                    && !HttpHeaders.TRANSFER_ENCODING.equals( headerName )
                    && !HttpHeaders.CONTENT_LENGTH.equals( headerName )
                    && !HttpHeaders.CONTENT_ENCODING.equals( headerName ) )
                {
                    req.setHeader( headerName, orig.getHeader( headerName ) );
                }
            }
        }
        req.setHeader( "X-Brain-Version", version );
        req.setHeader( "X-CLM-Token", licenseManager.getLicenseFingerprint() );
    }

    private Response buildResponse( final HttpResponse response )
    {
        ResponseBuilder builder = Response.status( response.getStatusLine().getStatusCode() );

        // pass-back response metadata+content to servlet
        for ( final Header h : response.getAllHeaders() )
        {
            final String name = h.getName();
            // ignore Transfer-Encoding since httpclient should have handled it
            if ( !HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase( name )
                && !HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase( name )
                && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase( name )
                && !HttpHeaders.CONTENT_TYPE.equalsIgnoreCase( name ) )
            {
                builder.header( name, h.getValue() );
            }
        }

        final HttpEntity entity = response.getEntity();
        if ( entity != null )
        {
            if ( entity.getContentEncoding() != null )
            {
                builder.header( HttpHeaders.CONTENT_ENCODING, entity.getContentEncoding().getValue() );
            }
            builder.header( HttpHeaders.CONTENT_LENGTH, entity.getContentLength() );
            if ( entity.getContentType() != null )
            {
                builder.header( HttpHeaders.CONTENT_TYPE, entity.getContentType().getValue() );
            }

            builder.entity( new StreamingOutput()
            {
                @Override
                public void write( OutputStream output )
                    throws IOException
                {
                    entity.writeTo( output );
                }
            } );
        }
        return builder.build();
    }
    
    private String buildUri( HttpServletRequest base, String path, Map<String,String> queryParams, String... uriParams )
    {
        UriBuilder uriBuilder = UriBuilder.fromUri( config.getServerUrl() );
        uriBuilder.path( path );
        if ( base != null )
        {
            uriBuilder.replaceQuery( base.getQueryString() );
        }
        
        if ( queryParams != null )
        {
            for ( Entry<String, String> queryParam : queryParams.entrySet() )
            {
                uriBuilder.queryParam( queryParam.getKey(), queryParam.getValue() );
            }
        }
        
        String result = uriBuilder.build( (Object[]) uriParams ).toString();
        log.debug( "Constructed Saas URI: " + result );
        return result;
    }

    public static class PoolingClientConnectionManagerFactory
        implements ClientConnectionManagerFactory
    {

        @Override
        public ClientConnectionManager newInstance( HttpParams params, SchemeRegistry schemeRegistry )
        {
            ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager();
            connManager.setDefaultMaxPerRoute( connManager.getMaxTotal() );
            return connManager;
        }

    }

    private void loadVersion()
    {
        if ( version != null )
        {
            return;
        }
        try
        {
            Properties prop = VersionResource.get();
            version = prop.getProperty( "version", "Unknown" );
        }
        catch ( IOException e )
        {
            log.error( "Failed to load version", e );
            version = "Unknown";
        }
    }
}
