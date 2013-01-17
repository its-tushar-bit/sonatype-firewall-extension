/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Component
{
    private static final Logger log = LoggerFactory.getLogger( Component.class );

    private String groupId;

    private String artifactId;

    private String version;

    private String hash;

    private List<String> declaredLicenseNames = new ArrayList<String>();

    private List<String> observedLicenseNames = new ArrayList<String>();

    private List<String> overriddenLicenseNames = new ArrayList<String>();

    private LicenseStatus licenseStatus;

    private List<SecurityVulnerability> securityVulnerabilities;

    private int relativePopularity;

    private MatchState matchState;

    private Long catalogDate;

    private List<Integer> dependencyDepths = new ArrayList<Integer>();

    private List<String> labelIds = new ArrayList<String>();

    public Component()
    {
    }

    public Component( final String groupId, final String artifactId, final String version )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
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

    public List<SecurityVulnerability> getSecurityVulnerabilitiesBySeverity( float severity, String operator )
    {
        if ( getSecurityVulnerabilities().isEmpty() )
        {
            return Collections.emptyList();
        }

        List<SecurityVulnerability> result = new ArrayList<SecurityVulnerability>();
        for ( SecurityVulnerability securityVulnerability : securityVulnerabilities )
        {
            float securityVulnerabilitySeverity = 0;
            if ( securityVulnerability.getSeverity() != null )
            {
                securityVulnerabilitySeverity = securityVulnerability.getSeverity();
            }

            if ( "=".equals( operator ) && ( securityVulnerabilitySeverity == severity ) )
            {
                result.add( securityVulnerability );
            }
            else if ( ">".equals( operator ) && ( securityVulnerabilitySeverity > severity ) )
            {
                result.add( securityVulnerability );
            }
            else if ( ">=".equals( operator ) && ( securityVulnerabilitySeverity >= severity ) )
            {
                result.add( securityVulnerability );
            }
            else if ( "<".equals( operator ) && ( securityVulnerabilitySeverity < severity ) )
            {
                result.add( securityVulnerability );
            }
            else if ( "<=".equals( operator ) && ( securityVulnerabilitySeverity <= severity ) )
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

    public List<String> getDeclaredLicenseNames()
    {
        return declaredLicenseNames;
    }

    public void setDeclaredLicenseNames( List<String> declaredLicenseNames )
    {
        this.declaredLicenseNames.clear();

        if ( declaredLicenseNames == null )
        {
            return;
        }

        this.declaredLicenseNames.addAll( declaredLicenseNames );
    }

    public void addDeclaredLicenseName( String licenseName )
    {
        declaredLicenseNames.add( licenseName );
    }

    public List<String> getObservedLicenseNames()
    {
        return observedLicenseNames;
    }

    public void setObservedLicenseNames( List<String> observedLicenseNames )
    {
        this.observedLicenseNames.clear();

        if ( observedLicenseNames == null )
        {
            return;
        }

        this.observedLicenseNames.addAll( observedLicenseNames );
    }

    public void addObservedLicenseName( String licenseName )
    {
        observedLicenseNames.add( licenseName );
    }

    public List<String> getOverriddenLicenseNames()
    {
        return overriddenLicenseNames;
    }

    public void setOverriddenLicenseNames( List<String> overriddenLicenseNames )
    {
        this.overriddenLicenseNames.clear();

        if ( overriddenLicenseNames == null )
        {
            return;
        }

        this.overriddenLicenseNames.addAll( overriddenLicenseNames );
    }

    public void addOverriddenLicenseName( String licenseName )
    {
        overriddenLicenseNames.add( licenseName );
    }

    private boolean hasLicenseId( List<String> componentLicenseNames, String licenseId )
    {
        License license = License.getById( licenseId );
        if ( license == null )
        {
            log.warn( "Unknown license id {}", licenseId );
            return false;
        }
        return componentLicenseNames.contains( license.getShortDisplayName() );
    }

    public boolean hasLicenseId( String licenseId )
    {
        if ( !overriddenLicenseNames.isEmpty() )
        {
            return hasLicenseId( overriddenLicenseNames, licenseId );
        }
        if ( hasLicenseId( declaredLicenseNames, licenseId ) )
        {
            return true;
        }
        return hasLicenseId( observedLicenseNames, licenseId );
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

    public List<Integer> getDependencyDepths()
    {
        return dependencyDepths;
    }

    public void setDependencyDepths( List<Integer> dependencyDepths )
    {
        this.dependencyDepths.clear();

        if ( dependencyDepths == null )
        {
            return;
        }

        this.dependencyDepths.addAll( dependencyDepths );
    }

    public void addDependencyDepth( int dependencyDepth )
    {
        dependencyDepths.add( dependencyDepth );
    }

    public void addLabelId( String labelId )
    {
        labelIds.add( labelId );
    }

    public boolean hasLabelId( String labelId )
    {
        return labelIds.contains( labelId );
    }
}
