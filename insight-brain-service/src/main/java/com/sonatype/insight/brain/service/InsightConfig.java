/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URL;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonatype.insight.portal.mail.MailConfig;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.HttpConfiguration;
import com.yammer.dropwizard.validation.ValidationMethod;

public class InsightConfig
    extends Configuration
{
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
    private MailConfig mail = new MailConfig()
    {
        {
            setHostname( "127.0.0.1" );
            setPort( 587 );
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
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }
}
