/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.plexus.util.StringUtils;

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
