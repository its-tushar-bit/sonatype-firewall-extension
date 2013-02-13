/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
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

    private String hash;

    private String constraintId;

    private int conditionNumber;

    public ComponentFact()
    {
    }

    public ComponentFact( final Component component, final String constraintId )
    {
        this( component, constraintId, -1 /* indicates all conditions */);
    }

    public ComponentFact( final Component component, final String constraintId, final int conditionNumber )
    {
        this.groupId = component.getGroupId();
        this.artifactId = component.getArtifactId();
        this.version = component.getVersion();
        this.hash = component.getHash();
        this.constraintId = constraintId;
        this.conditionNumber = conditionNumber;
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

    public int getConditionNumber()
    {
        return conditionNumber;
    }

    @Override
    public String toString()
    {
        return "\n  Component(gav=" + groupId + ':' + artifactId + ':' + version + ", hash=" + hash + ") ";
    }

    public String getHash()
    {
        return hash;
    }
}
