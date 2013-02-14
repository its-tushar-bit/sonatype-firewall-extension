/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

public class RestClientConfiguration
{

    private final Configuration config;

    public RestClientConfiguration()
    {
        config = new Configuration();
    }

    Configuration getConfig()
    {
        return config;
    }

    public String getServerUrl()
    {
        return config.getServerUrl();
    }

    public RestClientConfiguration setServerUrl( final String serverUrl )
    {
        config.setServerUrl( serverUrl );
        return this;
    }

    public String getProxyHost()
    {
        return config.getProxyHost();
    }

    public RestClientConfiguration setProxyHost( final String proxyHost )
    {
        config.setProxyHost( proxyHost );
        return this;
    }

    public int getProxyPort()
    {
        return config.getProxyPort();
    }

    public RestClientConfiguration setProxyPort( final int proxyPort )
    {
        config.setProxyPort( proxyPort );
        return this;
    }

    public RestClientConfiguration setProxy( final String proxy )
    {
        config.setProxy( proxy );
        return this;
    }

}
