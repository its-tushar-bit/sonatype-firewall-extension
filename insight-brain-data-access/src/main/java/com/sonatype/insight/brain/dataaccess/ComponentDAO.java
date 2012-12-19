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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.json.store.JsonUtils;

public class ComponentDAO
{
    public List<Component> getAll( final byte[] licenseData, final byte[] securityData )
    {
        final Map<String, Component> componentsByGAV = new LinkedHashMap<String, Component>();

        JsonNode licenseJson = loadJson( licenseData );
        if ( licenseJson != null )
        {
            licenseJson = licenseJson.get( "aaData" );
            if ( licenseJson != null )
            {
                final ArrayNode licenseJsonArray = (ArrayNode) licenseJson;
                for ( int i = 0; i < licenseJsonArray.size(); i++ )
                {
                    final JsonNode artifactLicenseJson = licenseJsonArray.get( i );
                    final String groupId = artifactLicenseJson.get( "groupId" ).asText();
                    final String artifactId = artifactLicenseJson.get( "artifactId" ).asText();
                    final String version = artifactLicenseJson.get( "version" ).asText();
                    final String licenseThreat = artifactLicenseJson.get( "effectiveLicenseThreat" ).asText();

                    final String key = getComponentKey( groupId, artifactId, version );
                    Component component = componentsByGAV.get( key );
                    if ( component == null )
                    {
                        component = new Component();
                        component.setGroupId( groupId );
                        component.setArtifactId( artifactId );
                        component.setVersion( version );
                        componentsByGAV.put( key, component );
                    }
                    component.setLicenseThreat( licenseThreat );
                    component.setDeclaredLicenseNames( jsonStringArrayToList( artifactLicenseJson.get( "declaredLicenses" ) ) );
                    component.setObservedLicenseNames( jsonStringArrayToList( artifactLicenseJson.get( "observedLicenses" ) ) );
                    // TODO Load effective license data too?
                }
            }
        }
        JsonNode securityJson = loadJson( securityData );
        if ( securityJson != null )
        {
            securityJson = securityJson.get( "aaData" );
            if ( securityJson != null )
            {
                final ArrayNode securityJsonArray = (ArrayNode) securityJson;
                for ( int i = 0; i < securityJsonArray.size(); i++ )
                {
                    final JsonNode securityVulnerabilityJson = securityJsonArray.get( i );
                    final String groupId = securityVulnerabilityJson.get( "groupId" ).asText();
                    final String artifactId = securityVulnerabilityJson.get( "artifactId" ).asText();
                    final String version = securityVulnerabilityJson.get( "version" ).asText();
                    final String source = securityVulnerabilityJson.get( "source" ).asText();
                    final String reference = securityVulnerabilityJson.get( "reference" ).asText();
                    final JsonNode severityJson = securityVulnerabilityJson.get( "score" );
                    final Float severity = severityJson == null ? null : (float) severityJson.asDouble();

                    final String key = getComponentKey( groupId, artifactId, version );
                    Component component = componentsByGAV.get( key );
                    if ( component == null )
                    {
                        component = new Component();
                        component.setGroupId( groupId );
                        component.setArtifactId( artifactId );
                        component.setVersion( version );
                        componentsByGAV.put( key, component );
                    }
                    final SecurityVulnerability securityVulnerability = new SecurityVulnerability();
                    securityVulnerability.setSource( source );
                    securityVulnerability.setRefId( reference );
                    securityVulnerability.setSeverity( severity );
                    component.addSecurityVulnerability( securityVulnerability );
                }
            }
        }

        final List<Component> result = new ArrayList<Component>();
        result.addAll( componentsByGAV.values() );
        return result;
    }

    private static String getComponentKey( final String groupId, final String artifactId, final String version )
    {
        return groupId + ':' + artifactId + ':' + version;
    }

    private static JsonNode loadJson( final byte[] data )
    {
        try
        {
            return JsonUtils.parse( data );
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private List<String> jsonStringArrayToList( JsonNode jsonNode )
    {
        if ( jsonNode == null )
        {
            return null;
        }
        ArrayNode jsonArray = (ArrayNode) jsonNode;
        if ( jsonArray.size() == 0 )
        {
            return null;
        }
        List<String> result = new ArrayList<String>();
        for ( int i = 0; i < jsonArray.size(); i++ )
        {
            result.add( jsonArray.get( i ).asText() );
        }
        return result;
    }
}
