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
    @Id
    @Column( name = "hash_gav_id" )
    private String id;

    @Column( name = "hash" )
    private String hash;

    @Column( name = "group_id" )
    private String groupId;

    @Column( name = "artifact_id" )
    private String artifactId;

    @Column( name = "version" )
    private String version;

    @Column( name = "extension" )
    private String extension;

    @Column( name = "classifier" )
    private String classifier;

    public HashGAV()
    {
    }

    public HashGAV( String hash, String groupId, String artifactId, String version, String extension, String classifier )
    {
        setHash( hash );
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        setExtension( extension );
        setClassifier( classifier );
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

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        if ( classifier != null && classifier.trim().isEmpty() )
        {
            classifier = null;
        }
        this.classifier = classifier;
    }

    public String getExtension()
    {
        return extension;
    }

    public void setExtension( String extension )
    {
        if ( extension != null && extension.trim().isEmpty() )
        {
            extension = null;
        }
        this.extension = extension;
    }

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
        return groupId + ':' + artifactId + ':' + version + ':' + extension + ':' + classifier;
    }
}
