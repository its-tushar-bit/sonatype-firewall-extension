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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import com.sonatype.insight.brain.model.rule.Rule;

public class RuleDAO
{
    public static final String RULE_FILENAME = "rule.json";

    private final File dataStoreDir;

    public RuleDAO( File dataStoreDir )
    {
        this.dataStoreDir = dataStoreDir;
    }

    public List<Rule> getByApplicationId( String applicationId )
    {
        File ruleFile = getRuleFile( applicationId );
        return loadJson( ruleFile );
    }

    public void insert( String applicationId, Rule rule )
    {
        File ruleFile = getRuleFile( applicationId );
        List<Rule> rules = loadJson( ruleFile );

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

    public void update( String applicationId, Rule rule )
    {
        // TODO Throw an exception if the rule does not exist
        File ruleFile = getRuleFile( applicationId );
        List<Rule> rules = loadJson( ruleFile );
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

    public void delete( String applicationId, Rule rule )
    {
        // TODO Throw an exception if the rule does not exist ?
        File ruleFile = getRuleFile( applicationId );
        List<Rule> rules = loadJson( ruleFile );
        for ( int i = 0; i < rules.size(); i++ )
        {
            if ( rule.getId().equals( rules.get( i ).getId() ) )
            {
                rules.remove( i );
                break;
            }
        }

        saveJson( ruleFile, rules );
    }

    private File getRuleFile( String applicationId )
    {
        return new File( new File( dataStoreDir, applicationId ), RULE_FILENAME );
    }

    private void saveJson( File ruleFile, List<Rule> rules )
    {
        try
        {
            ruleFile.getParentFile().mkdirs();
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure( SerializationConfig.Feature.INDENT_OUTPUT, true );
            mapper.writeValue( ruleFile, rules );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private List<Rule> loadJson( File ruleFile )
    {
        List<Rule> result = new ArrayList<Rule>();
        if ( !ruleFile.exists() )
        {
            return result;
        }

        try
        {
            ObjectMapper mapper = new ObjectMapper();
            Rule[] rules = mapper.readValue( ruleFile, Rule[].class );
            result.addAll( Arrays.asList( rules ) );
            return result;
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private String newUUID()
    {
        return UUID.randomUUID().toString().replace( "-", "" );
    }
}
