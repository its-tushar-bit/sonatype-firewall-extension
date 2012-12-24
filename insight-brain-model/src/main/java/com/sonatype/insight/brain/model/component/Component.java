/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseValueType;

public class Component
{
    private static final Logger log = LoggerFactory.getLogger( Component.class );

    private String groupId;

    private String artifactId;

    private String version;

    private String licenseThreat;

    private int relativePopularity;

    private List<String> declaredLicenseNames = new ArrayList<String>();

    private List<String> observedLicenseNames = new ArrayList<String>();

    private List<SecurityVulnerability> securityVulnerabilities;

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
            if ( "=".equals( operator ) && ( securityVulnerability.getSeverity() == severity ) )
            {
                result.add( securityVulnerability );
            }
            else if ( ">".equals( operator ) && ( securityVulnerability.getSeverity() > severity ) )
            {
                result.add( securityVulnerability );
            }
            else if ( ">=".equals( operator ) && ( securityVulnerability.getSeverity() >= severity ) )
            {
                result.add( securityVulnerability );
            }
            else if ( "<".equals( operator ) && ( securityVulnerability.getSeverity() < severity ) )
            {
                result.add( securityVulnerability );
            }
            else if ( "<=".equals( operator ) && ( securityVulnerability.getSeverity() <= severity ) )
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

    public String getLicenseThreat()
    {
        return licenseThreat;
    }

    public void setLicenseThreat( final String licenseThreat )
    {
        this.licenseThreat = licenseThreat;
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

    public boolean hasDeclaredLicenseInList( String[] licenseIds )
    {
        return hasLicenseInList( declaredLicenseNames, licenseIds );
    }

    public boolean hasObservedLicenseInList( String[] licenseIds )
    {
        return hasLicenseInList( observedLicenseNames, licenseIds );
    }

    private boolean hasLicenseInList( List<String> componentLicenseNames, String[] licenseIds )
    {
        if ( licenseIds == null )
        {
            return false;
        }

        for ( String licenseId : licenseIds )
        {
            License license = LicenseValueType.getLicenseById( licenseId );
            if (license == null)
            {
                log.warn( "Unknown license id {}", licenseId );
                continue;
            }
            if ( componentLicenseNames.contains( license.getShortDisplayName() ) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasDeclaredLicenseNotInList( String[] licenseIds )
    {
        return hasLicenseNotInList( declaredLicenseNames, licenseIds );
    }

    public boolean hasObservedLicenseNotInList( String[] licenseIds )
    {
        return hasLicenseNotInList( observedLicenseNames, licenseIds );
    }

    private boolean hasLicenseNotInList( List<String> componentLicenseNames, String[] licenseIds )
    {
        if ( licenseIds == null )
        {
            return false;
        }

        List<String> licenseNames = new ArrayList<String>();
        for ( String licenseId : licenseIds )
        {
            License license = LicenseValueType.getLicenseById( licenseId );
            if ( license == null )
            {
                log.warn( "Unknown license id {}", licenseId );
                continue;
            }
            licenseNames.add( license.getShortDisplayName() );
        }
        for ( String componentLicenseName : componentLicenseNames )
        {
            if ( !licenseNames.contains( componentLicenseName ) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasLicenseInList( String[] licenseIds )
    {
        if ( hasDeclaredLicenseInList( licenseIds ) )
        {
            return true;
        }
        return hasObservedLicenseInList( licenseIds );
    }

    public boolean hasLicenseNotInList( String[] licenseIds )
    {
        if ( hasDeclaredLicenseNotInList( licenseIds ) )
        {
            return true;
        }
        return hasObservedLicenseNotInList( licenseIds );
    }

    public int getRelativePopularity()
    {
        return relativePopularity;
    }

    public void setRelativePopularity( int relativePopularity )
    {
        this.relativePopularity = relativePopularity;
    }
}
