/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;

public class PolicyDigester
{
    @SuppressWarnings( "unchecked" )
    public static List<PolicyAlert>[] digestPolicyAlerts( final List<PolicyAlert> newAlerts,
                                                          final List<PolicyAlert> oldAlerts )
    {
        final List<PolicyAlert> appeared = new ArrayList<PolicyAlert>();
        final List<PolicyAlert> cleared = new ArrayList<PolicyAlert>();

        int i = 0, j = 0;
        while ( true )
        {
            if ( oldAlerts == null || j >= oldAlerts.size() )
            {
                if ( newAlerts == null || i >= newAlerts.size() )
                {
                    break; // nothing left
                }
                appeared.add( newAlerts.get( i++ ) );
            }
            else if ( newAlerts == null || i >= newAlerts.size() )
            {
                cleared.add( oldAlerts.get( j++ ) );
            }
            else
            {
                final PolicyAlert newAlert = newAlerts.get( i );
                final PolicyAlert oldAlert = oldAlerts.get( j );

                final PolicyFact newTrigger = newAlert.getTrigger();
                final PolicyFact oldTrigger = oldAlert.getTrigger();

                final int comparison = newTrigger.getPolicyId().compareTo( oldTrigger.getPolicyId() );

                if ( comparison < 0 )
                {
                    appeared.add( newAlert );
                    i++;
                }
                else if ( comparison > 0 )
                {
                    cleared.add( oldAlert );
                    j++;
                }
                else
                {
                    final List<ComponentFact>[] results =
                        digestComponentFacts( newTrigger.getComponentFacts(), oldTrigger.getComponentFacts() );

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
    public static List<ComponentFact>[] digestComponentFacts( final List<ComponentFact> newFacts,
                                                              final List<ComponentFact> oldFacts )
    {
        final List<ComponentFact> appeared = new ArrayList<ComponentFact>();
        final List<ComponentFact> cleared = new ArrayList<ComponentFact>();

        int i = 0, j = 0;
        while ( true )
        {
            if ( oldFacts == null || j >= oldFacts.size() )
            {
                if ( newFacts == null || i >= newFacts.size() )
                {
                    break; // nothing left
                }
                appeared.add( newFacts.get( i++ ) );
            }
            else if ( newFacts == null || i >= newFacts.size() )
            {
                cleared.add( oldFacts.get( j++ ) );
            }
            else
            {
                final ComponentFact newFact = newFacts.get( i );
                final ComponentFact oldFact = oldFacts.get( j );

                final String newGav = newFact.getGroupId() + ':' + newFact.getArtifactId() + ':' + newFact.getVersion();
                final String oldGav = oldFact.getGroupId() + ':' + oldFact.getArtifactId() + ':' + oldFact.getVersion();
                final int comparison =
                    ( newGav + '|' + newFact.getHash() ).compareTo( oldGav + '|' + oldFact.getHash() );

                if ( comparison < 0 )
                {
                    appeared.add( newFact );
                    i++;
                }
                else if ( comparison > 0 )
                {
                    cleared.add( oldFact );
                    j++;
                }
                else
                {
                    final List<ConstraintFact>[] results =
                        digestConstraintFacts( newFact.getConstraintFacts(), oldFact.getConstraintFacts() );

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
    public static List<ConstraintFact>[] digestConstraintFacts( final List<ConstraintFact> newFacts,
                                                                final List<ConstraintFact> oldFacts )
    {
        final List<ConstraintFact> appeared = new ArrayList<ConstraintFact>();
        final List<ConstraintFact> cleared = new ArrayList<ConstraintFact>();

        int i = 0, j = 0;
        while ( true )
        {
            if ( oldFacts == null || j >= oldFacts.size() )
            {
                if ( newFacts == null || i >= newFacts.size() )
                {
                    break; // nothing left
                }
                appeared.add( newFacts.get( i++ ) );
            }
            else if ( newFacts == null || i >= newFacts.size() )
            {
                cleared.add( oldFacts.get( j++ ) );
            }
            else
            {
                final ConstraintFact newFact = newFacts.get( i );
                final ConstraintFact oldFact = oldFacts.get( j );

                final int comparison = newFact.getConstraintId().compareTo( oldFact.getConstraintId() );

                if ( comparison < 0 )
                {
                    appeared.add( newFact );
                    i++;
                }
                else if ( comparison > 0 )
                {
                    cleared.add( oldFact );
                    j++;
                }
                else
                {
                    final List<ConditionFact>[] results =
                        digestConditionFacts( newFact.getConditionFacts(), oldFact.getConditionFacts() );

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
    public static List<ConditionFact>[] digestConditionFacts( final List<ConditionFact> newFacts,
                                                              final List<ConditionFact> oldFacts )
    {
        final List<ConditionFact> appeared = new ArrayList<ConditionFact>();
        final List<ConditionFact> cleared = new ArrayList<ConditionFact>();

        int i = 0, j = 0;
        while ( true )
        {
            if ( oldFacts == null || j >= oldFacts.size() )
            {
                if ( newFacts == null || i >= newFacts.size() )
                {
                    break; // nothing left
                }
                appeared.add( newFacts.get( i++ ) );
            }
            else if ( newFacts == null || i >= newFacts.size() )
            {
                cleared.add( oldFacts.get( j++ ) );
            }
            else
            {
                final ConditionFact newFact = newFacts.get( i );
                final ConditionFact oldFact = oldFacts.get( j );

                final int comparison = newFact.getSummary().compareTo( oldFact.getSummary() );

                if ( comparison < 0 )
                {
                    appeared.add( newFact );
                    i++;
                }
                else if ( comparison > 0 )
                {
                    cleared.add( oldFact );
                    j++;
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
