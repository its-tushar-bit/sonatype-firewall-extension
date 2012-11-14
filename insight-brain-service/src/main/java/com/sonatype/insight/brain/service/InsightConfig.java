/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import javax.validation.constraints.NotNull;

import org.codehaus.jackson.annotate.JsonProperty;

import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.HttpConfiguration;

public class InsightConfig
    extends Configuration
{
    {
        http = new HttpConfiguration()
        {
            {
                port = adminPort = 8070;
            }
        };
    }

    @NotNull
    @JsonProperty
    protected String saasAddress = "https://insight.sonatype.com/";

    @NotNull
    @JsonProperty
    protected String sonatypeWork = "target/sonatype-work";

    public String getSaasAddress()
    {
        return saasAddress;
    }

    public File getSonatypeWork()
    {
        return new File( sonatypeWork );
    }
}
