/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table( name = "application" )
public class Application
    implements HasStringId
{
    @Id
    @Column( name = "application_id" )
    private String id;

    @Column( name = "public_id" )
    private String publicId;

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

    public String getPublicId()
    {
        return publicId;
    }

    public void setPublicId( String publicId )
    {
        this.publicId = publicId;
    }
}
