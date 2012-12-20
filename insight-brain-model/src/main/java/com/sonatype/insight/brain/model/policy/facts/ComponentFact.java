/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonatype.insight.brain.model.component.Component;

public class ComponentFact
{
    private String groupId;

    private String artifactId;

    private String version;

    private String constraintId;

    public ComponentFact()
    {
    }

    public ComponentFact( final Component component, final String constraintId )
    {
        this.groupId = component.getGroupId();
        this.artifactId = component.getArtifactId();
        this.version = component.getVersion();
        this.constraintId = constraintId;
    }

    @JsonIgnore
    public String getGAV()
    {
        return groupId + ':' + artifactId + ':' + version;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getConstraintId()
    {
        return constraintId;
    }

    @Override
    public String toString()
    {
        return "\n  Component(" + groupId + ':' + artifactId + ':' + version + ") ";
    }
}
