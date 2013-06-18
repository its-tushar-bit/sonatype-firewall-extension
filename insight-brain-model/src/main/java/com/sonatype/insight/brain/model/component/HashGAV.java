/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table( name = "hash_gav" )
public class HashGAV
    implements HasStringId
{
    private String id;

    private String hash;

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
        // We use only the first 10 bytes of the hashes, so we have to truncate to the first 20 chars in the string
        // representation of a hash.
        if ( hash != null && hash.length() > 20 )
        {
            hash = hash.substring( 0, 20 );
        }
        this.hash = hash;
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
