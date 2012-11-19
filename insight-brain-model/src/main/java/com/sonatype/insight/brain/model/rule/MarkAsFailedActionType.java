/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

import java.util.List;

public class MarkAsFailedActionType
    implements ActionType
{
    public static final String ID = "MarkAsFailed";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Mark as failed";
    }

    @Override
    public List<String> getAvailableValues()
    {
        return null;
    }

    @Override
    public String generateDroolsCode( Action action )
    {
        // TODO Auto-generated method stub
        return null;
    }
}
