/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.rule;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.service.InsightWork;

@Path( "/rest/rule/{appId}" )
public class RuleResource
{
    @Context
    InsightWork work;

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<Rule> getRules( @PathParam( "appId" ) final String appId )
    {
        return null;
    }
}
