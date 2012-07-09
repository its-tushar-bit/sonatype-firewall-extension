/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import static com.sonatype.insight.ci.client.DataStore.loadData;
import static com.sonatype.insight.ci.client.DataStore.logData;
import static com.sonatype.insight.ci.client.DataStore.parseData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

public final class Auditing
{
    private static final String XFF_HEADER = "X-Forwarded-For";

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

    public static SyndFeed getAuditFeed( final File auditDir )
        throws IOException
    {
        final SyndFeedImpl feed = new SyndFeedImpl();

        feed.setFeedType( "rss_2.0" );
        feed.setPublishedDate( new Date() );
        feed.setAuthor( "Insight CI" );
        feed.setTitle( "Insight" );

        feed.setDescription( "Insight Audit Log" );

        final List<SyndEntry> entries = new ArrayList<SyndEntry>();
        entries.addAll( getAuditEntries( auditDir, "security.json" ) );
        entries.addAll( getAuditEntries( auditDir, "licenses.json" ) );
        feed.setEntries( entries );

        return feed;
    }

    public static List<SyndEntry> getAuditEntries( final File auditDir, final String name )
        throws IOException
    {
        final File auditFile = new File( auditDir, name );
        if ( !auditFile.canRead() )
        {
            return Collections.emptyList();
        }

        final String kind = StringUtils.chompLast( StringUtils.chompLast( name, ".json" ), "s" );

        final ArrayNode dataLog = loadData( auditFile );
        final List<SyndEntry> entries = new ArrayList<SyndEntry>( dataLog.size() );
        for ( final JsonNode event : dataLog )
        {
            final JsonNode data = event.get( "data" );
            if ( data.size() > 0 )
            {
                final SyndEntryImpl entry = new SyndEntryImpl();

                entry.setPublishedDate( new Date( event.get( "time" ).asLong() ) );
                final String user = event.get( "user" ).asText();
                final String ip = event.get( "ip" ).asText();
                entry.setAuthor( user + ":" + ip );

                final List<String> summary = summarize( data, kind );

                entry.setTitle( summary.get( 0 ) );
                final StringBuilder buf = new StringBuilder();
                for ( int i = 1; i < summary.size(); i++ )
                {
                    buf.append( summary.get( i ) ).append( "<br>" );
                }

                final SyndContent description = new SyndContentImpl();
                description.setValue( buf.toString() );
                entry.setDescription( description );

                entries.add( entry );
            }
        }

        return entries;
    }

    public static void saveAugmentedData( final File auditDir, final String name, final InputStream data,
                                          final String user, final String ip )
        throws IOException
    {
        final File auditFile = new File( auditDir, name );
        try
        {
            logData( auditFile, user, ip, parseData( IOUtil.toByteArray( data ) ) );
        }
        finally
        {
            IOUtil.close( data );
        }
        auditDir.setLastModified( System.currentTimeMillis() );
    }

    private static List<String> summarize( final JsonNode data, final String kind )
    {
        final JsonNode status = data.get( 0 ).get( "status" );
        final JsonNode overriden = data.get( 0 ).get( "overriddenLicenses" );
        final JsonNode comment = data.get( 0 ).get( "comment" );

        final StringBuilder title = new StringBuilder();
        if ( status != null )
        {
            String label = status.asText();
            label = label.replace( "Open", "Re-opened" );
            label = label.replace( "Not Applicable", "Ignored" );
            label = label.replace( "Overridden", "Overrode" );
            title.append( label ).append( ' ' );
        }

        final int rows = data.size();
        title.append( rows ).append( ' ' ).append( kind ).append( rows != 1 ? " alerts" : " alert" );

        if ( overriden != null && overriden.size() > 0 )
        {
            title.append( " as " ).append( overriden.get( 0 ).asText() );
        }

        final List<String> summary = new ArrayList<String>();

        summary.add( title.toString() );
        if ( comment != null && StringUtils.isNotBlank( comment.asText() ) )
        {
            summary.add( comment.asText() );
            summary.add( "" );
        }
        for ( int i = 0; i < rows; i++ )
        {
            summary.add( Report.gav( data.get( i ) ) );
        }

        return summary;
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
}
