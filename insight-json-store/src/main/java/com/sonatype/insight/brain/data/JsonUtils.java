/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactory.Feature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ContainerNode;

public final class JsonUtils
{
    private static final JsonFactory JSON = new MappingJsonFactory().disable( Feature.INTERN_FIELD_NAMES );

    @SuppressWarnings( "unchecked" )
    public static <T extends ContainerNode<?>> T read( final File file )
        throws IOException
    {
        final JsonParser parser = JSON.createJsonParser( file );
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

    public static void write( final File file, final ContainerNode<?> data )
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

    @SuppressWarnings( "unchecked" )
    public static <T extends ContainerNode<?>> T parse( final byte[] buf )
        throws IOException
    {
        final JsonParser parser = JSON.createJsonParser( buf );
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

    public static byte[] generate( final ContainerNode<?> data )
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
}
