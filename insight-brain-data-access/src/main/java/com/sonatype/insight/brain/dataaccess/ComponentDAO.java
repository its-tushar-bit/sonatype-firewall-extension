/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sonatype.clm.dto.model.ComponentInfo;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.json.store.JsonUtils;

public class ComponentDAO
{
    private MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

    private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

    private void processJsonLicenseData( Component component, JsonNode jsonLicenseData )
    {
        final String statusString = JsonUtils.getNullableString( jsonLicenseData.get( "status" ) );
        final LicenseStatus status = LicenseStatus.getByName( statusString );
        component.setLicenseStatus( status );
        List<String> declaredLicenseNames = JsonUtils.getStringListFromArray( jsonLicenseData.get( "declaredLicenses" ) );
        component.setDeclaredLicenseIds( multiLicenseNamesToLicenseIds( declaredLicenseNames ) );
        List<String> observedLicenseNames = JsonUtils.getStringListFromArray( jsonLicenseData.get( "observedLicenses" ) );
        component.setObservedLicenseIds( multiLicenseNamesToLicenseIds( observedLicenseNames ) );
        List<String> overriddenLicenseNames = JsonUtils.getStringListFromArray( jsonLicenseData.get( "overriddenLicenses" ) );
        component.setOverriddenLicenseIds( multiLicenseNamesToLicenseIds( overriddenLicenseNames ) );
        // TODO Load effective license data too?
    }

