/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.CoordinatesValueType;

public class CoordinatesConditionType
    extends AbstractConditionType
    implements ConditionType
{
    public static final String ID = "Coordinates";

    private static List<String> supportedOperators;

    static
    {
        supportedOperators = new ArrayList<String>();
        supportedOperators.add( "match" );
        supportedOperators.add( "do not match" );
        supportedOperators = Collections.unmodifiableList( supportedOperators );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Coordinates (GAV)";
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public String generateDroolsCode( final Condition condition )
    {
        String groupId = "";
        String artifactId = "";
        String version = "";
        if ( condition.getValue() != null )
        {
            String[] coordinates = condition.getValue().split( ":" );
            if ( coordinates.length >= 1 )
            {
                groupId = coordinates[0];
            }
            if ( coordinates.length >= 2 )
            {
                artifactId = coordinates[1];
            }
            if ( coordinates.length >= 3 )
            {
                version = coordinates[2];
            }
        }

        // Drools does not allow a ! to negate the condition in this case, so we have to use "== false" :(
        return "getGroupId() != null && " + "new ArtifactCoordinate( \"" + groupId + "\", \"" + artifactId + "\", \""
            + version + "\" ).matches( getGroupId(), getArtifactId(), getVersion() )"
            + ( "match".equals( condition.getOperator() ) ? "" : " == false" );
    }

    @Override
    public String getValueTypeId()
    {
        return CoordinatesValueType.ID;
    }
}
