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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.service.InsightBrainService;

@Named
public class LicenseAwareFilter
    implements Filter
{
    private static final Logger log = LoggerFactory.getLogger( LicenseAwareFilter.class );

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
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String fullUrl = req.getRequestURL().toString();

        //not 100% happy with this logic, ultimately I would like to have some annotations in the Resource objects that i could check
        boolean isHtmlRequest = fullUrl.contains( "/index.html" );
        boolean isLicenseHtmlRequest = fullUrl.contains( InsightBrainService.UNLICENSED_ASSET_PATH ) && isHtmlRequest;
        boolean isLicenseNotRequired =
            fullUrl.contains( InsightBrainService.APPLICATION_ASSET_PATH )
                || fullUrl.contains( InsightBrainService.BRAIN_ASSET_PATH )
                || fullUrl.contains( InsightBrainService.POLICY_ASSET_PATH )
                || fullUrl.contains( InsightBrainService.UNLICENSED_ASSET_PATH )
                || fullUrl.contains( ProductLicenseResource.SERVICE_PATH );

        // allow any non-index.html asset through, or any request that is required for licensing
        if ( isLicenseNotRequired && ( !isHtmlRequest || isLicenseHtmlRequest ) )
        {
            chain.doFilter( request, response );
        }
        else
        {
            try
            {
                licenseManager.validate();
                chain.doFilter( request, response );
            }
            catch ( InvalidLicenseException e )
            {
                log.error( e.getMessage(), e );

                if ( fullUrl.contains( "/index.html" ) && !fullUrl.contains( InsightBrainService.UNLICENSED_ASSET_PATH ) )
                {
                    String path = req.getRequestURI();

                    String redirectUrl =
                        fullUrl.substring( 0, fullUrl.indexOf( path ) ) + InsightBrainService.UNLICENSED_ASSET_PATH
                            + "index.html";

                    resp.sendRedirect( redirectUrl );
                }
                else
                {
                    resp.sendError( 402, e.getMessage() );
                }
            }
        }

    }
}
