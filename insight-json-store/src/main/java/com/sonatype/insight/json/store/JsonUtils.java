/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactory.Feature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonUtils
{
    private static final JsonFactory JSON = new MappingJsonFactory().disable( Feature.INTERN_FIELD_NAMES );

    public static JsonStore fileStore( final File folder )
    {
        return new JsonFileStore( folder );
    }

    public static ContainerNode<?> stamp( final String user, final String ip, final String where,
                                          final ContainerNode<?> data )
    {
        final ObjectNode stampedData = objectNode( data );
        stampedData.put( "time", System.currentTimeMillis() );
        stampedData.put( "user", user );
        stampedData.put( "ip", ip );
        stampedData.put( "where", where );
        stampedData.put( "data", data );
        return stampedData;
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends ContainerNode<?>> T read( final File file )
        throws IOException
    {
        final JsonParser parser = JSON.createParser( file );
        try
        {
            return (T) parser.readValueAsTree();
        }
        finally
        {
            parser.close();
        }
    }

    public static <T> T read( final File file, final Class<? extends T> type )
        throws IOException
    {
        final JsonParser parser = JSON.createParser( file );
        try
        {
            return parser.readValueAs( type );
        }
        finally
        {
            parser.close();
        }
    }

    public static void write( final File file, final ContainerNode<?> data )
        throws IOException
    {
        file.getAbsoluteFile().getParentFile().mkdirs();
        final JsonGenerator generator = JSON.createGenerator( file, JsonEncoding.UTF8 );
        try
        {
            generator.useDefaultPrettyPrinter().writeTree( data );
        }
        finally
        {
            generator.close();
        }
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends ContainerNode<?>> T parse( final byte[] buf )
        throws IOException
    {
        final JsonParser parser = JSON.createParser( buf );
        try
        {
            return (T) parser.readValueAsTree();
        }
        finally
        {
            parser.close();
        }
    }

    public static <T> T parse( final byte[] buf, final Class<? extends T> type )
        throws IOException
    {
        final JsonParser parser = JSON.createParser( buf );
        try
        {
            return parser.readValueAs( type );
        }
        finally
        {
            parser.close();
        }
    }

    public static <T extends ContainerNode<?>> T parse( final String json )
        throws IOException
    {
        return parse( json.getBytes( "UTF-8" ) );
    }

    public static <T> T parse( final String json, final Class<? extends T> type )
        throws IOException
    {
        return parse( json.getBytes( "UTF-8" ), type );
    }

    public static byte[] generate( final ContainerNode<?> data )
        throws IOException
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final JsonGenerator generator = JSON.createGenerator( os, JsonEncoding.UTF8 );
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

    public static ArrayNode arrayNode( final ContainerNode<?> data )
    {
        return data != null ? data.arrayNode() : JsonNodeFactory.instance.arrayNode();
    }

    public static ObjectNode objectNode( final ContainerNode<?> data )
    {
        return data != null ? data.objectNode() : JsonNodeFactory.instance.objectNode();
    }
}
