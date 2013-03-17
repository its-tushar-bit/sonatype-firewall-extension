/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.validation.PortRange;

public class ProxyConfig
{
    @JsonProperty
    private String host = null;

    @PortRange
    @JsonProperty
    private int port = 80;

    @JsonProperty
    private String auth = null;

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getAuth()
    {
        return auth;
    }

    public void setHost( final String host )
    {
        this.host = host;
    }

    public void setPort( final int port )
    {
        this.port = port;
    }

    public void setAuth( final String auth )
    {
        this.auth = auth;
    }
}
