/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import java.util.List;

import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;

public class FailActionType
    implements ActionType
{
    public static final String ID = "fail";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Fail";
    }

    @Override
    public List<String> getAvailableTargets()
    {
        return null;
    }

    @Override
    public boolean isRequiresTarget()
    {
        return false;
    }

    @Override
    public String generateDroolsCode( final Action action )
    {
        return "insert( new PolicyFact( $component, drools.getRule().getName(), \"" + FailActionType.ID + "\" ) )";
    }
}
