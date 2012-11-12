package com.sonatype.insight.clm.service;

import java.lang.reflect.Type;

import javax.ws.rs.core.Context;

import com.sonatype.insight.scan.upload.HttpClientUtils;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

public class InsightProxy
    implements InjectableProvider<Context, Type>, Injectable<InsightProxy>
{
    public ComponentScope getScope()
    {
        return ComponentScope.Singleton;
    }

    public Injectable<?> getInjectable( final ComponentContext ic, final Context a, final Type c )
    {
        return c.toString().startsWith( "class com.sonatype.insight" ) ? this : null;
    }

    public InsightProxy getValue()
    {
        return this;
    }

    private final InsightConfiguration insightConfig;

    public InsightProxy( final InsightConfiguration insightConfig )
    {
        this.insightConfig = insightConfig;
    }

    public <T extends HttpClientUtils.Configuration> T contextualize( final T httpConfig )
    {
        httpConfig.setServerUrl( insightConfig.getSaasAddress() );
        // TODO: proxy settings
        return httpConfig;
    }
}
