/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

import java.util.ArrayList;
import java.util.List;

public class AddLabelActionType
    implements ActionType
{
    public static final String ID = "AddLabel";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Add label";
    }

    @Override
    public List<String> getAvailableValues()
    {
        // TODO Return a list of known labels
        List<String> label = new ArrayList<String>();
        label.add( "Whitelist" );
        label.add( "Blacklist" );
        label.add( "Big no-no" );
        label.add( "Must have" );
        return label;
    }

    @Override
    public String generateDroolsCode( Action action )
    {
        // TODO Auto-generated method stub
        return null;
    }
}
