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
    }

    @Test
    public void testSetComponentLabels_Null()
    {
        Set<String> noLabels = null;
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, noLabels, null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, toLabelSet( "Label1", "Label2" ), null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 2, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, noLabels, null );
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

        dao.setComponentLabels( applicationId, hash, noLabels, null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, toLabelSet( "Label1", "Label2" ), null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 2, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, noLabels, null );
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

        dao.setComponentLabels( applicationId, hash, newLabels, null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( newLabels, componentLabels );

        newLabels = toLabelSet( "New Label" );
        dao.setComponentLabels( applicationId, hash, newLabels, null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( newLabels, componentLabels );
    }

    @Test
    public void testSetComponentLabels_Two()
    {
        Set<String> newLabels = toLabelSet( "Label1", "Label2" );
        ComponentLabelDAO dao = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        Assert.assertEquals( 0, componentLabels.size() );

        dao.setComponentLabels( applicationId, hash, newLabels, null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( newLabels, componentLabels );

        newLabels = toLabelSet( "New Label1", "New Label2" );
        dao.setComponentLabels( applicationId, hash, newLabels, null );
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

        dao.setComponentLabels( applicationId, hash, toLabelSet( "Label", "label" ), null );
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

        dao.setComponentLabels( applicationId, hash, toLabelSet( "Label" ), null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( toLabelSet( "Label" ), componentLabels );

        dao.setComponentLabels( applicationId, hash, toLabelSet( "label" ), null );
        componentLabels = dao.getByApplicationIdAndHash( applicationId, hash );
        Assert.assertNotNull( componentLabels );
        assertComponentLabels( toLabelSet( "Label" ), componentLabels );
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
