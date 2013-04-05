/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;

public class ComponentLabelDAO
    extends AbstractOperationalSqlDAO<ComponentLabel>
{
    List<ComponentLabel> getByLabelId( EntityManager em, String labelId )
    {
        String sQuery = "SELECT entity FROM ComponentLabel entity" + //
            " WHERE entity.labelId=?1";
        return getList( em, sQuery, labelId );
    }

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

    @Override
    public void update( EntityManager em, ComponentLabel entity )
    {
        throw new UnsupportedOperationException();
    }

    public void setComponentLabels( String applicationId, String hash, Set<String> stringLabels, Color defaultColor )
    {
        if ( stringLabels == null )
        {
            stringLabels = new LinkedHashSet<String>();
        }

        for ( String label : stringLabels )
        {
            if ( label.length() > 50 )
            {
                throw new InvalidLabelException( "The label '" + label
                    + "' exceeds the maximum length of 50 characters" );
            }
        }

        // Check labels are unique case insensitive
        Set<String> labelsLowercase = new LinkedHashSet<String>();
        Iterator<String> stringLabelsIter = stringLabels.iterator();
        while ( stringLabelsIter.hasNext() )
        {
            String labelLowercase = stringLabelsIter.next().toLowerCase( Locale.ENGLISH );
            if ( labelsLowercase.contains( labelLowercase ) )
            {
                stringLabelsIter.remove();
            }
            else
            {
                labelsLowercase.add( labelLowercase );
            }
        }

        LabelDAO labelDAO = new LabelDAO();
        EntityManager em = createEntityManager();
        try
        {
            em.getTransaction().begin();

            // Remove obsolete labels
            List<ComponentLabel> oldComponentLabels = new ArrayList<ComponentLabel>();
            oldComponentLabels.addAll( getByApplicationIdAndHash( em, applicationId, hash ) );
            Iterator<ComponentLabel> iterOldComponentLabel = oldComponentLabels.iterator();
            while ( iterOldComponentLabel.hasNext() )
            {
                boolean deleteOldLabel = true;
                ComponentLabel oldComponentLabel = iterOldComponentLabel.next();
                Label oldLabel = labelDAO.getById( em, oldComponentLabel.getLabelId() );
                Iterator<String> iterStringLabels = stringLabels.iterator();
                while ( iterStringLabels.hasNext() )
                {
                    String stringLabel = iterStringLabels.next();
                    if ( oldLabel.getLabelLowercase().equals( stringLabel.toLowerCase( Locale.ENGLISH ) ) )
                    {
                        // This label already exists
                        iterStringLabels.remove();
                        deleteOldLabel = false;
                        break;
                    }
                }

                if ( deleteOldLabel )
                {
                    delete( em, oldComponentLabel );
                    iterOldComponentLabel.remove();
                }
            }

            // Add new labels
            // stringLabels contains only new labels now
            for ( String stringLabel : stringLabels )
            {
                String labelLowercase = stringLabel.toLowerCase( Locale.ENGLISH );
                Label label = labelDAO.getByApplicationIdAndLowercaseLabel( em, applicationId, labelLowercase );
                if ( label == null )
                {
                    label = new Label();
                    label.setApplicationId( applicationId );
                    label.setLabel( stringLabel );
                    label.setColor( defaultColor );
                    labelDAO.insert( em, label );
                }
                ComponentLabel componentLabel = new ComponentLabel();
                componentLabel.setApplicationId( applicationId );
                componentLabel.setHash( hash );
                componentLabel.setLabelId( label.getId() );
                insert( em, componentLabel );
            }

            em.getTransaction().commit();
        }
        finally
        {
            close( em );
        }
    }
}
