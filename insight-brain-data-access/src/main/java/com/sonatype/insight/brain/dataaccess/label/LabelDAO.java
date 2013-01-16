/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;

public class LabelDAO
    extends AbstractSqlDAO<Label>
{
    public List<Label> getByApplicationId( String applicationId )
    {
        String sQuery = "SELECT label FROM Label label" + //
            " WHERE label.applicationId=?1" + //
            " ORDER BY label.labelLowercase";
        return getList( sQuery, applicationId );
    }

    public List<Label> getByApplicationIdAndHash( String applicationId, String hash )
    {
        String sQuery = "SELECT label FROM Label label, ComponentLabel componentLabel" + //
            " WHERE label.id=componentLabel.labelId AND label.applicationId=componentLabel.applicationId" + //
            " AND label.applicationId=?1 AND componentLabel.hash=?2" + //
            " ORDER BY label.labelLowercase";
        return getList( sQuery, applicationId, hash );
    }

    public Label getByApplicationIdAndLowercaseLabel( String applicationId, String labelLowercase )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByApplicationIdAndLowercaseLabel( em, applicationId, labelLowercase );
        }
        finally
        {
            close( em );
        }
    }

    Label getByApplicationIdAndLowercaseLabel( EntityManager em, String applicationId, String labelLowercase )
    {
        String sQuery = "SELECT label FROM Label label" + //
            " WHERE  label.applicationId=?1 AND label.labelLowercase=?2";
        return get( em, sQuery, applicationId, labelLowercase );
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
        List<ComponentLabel> componentLabels = componentLabelDAO.getByLabelId( label.getId() );
        for ( ComponentLabel componentLabel : componentLabels )
        {
            componentLabelDAO.delete( em, componentLabel );
        }
        super.delete( em, label );
    }
}
