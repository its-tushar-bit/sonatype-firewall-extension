package com.sonatype.insight.brain.dataaccess.label;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.model.label.ComponentLabel;

public class ComponentLabelDAO
    extends AbstractSqlDAO<ComponentLabel>
{
    public List<ComponentLabel> getByApplicationId( String applicationId )
    {
        String sQuery = "SELECT label FROM ComponentLabel label" + //
            " WHERE label.applicationId=?1";
        return getList( sQuery, applicationId );
    }

    public List<ComponentLabel> getByApplicationIdAndHash( String applicationId, String hash )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByApplicationIdAndHash( em, applicationId, hash );
        }
        finally
        {
            close( em );
        }
    }

    public List<ComponentLabel> getByApplicationIdAndHash( EntityManager em, String applicationId, String hash )
    {
        String sQuery = "SELECT label FROM ComponentLabel label" + //
            " WHERE label.applicationId=?1 AND label.hash=?2";
        return getList( em, sQuery, applicationId, hash );
    }
}
