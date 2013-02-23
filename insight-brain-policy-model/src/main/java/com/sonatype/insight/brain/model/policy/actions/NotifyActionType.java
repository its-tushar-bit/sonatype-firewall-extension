/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;

public class NotifyActionType
    implements ActionType
{
    public static final String ID = Action.ID_NOTIFY;

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
    
    @Override
    public String getSummary()
    {
        return "Notification Sent";
    }
}
