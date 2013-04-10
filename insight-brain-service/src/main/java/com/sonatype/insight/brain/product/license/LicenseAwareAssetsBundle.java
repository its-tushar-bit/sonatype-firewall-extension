package com.sonatype.insight.brain.product.license;

import static com.google.common.base.Preconditions.checkArgument;

import javax.inject.Named;
import javax.servlet.Servlet;

import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Environment;

@Named
public class LicenseAwareAssetsBundle
    extends AssetsBundle
{
    private final String uriPath;

    private Servlet servlet;

    public LicenseAwareAssetsBundle( String licensedResourcePath, String unlicensedResourcePath, String uriPath,
                                     String indexFile )
    {
        checkArgument( licensedResourcePath.startsWith( "/" ), "%s is not an absolute path", licensedResourcePath );
        checkArgument( !"/".equals( licensedResourcePath ), "%s is the classpath root", licensedResourcePath );
        checkArgument( unlicensedResourcePath.startsWith( "/" ), "%s is not an absolute path", unlicensedResourcePath );
        checkArgument( !"/".equals( unlicensedResourcePath ), "%s is the classpath root", unlicensedResourcePath );
        this.uriPath = uriPath.endsWith( "/" ) ? uriPath : ( uriPath + '/' );
        this.servlet =
            new LicenseAwareAssetServlet( licensedResourcePath.endsWith( "/" ) ? licensedResourcePath
                            : ( licensedResourcePath + '/' ),
                                          unlicensedResourcePath.endsWith( "/" ) ? unlicensedResourcePath
                                                          : ( unlicensedResourcePath + '/' ), uriPath, indexFile );
    }

    @Override
    public void run( Environment environment )
    {
        environment.addServlet( servlet, uriPath + '*' );
    }

    public Servlet getServlet()
    {
        return servlet;
    }
}
