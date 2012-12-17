/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import java.util.List;

import com.sonatype.insight.brain.model.policy.ActionType;

public class NotifyActionType
    implements ActionType
{
    public static final String ID = "notify";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Notify";
    }

    @Override
    public List<String> getAvailableTargets()
    {
        return null;
    }

    @Override
    public boolean isRequiresTarget()
    {
        return true;
    }
}
