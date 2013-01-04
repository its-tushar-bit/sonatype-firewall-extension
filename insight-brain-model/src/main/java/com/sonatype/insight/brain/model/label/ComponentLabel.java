/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.label;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table( name = "component_label" )
public class ComponentLabel
    implements HasStringId
{
    @Id
    @Column( name = "component_label_id" )
    private String id;

    @Column( name = "application_id" )
    private String applicationId;

    @Column( name = "label_id" )
    private String labelId;

    @Column( name = "hash" )
    private String hash;

    public ComponentLabel()
    {
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public void setId( String id )
    {
        this.id = id;
    }

    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId( String applicationId )
    {
        this.applicationId = applicationId;
    }

    public String getHash()
    {
        return hash;
    }

    public void setHash( String hash )
    {
        this.hash = hash;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public void setLabelId( String labelId )
    {
        this.labelId = labelId;
    }
}
