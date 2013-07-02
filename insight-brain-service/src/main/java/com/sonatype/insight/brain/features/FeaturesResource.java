/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Named
@Path( FeaturesResource.SERVICE_PATH )
public class FeaturesResource
{
    public static final String SERVICE_PATH = "rest/features";

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<String> getFeatures()
    {
        // Changes to this list should be replicated in brain.client.js
        return Arrays.asList( "policy", "labels", "release-graph", "policy-violations", "notification",
                              "reevaluate-policy" );
    }
}
