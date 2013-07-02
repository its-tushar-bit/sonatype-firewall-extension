/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

/**
 * Identity of a component in the Maven coordinate system.
 * 
 * Refer to http://maven.apache.org/pom.html#Maven_Coordinates
 * 
 * @since 1.4.1
 */
public class MavenCoordinates
{
    private String groupId;

    private String artifactId;

    private String version;

    private String extension;

    private String classifier;

    public MavenCoordinates()
    {
    }

    public MavenCoordinates( String groupId, String artifactId, String version, String extension, String classifier )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.extension = extension;
        this.classifier = classifier;
    }
    
    /**
     * Creates a new copy of the given coordinate.
     */
    public static MavenCoordinates copy( MavenCoordinates coordinate )
    {
        return new MavenCoordinates( coordinate.getGroupId(), coordinate.getArtifactId(), coordinate.getVersion(), 
              coordinate.getExtension(), coordinate.getClassifier() );
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

    public String getGAVECString()
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( groupId ).append( ':' ).append( artifactId ).append( ':' ).append( version );
        if ( extension != null )
        {
            buffer.append( ':' ).append( extension );
        }
        if ( classifier != null )
        {
            buffer.append( ':' ).append( classifier );
        }
        return buffer.toString();
    }
}
