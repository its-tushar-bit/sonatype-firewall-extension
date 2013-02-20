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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseStatus;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.brain.MatchedComponent;
import com.sonatype.insight.model.brain.SecurityIssue;

public class ComponentDAO
{
    private MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

    private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

    private void processJsonLicenseData( Component component, JsonNode jsonLicenseData )
    {
        final String statusString = JsonUtils.getNullableString( jsonLicenseData.get( "status" ) );
        final LicenseStatus status = LicenseStatus.getByName( statusString );
        List<String> declaredLicenseNames = jsonStringArrayToList( jsonLicenseData.get( "declaredLicenses" ) );
        component.setDeclaredLicenseIds( multiLicenseNamesToLicenseIds( declaredLicenseNames ) );
        List<String> observedLicenseNames = jsonStringArrayToList( jsonLicenseData.get( "observedLicenses" ) );
        component.setObservedLicenseIds( multiLicenseNamesToLicenseIds( observedLicenseNames ) );
        List<String> overriddenLicenseNames = jsonStringArrayToList( jsonLicenseData.get( "overriddenLicenses" ) );
        component.setOverriddenLicenseIds( multiLicenseNamesToLicenseIds( overriddenLicenseNames ) );
        // TODO Load effective license data too?
        component.setLicenseStatus( status );
    }

    public List<Component> getAll( String applicationId, final byte[] licenseData, final byte[] securityData,
                                   final byte[] bomData, final byte[] dependencyData )
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
                    final JsonNode jsonLicenseNode = licenseJsonArray.get( i );
                    final String groupId = jsonLicenseNode.get( "groupId" ).asText();
                    final String artifactId = jsonLicenseNode.get( "artifactId" ).asText();
                    final String version = jsonLicenseNode.get( "version" ).asText();

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

                    processJsonLicenseData( component, jsonLicenseNode );
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
                        final long catalogDate = componentJson.get( "createTime" ).asLong();
                        component.setCatalogDate( catalogDate );
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

        // Load license threat group data
        for ( Component component : result )
        {
            loadLicenseThreatGroups( applicationId, component );
        }

        // Load label data
        ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
        for ( Component component : result )
        {
            loadComponentLabels( applicationId, component, componentLabelDAO );
        }
        return result;
    }

    public Component getComponent( String applicationId, MatchedComponent matchComponent, JsonNode jsonLicenseNode,
                                   ArrayNode jsonSvNode )
    {
        Component component = new Component();
        component.setArtifactId( matchComponent.getArtifactId() );
        component.setGroupId( matchComponent.getGroupId() );
        component.setVersion( matchComponent.getVersion() );
        if ( jsonLicenseNode != null )
        {
            processJsonLicenseData( component, jsonLicenseNode );
        }
        component.setCatalogDate( matchComponent.getCatalogDate() );
        component.setDeclaredLicenseIds( matchComponent.getDeclaredLicenseIds() );
        component.setObservedLicenseIds( matchComponent.getObservedLicenseIds() );
        component.setHash( matchComponent.getHash() );
        component.setMatchState( matchComponent.isSimpleMatch() ? MatchState.EXACT : MatchState.SIMILAR );

        for ( SecurityIssue issue : matchComponent.getSecurityThreats() )
        {
            component.addSecurityVulnerability( new SecurityVulnerability( issue.getSource(), issue.getRefid(),
                                                                           issue.getSeverity() ) );
        }
        if ( jsonSvNode != null )
        {
            processJsonSvData( component, jsonSvNode );
        }

        loadLicenseThreatGroups( applicationId, component );

        loadComponentLabels( applicationId, component, new ComponentLabelDAO() );

        return component;
    }

    private void processJsonSvData( Component component, ArrayNode jsonSvNode )
    {
        SecurityVulnerability[] svs =
            component.getSecurityVulnerabilities().toArray( new SecurityVulnerability[component.getSecurityVulnerabilities().size()] );
        for ( int i = 0; i < jsonSvNode.size(); i++ )
        {
            ValueNode node = (ValueNode) jsonSvNode.get( i ).get( "status" );
            if ( node != null && !( node instanceof NullNode ) )
            {
                svs[i].setStatus( SecurityVulnerabilityStatus.getByName( node.asText() ) );
            }
        }
    }

    private void loadComponentLabels( String applicationId, Component component, ComponentLabelDAO componentLabelDAO )
    {
        List<ComponentLabel> componentLabels =
            componentLabelDAO.getByApplicationIdAndHash( applicationId, component.getHash() );
        for ( ComponentLabel componentLabel : componentLabels )
        {
            component.addLabelId( componentLabel.getLabelId() );
        }
    }

    public void loadLicenseThreatGroups( String applicationId, Component component )
    {
        Set<String> licenseIds = new LinkedHashSet<String>();
        licenseIds.addAll( component.getOverriddenLicenseIds() );
        if ( licenseIds.isEmpty() )
        {
            licenseIds.addAll( component.getDeclaredLicenseIds() );
            licenseIds.addAll( component.getObservedLicenseIds() );
        }
        for ( String licenseId : licenseIds )
        {
            component.addLicenseThreatGroup( licenseThreatGroupDAO.getByApplicationIdAndLicenseId( applicationId,
                                                                                                   licenseId ) );
        }
    }

    private Set<String> multiLicenseNamesToLicenseIds( List<String> multiLicenseNames )
    {
        if ( multiLicenseNames == null )
        {
            return null;
        }
        Set<String> licenseIds = new LinkedHashSet<String>();
        for ( String multiLicenseName : multiLicenseNames )
        {
            String multiLicenseId = multiLicenseDAO.getByNameNotNull( multiLicenseName ).getId();
            Set<License> licenses = multiLicenseDAO.getLicensesByMultiLicenseId( multiLicenseId );
            for ( License license : licenses )
            {
                licenseIds.add( license.getId() );
            }
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
