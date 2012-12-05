package com.sonatype.insight.brain.dataaccess.label;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
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

    public Label getByApplicationIdAndLowercaseLabel( String applicationId, String labelLowercase )
    {
        String sQuery = "SELECT label FROM Label label" + //
            " WHERE  label.applicationId=?1 AND label.labelLowercase=?2";
        return get( sQuery, applicationId, labelLowercase );
    }

    @Override
    protected Label getById( EntityManager em, String id )
    {
        String sQuery = "SELECT label FROM Label label" + //
            " WHERE label.id=?1";
        return get( em, sQuery, id );
    }
}
