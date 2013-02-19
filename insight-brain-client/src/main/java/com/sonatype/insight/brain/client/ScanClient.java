/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;

import org.apache.http.entity.FileEntity;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadResult;

public final class ScanClient
    extends AbstractClient
{
    private final String appId;

    public ScanClient( final Configuration config, final String appId )
    {
        super( config );

        this.appId = UrlUtils.encodeUrlComponent( appId );
    }

    public String uploadCiScan( final File scanFile )
        throws IOException
    {
        final Result result = path( "rest/ci/scan", appId ).put( new FileEntity( scanFile, "application/x-gzip" ) );
        return handleUpload( result );
    }

    public String uploadRepoManScan( final File scanFile )
        throws IOException
    {
        final Result result = path( "rest/rm/scan", appId ).put( new FileEntity( scanFile, "application/x-gzip" ) );
        return handleUpload( result );
    }

    private String handleUpload( Result result )
        throws IOException
    {
        final int status = result.status();
        final String text = result.text();
        if ( status >= 300 )
        {
            throw new IOException( "Error code " + status + ": " + text );
        }
        BOMCheckScanUploadResult dto = JsonUtils.parse( text, BOMCheckScanUploadResult.class );
        return dto.getScanId();
    }
}
