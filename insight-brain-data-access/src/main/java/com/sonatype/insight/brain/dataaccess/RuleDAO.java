/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sonatype.insight.brain.model.rule.Rule;

public class RuleDAO
{
    public static final String RULE_FILENAME = "rule.json";

    private static final Logger log = LoggerFactory.getLogger( RuleDAO.class );

    private final File dataStoreDir;

    public RuleDAO( final File dataStoreDir )
    {
        this.dataStoreDir = dataStoreDir;
    }

    public List<Rule> getByApplicationId( final String applicationId )
    {
        final File ruleFile = getRuleFile( applicationId );
        log.debug( "Loading rules from {}", ruleFile.getAbsolutePath() );
        return loadJson( ruleFile );
    }

    public void insert( final String applicationId, final Rule rule )
    {
        final File ruleFile = getRuleFile( applicationId );
        final List<Rule> rules = loadJson( ruleFile );

        if ( rule.getId() == null || rule.getId().trim().isEmpty() )
        {
            rule.setId( newUUID() );
        }
        else
        {
            // TODO Throw an exception if the rule exists already
        }
        rules.add( rule );

        saveJson( ruleFile, rules );
    }

    public void update( final String applicationId, final Rule rule )
    {
        // TODO Throw an exception if the rule does not exist
        final File ruleFile = getRuleFile( applicationId );
        final List<Rule> rules = loadJson( ruleFile );
        for ( int i = 0; i < rules.size(); i++ )
        {
            if ( rule.getId().equals( rules.get( i ).getId() ) )
            {
                rules.set( i, rule );
                break;
            }
        }

        saveJson( ruleFile, rules );
    }

    public void delete( final String applicationId, final String ruleId )
    {
        // TODO Throw an exception if the rule does not exist ?
        final File ruleFile = getRuleFile( applicationId );
        final List<Rule> rules = loadJson( ruleFile );
        for ( int i = 0; i < rules.size(); i++ )
        {
            if ( ruleId.equals( rules.get( i ).getId() ) )
            {
                rules.remove( i );
                break;
            }
        }

        saveJson( ruleFile, rules );
    }

    private File getRuleFile( final String applicationId )
    {
        return new File( new File( dataStoreDir, applicationId ), RULE_FILENAME );
    }

    private void saveJson( final File ruleFile, final List<Rule> rules )
    {
        try
        {
            ruleFile.getParentFile().mkdirs();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure( SerializationFeature.INDENT_OUTPUT, true );
            mapper.writeValue( ruleFile, rules );
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private List<Rule> loadJson( final File ruleFile )
    {
        final List<Rule> result = new ArrayList<Rule>();
        if ( !ruleFile.exists() )
        {
            return result;
        }

        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            final Rule[] rules = mapper.readValue( ruleFile, Rule[].class );
            result.addAll( Arrays.asList( rules ) );
            return result;
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private String newUUID()
    {
        return UUID.randomUUID().toString().replace( "-", "" );
    }
}
