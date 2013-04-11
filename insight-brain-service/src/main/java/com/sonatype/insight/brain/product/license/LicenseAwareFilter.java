package com.sonatype.insight.brain.product.license;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.InsightBrainService;

@Named
public class LicenseAwareFilter
    implements Filter
{
    @Inject
    private CLMLicenseManager licenseManager;

    @Override
    public void init( FilterConfig filterConfig )
        throws ServletException
    {
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
        throws IOException, ServletException
    {
        if ( !licenseManager.isLicensedInstalled() )
        {
            HttpServletRequest req = (HttpServletRequest) request;

            String fullUrl = req.getRequestURL().toString();
            String path = req.getRequestURI();

            HttpServletResponse resp = (HttpServletResponse) response;

            String redirectUrl =
                fullUrl.substring( 0, fullUrl.indexOf( path ) ) + InsightBrainService.UNLICENSED_ASSET_PATH
                    + "index.html";

            resp.sendRedirect( redirectUrl );
        }
        else
        {
            chain.doFilter( request, response );
        }
    }
}
