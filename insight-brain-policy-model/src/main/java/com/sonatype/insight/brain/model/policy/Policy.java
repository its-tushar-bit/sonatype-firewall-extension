/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;

public class Policy
{
    private static final Logger log = LoggerFactory.getLogger( Policy.class );

    private String id;

    private String name;

    /**
     * @since 1.6
     */
    private String ownerId;

    private boolean enabled = true;

    private int threatLevel = 5;

    private List<Constraint> constraints;

    private Map<String, List<Action>> actions;

    public Policy()
    {
    }

    public Policy( final String id, final String name )
    {
        this.id = id;
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId( final String id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public int getThreatLevel()
    {
        return threatLevel;
    }

    public void setThreatLevel( final int threatLevel )
    {
        this.threatLevel = threatLevel;
    }

    public List<Constraint> getConstraints()
    {
        return constraints;
    }

    public void setConstraints( final List<Constraint> constraints )
    {
        this.constraints = constraints;
    }

    public void addConstraint( final Constraint constraint )
    {
        if ( constraints == null )
        {
            constraints = new ArrayList<Constraint>();
        }
        constraints.add( constraint );
    }

    public Map<String, List<Action>> getActions()
    {
        return actions;
    }

    public void setActions( final Map<String, List<Action>> actions )
    {
        this.actions = actions;
    }

    public List<Action> getActions( final String stageTypeId )
    {
        return actions != null ? actions.get( stageTypeId ) : null;
    }

    public void setActions( final String stageTypeId, final List<Action> stageActions )
    {
        if ( actions == null )
        {
            actions = new HashMap<String, List<Action>>();
        }
        actions.put( stageTypeId, stageActions );
    }

    public void addAction( final String stageTypeId, final Action action )
    {
        List<Action> stageActions = getActions( stageTypeId );
        if ( stageActions == null )
        {
            setActions( stageTypeId, stageActions = new ArrayList<Action>() );
        }
        stageActions.add( action );
    }

    public ValidationResult validate( String ownerId )
    {
        return validate( ownerId, false );
    }

    public ValidationResult validate( String ownerId, boolean forEvaluation )
    {
        log.debug( "Validating " + this.toString() );

        ValidationResult result = new ValidationResult();
        if ( forEvaluation )
        {
            // if only doing evaluation, go with lenient name validation to support legacy policies
            if ( name == null || name.trim().isEmpty() )
            {
                result.addError( "The policy name must not be null or empty" );
            }
        }
        else
        {
            // if inserting/updating a policy, go with strict name validation
            try
            {
                NameHelper.validate( name );
            }
            catch ( InvalidNameException e )
            {
                result.addError( e.getMessage().replace( "Name", "The policy name" ) );
            }
        }
        if ( constraints == null || constraints.isEmpty() )
        {
            result.addError( "Policy '" + name + "' has no constraints" );
        }
        else
        {
            ValidationResult constraintResult = new ValidationResult();
            Set<String> constraintNames = new LinkedHashSet<String>();
            for ( Constraint constraint : constraints )
            {
                String constraintName = constraint.getName();
                if ( constraintName != null && !constraintName.trim().isEmpty() )
                {
                    if ( constraintNames.contains( constraintName ) )
                    {
                        constraintResult.addError( "Duplicate constraint name '" + constraintName + "'" );
                    }
                    else
                    {
                        constraintNames.add( constraintName );
                    }
                }
                constraintResult.merge( constraint.validate( ownerId ) );
            }
            if ( !constraintResult.isValid() )
            {
                result.addError( "Policy '" + name + "' has invalid constraints:" );
                result.merge( constraintResult );
            }
        }

        if ( actions != null )
        {
            ValidationResult actionResult = new ValidationResult();
            for ( String stageTypeId : actions.keySet() )
            {
                for ( Action action : actions.get( stageTypeId ) )
                {
                    ActionType actionType = ActionTypes.getById( action.getActionTypeId() );
                    actionResult.merge( actionType.validateAction( action ) );
                }
            }
            if ( !actionResult.isValid() )
            {
                result.addError( "Policy '" + name + "' has invalid actions:" );
                result.merge( actionResult );
            }
        }

        if ( !result.isValid() )
        {
            log.debug( "Validation result: " + result.toMessageString() );
        }

        return result;
    }

    @Override
    public String toString()
    {
        return "Policy [id=" + id + ", name=" + name + "]";
    }

    public Constraint getConstraintById( String constraintId )
    {
        for ( Constraint constraint : constraints )
        {
            if ( constraint.getId() != null && constraint.getId().equals( constraintId ) )
            {
                return constraint;
            }
        }
        return null;
    }

    public String getOwnerId()
    {
        return ownerId;
    }

    public void setOwnerId( String ownerId )
    {
        this.ownerId = ownerId;
    }
}
