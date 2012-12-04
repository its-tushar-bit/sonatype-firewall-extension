package com.sonatype.insight.brain.dataaccess.label;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.label.ApplicationLabel;

public class ApplicationLabelDAO
    extends AbstractSqlDAO<ApplicationLabel>
{
    public List<ApplicationLabel> getByApplicationId( String applicationId )
    {
        String sQuery = "SELECT label FROM ApplicationLabel label" + //
            " WHERE label.applicationId=?1" + //
            " ORDER BY label.labelLowercase";
        return getList( sQuery, applicationId );
    }

    public ApplicationLabel getByLowercaseLabel( String applicationId, String labelLowercase )
    {
        String sQuery = "SELECT label FROM ApplicationLabel label" + //
            " WHERE  label.applicationId=?1 AND label.labelLowercase=?2";
        return get( sQuery, applicationId, labelLowercase );
    }

    @Override
    protected ApplicationLabel getById( EntityManager em, String applicationLabelId )
    {
        if ( applicationLabelId == null || applicationLabelId.isEmpty() )
        {
            throw new DataAccessException( "The applicationId cannot be null or empty." );
        }

        String sQuery = "SELECT label FROM ApplicationLabel label" + //
            " WHERE label.id=?1";
        return get( em, sQuery, applicationLabelId );
    }
}
