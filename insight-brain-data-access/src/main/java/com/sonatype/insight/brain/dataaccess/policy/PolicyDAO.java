/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

public class PolicyDAO
{
    public static final String POLICY_FILENAME = "policy.json";

    private static final Logger log = LoggerFactory.getLogger( PolicyDAO.class );

    private final File workDir;

    public PolicyDAO( final File workDir )
    {
        this.workDir = workDir;
    }

    public List<Policy> getByApplicationId( final String appId )
    {
        final List<Policy> result = new ArrayList<Policy>();
        final JsonStore store = policyStore( appId );
        try
        {
            final ArrayNode policies = loadPolicies( store );
            Collections.addAll( result, JsonUtils.asPojo( policies, Policy[].class ) );
        }
        catch ( final Exception e )
        {
            log.error( "Failed to load policies", e );
        }
        return result;
    }

    public Policy insert( final String appId, final Policy policy )
    {
        final JsonStore store = policyStore( appId );
        try
        {
            final ArrayNode policies = loadPolicies( store );
            if ( policy.getId() == null || policy.getId().trim().isEmpty() )
            {
                policy.setId( newUUID() );
            }
            else
            {
                // TODO Throw an exception if the policy exists already
            }
            policies.add( JsonUtils.asTree( policy ) );
            savePolicies( store, policies );
        }
        catch ( final Exception e )
        {
            log.error( "Failed to insert policy " + policy, e );
        }
        return policy;
    }

    public Policy update( final String appId, final Policy policy )
    {
        final JsonStore store = policyStore( appId );
        try
        {
            final ArrayNode policies = loadPolicies( store );
            for ( int i = 0; i < policies.size(); i++ )
            {
                if ( policy.getId().equals( policies.get( i ).get( "id" ).asText() ) )
                {
                    policies.set( i, JsonUtils.asTree( policy ) );
                    savePolicies( store, policies );
                    break;
                }
            }
            // TODO Throw an exception if the policy does not exist
        }
        catch ( final Exception e )
        {
            log.error( "Failed to update policy " + policy, e );
        }
        return policy;
    }

    public void delete( final String appId, final String policyId )
    {
        final JsonStore store = policyStore( appId );
        try
        {
            final ArrayNode policies = loadPolicies( store );
            for ( int i = 0; i < policies.size(); i++ )
            {
                if ( policyId.equals( policies.get( i ).get( "id" ).asText() ) )
                {
                    policies.remove( i );
                    savePolicies( store, policies );
                    break;
                }
            }
            // TODO Throw an exception if the policy does not exist
        }
        catch ( final Exception e )
        {
            log.error( "Failed to delete policy " + policyId, e );
        }
    }

    private static ArrayNode loadPolicies( final JsonStore store )
        throws IOException
    {
        final ArrayNode policies = (ArrayNode) store.restore( POLICY_FILENAME );
        return policies != null ? policies : JsonUtils.arrayNode( null );
    }

    private static void savePolicies( final JsonStore store, final ArrayNode policies )
        throws IOException
    {
        store.commit( POLICY_FILENAME, policies );
    }

    private JsonStore policyStore( final String appId )
    {
        return JsonUtils.fileStore( new File( workDir, "policy" + File.separatorChar + appId ) );
    }

    private static String newUUID()
    {
        return UUID.randomUUID().toString().replace( "-", "" );
    }
}
