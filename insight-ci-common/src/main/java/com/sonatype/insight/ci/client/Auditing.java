/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import static com.sonatype.insight.ci.client.DataStore.augmentTable;
import static com.sonatype.insight.ci.client.DataStore.filterDataLog;
import static com.sonatype.insight.ci.client.DataStore.logData;
import static com.sonatype.insight.ci.client.DataStore.parseData;
import static com.sonatype.insight.ci.client.DataStore.streamData;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Auditing
{
    private static final String XFF_HEADER = "X-Forwarded-For";

    private static final ConcurrentMap<String, AuditLock> LOCK_TABLE = new ConcurrentHashMap<String, AuditLock>();

    private static final String[] NO_FILE_NAMES = {};

    private static final FilenameFilter JSON_FILES = new FilenameFilter()
    {
        public boolean accept( final File dir, final String name )
        {
            return name.endsWith( ".json" );
        }
    };

    public static String findUser( final HttpServletRequest request )
    {
        String user = null;
        final Principal principal = request.getUserPrincipal();
        if ( principal != null )
        {
            user = principal.getName();
        }
        return user != null ? user : "anonymous";
    }

    public static String findIP( final HttpServletRequest request )
    {
        String ip = null;
        final String xff = request.getHeader( XFF_HEADER );
        if ( StringUtils.isNotEmpty( xff ) )
        {
            ip = resolveIp( xff.split( "," ) );
        }
        return ip != null ? ip : request.getRemoteAddr();
    }

    public static byte[] filterAuditLog( final File auditDir, final byte[] key, final String... names )
        throws IOException
    {
        final ObjectNode keyData = parseData( key );
        final ObjectNode log = keyData.objectNode();

        final AuditLock lock = lockFor( auditDir );

        lock.sharedLock();
        try
        {
            String[] fileNames = names;
            if ( names.length == 0 || names[0].length() == 0 )
            {
                fileNames = listAugmentedData( auditDir );
            }
            for ( final String name : fileNames )
            {
                final File file = new File( auditDir, name );
                if ( file.canRead() )
                {
                    log.withArray( "aaData" ).addAll( filterDataLog( file, keyData ) );
                }
            }
        }
        finally
        {
            lock.sharedUnlock();
        }

        return log.withArray( "aaData" ).size() > 0 ? streamData( log ) : null;
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
        return names != null ? names : NO_FILE_NAMES;
    }

    public static void saveAugmentedData( final File auditDir, final String name, final InputStream data,
                                          final String user, final String ip, final String where )
        throws IOException
    {
        final AuditLock lock = lockFor( auditDir );

        lock.exclusiveLock();
        try
        {
            final File auditFile = new File( auditDir, name );
            try
            {
                logData( auditFile, user, ip, where, parseData( IOUtil.toByteArray( data ) ) );
            }
            finally
            {
                IOUtil.close( data );
            }
        }
        finally
        {
            lock.exclusiveUnlock();
        }
    }

    public static ContainerNode<?> applyAugmentedData( final ContainerNode<?> table, final File auditDir,
                                                       final String name )
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

    private static String resolveIp( final String... ips )
    {
        String ip4 = null;
        String ip6 = null;

        for ( final String ip : ips )
        {
            final InetAddress address;
            try
            {
                address = InetAddress.getByAddress( ip.getBytes() );
            }
            catch ( final UnknownHostException e )
            {
                continue;
            }
            if ( address instanceof Inet4Address )
            {
                ip4 = ip;
                continue;
            }
            if ( address instanceof Inet6Address )
            {
                ip6 = ip;
                continue;
            }
        }

        if ( ip4 != null )
        {
            return ip4;
        }
        if ( ip6 != null )
        {
            return ip6;
        }

        return ips.length > 0 ? ips[0] : null;
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
