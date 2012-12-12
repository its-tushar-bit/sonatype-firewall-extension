/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.contexts;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.ContextType;

public class ContextTypes
{
    private static final Map<String, ContextType> allContextTypes = new LinkedHashMap<String, ContextType>();

    static
    {
        add( new ProcureContextType() );
        add( new DevelopContextType() );
        add( new BuildContextType() );
        add( new ReleaseContextType() );
        add( new OperateContextType() );
    }

    public static Collection<ContextType> getAll()
    {
        return allContextTypes.values();
    }

    private static void add( final ContextType contextType )
    {
        if ( allContextTypes.keySet().contains( contextType.getId() ) )
        {
            throw new IllegalStateException( "Duplicate context type id: " + contextType.getId() );
        }
        allContextTypes.put( contextType.getId(), contextType );
    }

    public static ContextType getById( final String contextTypeId )
    {
        // TODO throw exception if contextTypeId is unknown
        return allContextTypes.get( contextTypeId );
    }
}
