/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;

public class LabelDAOTest
    extends AbstractDbDAOTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        LabelDAO dao = new LabelDAO();

        // Create
        Label label = new Label();
        label.setApplicationId( applicationId );
        label.setLabel( "My label" );
        label.setColor( Color.blue );
        dao.insert( label );
        Assert.assertNotNull( label.getId() );

        label = dao.getById( label.getId() );
        Assert.assertNotNull( label );
        assertLabel( applicationId, "My label", Color.blue, label );

        // Update
        label.setLabel( "My updated label" );
        dao.update( label );

        label = dao.getById( label.getId() );
        Assert.assertNotNull( label );
        assertLabel( applicationId, "My updated label", Color.blue, label );

        // Delete
        dao.delete( label );

        label = dao.getById( label.getId() );
        Assert.assertNull( label );
    }

    @Test
    public void testCascadeDelete()
    {
        LabelDAO labelDAO = new LabelDAO();
        ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

        // Create
        Label label = new Label();
        label.setApplicationId( applicationId );
        label.setLabel( "My label" );
        label.setColor( Color.blue );
        labelDAO.insert( label );
        Assert.assertNotNull( label.getId() );

        ComponentLabel componentLabel = new ComponentLabel();
        componentLabel.setApplicationId( applicationId );
        componentLabel.setLabelId( label.getId() );
        componentLabel.setHash( "ababababab" );
        componentLabelDAO.insert( componentLabel );

        // Delete
        labelDAO.delete( label );

        label = labelDAO.getById( label.getId() );
        Assert.assertNull( label );
    }

    private void assertLabel( String applicationId, String label, Color color, Label actual )
    {
        Assert.assertEquals( applicationId, actual.getApplicationId() );
        Assert.assertEquals( label, actual.getLabel() );
        Assert.assertEquals( label.toLowerCase( Locale.ENGLISH ), actual.getLabelLowercase() );
        Assert.assertEquals( color, actual.getColor() );
    }
}
