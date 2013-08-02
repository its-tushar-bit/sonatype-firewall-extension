/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.util.Map;

import com.sonatype.insight.client.utils.AbstractServletClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.ServletResult;
import com.sonatype.insight.client.utils.UrlUtils;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

public final class ReportClient
    extends AbstractServletClient<ReportClient>
{
    private final String appId;

    private final String scanId;

    public ReportClient( final Configuration config, final String appId, final String scanId )
    {
        super( config );

        if ( scanId == null || scanId.trim().isEmpty() )
        {
            throw new IllegalArgumentException( "Cannot create a ReportClient without a scanId" );
        }
        this.appId = UrlUtils.encodeUrlComponent( appId );
        this.scanId = UrlUtils.encodeUrlComponent( scanId );
    }

    public ServletResult embedReport( final String path )
        throws IOException
    {
        return path( "rest/report", appId, scanId, "embedReport", path ).get();
    }

    public ServletResult printReport( final String projectName, final int buildNumber )
        throws IOException
    {
        final String[] params = { "projectName", projectName, "buildNumber", String.valueOf( buildNumber ) };
        return path( "rest/report", appId, scanId, "printReport" ).query( params ).get();
    }

    public ServletResult artifactDetails( final String groupId, final String artifactId, final String version )
        throws IOException
    {
        final String[] params = { "groupId", groupId, "artifactId", artifactId, "version", version };
        return path( "rest/report", appId, scanId, "artifactDetails" ).query( params ).get();
    }

    public ServletResult augmentData( final String path, final String jsonData, final String user, final String where )
        throws IOException
    {
        final String[] params = { "user", user, "where", where };
    final StringEntity entity = new StringEntity(jsonData, ContentType.APPLICATION_JSON);
        return path( "rest/report", appId, scanId, "augmentData", path ).query( params ).post( entity );
    }

    public ServletResult auditLog( final String path, final String jsonKey )
        throws IOException
    {
        final String[] params = { "key", jsonKey };
        return path( "rest/report", appId, scanId, "auditLog", path ).query( params ).get();
    }

    public ServletResult handleRelativeToReport( final String path, final Map<String, String[]> query )
        throws IOException
    {
        return handle( UrlUtils.appendUrlPaths( "rest/report", appId, scanId, path ), query );
    }
}
