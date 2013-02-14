/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import java.io.IOException;
import java.io.InputStream;

public abstract class RepositoryItem
{

    public interface Coords
    {

        String toIdentifier();

    }

    public static class MavenCoords
        implements Coords
    {

        private final String id;

        public MavenCoords( final String groupId, final String artifactId, final String version,
                            final String classifier, final String extension )
        {
            final StringBuilder buffer = new StringBuilder( 128 );
            buffer.append( groupId );
            buffer.append( ':' ).append( artifactId );
            buffer.append( ':' ).append( extension );
            if ( classifier != null && !classifier.isEmpty() )
            {
                buffer.append( ':' ).append( classifier );
            }
            buffer.append( ':' ).append( version );
            id = buffer.toString();
        }

        @Override
        public String toIdentifier()
        {
            return id;
        }

        @Override
        public String toString()
        {
            return toIdentifier();
        }

    }

    public abstract String getPath();

    public abstract Coords getCoordinates();

    public abstract InputStream newInputStream()
        throws IOException;

    public String getSha1()
    {
        // override if at hand, otherwise calculated on-demand
        return null;
    }

    @Override
    public String toString()
    {
        return getPath();
    }

}
