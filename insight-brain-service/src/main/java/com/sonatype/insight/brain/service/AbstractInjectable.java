/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.lang.reflect.Type;

import javax.ws.rs.core.Context;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

public abstract class AbstractInjectable<T>
    implements InjectableProvider<Context, Type>, Injectable<AbstractInjectable<T>>
{
    public final ComponentScope getScope()
    {
        return ComponentScope.Singleton;
    }

    public final Injectable<?> getInjectable( final ComponentContext ic, final Context a, final Type c )
    {
        return c == getClass() ? this : null;
    }

    public final AbstractInjectable<T> getValue()
    {
        return this;
    }
}
