/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.insight.client.utils.Result;

@SuppressWarnings( "serial" )
public class ClientException
    extends RuntimeException
{
    private final Result result;

    public ClientException( Result result )
    {
        this.result = result;
    }

    public ClientException( Result result, Throwable cause )
    {
        super( cause );
        this.result = result;
    }

    public Result getResult()
    {
        return result;
    }
}
