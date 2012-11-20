/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;

public class ComponentDAO
{
    public List<Component> getAll( byte[] licenseData, byte[] securityData )
    {
        Map<String, Component> componentsByGAV = new LinkedHashMap<String, Component>();

        // TODO load license data too
        JsonNode securityJson = loadJson( securityData );
        if ( securityJson != null )
        {
            securityJson = securityJson.get( "aaData" );
            if ( securityJson != null )
            {
                ArrayNode securityJsonArray = (ArrayNode) securityJson;
                for ( int i = 0; i < securityJsonArray.size(); i++ )
                {
                    JsonNode securityVulnerabilityJson = securityJsonArray.get( i );
                    String groupId = securityVulnerabilityJson.get( "groupId" ).asText();
                    String artifactId = securityVulnerabilityJson.get( "artifactId" ).asText();
                    String version = securityVulnerabilityJson.get( "version" ).asText();
                    String source = securityVulnerabilityJson.get( "source" ).asText();
                    String reference = securityVulnerabilityJson.get( "reference" ).asText();
                    JsonNode scoreJson = securityVulnerabilityJson.get( "score" );
                    Float score = ( scoreJson == null ? null : (float) scoreJson.asDouble() );

                    String key = getComponentKey( groupId, artifactId, version );
                    Component component = componentsByGAV.get( key );
                    if ( component == null )
                    {
                        component = new Component();
                        component.setGroupId( groupId );
                        component.setArtifactId( artifactId );
                        component.setVersion( version );
                        componentsByGAV.put( key, component );
                    }
                    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
                    securityVulnerability.setSource( source );
                    securityVulnerability.setRefId( reference );
                    securityVulnerability.setScore( score );
                    component.addSecurityVulnerability( securityVulnerability );
                }
            }
        }

        List<Component> result = new ArrayList<Component>();
        result.addAll( componentsByGAV.values() );
        return result;
    }

    private String getComponentKey( String groupId, String artifactId, String version )
    {
        return groupId + ':' + artifactId + ':' + version;
    }

    private JsonNode loadJson( byte[] data )
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree( data );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( e );
        }
    }
}
