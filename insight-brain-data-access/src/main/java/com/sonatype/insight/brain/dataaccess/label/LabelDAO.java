/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;

public class LabelDAO
    extends AbstractOperationalSqlDAO<Label>
{
    public List<Label> getByOwnerId( String ownerId )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByOwnerId( em, ownerId );
        }
        finally
        {
            close( em );
        }
    }

    public List<Label> getByOwnerId( EntityManager em, String ownerId )
    {
        String sQuery = "SELECT label FROM Label label" + //
            " WHERE label.ownerId=?1" + //
            " ORDER BY label.labelLowercase";
        return getList( em, sQuery, ownerId );
    }

    public List<Label> getByOwnerIdAndHash( String ownerId, String hash )
    {
        String sQuery = "SELECT label FROM Label label, ComponentLabel componentLabel" + //
            " WHERE label.id=componentLabel.labelId AND label.ownerId=componentLabel.ownerId" + //
            " AND label.ownerId=?1 AND componentLabel.hash=?2" + //
            " ORDER BY label.labelLowercase";
        return getList( sQuery, ownerId, hash );
    }

    public Label getByOwnerIdAndLowercaseLabel( String ownerId, String labelLowercase )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByOwnerIdAndLowercaseLabel( em, ownerId, labelLowercase );
        }
        finally
        {
            close( em );
        }
    }

    public Label getByOwnerIdAndLowercaseLabel( EntityManager em, String ownerId, String labelLowercase )
    {
        String sQuery = "SELECT label FROM Label label" + //
            " WHERE  label.ownerId=?1 AND label.labelLowercase=?2";
        return get( em, sQuery, ownerId, labelLowercase );
    }

    @Override
    protected Label getById( EntityManager em, String id )
    {
        String sQuery = "SELECT label FROM Label label" + //
            " WHERE label.id=?1";
        return get( em, sQuery, id );
    }

    @Override
    public void delete( EntityManager em, Label label )
    {
        ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = componentLabelDAO.getByLabelId( em, label.getId() );
        for ( ComponentLabel componentLabel : componentLabels )
        {
            componentLabelDAO.delete( em, componentLabel );
        }
        super.delete( em, label );
    }

    private void validateLabelText( String label )
    {
        if ( label == null || label.isEmpty() )
        {
            throw new InvalidLabelException( "The label text cannot be null or empty" );
        }
        if ( label.contains( " " ) )
        {
            throw new InvalidLabelException( "The label text cannot contain spaces" );
        }
        if ( label.contains( "\t" ) )
        {
            throw new InvalidLabelException( "The label text cannot contain tabs" );
        }
    }

    @Override
    public void insert( EntityManager em, Label label )
    {
        validateLabelText( label.getLabel() );
        validateLabelUnique( em, label, false );
        super.insert( em, label );
    }

    private void validateLabelUnique( EntityManager em, Label label, boolean update )
        throws InvalidLabelException
    {
        // first, check the same label does not exist in for the same owner
        // this is enforced by db unique key, but checking in java gives nicer error message
        Label otherLabel = getByOwnerIdAndLowercaseLabel( em, label.getOwnerId(), label.getLabelLowercase() );
        if ( otherLabel != null && ( !update || !otherLabel.getId().equals( label.getId() ) ) )
        {
            throw new InvalidLabelException( "A label with the same name already exists" );
        }

        // igorf: references to other entities ain't exactly pretty, but I this LabelDAO is the right place to enforce
        // label uniqueness constraints

        // owner can be an app, make sure organization does not have this label already
        String aQuery = "SELECT label FROM Label label, Application app" + //
            " WHERE label.ownerId=app.organizationId AND app.id=?1" + //
            "    AND label.labelLowercase=?2";
        if ( get( em, aQuery, label.getOwnerId(), label.getLabelLowercase() ) != null )
        {
            throw new InvalidLabelException( "A label with the same name already exists" );
        }

        // owner can be an org, make sure none of org's apps have this label already
        String oQuery = "SELECT label FROM Label label, Application app" + //
            " WHERE label.ownerId=app.id AND app.organizationId=?1" + //
            "    AND label.labelLowercase=?2";
        if ( get( em, oQuery, label.getOwnerId(), label.getLabelLowercase() ) != null )
        {
            throw new InvalidLabelException( "A label with the same name already exists" );
        }
    }

    @Override
    public void update( EntityManager em, Label label )
    {
        validateLabelText( label.getLabel() );
        validateLabelUnique( em, label, true );
        super.update( em, label );
    }
}
