/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class DataStore
{
    private static final JsonFactory JSON = new MappingJsonFactory();

    public static ContainerNode<?> parseData( final byte[] buf )
        throws IOException
    {
        return JSON.createJsonParser( buf ).readValueAsTree();
    }

    public static byte[] streamData( final ContainerNode<?> data )
        throws IOException
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        JSON.createJsonGenerator( os, JsonEncoding.UTF8 ).writeTree( data );
        return os.toByteArray();
    }

    public static ArrayNode loadAugmentedRows( final File file )
        throws IOException
    {
        final ArrayNode auditedEntries = JSON.createJsonParser( file ).readValueAsTree();
        final ArrayNode augmentedRows = auditedEntries.arrayNode();
        for ( final JsonNode entry : auditedEntries )
        {
            augmentedRows.addAll( (ArrayNode) entry.get( "rows" ) );
        }
        return augmentedRows;
    }

    public static void saveAugmentedRows( final File file, final String user, final String ip,
                                          final ContainerNode<?> rows )
        throws IOException
    {
        final ArrayNode auditedEntries;
        if ( file.exists() )
        {
            auditedEntries = JSON.createJsonParser( file ).readValueAsTree();
        }
        else
        {
            auditedEntries = rows.arrayNode();
        }

        final ObjectNode entry = auditedEntries.insertObject( 0 );
        entry.put( "time", System.currentTimeMillis() );

        entry.put( "user", user );
        entry.put( "ip", ip );
        entry.put( "rows", rows );

        JSON.createJsonGenerator( file, JsonEncoding.UTF8 ).writeTree( auditedEntries );
    }

    public static ContainerNode<?> augmentTable( final ContainerNode<?> table, final ArrayNode augmentedRows )
    {
        final ArrayNode rows = (ArrayNode) table.get( "aaData" );
        for ( int x = 0; x < rows.size(); x++ )
        {
            for ( int y = 0; y < augmentedRows.size(); y++ )
            {
                try
                {
                    // once an augmented row had been applied, remove it since it won't match any other rows
                    rows.set( x, augment( (ObjectNode) rows.get( x ), (ObjectNode) augmentedRows.get( y ) ) );
                    augmentedRows.remove( y-- );
                    break;
                }
                catch ( final JsonMappingException e )
                {
                    // incompatible data, try next row from secondary table
                }
            }
        }
        return table;
    }

    public static ObjectNode augment( final ObjectNode primary, final ObjectNode secondary )
        throws JsonMappingException
    {
        final ObjectNode[] result = { primary };
        for ( final Entry<String, JsonNode> field : each( secondary.fields() ) )
        {
            final String name = field.getKey();
            final JsonNode primaryValue = primary.get( name );
            final JsonNode secondaryValue = field.getValue();
            if ( primaryValue == null )
            {
                mutate( result, primary ).put( name, secondaryValue ); // pure augmented data
            }
            else if ( primaryValue.isObject() && secondaryValue.isObject() )
            {
                final ObjectNode value = augment( (ObjectNode) primaryValue, (ObjectNode) secondaryValue );
                if ( primaryValue != value )
                {
                    mutate( result, primary ).put( name, value ); // patch in augmented result
                }
            }
            else if ( !primaryValue.equals( secondaryValue ) )
            {
                throw new JsonMappingException( "Inconsistent data" );
            }
        }
        return result[0];
    }

    private static ObjectNode mutate( final ObjectNode[] result, final ObjectNode original )
    {
        if ( result[0] == original )
        {
            // perform shallow copy so we can patch in any augmented fields
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
