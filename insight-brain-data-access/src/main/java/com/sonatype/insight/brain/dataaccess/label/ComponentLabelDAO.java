package com.sonatype.insight.brain.dataaccess.label;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.model.label.ComponentLabel;

public class ComponentLabelDAO
    extends AbstractSqlDAO<ComponentLabel>
{
    public ComponentLabel getDeletedById( String id )
    {
        String sQuery = "SELECT label FROM ComponentLabel label" + //
            " WHERE label.id=?1 AND label.active=false";
        return get( sQuery, id );
    }

    public List<ComponentLabel> getByApplicationId( String applicationId )
    {
        String sQuery = "SELECT label FROM ComponentLabel label" + //
            " WHERE label.applicationId=?1 AND label.active=true" + //
            " ORDER BY label.labelLowercase";
        return getList( sQuery, applicationId );
    }

    public List<ComponentLabel> getByApplicationIdAndArtifactHash( String applicationId, String hash )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByApplicationIdAndArtifactHash( em, applicationId, hash );
        }
        finally
        {
            close( em );
        }
    }

    public List<ComponentLabel> getByApplicationIdAndArtifactHash( EntityManager em, String applicationId,
                                                                             String hash )
    {
        String sQuery = "SELECT label FROM ComponentLabel label" + //
            " WHERE label.applicationId=?1 AND label.hash=?2 AND label.active=true" + //
            " ORDER BY label.labelLowercase";
        return getList( em, sQuery, applicationId, hash );
    }

    public List<ComponentLabel> getByLabel( String applicationId, String labelLowercase )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByLabel( em, applicationId, labelLowercase );
        }
        finally
        {
            close( em );
        }
    }

    public List<ComponentLabel> getByLabel( EntityManager em, String applicationId, String labelLowercase )
    {
        String sQuery = "SELECT label FROM ComponentLabel label" + //
            " WHERE label.applicationId=?1 AND label.labelLowercase=?2 AND label.active=true";

        return getList( em, sQuery, applicationId, labelLowercase );
    }

    @Override
    public void insert( EntityManager em, ComponentLabel label )
    {
        if ( label.getCreateUserId() == null )
        {
            throw new IllegalArgumentException( "ComponentLabel.createUserId must be provided" );
        }
        label.setCreateTime( new Date() );
        label.setDeleteTime( ComponentLabel.NULL_DELETE_TIME );
        super.insert( em, label );
    }

    @Override
    public void delete( EntityManager em, ComponentLabel label )
    {
        if ( label.getDeleteUserId() == null )
        {
            throw new IllegalArgumentException( "ComponentLabel.deleteUserId must be provided" );
        }
        label.setDeleteTime( new Date() );
        label.setActive( false );
        super.update( em, label );
    }
}
