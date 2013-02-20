/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.facts.ComponentFact;
import com.sonatype.insight.brain.model.policy.facts.ConditionFact;
import com.sonatype.insight.brain.model.policy.facts.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.PolicyFact;

public class PolicyDigester
{
    @SuppressWarnings( "unchecked" )
    public static List<PolicyAlert>[] digestPolicyAlerts( final List<PolicyAlert> oldAlerts,
                                                          final List<PolicyAlert> newAlerts )
    {
        final List<PolicyAlert> appeared = new ArrayList<PolicyAlert>();
        final List<PolicyAlert> cleared = new ArrayList<PolicyAlert>();

        int i = 0, j = 0;
        while ( true )
        {
            if ( oldAlerts == null || i >= oldAlerts.size() )
            {
                if ( newAlerts == null || j >= newAlerts.size() )
                {
                    break; // nothing left
                }
                appeared.add( newAlerts.get( j++ ) );
            }
            else if ( newAlerts == null || j >= newAlerts.size() )
            {
                cleared.add( oldAlerts.get( i++ ) );
            }
            else
            {
                final PolicyAlert oldAlert = oldAlerts.get( i );
                final PolicyAlert newAlert = newAlerts.get( j );

                final PolicyFact oldTrigger = oldAlert.getTrigger();
                final PolicyFact newTrigger = newAlert.getTrigger();

                final int comparison = oldTrigger.getPolicyId().compareTo( newTrigger.getPolicyId() );

                if ( comparison > 0 )
                {
                    appeared.add( newAlert );
                    j++;
                }
                else if ( comparison < 0 )
                {
                    cleared.add( oldAlert );
                    i++;
                }
                else
                {
                    final List<ComponentFact>[] results =
                        digestComponentFacts( oldTrigger.getComponentFacts(), newTrigger.getComponentFacts() );

                    if ( results != null )
                    {
                        if ( !results[0].isEmpty() )
                        {
                            appeared.add( newAlert.with( newTrigger.with( results[0] ) ) );
                        }
                        if ( !results[1].isEmpty() )
                        {
                            cleared.add( oldAlert.with( oldTrigger.with( results[1] ) ) );
                        }
                    }

                    i++;
                    j++;
                }
            }
        }

        if ( appeared.isEmpty() && cleared.isEmpty() )
        {
            return null;
        }

        return new List[] { appeared, cleared };
    }

    @SuppressWarnings( "unchecked" )
    public static List<ComponentFact>[] digestComponentFacts( final List<ComponentFact> oldFacts,
                                                              final List<ComponentFact> newFacts )
    {
        final List<ComponentFact> appeared = new ArrayList<ComponentFact>();
        final List<ComponentFact> cleared = new ArrayList<ComponentFact>();

        int i = 0, j = 0;
        while ( true )
        {
            if ( oldFacts == null || i >= oldFacts.size() )
            {
                if ( newFacts == null || j >= newFacts.size() )
                {
                    break; // nothing left
                }
                appeared.add( newFacts.get( j++ ) );
            }
            else if ( newFacts == null || j >= newFacts.size() )
            {
                cleared.add( oldFacts.get( i++ ) );
            }
            else
            {
                final ComponentFact oldFact = oldFacts.get( i );
                final ComponentFact newFact = newFacts.get( j );

                final int comparison =
                    ( oldFact.getGAV() + '|' + oldFact.getHash() ).compareTo( newFact.getGAV() + '|'
                        + newFact.getHash() );

                if ( comparison > 0 )
                {
                    appeared.add( newFact );
                    j++;
                }
                else if ( comparison < 0 )
                {
                    cleared.add( oldFact );
                    i++;
                }
                else
                {
                    final List<ConstraintFact>[] results =
                        digestConstraintFacts( oldFact.getConstraintFacts(), newFact.getConstraintFacts() );

                    if ( results != null )
                    {
                        if ( !results[0].isEmpty() )
                        {
                            appeared.add( newFact.with( results[0] ) );
                        }
                        if ( !results[1].isEmpty() )
                        {
                            cleared.add( oldFact.with( results[1] ) );
                        }
                    }

                    i++;
                    j++;
                }
            }
        }

        if ( appeared.isEmpty() && cleared.isEmpty() )
        {
            return null;
        }

        return new List[] { appeared, cleared };
    }

    @SuppressWarnings( "unchecked" )
    public static List<ConstraintFact>[] digestConstraintFacts( final List<ConstraintFact> oldFacts,
                                                                final List<ConstraintFact> newFacts )
    {
        final List<ConstraintFact> appeared = new ArrayList<ConstraintFact>();
        final List<ConstraintFact> cleared = new ArrayList<ConstraintFact>();

        int i = 0, j = 0;
        while ( true )
        {
            if ( oldFacts == null || i >= oldFacts.size() )
            {
                if ( newFacts == null || j >= newFacts.size() )
                {
                    break; // nothing left
                }
                appeared.add( newFacts.get( j++ ) );
            }
            else if ( newFacts == null || j >= newFacts.size() )
            {
                cleared.add( oldFacts.get( i++ ) );
            }
            else
            {
                final ConstraintFact oldFact = oldFacts.get( i );
                final ConstraintFact newFact = newFacts.get( j );

                final int comparison = ( oldFact.getConstraintId() ).compareTo( newFact.getConstraintId() );

                if ( comparison > 0 )
                {
                    appeared.add( newFact );
                    j++;
                }
                else if ( comparison < 0 )
                {
                    cleared.add( oldFact );
                    i++;
                }
                else
                {
                    final List<ConditionFact>[] results =
                        digestConditionFacts( oldFact.getConditionFacts(), newFact.getConditionFacts() );

                    if ( results != null )
                    {
                        if ( !results[0].isEmpty() )
                        {
                            appeared.add( newFact.with( results[0] ) );
                        }
                        if ( !results[1].isEmpty() )
                        {
                            cleared.add( oldFact.with( results[1] ) );
                        }
                    }

                    i++;
                    j++;
                }
            }
        }

        if ( appeared.isEmpty() && cleared.isEmpty() )
        {
            return null;
        }

        return new List[] { appeared, cleared };
    }

    @SuppressWarnings( "unchecked" )
    public static List<ConditionFact>[] digestConditionFacts( final List<ConditionFact> oldFacts,
                                                              final List<ConditionFact> newFacts )
    {
        final List<ConditionFact> appeared = new ArrayList<ConditionFact>();
        final List<ConditionFact> cleared = new ArrayList<ConditionFact>();

        int i = 0, j = 0;
        while ( true )
        {
            if ( oldFacts == null || i >= oldFacts.size() )
            {
                if ( newFacts == null || j >= newFacts.size() )
                {
                    break; // nothing left
                }
                appeared.add( newFacts.get( j++ ) );
            }
            else if ( newFacts == null || j >= newFacts.size() )
            {
                cleared.add( oldFacts.get( i++ ) );
            }
            else
            {
                final ConditionFact oldFact = oldFacts.get( i );
                final ConditionFact newFact = newFacts.get( j );

                final int comparison = ( oldFact.getSummary() ).compareTo( newFact.getSummary() );

                if ( comparison > 0 )
                {
                    appeared.add( newFact );
                    j++;
                }
                else if ( comparison < 0 )
                {
                    cleared.add( oldFact );
                    i++;
                }
                else
                {
                    i++;
                    j++;
                }
            }
        }

        if ( appeared.isEmpty() && cleared.isEmpty() )
        {
            return null;
        }

        return new List[] { appeared, cleared };
    }
}
