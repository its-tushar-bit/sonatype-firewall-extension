/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import org.apache.http.entity.StringEntity;

import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.UrlUtils;

public final class ReportClient
    extends AbstractServletClient<ReportClient>
{
    private final String appId;

    private final String scanId;

    public ReportClient( final Configuration config, final String appId, final String scanId )
    {
        super( config );

        this.appId = UrlUtils.encodeUrlComponent( appId );
        this.scanId = UrlUtils.encodeUrlComponent( scanId );
    }

    public Result html( final String path )
        throws IOException
    {
        return consume( request( "rest/report", appId, scanId, "html", path ).get() );
    }

    public Result printReport( final String projectName, final int buildNumber )
        throws IOException
    {
        final String[] params = { "projectName", projectName, "buildNumber", String.valueOf( buildNumber ) };
        return consume( request( "rest/report", appId, scanId, "printReport" ).query( params ).get() );
    }

    public Result artifactDetails( final String groupId, final String artifactId, final String version )
        throws IOException
    {
        final String[] params = { "groupId", groupId, "artifactId", artifactId, "version", version };
        return consume( request( "rest/report", appId, scanId, "artifactDetails" ).query( params ).get() );
    }

    public Result augmentData( final String path, final String jsonData )
        throws IOException
    {
        final StringEntity entity = new StringEntity( jsonData, "application/json", "UTF-8" );
        return consume( request( "rest/report", appId, scanId, "augmentData", path ).post( entity ) );
    }

    public Result auditLog( final String path, final String jsonKey )
        throws IOException
    {
        return consume( request( "rest/report", appId, scanId, "auditLog", path ).query( "key", jsonKey ).get() );
    }
}
