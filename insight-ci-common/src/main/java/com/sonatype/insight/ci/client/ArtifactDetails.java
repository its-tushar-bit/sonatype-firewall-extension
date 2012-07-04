/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import java.util.Map;

import com.sonatype.insight.scan.upload.ReportDataResult;

public final class ArtifactDetails
{
    public final int statusCode;

    public final Map<String, String> headers;

    public final byte[] data;

    ArtifactDetails( final ReportDataResult result )
    {
        statusCode = result.getStatusCode();
        headers = result.getHeaders();
        data = result.getData();
    }
}
