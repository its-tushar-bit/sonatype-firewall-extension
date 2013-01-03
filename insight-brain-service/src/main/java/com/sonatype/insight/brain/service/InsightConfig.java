/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.HttpConfiguration;

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
    private String saasAddress = "https://insight.sonatype.com/";

    @NotNull
    @JsonProperty
    private String sonatypeWork = "sonatype-work/clm-server";

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

    public void setSaasAddress( final String saasAddress )
    {
        this.saasAddress = saasAddress;
    }

    public void setSonatypeWork( final String sonatypeWork )
    {
        this.sonatypeWork = sonatypeWork;
    }
}
