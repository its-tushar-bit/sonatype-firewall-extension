/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import org.codehaus.jackson.annotate.JsonProperty;

import com.yammer.dropwizard.config.Configuration;

public class InsightConfiguration
    extends Configuration
{
    @JsonProperty
    private String saasAddress = "http://127.0.0.1:8085/insight-portal/";

    @JsonProperty
    private String sonatypeWork = "sonatype-work";

    public String getSaasAddress()
    {
        return saasAddress;
    }

    public File getSonatypeWork()
    {
        return new File( sonatypeWork );
    }
}
