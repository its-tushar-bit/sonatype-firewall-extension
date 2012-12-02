/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.rule;

import java.io.File;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.RuleDAO;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.service.InsightWork;

@Path( RuleResource.SERVICE_PATH )
public class RuleResource
{
    public static final String SERVICE_PATH = "/rest/policy/rule/{appId}";

    private static final Logger log = LoggerFactory.getLogger( RuleResource.class );

    @Context
    private InsightWork work;

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<Rule> getRules( @PathParam( "appId" ) final String appId )
    {
        log.debug( "Received request to get all rules for appId {}", appId );

        File ruleDir = work.getRuleDir();
        log.debug( "Loading rules from {}", ruleDir.getAbsolutePath() );
        RuleDAO ruleDAO = new RuleDAO( ruleDir );
        return ruleDAO.getByApplicationId( appId );
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Rule addRule( @PathParam( "appId" ) final String appId, Rule rule )
    {
        log.debug( "Received request to add rule for appId {}", appId );

        File ruleDir = work.getRuleDir();
        RuleDAO ruleDAO = new RuleDAO( ruleDir );
        ruleDAO.insert( appId, rule );
        return rule;
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Rule updateRule( @PathParam( "appId" ) final String appId, Rule rule )
    {
        log.debug( "Received request to update rule for appId {}, rule id {}", appId, rule.getId() );

        File ruleDir = work.getRuleDir();
        RuleDAO ruleDAO = new RuleDAO( ruleDir );
        ruleDAO.update( appId, rule );
        return rule;
    }

    @DELETE
    @Path( "{ruleId}" )
    public void deleteRule( @PathParam( "appId" ) final String appId, @PathParam( "ruleId" ) final String ruleId )
    {
        log.debug( "Received request to delete rule for appId {}, rule id {}", appId, ruleId );

        File ruleDir = work.getRuleDir();
        RuleDAO ruleDAO = new RuleDAO( ruleDir );
        ruleDAO.delete( appId, ruleId );
        return;
    }
}
