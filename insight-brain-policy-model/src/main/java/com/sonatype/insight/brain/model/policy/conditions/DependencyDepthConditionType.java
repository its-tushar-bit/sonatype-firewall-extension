/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;

public class DependencyDepthConditionType
    extends AbstractConditionType
{
    public static final String ID = "DependencyDepth";

    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "is direct dependency" );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Dependency Depth";
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public String generateDroolsCode( final Condition condition )
    {
        return "getDependencyDepths().contains( Integer.valueOf( 1 ) )";
    }

    @Override
    public String explainMatch( final Condition condition, final Component component )
    {
        return ( component.getDependencyDepths().contains( 1 ) ? "Was" : "Not" ) + " a direct dependency";
    }

    @Override
    public String getValueTypeId()
    {
        return null;
    }
}
