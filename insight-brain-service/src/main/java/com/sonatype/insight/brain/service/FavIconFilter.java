/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class FavIconFilter
    implements javax.servlet.Filter
{
    private String faviconPath;

    public FavIconFilter( String assetPath )
    {
        if ( assetPath.endsWith( "/" ) )
        {
            this.faviconPath = assetPath + "favicon.ico";
        }
        else
        {
            this.faviconPath = assetPath + "/favicon.ico";
        }
    }

    @Override
    public void init( FilterConfig filterConfig )
        throws ServletException
    {
    }

    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
        throws IOException, ServletException
    {
        String path = ( (HttpServletRequest) request ).getPathInfo();
        if ( path.endsWith( "/favicon.ico" ) )
        {
            request.getRequestDispatcher( faviconPath ).forward( request, response );
        }
        else
        {
            chain.doFilter( request, response );
        }
    }

    @Override
    public void destroy()
    {
    }

}
