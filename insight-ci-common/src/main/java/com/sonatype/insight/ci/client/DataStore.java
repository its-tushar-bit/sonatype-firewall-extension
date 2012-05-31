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
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class DataStore
{
    private static final JsonFactory JSON = new MappingJsonFactory();

    public static TreeNode parseData( final byte[] buf )
        throws IOException
    {
        return JSON.createJsonParser( buf ).readValueAsTree();
    }

    public static byte[] streamData( final TreeNode data )
        throws IOException
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        JSON.createJsonGenerator( os, JsonEncoding.UTF8 ).writeTree( data );
        return os.toByteArray();
    }

    public static TreeNode loadData( final File file )
        throws IOException
    {
        return JSON.createJsonParser( file ).readValueAsTree();
    }

    public static void saveData( final File file, final TreeNode data )
        throws IOException
    {
        TreeNode root = data;
        if ( file.exists() )
        {
            root = table( data ).addAll( table( loadData( file ) ) );
        }
        JSON.createJsonGenerator( file, JsonEncoding.UTF8 ).writeTree( root );
    }

    public static TreeNode augmentData( final TreeNode primaryData, final TreeNode secondaryData )
    {
        final ArrayNode primaryTable = table( primaryData );
        final ArrayNode secondaryTable = table( secondaryData );
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
                    matched = secondaryTable.remove( i-- );
                    break;
                }
                catch ( final JsonMappingException e )
                {
                    // incompatible data, try next row from secondary table
                }
            }
            if ( matched == null )
            {
                table.add( primary ); // row was not augmented
            }
        }

        if ( primaryData instanceof ObjectNode )
        {
            final ObjectNode data = primaryTable.objectNode();
            data.put( "aaData", table );
            return data;
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

    private static ArrayNode table( final TreeNode table )
    {
        return (ArrayNode) ( table instanceof ObjectNode ? ( (ObjectNode) table ).get( "aaData" ) : table );
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
