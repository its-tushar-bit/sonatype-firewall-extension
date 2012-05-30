/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class DataStore
{
    public static ArrayNode augment( final ArrayNode primaryTable, final ArrayNode secondaryTable )
    {
        final ArrayNode table = primaryTable.arrayNode();
        for ( final JsonNode primary : primaryTable )
        {
            JsonNode matched = null;
            for ( int i = 0; i < secondaryTable.size(); i++ )
            {
                try
                {
                    // once a secondary row is applied, remove it since it won't match any other rows
                    table.add( augment( (ObjectNode) primary, (ObjectNode) secondaryTable.get( i ) ) );
                    matched = secondaryTable.remove( i );
                    break;
                }
                catch ( final JsonMappingException e )
                {
                    // incompatible rows, try next row from secondary table
                }
            }
            if ( matched == null )
            {
                table.add( primary );
            }
        }
        return table;
    }

    public static ObjectNode augment( final ObjectNode primary, final ObjectNode secondary )
        throws JsonMappingException
    {
        ObjectNode[] result = { primary };
        for ( final Entry<String, JsonNode> field : each( secondary.fields() ) )
        {
            final String name = field.getKey();
            final JsonNode primaryValue = primary.get( name );
            final JsonNode secondaryValue = field.getValue();
            if ( primaryValue == null )
            {
                mutate( result, primary ).put( name, secondaryValue );
            }
            else if ( primaryValue.isObject() && secondaryValue.isObject() )
            {
                final ObjectNode value = augment( (ObjectNode) primaryValue, (ObjectNode) secondaryValue );
                if ( primaryValue != value )
                {
                    mutate( result, primary ).put( name, value );
                }
            }
            else if ( !primaryValue.equals( secondaryValue ) )
            {
                throw new JsonMappingException( "Inconsistent data" );
            }
        }
        return result[0];
    }

    private static ObjectNode mutate( ObjectNode[] result, ObjectNode original )
    {
        if ( result[0] == original )
        {
            result[0] = (ObjectNode) original.objectNode().putAll( original );
        }
        return result[0];
    }

    private static <T> Iterable<T> each( final Iterator<T> itr )
    {
        return new Iterable<T>()
        {
            public Iterator<T> iterator()
            {
                return itr;
            }
        };
    }
}
