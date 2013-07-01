/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ValidationResult;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

public class PolicyDAO
{
    public static final String POLICY_FILENAME = "policy.json";

    private static final Logger log = LoggerFactory.getLogger( PolicyDAO.class );

    private final File workDir;

    private String user;

    private String ip;

    private String where;

    public PolicyDAO( final File workDir )
    {
        this.workDir = workDir;
    }

    public List<Policy> getByOwnerId( final String ownerId )
    {
        final List<Policy> result = new ArrayList<Policy>();
        final JsonStore store = policyStore( ownerId );
        try
        {
            final ArrayNode policies = loadPolicies( store );
            Collections.addAll( result, JsonUtils.asPojo( policies, Policy[].class ) );
        }
        catch ( final IOException e )
        {
            log.error( "Failed to load policies", e );
            throw new IllegalStateException( e );
        }
        return result;
    }

    public Policy insert( final String ownerId, final Policy policy )
    {
        ValidationResult validationResult = policy.validate( ownerId );
        if ( validationResult != null && !validationResult.isValid() )
        {
            throw new InvalidPolicyException( validationResult );
        }

        final JsonStore store = policyStore( ownerId );
        try
        {
            final ArrayNode policiesJson = loadPolicies( store );
            Policy[] existingPolicies = JsonUtils.asPojo( policiesJson, Policy[].class );
            for ( Policy existingPolicy : existingPolicies )
            {
                if ( policy.getName().equals( existingPolicy.getName() ) )
                {
                    throw new InvalidPolicyException( "A policy with name '" + policy.getName() + "' exists already" );
                }
            }

            // Allocate unique ids to the policy and its constraints
            policy.setId( newUUID() );
            for ( Constraint constraint : policy.getConstraints() )
            {
                constraint.setId( newUUID() );
            }

            policiesJson.add( JsonUtils.asTree( policy ) );
            savePolicies( store, policiesJson );
        }
        catch ( final IOException e )
        {
            log.error( "Failed to insert policy {}", policy, e );
            throw new IllegalStateException( e );
        }
        return policy;
    }

    public Policy update( final String ownerId, final Policy policy )
    {
        ValidationResult validationResult = policy.validate( ownerId );
        if ( validationResult != null && !validationResult.isValid() )
        {
            throw new InvalidPolicyException( validationResult );
        }

        final JsonStore store = policyStore( ownerId );
        try
        {
            boolean updated = false;
            final ArrayNode policiesJson = loadPolicies( store );
            for ( int i = 0; i < policiesJson.size(); i++ )
            {
                JsonNode oldPolicyJson = policiesJson.get( i );
                Policy existingPolicy = JsonUtils.asPojo( oldPolicyJson, Policy.class );
                if ( policy.getId().equals( existingPolicy.getId() ) )
                {
                    // Allocate ids to new constraints
                    for ( Constraint constraint : policy.getConstraints() )
                    {
                        if ( existingPolicy.getConstraintById( constraint.getId() ) == null )
                        {
                            // This is a new constraint
                            constraint.setId( newUUID() );
                        }
                    }

                    // Update the policy
                    policiesJson.set( i, JsonUtils.asTree( policy ) );
                    updated = true;
                }
                else
                {
                    if ( policy.getName().equals( existingPolicy.getName() ) )
                    {
                        throw new InvalidPolicyException( "A policy with name '" + policy.getName()
                            + "' exists already" );
                    }
                }
            }

            if ( !updated )
            {
                throw new InvalidPolicyException( "The policy does not exist" );
            }

            savePolicies( store, policiesJson );
        }
        catch ( final IOException e )
        {
            log.error( "Failed to update policy {}", policy, e );
            throw new IllegalStateException( e );
        }
        return policy;
    }

    public void delete( final String ownerId, final String policyId )
    {
        final JsonStore store = policyStore( ownerId );
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
        catch ( final IOException e )
        {
            log.error( "Failed to delete policy {}", policyId, e );
            throw new IllegalStateException( e );
        }
    }

    public void deleteByOwnerId( final String ownerId )
    {
        final File policyDir = getPolicyDir( ownerId );
        try
        {
            FileUtils.deleteDirectory( policyDir );
        }
        catch ( IOException e )
        {
            log.error( "Failed to bulk delete policies for {}", ownerId, e );
            throw new IllegalStateException( e );
        }
    }

    public PolicyDAO session( final String _user, final String _ip, final String _where )
    {
        user = _user;
        ip = _ip;
        where = _where;
        return this;
    }

    private static ArrayNode loadPolicies( final JsonStore store )
        throws IOException
    {
        final ArrayNode policies = (ArrayNode) store.restore( POLICY_FILENAME );
        return policies != null ? policies : JsonUtils.arrayNode( null );
    }

    private void savePolicies( final JsonStore store, final ArrayNode policies )
        throws IOException
    {
        store.commit( POLICY_FILENAME, JsonUtils.stamp( user, ip, where, policies ) );
    }

    private JsonStore policyStore( final String ownerId )
    {
        return JsonUtils.fileStore( getPolicyDir( ownerId ) );
    }

    public File getPolicyDir( final String ownerId )
    {
        return new File( workDir, "policy/" + ownerId );
    }

    private static String newUUID()
    {
        return UUID.randomUUID().toString().replace( "-", "" );
    }
}
