/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

public class Context
{
    private String contextTypeId;

    public Context()
    {
    }

    public Context( final String contextTypeId )
    {
        this.contextTypeId = contextTypeId;
    }

    public String getContextTypeId()
    {
        return contextTypeId;
    }

    public void setContextTypeId( final String contextTypeId )
    {
        this.contextTypeId = contextTypeId;
    }
}
