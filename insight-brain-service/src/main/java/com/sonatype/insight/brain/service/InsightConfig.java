/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URL;

import javax.mail.internet.InternetAddress;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonatype.insight.portal.mail.MailConfig;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.HttpConfiguration;
import com.yammer.dropwizard.validation.ValidationMethod;

public class InsightConfig
    extends Configuration
{
    private static final Logger log = LoggerFactory.getLogger( InsightConfig.class );

    {
        setHttpConfiguration( new HttpConfiguration()
        {
            {
                setPort( 8070 );
                setAdminPort( 8070 );
            }
        } );
    }

    @NotNull
    @JsonProperty
    private ProxyConfig proxy = new ProxyConfig();

    @NotNull
    @JsonProperty
    private MailConfig mail = new MailConfig()
    {
        {
            setHostname( "127.0.0.1" );
            setPort( 587 );
            setSystemEmail( "SonatypeCLM@localhost" );
            setSystemPersonal( "Sonatype CLM" );
        }
    };

    @JsonProperty
    private String baseUrl;

    @NotNull
    @JsonProperty
    private String saasAddress = "https://insight.sonatype.com/";

    @NotNull
    @JsonProperty
    private String sonatypeWork = "sonatype-work/clm-server";

    @NotNull
    @JsonProperty
    private int releaseGraphCacheSize = 1000;

    public ProxyConfig getProxyConfig()
    {
        return proxy;
    }

    public MailConfig getMailConfig()
    {
        return mail;
    }

    public int getReleaseGraphCacheSize()
    {
        return releaseGraphCacheSize;
    }

    public String getSaasAddress()
    {
        return saasAddress;
    }

    public File getSonatypeWork()
    {
        return new File( sonatypeWork );
    }

    public File getConfigDir()
    {
        return new File( sonatypeWork, "config" );
    }

    public void setProxyConfig( ProxyConfig proxyConfig )
    {
        this.proxy = proxyConfig;
    }

    public void setMailConfig( final MailConfig mailConfig )
    {
        this.mail = mailConfig;
    }

    public void setReleaseGraphCacheSize( int releaseGraphCacheSize )
    {
        this.releaseGraphCacheSize = releaseGraphCacheSize;
    }

    public void setSaasAddress( final String saasAddress )
    {
        this.saasAddress = saasAddress;
    }

    public void setSonatypeWork( final String sonatypeWork )
    {
        this.sonatypeWork = sonatypeWork;
    }

    public String getBaseUrl()
    {
        if ( baseUrl != null )
        {
            return baseUrl;
        }
        return "http://localhost:" + getHttpConfiguration().getPort() + "/";
    }

    public void setBaseUrl( String baseUrl )
    {
        this.baseUrl = baseUrl;
        if ( baseUrl != null && !baseUrl.endsWith( "/" ) )
        {
            this.baseUrl += '/';
        }
    }

    @ValidationMethod( message = "baseUrl is invalid" )
    public boolean isValidBaseUrl()
    {
        try
        {
            new URL( getBaseUrl() );
            return true;
        }
        catch ( Exception e )
        {
            log.error( "Invalid baseUrl: {}", e.getMessage() );
            return false;
        }
    }

    @ValidationMethod( message = "mail.systemEmail is invalid" )
    public boolean isValidSystemMailAddress()
    {
        try
        {
            new InternetAddress( getMailConfig().getSystemEmail() );
            return true;
        }
        catch ( Exception e )
        {
            log.error( "Invalid mail.systemEmail: {}", e.getMessage() );
            return false;
        }
    }
}
