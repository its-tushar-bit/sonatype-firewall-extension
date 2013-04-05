/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.plexus.util.FileUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonFileStore
    implements JsonStore
{
    private static final ConcurrentMap<String, CountingLock> LOCK_TABLE = new ConcurrentHashMap<String, CountingLock>();

    private final File folder;

    public JsonFileStore( final File folder )
    {
        this.folder = folder;
    }

    @Override
    public void commit( final String path, final ContainerNode<?> data )
        throws IOException
    {
        final CountingLock lock = lockFor( folder );

        lock.exclusiveLock();
        try
        {
            final File file = new File( folder, path );

            final ArrayNode log;
            if ( file.exists() )
            {
                log = JsonUtils.read( file );
            }
            else
            {
                log = JsonUtils.arrayNode( data );
            }

            // newest entries appear at the top of the log
            JsonUtils.write( file, log.insert( 0, data ) );
        }
        finally
        {
            lock.exclusiveUnlock();
        }
    }

    @Override
    public ContainerNode<?> restore( String path )
        throws IOException
    {
        final CountingLock lock = lockFor( folder );

        lock.sharedLock();
        try
        {
            final File file = new File( folder, path );

            if ( file.exists() )
            {
                JsonNode data = JsonUtils.read( file ).get( 0 );
                if ( data != null && data.has( "data" ) ) // stamped data?
                {
                    data = data.get( "data" );
                }
                return (ContainerNode<?>) data;
            }

            return null;
        }
        finally
        {
            lock.sharedUnlock();
        }
    }

    @Override
    public Iterable<String> list()
        throws IOException
    {
        if ( folder.exists() )
        {
            final List<String> filenames = FileUtils.getFileNames( folder, null, null, false );
            final String[] elements = filenames.toArray( new String[filenames.size()] );
            Arrays.sort( elements );
            return Arrays.asList( elements );
        }
        return Collections.emptyList();
    }

    @Override
    public int modificationCount()
    {
        return lockFor( folder ).count();
    }

    @Override
    public ContainerNode<?> history( final ContainerNode<?> key, final String... paths )
        throws IOException
    {
        final CountingLock lock = lockFor( folder );

        lock.sharedLock();
        try
        {
            Iterable<String> filenames = Arrays.asList( paths );
            if ( paths.length == 0 || paths[0].length() == 0 )
            {
                filenames = list();
            }

            final ObjectNode log = JsonUtils.objectNode( key );
            final ArrayNode entries = log.putArray( "aaData" );

            for ( final String name : filenames )
            {
                final File file = new File( folder, name );
                if ( file.canRead() )
                {
                    entries.addAll( filterLog( file, (ObjectNode) key ) );
                }
            }

            return entries.size() > 0 ? log : null;
        }
        finally
        {
            lock.sharedUnlock();
        }
    }

    @Override
    public <T extends ContainerNode<?>> T augment( final T key, final String... paths )
        throws IOException
    {
        final CountingLock lock = lockFor( folder );

        lock.sharedLock();
        try
        {
            T table = key;
            for ( final String path : paths )
            {
                final File file = new File( folder, path );
                if ( file.canRead() )
                {
                    table = augmentTable( table, (ArrayNode) JsonUtils.read( file ) );
                }
            }
            return table;
        }
        finally
        {
            lock.sharedUnlock();
        }
    }

    private static ArrayNode filterLog( final File file, final ObjectNode key )
        throws IOException
    {
        final ArrayNode log = JsonUtils.read( file );
        final ArrayNode filteredLog = JsonUtils.arrayNode( log );
        for ( int x = 0; x < log.size(); x++ )
        {
            final ObjectNode entry;
            ContainerNode<?> data = (ContainerNode<?>) log.get( x );
            if ( data != null && data.has( "data" ) ) // stamped data?
            {
                entry = (ObjectNode) data;
                data = (ContainerNode<?>) entry.remove( "data" );
                entry.put( "filename", file.getName() );
            }
            else
            {
                entry = JsonUtils.objectNode( data );
            }
            if ( data instanceof ArrayNode )
            {
                for ( int y = 0; y < data.size(); y++ )
                {
                    try
                    {
                        filteredLog.add( augment( key, (ObjectNode) data.get( y ) ).putAll( entry ) );
                    }
                    catch ( final JsonMappingException e )
                    {
                        // incompatible data, try next entry from audit log
                    }
                }
            }
            else
            {
                try
                {
                    filteredLog.add( augment( key, (ObjectNode) data ).putAll( entry ) );
                }
                catch ( final JsonMappingException e )
                {
                    // incompatible data, try next entry from audit log
                }
            }
        }

        return filteredLog;
    }

    private static <T extends ContainerNode<?>> T augmentTable( final T table, final ArrayNode log )
    {
        // first aggregate all the changes found in the data log
        final List<JsonNode> changes = new ArrayList<JsonNode>();
        for ( int x = 0; x < log.size(); x++ )
        {
            ContainerNode<?> data = (ContainerNode<?>) log.get( x );
            if ( data != null && data.has( "data" ) ) // stamped data?
            {
                data = (ContainerNode<?>) data.get( "data" );
            }
            if ( data instanceof ArrayNode )
            {
                for ( int y = 0; y < data.size(); y++ )
                {
                    changes.add( data.get( y ) );
                }
            }
            else
            {
                changes.add( data );
            }
        }

        // check each row in turn against the candidate changes
        final ArrayNode rows = (ArrayNode) ( table instanceof ArrayNode ? table : table.get( "aaData" ) );
        if ( rows != null )
        {
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
        }
        else
        {
            // treat solitary object as a single row
            final ObjectNode row = (ObjectNode) table;
            for ( int y = 0; y < changes.size(); y++ )
            {
                try
                {
                    row.setAll( augment( row, (ObjectNode) changes.get( y ) ) );
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

    private static ObjectNode augment( final ObjectNode primary, final ObjectNode secondary )
        throws JsonMappingException
    {
        if ( primary == null )
        {
            return secondary;
        }
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
            else if ( primaryValue.isObject() && secondaryValue != null && secondaryValue.isObject() )
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
            @Override
            public Iterator<T> iterator()
            {
                return itr;
            }
        };
    }

    private static CountingLock lockFor( final File folder )
    {
        CountingLock lock = LOCK_TABLE.get( folder.getAbsolutePath() );
        if ( lock == null )
        {
            final CountingLock newLock = new CountingLock( folder.exists() ? 1 : 0 );
            lock = LOCK_TABLE.putIfAbsent( folder.getAbsolutePath(), newLock );
            if ( lock == null )
            {
                lock = newLock;
            }
        }
        return lock;
    }
}
