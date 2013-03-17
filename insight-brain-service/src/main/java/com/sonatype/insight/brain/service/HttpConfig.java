/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.yammer.dropwizard.config.HttpConfiguration;

/**
 * Custom {@link HttpConfiguration} with updated defaults. We used to set them externally in InsightConfig, but if
 * someone chose to customize one of the properties then the newly deserialized class would not include our changes.
 * Setting them in the constructor means they always get applied first. Uses mixin to apply "JsonDeserialize.as".
 */
@JsonDeserialize( as = HttpConfig.class )
public class HttpConfig
    extends HttpConfiguration
{
    public static class Module
        extends SimpleModule
    {
        public Module()
        {
            // makes it look like JsonDeserialize.as was on original class
            setMixInAnnotation( HttpConfiguration.class, HttpConfig.class );
        }
    }

    public HttpConfig()
    {
        setPort( 8070 );
        setAdminPort( 8070 );
    }
}
