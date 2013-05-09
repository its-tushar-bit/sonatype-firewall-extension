/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonatype.insight.brain.model.license.LicenseStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

public class Component
{
    private String groupId;

    private String artifactId;

    private String version;

    private String hash;

    private Set<String> declaredLicenseIds = new LinkedHashSet<String>();

    private Set<String> observedLicenseIds = new LinkedHashSet<String>();

    private Set<String> overriddenLicenseIds = new LinkedHashSet<String>();

    private Map<String, LicenseThreatGroup> licenseThreatGroupsById = new LinkedHashMap<String, LicenseThreatGroup>();

    private LicenseStatus licenseStatus;

    private List<SecurityVulnerability> securityVulnerabilities;

    private int relativePopularity;

    private MatchState matchState = MatchState.UNKNOWN;

    private Long catalogDate;

    private List<String> labelIds = new ArrayList<String>();

    private boolean proprietary;

    public Component()
    {
    }

    public Component( final String groupId, final String artifactId, final String version, MatchState matchState )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.matchState = matchState;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( final String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( final String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( final String version )
    {
        this.version = version;
    }

    public List<SecurityVulnerability> getSecurityVulnerabilitiesByStatusId( String securityVulnerabilityStatusId )
    {
        if ( getSecurityVulnerabilities().isEmpty() )
        {
            return Collections.emptyList();
        }

        SecurityVulnerabilityStatus status = SecurityVulnerabilityStatus.getById( securityVulnerabilityStatusId );
        List<SecurityVulnerability> result = new ArrayList<SecurityVulnerability>();
        for ( SecurityVulnerability securityVulnerability : securityVulnerabilities )
        {
            if ( status.equals( securityVulnerability.getStatus() ) )
            {
                result.add( securityVulnerability );
            }
        }
        return result;
    }

    public List<SecurityVulnerability> getSecurityVulnerabilities()
    {
        if ( securityVulnerabilities == null )
        {
            return Collections.emptyList();
        }
        return securityVulnerabilities;
    }

    public void setSecurityVulnerabilities( final List<SecurityVulnerability> securityVulnerabilities )
    {
        this.securityVulnerabilities = securityVulnerabilities;
    }

    public void addSecurityVulnerability( final SecurityVulnerability securityVulnerability )
    {
        if ( securityVulnerabilities == null )
        {
            securityVulnerabilities = new ArrayList<SecurityVulnerability>();
        }
        securityVulnerabilities.add( securityVulnerability );
    }

    @JsonIgnore
    public String getGAV()
    {
        return groupId + ':' + artifactId + ':' + version;
    }

    public Set<String> getDeclaredLicenseIds()
    {
        return declaredLicenseIds;
    }

    public void setDeclaredLicenseIds( Set<String> declaredLicenseIds )
    {
        this.declaredLicenseIds.clear();

        if ( declaredLicenseIds == null )
        {
            return;
        }

        this.declaredLicenseIds.addAll( declaredLicenseIds );
    }

    public void addDeclaredLicenseId( String licenseId )
    {
        declaredLicenseIds.add( licenseId );
    }

    public Set<String> getObservedLicenseIds()
    {
        return observedLicenseIds;
    }

    public void setObservedLicenseIds( Set<String> observedLicenseIds )
    {
        this.observedLicenseIds.clear();

        if ( observedLicenseIds == null )
        {
            return;
        }

        this.observedLicenseIds.addAll( observedLicenseIds );
    }

    public void addObservedLicenseId( String licenseId )
    {
        observedLicenseIds.add( licenseId );
    }

    public Set<String> getOverriddenLicenseIds()
    {
        return overriddenLicenseIds;
    }

    public void setOverriddenLicenseIds( Set<String> overriddenLicenseIds )
    {
        this.overriddenLicenseIds.clear();

        if ( overriddenLicenseIds == null )
        {
            return;
        }

        this.overriddenLicenseIds.addAll( overriddenLicenseIds );
    }

    public void addOverriddenLicenseId( String licenseId )
    {
        overriddenLicenseIds.add( licenseId );
    }

    public boolean hasLicenseId( String licenseId )
    {
        if ( !overriddenLicenseIds.isEmpty() )
        {
            return overriddenLicenseIds.contains( licenseId );
        }
        if ( declaredLicenseIds.contains( licenseId ) )
        {
            return true;
        }
        return observedLicenseIds.contains( licenseId );
    }

    public Set<String> getLicenseIds()
    {
        if ( !overriddenLicenseIds.isEmpty() )
        {
            return overriddenLicenseIds;
        }

        final Set<String> licenseIds = new HashSet<String>();
        licenseIds.addAll( declaredLicenseIds );
        licenseIds.addAll( observedLicenseIds );
        return licenseIds;
    }

    public int getRelativePopularity()
    {
        return relativePopularity;
    }

    public void setRelativePopularity( int relativePopularity )
    {
        this.relativePopularity = relativePopularity;
    }

    public LicenseStatus getLicenseStatus()
    {
        if ( licenseStatus == null )
        {
            licenseStatus = LicenseStatus.OPEN;
        }
        return licenseStatus;
    }

    public void setLicenseStatus( LicenseStatus licenseStatus )
    {
        this.licenseStatus = licenseStatus;
    }

    public MatchState getMatchState()
    {
        return matchState;
    }

    public void setMatchState( MatchState matchState )
    {
        this.matchState = matchState;
    }

    public String getHash()
    {
        return hash;
    }

    public void setHash( String hash )
    {
        this.hash = hash;
    }

    public Long getCatalogDate()
    {
        return catalogDate;
    }

    public void setCatalogDate( Long catalogDate )
    {
        this.catalogDate = catalogDate;
    }

    public void addLabelId( String labelId )
    {
        labelIds.add( labelId );
    }

    public boolean hasLabelId( String labelId )
    {
        return labelIds.contains( labelId );
    }

    public List<String> getLabelIds()
    {
        return labelIds;
    }

    public void addLicenseThreatGroup( LicenseThreatGroup licenseThreatGroup )
    {
        if ( licenseThreatGroup == null )
        {
            return;
        }
        licenseThreatGroupsById.put( licenseThreatGroup.getId(), licenseThreatGroup );
    }

    public boolean hasLicenseInLicenseThreatGroup( String licenseThreatGroupId )
    {
        return licenseThreatGroupsById.keySet().contains( licenseThreatGroupId );
    }

    @JsonIgnore
    public Set<LicenseThreatGroup> getLicenseThreatGroups()
    {
        final Set<LicenseThreatGroup> licenseThreatGroups = new LinkedHashSet<LicenseThreatGroup>();
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroupsById.values() )
        {
            licenseThreatGroups.add( licenseThreatGroup );
        }
        return licenseThreatGroups;
    }

    public Integer getLicenseThreatLevel()
    {
        Integer threatLevel = null;

        for ( LicenseThreatGroup licenseThreatGroup : getLicenseThreatGroups() )
        {
            threatLevel = Math.max( threatLevel != null ? threatLevel : 0, licenseThreatGroup.getThreatLevel() );
        }

        return threatLevel;
    }

    @Override
    public String toString()
    {
        return getHash() + " " + getMatchState();
    }

    public boolean isProprietary()
    {
        return proprietary;
    }

    public void setProprietary( boolean proprietary )
    {
        this.proprietary = proprietary;
    }
}
