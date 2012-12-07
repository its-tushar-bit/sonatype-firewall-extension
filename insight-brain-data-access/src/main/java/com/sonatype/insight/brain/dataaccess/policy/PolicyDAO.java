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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sonatype.insight.brain.model.policy.Policy;

public class PolicyDAO
{
    public static final String POLICY_FILENAME = "policy.json";

    private static final Logger log = LoggerFactory.getLogger( PolicyDAO.class );

    private final File dataStoreDir;

    public PolicyDAO( final File dataStoreDir )
    {
        this.dataStoreDir = dataStoreDir;
    }

    public List<Policy> getByApplicationId( final String applicationId )
    {
        final File policyFile = getPolicyFile( applicationId );
        log.debug( "Loading policies from {}", policyFile.getAbsolutePath() );
        return loadJson( policyFile );
    }

    public void insert( final String applicationId, final Policy policy )
    {
        final File policyFile = getPolicyFile( applicationId );
        final List<Policy> policies = loadJson( policyFile );

        if ( policy.getId() == null || policy.getId().trim().isEmpty() )
        {
            policy.setId( newUUID() );
        }
        else
        {
            // TODO Throw an exception if the policy exists already
        }
        policies.add( policy );

        saveJson( policyFile, policies );
    }

    public void update( final String applicationId, final Policy policy )
    {
        // TODO Throw an exception if the policy does not exist
        final File policyFile = getPolicyFile( applicationId );
        final List<Policy> policies = loadJson( policyFile );
        for ( int i = 0; i < policies.size(); i++ )
        {
            if ( policy.getId().equals( policies.get( i ).getId() ) )
            {
                policies.set( i, policy );
                break;
            }
        }

        saveJson( policyFile, policies );
    }

    public void delete( final String applicationId, final String policyId )
    {
        // TODO Throw an exception if the policy does not exist ?
        final File policyFile = getPolicyFile( applicationId );
        final List<Policy> policies = loadJson( policyFile );
        for ( int i = 0; i < policies.size(); i++ )
        {
            if ( policyId.equals( policies.get( i ).getId() ) )
            {
                policies.remove( i );
                break;
            }
        }

        saveJson( policyFile, policies );
    }

    private File getPolicyFile( final String applicationId )
    {
        return new File( new File( dataStoreDir, applicationId ), POLICY_FILENAME );
    }

    private static void saveJson( final File policyFile, final List<Policy> policies )
    {
        try
        {
            policyFile.getParentFile().mkdirs();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure( SerializationFeature.INDENT_OUTPUT, true );
            mapper.writeValue( policyFile, policies );
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private static List<Policy> loadJson( final File policyFile )
    {
        final List<Policy> result = new ArrayList<Policy>();
        if ( !policyFile.exists() )
        {
            return result;
        }

        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            final Policy[] policies = mapper.readValue( policyFile, Policy[].class );
            Collections.addAll( result, policies );
            return result;
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private static String newUUID()
    {
        return UUID.randomUUID().toString().replace( "-", "" );
    }
}
