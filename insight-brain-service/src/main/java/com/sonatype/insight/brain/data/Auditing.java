/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.data;

import static com.sonatype.insight.brain.data.DataStore.augmentTable;
import static com.sonatype.insight.brain.data.DataStore.filterDataLog;
import static com.sonatype.insight.brain.data.DataStore.logData;
import static com.sonatype.insight.brain.data.DataStore.parseData;
import static com.sonatype.insight.brain.data.DataStore.streamData;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ContainerNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Auditing
{
    private static final Logger logger = LoggerFactory.getLogger( Auditing.class );

    private static final ConcurrentMap<String, AuditLock> LOCK_TABLE = new ConcurrentHashMap<String, AuditLock>();

    private static final String[] NO_FILE_NAMES = {};

    private static final FilenameFilter JSON_FILES = new FilenameFilter()
    {
        @Override
        public boolean accept( final File dir, final String name )
        {
            return name.endsWith( ".json" );
        }
    };

    public static byte[] filterAuditLog( final File auditDir, final byte[] key, final String... names )
        throws IOException
    {
        final ObjectNode keyData = parseData( key != null ? key : "{}".getBytes( "UTF-8" ) );
        final ObjectNode log = keyData.objectNode();

        final ArrayNode entries = log.putArray( "aaData" );

        final AuditLock lock = lockFor( auditDir );

        lock.sharedLock();
        try
        {
            String[] fileNames = names;
            if ( names.length == 0 || names[0].length() == 0 )
            {
                fileNames = listAugmentedData( auditDir );
            }
            logger.debug( "Filtering audit log from directory {}, files {}", auditDir.getAbsolutePath(), fileNames );
            for ( final String name : fileNames )
            {
                final File file = new File( auditDir, name );
                if ( file.canRead() )
                {
                    entries.addAll( filterDataLog( file, keyData ) );
                }
            }
        }
        finally
        {
            lock.sharedUnlock();
        }

        return entries.size() > 0 ? streamData( log ) : null;
    }

    public static boolean isData( final String name )
    {
        return JSON_FILES.accept( null, name );
    }

    public static int getModificationCount( final File auditDir )
    {
        return lockFor( auditDir ).modCount();
    }

    public static String[] listAugmentedData( final File auditDir )
    {
        final String[] names = auditDir.list( JSON_FILES );
        if ( names != null && names.length > 0 )
        {
            Arrays.sort( names );
            return names;
        }
        return NO_FILE_NAMES;
    }

    public static void saveAugmentedData( final File auditDir, final String name, final InputStream data,
                                          final String user, final String ip, final String where )
        throws IOException
    {
        try
        {
            saveData( auditDir, name, parseData( IOUtil.toByteArray( data ) ), user, ip, where );
        }
        finally
        {
            IOUtil.close( data );
        }
    }

    public static void saveData( final File auditDir, final String name, final ContainerNode data, final String user,
                                 final String ip, final String where )
        throws IOException
    {
        final AuditLock lock = lockFor( auditDir );

        lock.exclusiveLock();
        try
        {
            final File auditFile = new File( auditDir, name );
            logData( auditFile, user, ip, where, data );
        }
        finally
        {
            lock.exclusiveUnlock();
        }
    }

    public static ContainerNode applyAugmentedData( final ContainerNode table, final File auditDir, final String name )
        throws IOException
    {
        final File auditFile = new File( auditDir, name );
        if ( !auditFile.canRead() )
        {
            return table;
        }

        final AuditLock lock = lockFor( auditDir );

        lock.sharedLock();
        try
        {
            return augmentTable( table, auditFile );
        }
        finally
        {
            lock.sharedUnlock();
        }
    }

    private static AuditLock lockFor( final File auditDir )
    {
        AuditLock lock = LOCK_TABLE.get( auditDir.getName() );
        if ( lock == null )
        {
            final AuditLock newLock = new AuditLock( auditDir.exists() ? 1 : 0 );
            lock = LOCK_TABLE.putIfAbsent( auditDir.getName(), newLock );
            if ( lock == null )
            {
                lock = newLock;
            }
        }
        return lock;
    }

    private static final class AuditLock
    {
        private final ReadWriteLock impl = new ReentrantReadWriteLock();

        private final AtomicInteger modCount;

        AuditLock( final int initialCount )
        {
            modCount = new AtomicInteger( initialCount );
        }

        void sharedLock()
        {
            impl.readLock().lock();
        }

        void sharedUnlock()
        {
            impl.readLock().unlock();
        }

        void exclusiveLock()
        {
            impl.writeLock().lock();
        }

        void exclusiveUnlock()
        {
            try
            {
                modCount.incrementAndGet();
            }
            finally
            {
                impl.writeLock().unlock();
            }
        }

        int modCount()
        {
            return modCount.get();
        }
    }
}
