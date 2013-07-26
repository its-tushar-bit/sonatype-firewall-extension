/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.model.HasStringId;

/**
 * Association of a component hash to a Maven coordinate.
 * 
 * @since 1.4.1
 */
@Entity
@Table( name = "hash_gav" )
public class HashGAV
    implements HasStringId
{
    private String id;

    private String hash;

    private String comment;

    private Date createTime;

    /**
     * Convenience object to store Maven coordinates.
     */
    private MavenCoordinates coords;

    public HashGAV()
    {
        coords = new MavenCoordinates();
    }

    public HashGAV( String hash, String groupId, String artifactId, String version, String extension, String classifier )
    {
        setHash( hash );
        coords = new MavenCoordinates( groupId, artifactId, version, extension, classifier );
    }

    @Id
    @Column( name = "hash_gav_id" )
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

    @Column( name = "group_id" )
    public String getGroupId()
    {
        return coords.getGroupId();
    }

    public void setGroupId( String groupId )
    {
        this.coords.setGroupId( groupId );
    }

    @Column( name = "artifact_id" )
    public String getArtifactId()
    {
        return coords.getArtifactId();
    }

    public void setArtifactId( String artifactId )
    {
        this.coords.setArtifactId( artifactId );
    }

    @Column( name = "version" )
    public String getVersion()
    {
        return coords.getVersion();
    }

    public void setVersion( String version )
    {
        this.coords.setVersion( version );
    }

    @Column( name = "classifier" )
    public String getClassifier()
    {
        return coords.getClassifier();
    }

    public void setClassifier( String classifier )
    {
        this.coords.setClassifier( classifier );
    }

    @Column( name = "extension" )
    public String getExtension()
    {
        return coords.getExtension();
    }

    public void setExtension( String extension )
    {
        this.coords.setExtension( extension );
    }

    @Column( name = "hash" )
    public String getHash()
    {
        return hash;
    }

    public void setHash( String hash )
    {
        this.hash = HashHelper.truncateHash( hash );
    }

    @Column( name = "comment" )
    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    @Column( name = "create_time" )
    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime( Date createTime )
    {
        this.createTime = createTime;
    }

    public Long getCreateTimeLong()
    {
        return ( createTime != null ) ? createTime.getTime() : null;
    }

    public String getGAVECString()
    {
        return coords.getGAVECString();
    }

    public MavenCoordinates getCoordinates()
    {
        return MavenCoordinates.copy( coords );
    }
}
