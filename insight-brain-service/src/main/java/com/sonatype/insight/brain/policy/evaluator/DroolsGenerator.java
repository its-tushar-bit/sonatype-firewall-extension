/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.insight.brain.model.rule.Action;
import com.sonatype.insight.brain.model.rule.ActionType;
import com.sonatype.insight.brain.model.rule.AllActionTypes;
import com.sonatype.insight.brain.model.rule.AllConditionTypes;
import com.sonatype.insight.brain.model.rule.ConditionType;
import com.sonatype.insight.brain.model.rule.LogicalOperator;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.model.rule.SimpleCondition;

public class DroolsGenerator
{
    private static final String INDENT = "    ";

    public String generate( List<Rule> rules )
    {
        StringBuilder droolsCode = new StringBuilder();
        // TODO add imports for drools
        droolsCode.append( "import com.sonatype.insight.brain.model.component.Component\n" );
        droolsCode.append( "import com.sonatype.insight.brain.model.component.PolicyFact\n" );
        droolsCode.append( "import com.sonatype.insight.brain.model.component.SecurityVulnerability\n" );
        for ( Rule rule : rules )
        {
            if ( !rule.isEnabled() )
            {
                continue;
            }

            droolsCode.append( '\n' );
            droolsCode.append( "// Rule name: " ).append( rule.getName() ).append( '\n' );
            droolsCode.append( "rule \"" ).append( rule.getId() ).append( "\"\n" );
            droolsCode.append( "when\n" );
            droolsCode.append( INDENT ).append( "$component : Component\n" );
            droolsCode.append( INDENT ).append( "(\n" );
            int conditionIndex = 0;
            for ( SimpleCondition condition : rule.getConditions() )
            {
                if ( conditionIndex > 0 )
                {
                    droolsCode.append( INDENT ).append( INDENT );
                    if ( rule.getOperator() == LogicalOperator.AND )
                    {
                        droolsCode.append( "&&\n" );
                    }
                    else
                    {
                        droolsCode.append( "||\n" );
                    }
                }
                droolsCode.append( INDENT ).append( INDENT ).append( "( " );
                ConditionType conditionType = AllConditionTypes.getById( condition.getConditionTypeId() );
                droolsCode.append( conditionType.generateDroolsCode( condition ) );
                droolsCode.append( " )\n" );
                conditionIndex++;
            }
            droolsCode.append( INDENT ).append( ")\n" );
            droolsCode.append( "then\n" );
            for ( Action action : rule.getActions() )
            {
                ActionType actionType = AllActionTypes.getById( action.getActionTypeId() );
                droolsCode.append( INDENT ).append( actionType.generateDroolsCode( action ) ).append( ";" );
                droolsCode.append( "\n" );
            }
            droolsCode.append( "end\n" );
        }
        return droolsCode.toString();
    }
}
