/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatchState
{
    private final static Map<String, MatchState> stateByName = new LinkedHashMap<String, MatchState>();

    private final static Map<String, MatchState> stateById = new LinkedHashMap<String, MatchState>();

    private final static List<MatchState> all = new ArrayList<MatchState>();

    // Note: The order the statuses are defined here determines the order they are displayed in the UI
    public static final MatchState EXACT = new MatchState( "exact", "Exact" );

    public static final MatchState SIMILAR = new MatchState( "similar", "Similar" );

    public static final MatchState UNKNOWN = new MatchState( "unknown", "Unknown" );

    private final String id;

    private final String name;

    private MatchState( String id, String name )
    {
        this.id = id;
        this.name = name;
        stateById.put( id, this );
        stateByName.put( name, this );
        all.add( this );
    }

    public static MatchState getByName( String name )
    {
        return stateByName.get( name );
    }

    public static MatchState getById( String id )
    {
        return stateById.get( id );
    }

    public static List<MatchState> getAll()
    {
        return Collections.unmodifiableList( all );
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }
}
