/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;

public class ComponentLabelDAOTest
    extends AbstractDbDAOTest
{
    private final String hash = "ababababab";

    @After
    public void cleanUp()
    {
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        for ( ComponentLabel componentLabel : componentLabels )
        {
            dao.delete( componentLabel );
        }

        LabelDAO labelDAO = new LabelDAO();
        List<Label> labels = labelDAO.getByApplicationId( applicationId );
        for ( Label label : labels )
        {
            labelDAO.delete( label );
        }
    }

    @Test
    public void testSetComponentLabels_Null()
    {
        Set<String> noLabels = null;
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, noLabels, null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, toLabelSet( "Label1", "Label2" ), null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 2, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, noLabels, null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );
    }

    @Test
    public void testSetComponentLabels_None()
    {
        Set<String> noLabels = Collections.emptySet();
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, noLabels, null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, toLabelSet( "Label1", "Label2" ), null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 2, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, noLabels, null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );
    }

    @Test
    public void testSetComponentLabels_One()
    {
        Set<String> newLabels = toLabelSet( "Label" );
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, newLabels, null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( newLabels, componentLabels );

        newLabels = toLabelSet( "NewLabel" );
        dao.setComponentLabels( applicationId, hash, newLabels, null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( newLabels, componentLabels );
    }

    @Test
    public void testSetComponentLabels_DefaultColor()
    {
        Set<String> newLabels = toLabelSet( "Label" );
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        // Not null default color
        dao.setComponentLabels( applicationId, hash, newLabels, Color.orange );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( newLabels, componentLabels );

        LabelDAO labelDAO = new LabelDAO();
        List<Label> appLabels = labelDAO.getByApplicationId( applicationId );
        Assert.assertEquals( 1, appLabels.size() );
        Label appLabel = appLabels.get( 0 );
        Assert.assertEquals( Color.orange, appLabel.getColor() );

        // Null default color
        dao.setComponentLabels( applicationId, hash, newLabels, null /* defaultColor */);
        appLabels = labelDAO.getByApplicationId( applicationId );
        Assert.assertEquals( 1, appLabels.size() );
        appLabel = appLabels.get( 0 );
        Assert.assertEquals( Color.orange, appLabel.getColor() );
    }

    @Test
    public void testSetComponentLabels_Two()
    {
        Set<String> newLabels = toLabelSet( "Label1", "Label2" );
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, newLabels, null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( newLabels, componentLabels );

        newLabels = toLabelSet( "NewLabel1", "NewLabel2" );
        dao.setComponentLabels( applicationId, hash, newLabels, null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( newLabels, componentLabels );
    }

    @Test
    public void testSetComponentLabels_Duplicate()
    {
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, toLabelSet( "Label", "label" ), null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( toLabelSet( "Label" ), componentLabels );
    }

    @Test
    public void testSetComponentLabels_LabelAlreadyExists()
    {
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, toLabelSet( "Label" ), null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( toLabelSet( "Label" ), componentLabels );

        dao.setComponentLabels( applicationId, hash, toLabelSet( "label" ), null /* defaultColor */);
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( toLabelSet( "Label" ), componentLabels );
    }

    @Test
    public void testSetComponentLabels_TooLong()
        throws Exception
    {
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        Set<String> labels = toLabelSet( "A_very_long_label_that_exceeds_our_maximum_label_length_and_cannot_be_added" );
        try
        {
            dao.setComponentLabels( applicationId, hash, labels, null /* defaultColor */);
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !expected.getMessage().contains( "exceeds the maximum length" ) )
            {
                throw expected;
            }
        }
    }

    private void assertComponentLabels( Set<String> expectedLabels, List<ComponentLabel> actualLabels )
    {
        Assert.assertEquals( expectedLabels.size(), actualLabels.size() );

        LabelDAO labelDAO = new LabelDAO();
        Set<String> actualStringLabels = new LinkedHashSet<String>();
        for ( ComponentLabel componentLabel : actualLabels )
        {
            Label label = labelDAO.getById( componentLabel.getLabelId() );
            actualStringLabels.add( label.getLabelLowercase() );
        }

        for ( String expectedLabel : expectedLabels )
        {
            Assert.assertTrue( "Expected label " + expectedLabel,
                               actualStringLabels.contains( expectedLabel.toLowerCase( Locale.ENGLISH ) ) );
        }
    }

    private Set<String> toLabelSet( String... labels )
    {
        Set<String> labelSet = new LinkedHashSet<String>();
        for ( String label : labels )
        {
            labelSet.add( label );
        }
        return labelSet;
    }
}
