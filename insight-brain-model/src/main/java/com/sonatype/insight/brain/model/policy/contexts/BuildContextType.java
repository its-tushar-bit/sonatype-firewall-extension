/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.contexts;

import com.sonatype.insight.brain.model.policy.ContextType;

public class BuildContextType
    implements ContextType
{
    public static final String ID = "build";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Build";
    }
}
