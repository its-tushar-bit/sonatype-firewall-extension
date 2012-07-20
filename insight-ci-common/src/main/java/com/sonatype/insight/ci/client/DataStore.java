/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactory.Feature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class DataStore
{
    private static final JsonFactory JSON = new MappingJsonFactory().disable( Feature.INTERN_FIELD_NAMES );

    static void logData( final File file, final String user, final String ip, final String where,
                         final ContainerNode<?> data )
        throws IOException
    {
        final ArrayNode dataLog;
        if ( file.exists() )
        {
            dataLog = (ArrayNode) loadData( file );
        }
        else
        {
            dataLog = data.arrayNode();
        }

        // newest entries appear at the top of the data log
        final ObjectNode dataEntry = dataLog.insertObject( 0 );
        dataEntry.put( "time", System.currentTimeMillis() );

        dataEntry.put( "user", user );
        dataEntry.put( "ip", ip );
        dataEntry.put( "where", where );
        dataEntry.put( "data", data );

        saveData( file, dataLog );
    }

    static <T extends ContainerNode<?>> T augmentTable( final T table, final File file )
        throws IOException
    {
        final ArrayNode dataLog = (ArrayNode) loadData( file );

        // first aggregate all the changes found in the data log
        final List<JsonNode> changes = new ArrayList<JsonNode>();
        for ( int x = 0; x < dataLog.size(); x++ )
        {
            final ArrayNode data = (ArrayNode) dataLog.get( x ).get( "data" );
            for ( int y = 0; y < data.size(); y++ )
            {
                changes.add( data.get( y ) );
            }
        }

        // check each row in turn against the candidate changes
        final ArrayNode rows = (ArrayNode) ( table instanceof ArrayNode ? table : table.get( "aaData" ) );
        for ( int x = 0; x < rows.size(); x++ )
        {
            for ( int y = 0; y < changes.size(); y++ )
            {
                try
                {
                    // once change has been applied, remove it since it shouldn't match any other rows
                    rows.set( x, augment( (ObjectNode) rows.get( x ), (ObjectNode) changes.get( y ) ) );
                    changes.remove( y-- );
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

    static <T extends ContainerNode<?>> T loadData( final File file )
        throws IOException
    {
        final JsonParser parser = JSON.createJsonParser( file );
        try
        {
            return parser.readValueAsTree();
        }
        finally
        {
            parser.close();
        }
    }

    static <T> T loadData( final File file, final Class<? extends T> type )
        throws IOException
    {
        final JsonParser parser = JSON.createJsonParser( file );
        try
        {
            return parser.readValueAs( type );
        }
        finally
        {
            parser.close();
        }
    }

    static void saveData( final File file, final ContainerNode<?> data )
        throws IOException
    {
        file.getAbsoluteFile().getParentFile().mkdirs();
        final JsonGenerator generator = JSON.createJsonGenerator( file, JsonEncoding.UTF8 );
        try
        {
            generator.useDefaultPrettyPrinter().writeTree( data );
        }
        finally
        {
            generator.close();
        }
    }

    static <T extends ContainerNode<?>> T parseData( final byte[] buf )
        throws IOException
    {
        final JsonParser parser = JSON.createJsonParser( buf );
        try
        {
            return parser.readValueAsTree();
        }
        finally
        {
            parser.close();
        }
    }

    static <T> T parseData( final byte[] buf, final Class<? extends T> type )
        throws IOException
    {
        final JsonParser parser = JSON.createJsonParser( buf );
        try
        {
            return parser.readValueAs( type );
        }
        finally
        {
            parser.close();
        }
    }

    static byte[] streamData( final ContainerNode<?> data )
        throws IOException
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final JsonGenerator generator = JSON.createJsonGenerator( os, JsonEncoding.UTF8 );
        try
        {
            generator.useDefaultPrettyPrinter().writeTree( data );
        }
        finally
        {
            generator.close();
        }
        return os.toByteArray();
    }

    private static ObjectNode augment( final ObjectNode primary, final ObjectNode secondary )
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

    static byte[] augmentArtifactDetails( final byte[] detailData, final byte[] licenseData )
        throws IOException
    {
        byte[] augmentedDetailData = detailData;

        final ObjectNode details = parseData( detailData );

        final ContainerNode<?> licenses = parseData( licenseData );
        final ArrayNode artifacts = (ArrayNode) ( licenses instanceof ArrayNode ? licenses : licenses.get( "aaData" ) );

        final JsonNode overriddenLicenses = getOverriddenLicenses( details, artifacts );
        if ( overriddenLicenses != null )
        {
            details.put( "overriddenLicenses", overriddenLicenses );
            augmentedDetailData = streamData( details );
        }

        return augmentedDetailData;
    }

    private static JsonNode getOverriddenLicenses( final ObjectNode details, final ArrayNode artifacts )
    {
        final String groupId = details.path( "groupId" ).asText();
        final String artifactId = details.path( "artifactId" ).asText();
        final String version = details.path( "version" ).asText();

        for ( int i = 0; i < artifacts.size(); i++ )
        {
            final JsonNode row = artifacts.get( i );
            if ( artifactId.equals( row.path( "artifactId" ).asText() )
                && groupId.equals( row.path( "groupId" ).asText() ) && version.equals( row.path( "version" ).asText() ) )
            {
                return row.get( "overriddenLicenses" );
            }
        }
        return null;
    }
}
