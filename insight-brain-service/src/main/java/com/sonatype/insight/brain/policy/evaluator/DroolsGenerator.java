/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.contexts.BuildContextType;

public class DroolsGenerator
{
    private static final String INDENT = "    ";

    public String generate( final List<Policy> policies )
    {
        final StringBuilder droolsCode = new StringBuilder();
        // TODO add imports for drools
        droolsCode.append( "import com.sonatype.insight.brain.model.component.Component\n" );
        droolsCode.append( "import com.sonatype.insight.brain.model.component.PolicyFact\n" );
        droolsCode.append( "import com.sonatype.insight.brain.model.component.SecurityVulnerability\n" );
        for ( final Policy policy : policies )
        {
            if ( !policy.isEnabled() )
            {
                continue;
            }

            final List<Action> actions = policy.getActions( BuildContextType.ID /* TODO: current context */);
            if ( actions == null || actions.isEmpty() )
            {
                continue;
            }

            droolsCode.append( '\n' );
            droolsCode.append( "// Policy name: " ).append( policy.getName() ).append( '\n' );
            droolsCode.append( "rule \"" ).append( policy.getId() ).append( "\"\n" );
            droolsCode.append( "when\n" );
            droolsCode.append( INDENT ).append( "$component : Component\n" );
            droolsCode.append( INDENT ).append( "(\n" );

            int constraintIndex = 0;
            for ( final Constraint constraint : policy.getConstraints() )
            {
                if ( !constraint.isEnabled() )
                {
                    continue;
                }

                if ( constraintIndex > 0 )
                {
                    droolsCode.append( INDENT ).append( INDENT ).append( "||\n" );
                }
                droolsCode.append( INDENT ).append( INDENT ).append( "(\n" );

                int conditionIndex = 0;
                for ( final Condition condition : constraint.getConditions() )
                {
                    if ( conditionIndex > 0 )
                    {
                        droolsCode.append( INDENT ).append( INDENT ).append( INDENT );
                        if ( constraint.getOperator() == LogicalOperator.AND )
                        {
                            droolsCode.append( "&&\n" );
                        }
                        else
                        {
                            droolsCode.append( "||\n" );
                        }
                    }
                    droolsCode.append( INDENT ).append( INDENT ).append( INDENT ).append( "( " );
                    final ConditionType conditionType = ConditionTypes.getById( condition.getConditionTypeId() );
                    droolsCode.append( conditionType.generateDroolsCode( condition ) );
                    droolsCode.append( " )\n" );
                    conditionIndex++;
                }

                droolsCode.append( INDENT ).append( INDENT ).append( ")\n" );
                constraintIndex++;
            }

            droolsCode.append( INDENT ).append( ")\n" );
            droolsCode.append( "then\n" );
            for ( final Action action : actions )
            {
                final ActionType actionType = ActionTypes.getById( action.getActionTypeId() );
                droolsCode.append( INDENT ).append( actionType.generateDroolsCode( action ) ).append( ";" );
                droolsCode.append( "\n" );
            }
            droolsCode.append( "end\n" );
        }
        return droolsCode.toString();
    }
}
