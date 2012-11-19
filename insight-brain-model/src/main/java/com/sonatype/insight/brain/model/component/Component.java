/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.List;

public class Component
{
    private String groupId;

    private String artifactId;

    private String version;

    private List<SecurityVulnerability> securityVulnerabilities;

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public List<SecurityVulnerability> getSecurityVulnerabilities()
    {
        return securityVulnerabilities;
    }

    public void setSecurityVulnerabilities( List<SecurityVulnerability> securityVulnerabilities )
    {
        this.securityVulnerabilities = securityVulnerabilities;
    }
}
