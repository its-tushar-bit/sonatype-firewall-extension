/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table( name = "license_threat_group" )
public class LicenseThreatGroup
    implements HasStringId
{
    @Id
    @Column( name = "license_threat_group_id" )
    private String id;

    @Column( name = "application_id" )
    private String applicationId;

    @Column( name = "name" )
    private String name;

    @Column( name = "threat_level" )
    private int threatLevel;

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

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public int getThreatLevel()
    {
        return threatLevel;
    }

    public void setThreatLevel( int threatLevel )
    {
        this.threatLevel = threatLevel;
    }
}
