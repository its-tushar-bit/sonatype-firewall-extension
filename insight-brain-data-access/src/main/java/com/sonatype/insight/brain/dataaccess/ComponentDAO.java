/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.LicenseStatus;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.json.store.JsonUtils;

public class ComponentDAO
{
    public List<Component> getAll( String applicationId, final byte[] licenseData, final byte[] securityData,
                                   final byte[] bomData, final byte[] dependencyData )
    {
        MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

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

                    final String statusString = JsonUtils.getNullableString( artifactLicenseJson.get( "status" ) );
                    final LicenseStatus status = LicenseStatus.getByName( statusString );
                    final long catalogDate = artifactLicenseJson.get( "catalogDate" ).asLong();
                    List<String> declaredLicenseNames =
                        jsonStringArrayToList( artifactLicenseJson.get( "declaredLicenses" ) );
                    component.setDeclaredLicenseIds( licenseNamesToIds( multiLicenseDAO, declaredLicenseNames ) );
                    List<String> observedLicenseNames =
                        jsonStringArrayToList( artifactLicenseJson.get( "observedLicenses" ) );
                    component.setObservedLicenseIds( licenseNamesToIds( multiLicenseDAO, observedLicenseNames ) );
                    List<String> overriddenLicenseNames =
                        jsonStringArrayToList( artifactLicenseJson.get( "overriddenLicenses" ) );
                    component.setOverriddenLicenseIds( licenseNamesToIds( multiLicenseDAO, overriddenLicenseNames ) );
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
                        component = new Component();
                        unknownComponents.add( component );
                    }
                    String hash = componentJson.get( "hash" ).asText();
                    component.setHash( hash );
                    component.setMatchState( matchState );
                }
            }
        }

        // Load dependency data
        JsonNode dependencyJson = loadJson( dependencyData );
        if ( dependencyJson != null )
        {
            dependencyJson = dependencyJson.get( "gavDepths" );
            Iterator<String> dependencyJsonIter = dependencyJson.fieldNames();
            while ( dependencyJsonIter.hasNext() )
            {
                String gav = dependencyJsonIter.next();
                Component component = componentsByGAV.get( gav );
                if ( component == null )
                {
                    // Is it possible?
                    component = new Component();
                    String[] coordinates = gav.split( ":" );
                    component.setGroupId( coordinates[0] );
                    component.setArtifactId( coordinates[1] );
                    component.setVersion( coordinates[2] );
                    componentsByGAV.put( gav, component );
                }

                ArrayNode depthJsonArray = (ArrayNode) dependencyJson.get( gav );
                for ( int i = 0; i < depthJsonArray.size(); i++ )
                {
                    JsonNode depthJson = depthJsonArray.get( i );
                    int dependencyDepth = depthJson.asInt();
                    component.addDependencyDepth( dependencyDepth );
                }
            }
        }

        final List<Component> result = new ArrayList<Component>();
        result.addAll( componentsByGAV.values() );
        result.addAll( unknownComponents );

        // Load label data
        ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
        for ( Component component : result )
        {
            List<ComponentLabel> componentLabels =
                componentLabelDAO.getByApplicationIdAndHash( applicationId, component.getHash() );
            for ( ComponentLabel componentLabel : componentLabels )
            {
                component.addLabelId( componentLabel.getLabelId() );
            }
        }
        return result;
    }

    private List<String> licenseNamesToIds( MultiLicenseDAO multiLicenseDAO, List<String> licenseNames )
    {
        if ( licenseNames == null )
        {
            return null;
        }
        List<String> licenseIds = new ArrayList<String>();
        for ( String licenseName : licenseNames )
        {
            String licenseId = multiLicenseDAO.getByNameNotNull( licenseName ).getId();
            licenseIds.add( licenseId );
        }
        return licenseIds;
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
        if ( JsonUtils.isNull( jsonNode ) )
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
