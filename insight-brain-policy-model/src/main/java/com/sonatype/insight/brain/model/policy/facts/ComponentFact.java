/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonatype.insight.brain.model.component.Component;

public class ComponentFact
    implements Cloneable
{
    private String groupId;

    private String artifactId;

    private String version;

    private String hash;

    private List<ConstraintFact> constraintFacts;

    public ComponentFact()
    {
    }

    public ComponentFact( final Component component )
    {
        this.groupId = component.getGroupId();
        this.artifactId = component.getArtifactId();
        this.version = component.getVersion();
        this.hash = component.getHash();
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

    public String getHash()
    {
        return hash;
    }

    public List<ConstraintFact> getConstraintFacts()
    {
        return constraintFacts;
    }

    public void addConstraintFact( final ConstraintFact constraintFact )
    {
        if ( constraintFacts == null )
        {
            constraintFacts = new ArrayList<ConstraintFact>();
        }
        constraintFacts.add( constraintFact );
    }

    @JsonIgnore
    public ComponentFact with( final List<ConstraintFact> newConstraintFacts )
    {
        try
        {
            // shallow copy (field-by-field)
            final ComponentFact clone = (ComponentFact) this.clone();
            clone.constraintFacts = newConstraintFacts;
            return clone;
        }
        catch ( final CloneNotSupportedException e )
        {
            throw new UnsupportedOperationException();
        }
    }

    @JsonIgnore
    public ComponentFact with( final ConstraintFact... newConstraintFacts )
    {
        return with( Arrays.asList( newConstraintFacts ) );
    }

    @Override
    public String toString()
    {
        return "\n Component(gav=" + groupId + ':' + artifactId + ':' + version + ", hash=" + hash + ") "
            + constraintFacts;
    }
}