    public List<Component> getAll( String applicationId, final byte[] licenseData, final byte[] securityData,
                                   final byte[] bomData )
    {
        final Map<String, List<Component>> componentsByGAV = new LinkedHashMap<String, List<Component>>();
        final Map<String, Component> componentsByHash = new LinkedHashMap<String, Component>();

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
                    final String matchStateString = componentJson.get( "matchState" ).asText();
                    final MatchState matchState = MatchState.getById( matchStateString );
                    final String identificationSourceString =
                        JsonUtils.getNullableString( componentJson.get( "identificationSource" ) );
                    final IdentificationSource identificationSource = IdentificationSource.getById( identificationSourceString );
                    final boolean proprietary = componentJson.get( "proprietary" ).booleanValue();
                    String hash = componentJson.get( "hash" ).asText();

                    Component component = new Component();
                    component.setHash( hash );
                    component.setMatchState( matchState );
                    component.setProprietary( proprietary );
                    component.setIdentificationSource( identificationSource );
                    componentsByHash.put( hash, component );
                    if ( !matchState.equals( MatchState.UNKNOWN ) )
                    {
                        final String groupId = componentJson.get( "groupId" ).asText();
                        final String artifactId = componentJson.get( "artifactId" ).asText();
                        final String version = componentJson.get( "version" ).asText();
                        final JsonNode relativePopularityJson = componentJson.get( "relativePopularity" );
                        final int relativePopularity = (int) ( relativePopularityJson.asDouble() * 100 );
                        final long catalogDate = componentJson.get( "createTime" ).asLong();

                        component.setGroupId( groupId );
                        component.setArtifactId( artifactId );
                        component.setVersion( version );
                        component.setRelativePopularity( relativePopularity );
                        component.setCatalogDate( catalogDate );

                        String key = getComponentGAVKey( groupId, artifactId, version );
                        List<Component> components = componentsByGAV.get( key );
                        if ( components == null )
                        {
                            components = new ArrayList<Component>();
                            componentsByGAV.put( key, components );
                        }
                        components.add( component );
                    }
                    else
                    {
                        // Unknown component
                        unknownComponents.add( component );
                    }
                }
            }
        }

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

                    String key = getComponentGAVKey( groupId, artifactId, version );
                    List<Component> components = componentsByGAV.get( key );
                    if ( components != null )
                    {
                        for ( Component component : components )
                        {
                            processJsonLicenseData( component, jsonLicenseNode );
                        }
                    }
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

                    String key = getComponentGAVKey( groupId, artifactId, version );
                    List<Component> components = componentsByGAV.get( key );
                    if ( components != null )
                    {
                        for ( Component component : components )
                        {
                            SecurityVulnerability securityVulnerability = new SecurityVulnerability();
                            securityVulnerability.setSource( source );
                            securityVulnerability.setRefId( reference );
                            securityVulnerability.setSeverity( severity );
                            securityVulnerability.setStatus( status );

                            component.addSecurityVulnerability( securityVulnerability );
                        }
                    }
                }
            }
        }

        final List<Component> result = new ArrayList<Component>();
        result.addAll( componentsByHash.values() );
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

    public Component getComponent( String applicationId, ComponentInfo componentInfo, JsonNode jsonLicenseNode,
                                   ArrayNode jsonSVNode )
    {
        Component component = new Component();

        component.setHash( componentInfo.getHash() );
        component.setGroupId( componentInfo.getGroupId() );
        component.setArtifactId( componentInfo.getArtifactId() );
        component.setVersion( componentInfo.getVersion() );

        component.setMatchState( MatchState.getById( componentInfo.getMatchState() ) );
        if ( componentInfo.getIdentificationSource() != null )
        {
            component.setIdentificationSource( IdentificationSource.getById( componentInfo.getIdentificationSource() ) );
        }

        component.setCatalogDate( componentInfo.getCatalogDate() );
        if ( componentInfo.getRelativePopularity() != null )
        {
            component.setRelativePopularity( componentInfo.getRelativePopularity() );
        }

        if ( jsonLicenseNode != null )
        {
            processJsonLicenseData( component, jsonLicenseNode );
        }
        component.setDeclaredLicenseIds( multiLicenseIdsToLicenseIds( componentInfo.getDeclaredLicenseIds() ) );
        component.setObservedLicenseIds( multiLicenseIdsToLicenseIds( componentInfo.getObservedLicenseIds() ) );
        loadLicenseThreatGroups( applicationId, component );

        addSecurityVulnerabilities( component, componentInfo.getSecurityVulnerabilities(), jsonSVNode );

        loadComponentLabels( applicationId, component, new ComponentLabelDAO() );

        return component;
    }

    public Component getComponent( String applicationId, JsonNode jsonLicenseNode )
    {
        Component component = new Component();
        component.setArtifactId( jsonLicenseNode.get( "artifactId" ).asText() );
        component.setGroupId( jsonLicenseNode.get( "groupId" ).asText() );
        component.setVersion( jsonLicenseNode.get( "version" ).asText() );

        processJsonLicenseData( component, jsonLicenseNode );

        loadLicenseThreatGroups( applicationId, component );

        return component;
    }

    private void addSecurityVulnerabilities( Component component,
                                             List<com.sonatype.clm.dto.model.SecurityVulnerability> issues,
                                             ArrayNode jsonSVNode )
    {
        if ( issues == null )
        {
            return;
        }
        for ( com.sonatype.clm.dto.model.SecurityVulnerability issue : issues )
        {
            component.addSecurityVulnerability( new SecurityVulnerability( issue.getSource(), issue.getRefId(),
                                                                           issue.getSeverity() ) );
        }
        processJsonSVData( component, jsonSVNode );
    }

    private void processJsonSVData( Component component, ArrayNode jsonSVNodes )
    {
        if ( jsonSVNodes == null )
        {
            return;
        }
        List<SecurityVulnerability> svs = component.getSecurityVulnerabilities();
        for ( int i = 0; i < jsonSVNodes.size(); i++ )
        {
            JsonNode jsonSVNode = jsonSVNodes.get( i );
            String statusString = JsonUtils.getNullableString( jsonSVNode.get( "status" ) );
            if ( statusString != null )
            {
                SecurityVulnerabilityStatus status = SecurityVulnerabilityStatus.getByName( statusString );
                String source = jsonSVNode.get( "source" ).asText();
                String refId = jsonSVNode.get( "reference" ).asText();
                for ( SecurityVulnerability sv : svs )
                {
                    if ( sv.getSource().equals( source ) && sv.getRefId().equals( refId ) )
                    {
                        sv.setStatus( status );
                        break;
                    }
                }
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
        // Gather all license ids
        Set<String> licenseIds = new LinkedHashSet<String>();
        licenseIds.addAll( component.getOverriddenLicenseIds() );
        if ( licenseIds.isEmpty() )
        {
            licenseIds.addAll( component.getDeclaredLicenseIds() );
            licenseIds.addAll( component.getObservedLicenseIds() );
        }

        // Gather all license threat groups from the application
        for ( String licenseId : licenseIds )
        {
            List<LicenseThreatGroup> licenseThreatGroups =
                licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, licenseId );
            for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroups )
            {
                component.addLicenseThreatGroup( licenseThreatGroup );
            }
        }

        // Gather all license threat groups from the application's organization
        String organizationId = new ApplicationDAO().getByIdNotNull( applicationId ).getOrganizationId();
        if ( organizationId != null )
        {
            for ( String licenseId : licenseIds )
            {
                List<LicenseThreatGroup> licenseThreatGroups =
                    licenseThreatGroupDAO.getByOwnerIdAndLicenseId( organizationId, licenseId );
                for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroups )
                {
                    component.addLicenseThreatGroup( licenseThreatGroup );
                }
            }
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

    private Set<String> multiLicenseIdsToLicenseIds( Set<String> multiLicenseIds )
    {
        if ( multiLicenseIds == null )
        {
            return null;
        }
        Set<String> licenseIds = new LinkedHashSet<String>();
        for ( String multiLicenseId : multiLicenseIds )
        {
            Set<License> licenses = multiLicenseDAO.getLicensesByMultiLicenseId( multiLicenseId );
            for ( License license : licenses )
            {
                licenseIds.add( license.getId() );
            }
        }
        return licenseIds;
    }

    private static String getComponentGAVKey( final String groupId, final String artifactId, final String version )
    {
        return groupId + ':' + artifactId + ':' + version;
    }

    private static JsonNode loadJson( final byte[] data )
    {
        if ( data == null )
        {
            return null;
        }
        try
        {
            return JsonUtils.parse( data );
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( e );
        }
    }
}
