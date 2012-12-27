/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import org.codehaus.plexus.util.StringUtils;

//import org.springframework.util.AntPathMatcher;

/**
 * Copied from com.sonatype.nexus.procurement.ArtifactCoordinate.
 * <p>
 * The coordinates/conditions used to address one artifact. If we want to expand the rules, this is the place to do so.
 * The coordinates may be "fixed" (isFixed() returns TRUE), or "wildcarded". Examples (presented as G:A:V triplets):
 * 
 * <pre>
 * org.sonatype.nexus : nexus-indexer : 1.0 - is a fixed coordinate that points exactly to what is says
 * org.sonatype* : nexus-indexer : 1.* - is a wildcard coordinate, that points to (inclusive) group 'org.sonatype' and below (ie. 'org.sonatypefoo' or  'org.sonatype.blah'), and artifact named named 'nexus-indexer' in these groups, and any version that starts with '1.' 
 * org.sonatype.* : nexus-indexer : 1.0 - is a wildcard coordinate, that is like the above one, except it matches this group and its subgroups ONLY ('org.sonatypefoo' is NOT matched), and matches for version '1.0' only.
 * </pre>
 * 
 * The other fields (A, V) also are able to make use of '*' (wildcard), but it will be interpreted obly as "starts with"
 * (ie. in field A, 'nexus*' will be inerpreted as artifactId.startsWith("nexus"). Same stands for V field). In case of
 * <b>groups</b> (G field), the things are a little different: blah* means group starts with 'blah' (hence, and and
 * below), blah.* means only group 'blah' and groups below 'blah' like 'blah.foo', but not 'blahfoo.foo'.
 * <p>
 * An ArtifactCoordinate is matchable only against "fixed" coordinate, hence, two wildarcded coordinate cannot be
 * matched.
 * 
 * @author cstamas
 */
