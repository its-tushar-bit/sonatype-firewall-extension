/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class AllActionTypes
{
    private static final Map<String, ActionType> allActionTypes = new LinkedHashMap<String, ActionType>();

    static
    {
        add( new AddLabelActionType() );
        add( new MarkAsFailedActionType() );
    }

    public static Collection<ActionType> getAll()
    {
        return allActionTypes.values();
    }

    private static void add( final ActionType actionType )
    {
        if ( allActionTypes.keySet().contains( actionType.getId() ) )
        {
            throw new IllegalStateException( "Duplicate action type id: " + actionType.getId() );
        }
        allActionTypes.put( actionType.getId(), actionType );
    }

    public static ActionType getById( final String actionTypeId )
    {
        // TODO throw exception if actionTypeId is unknown
        return allActionTypes.get( actionTypeId );
    }
}
