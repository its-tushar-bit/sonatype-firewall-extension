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
import com.sonatype.insight.brain.model.component.LicenseStatus;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.json.store.JsonUtils;

public class ComponentDAO
{
    public List<Component> getAll( final byte[] licenseData, final byte[] securityData, final byte[] bomData )
    {
        final Map<String, Component> componentsByGAV = new LinkedHashMap<String, Component>();

        // Load license data. This is a little bit misleading since license data contains data that is not related to
        // licenses.
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

                    final String licenseCategory = artifactLicenseJson.get( "effectiveLicenseThreat" ).asText();
                    final String overriddenLicenseCategory =
                        JsonUtils.getNullableString( artifactLicenseJson.get( "overriddenLicenseThreat" ) );
                    final String statusString = JsonUtils.getNullableString( artifactLicenseJson.get( "status" ) );
                    final LicenseStatus status = LicenseStatus.getByName( statusString );
                    final long catalogDate = artifactLicenseJson.get( "catalogDate" ).asLong();
                    component.setLicenseCategoryId( licenseCategory );
                    component.setOverriddenLicenseCategoryId( overriddenLicenseCategory );
                    component.setDeclaredLicenseNames( jsonStringArrayToList( artifactLicenseJson.get( "declaredLicenses" ) ) );
                    component.setObservedLicenseNames( jsonStringArrayToList( artifactLicenseJson.get( "observedLicenses" ) ) );
                    component.setOverriddenLicenseNames( jsonStringArrayToList( artifactLicenseJson.get( "overriddenLicenses" ) ) );
                    // TODO Load effective license data too?
                    component.setLicenseStatus( status );
                    component.setCatalogDate( catalogDate );
                }
            }
        }

        // Load security data
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
                    final Float severity = JsonUtils.getNullableFloat( securityVulnerabilityJson.get( "score" ) );
                    final String statusString = JsonUtils.getNullableString( securityVulnerabilityJson.get( "status" ) );
                    final SecurityVulnerabilityStatus status = SecurityVulnerabilityStatus.getByName( statusString );

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
                    securityVulnerability.setStatus( status );
                    component.addSecurityVulnerability( securityVulnerability );
                }
            }
        }

        // Load bom data
        List<Component> unknownComponents = new ArrayList<Component>();
        JsonNode bomJson = loadJson( bomData );
        if ( bomJson != null )
        {
            bomJson = bomJson.get( "aaData" );
            if ( bomJson != null )
            {
                final ArrayNode bomJsonArray = (ArrayNode) bomJson;
                for ( int i = 0; i < bomJsonArray.size(); i++ )
                {
                    final JsonNode componentJson = bomJsonArray.get( i );
                    Component component;
                    final String matchStateString = componentJson.get( "matchState" ).asText();
                    final MatchState matchState = MatchState.getById( matchStateString );
                    if ( !matchState.equals( MatchState.UNKNOWN ) )
                    {
                        final String groupId = componentJson.get( "groupId" ).asText();
                        final String artifactId = componentJson.get( "artifactId" ).asText();
                        final String version = componentJson.get( "version" ).asText();
                        final JsonNode relativePopularityJson = componentJson.get( "relativePopularity" );
                        final int relativePopularity = (int) ( relativePopularityJson.asDouble() * 100 );

                        final String key = getComponentKey( groupId, artifactId, version );
                        component = componentsByGAV.get( key );
                        if ( component == null )
                        {
                            component = new Component();
                            component.setGroupId( groupId );
                            component.setArtifactId( artifactId );
                            component.setVersion( version );
                            componentsByGAV.put( key, component );
                        }
                        component.setRelativePopularity( relativePopularity );
                    }
                    else
                    {
                        // Unknown component
                        String hash = componentJson.get( "hash" ).asText();
                        component = new Component();
                        component.setHash( hash );
                        unknownComponents.add( component );
                    }
                    component.setMatchState( matchState );
                }
            }
        }

        final List<Component> result = new ArrayList<Component>();
        result.addAll( componentsByGAV.values() );
        result.addAll( unknownComponents );
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