public class ArtifactCoordinate
    implements Comparable<ArtifactCoordinate>
{
    private static final String PLACEHOLDER = "*";

    private String groupId;

    private String artifactId;

    private String version;

    // private transient AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Constructs an ArtifactCoordinate.
     */
    public ArtifactCoordinate()
    {
    }

    /**
     * Constructs an ArtifactCoordinate.
     * 
     * @param groupId
     * @param artifactId
     * @param version
     */
    public ArtifactCoordinate( String groupId, String artifactId, String version )
    {
        this.groupId = groupId;

        this.artifactId = artifactId;

        this.version = version;
    }

    /**
     * Returns true when all GAV coordinates are present, and none of them contains a '*' joker character.
     * 
     * @return
     */
    public boolean isFixed()
    {
        // fixed is when all GAV is given and none of those contains placeholder
        // when fixed, it identifies exactly one artifact
        return !StringUtils.isEmpty( getGroupId() ) && !StringUtils.isEmpty( getArtifactId() )
            && !StringUtils.isEmpty( getVersion() ) && !getGroupId().contains( PLACEHOLDER )
            && !getArtifactId().contains( PLACEHOLDER ) && !getVersion().contains( PLACEHOLDER );
    }

    /**
     * Returns true if this ArtifactCoordinate matches the passed path.
     * 
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    // public boolean matches( String path )
    // {
    // StringBuffer pathBuf = new StringBuffer( "/" );
    //
    // String gid = getGroupId();
    //
    // // a* -> a*/**/
    // // a.* -> a/**
    // if ( gid.endsWith( ".*" ) )
    // {
    // gid = gid.substring( 0, gid.length() - 2 ) + "/**";
    // }
    // else if ( gid.endsWith( "*" ) )
    // {
    // gid = gid.substring( 0, gid.length() - 1 ) + "*/**";
    // }
    //
    // pathBuf.append( gid.replace( '.', '/' ) );
    //
    // if ( !PLACEHOLDER.equals( getArtifactId() ) || !PLACEHOLDER.equals( getVersion() ) )
    // {
    // pathBuf.append( "/" );
    //
    // pathBuf.append( getArtifactId() );
    //
    // if ( !PLACEHOLDER.equals( getVersion() ) )
    // {
    // pathBuf.append( "/" );
    //
    // pathBuf.append( getVersion() );
    // }
    // else
    // {
    // pathBuf.append( "/" );
    //
    // pathBuf.append( "**" );
    // }
    // }
    // else
    // {
    // pathBuf.append( "/*/*" );
    // }
    //
    // return pathMatcher.match( pathBuf.toString(), path );
    // }

    /**
     * Returns true if this ArtifactCoordinate matches the passed coordinates.
     * 
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public boolean matches( String groupId, String artifactId, String version )
    {
        return matchesGroup( getGroupId(), groupId ) && matches( getArtifactId(), artifactId )
            && matches( getVersion(), version );
    }

    /**
     * Returns true if this ArtifactCoordinate matches the passed in fixed ArtifactCoordinate coordinates.
     * 
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public boolean matches( ArtifactCoordinate coordinate )
    {
        return coordinate.isFixed()
            && matches( coordinate.getGroupId(), coordinate.getArtifactId(), coordinate.getVersion() );
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

    // internal

    private int getMatchableCharacters()
    {
        return cleanseCoordinate( getGroupId() ).length() + cleanseCoordinate( getArtifactId() ).length()
            + cleanseCoordinate( getVersion() ).length();
    }

    private String cleanseCoordinate( String coordinate )
    {
        if ( coordinate.endsWith( PLACEHOLDER ) )
        {
            coordinate = coordinate.substring( 0, coordinate.indexOf( PLACEHOLDER ) );

            if ( coordinate.endsWith( "." ) )
            {
                return coordinate.substring( 0, coordinate.length() - 1 );
            }
            else
            {
                return coordinate;
            }
        }

        return coordinate;
    }

    /**
     * A utility method that handles group coordinates as matchable target. The meaning of them are:
     * 
     * <pre>
     * * (or null) - matches all
     * some.value - matches exactly 'some.value'
     * some.value* - matches by prefix, so 'some.value', 'some.value.more' are all ok
     * some.value.* - matches only subgroups, so 'some.value.more1', 'some.value.more2' is ok only
     * </pre>
     * 
     * @param coordinate
     * @param value
     * @return
     */
    private boolean matchesGroup( String coordinate, String value )
    {
        if ( StringUtils.isEmpty( coordinate ) || PLACEHOLDER.equals( coordinate ) )
        {
            // empty: not specified, it matches all
            return true;
        }
        else if ( StringUtils.isEmpty( value ) )
        {
            // cordinate not empty, value empty, no match
            return false;
        }
        else if ( coordinate.endsWith( "." + PLACEHOLDER ) )
        {
            if ( value.length() <= coordinate.length() - 2 )
            {
                // coordinate ends with a joker, matches if it is prefix of value
                return value.startsWith( coordinate.substring( 0, coordinate.length() - 2 ) );
            }
            else
            {
                // coordinate ends with a joker, matches if it is prefix of value
                return value.startsWith( coordinate.substring( 0, coordinate.length() - 1 ) );
            }
        }
        else if ( coordinate.endsWith( PLACEHOLDER ) )
        {
            // coordinate ends with a joker, matches if it is prefix of value
            return value.startsWith( coordinate.substring( 0, coordinate.length() - 1 ) );
        }
        else
        {
            // coordinate has no joker, matches if equals
            return coordinate.equals( value );
        }
    }

    /**
     * A utility method that handles A and V coordinates as matchable target. These are handled a bit differently that G
     * coordinates! The meaning of them are:
     * 
     * <pre>
     * null - matches all
     * some.value - matches exactly 'some.value'
     * somevalue* - matches by prefix just before the '*'
     * </pre>
     * 
     * @param coordinate
     * @param value
     * @return
     */
    private boolean matches( String coordinate, String value )
    {
        if ( StringUtils.isEmpty( coordinate ) || PLACEHOLDER.equals( coordinate ) )
        {
            // empty: not specified, it matches all
            return true;
        }
        else if ( StringUtils.isEmpty( value ) )
        {
            // cordinate not empty, value empty, no match
            return false;
        }
        else if ( coordinate.endsWith( PLACEHOLDER ) )
        {
            // coordinate ends with a joker, matches if it is prefix of value
            return value.startsWith( coordinate.substring( 0, coordinate.length() - 1 ) );
        }
        else
        {
            // coordinate has no joker, matches if equals
            return coordinate.equals( value );
        }
    }

    // Object

    @Override
    public int compareTo( ArtifactCoordinate o )
    {
        return getMatchableCharacters() - o.getMatchableCharacters();
    }

    @Override
    public String toString()
    {
        return gavToString( getGroupId(), getArtifactId(), getVersion() );
    }

    public static String gavToString( String g, String a, String v )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( g ).append( ":" ).append( a ).append( ":" ).append( v );

        return sb.toString();
    }
}
