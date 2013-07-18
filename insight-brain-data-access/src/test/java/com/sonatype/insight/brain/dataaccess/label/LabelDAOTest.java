/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;

public class LabelDAOTest
    extends AbstractDbDAOTest
{
    @Override
    public void setUp()
    {
        // 'super.setUp()' is intentionally omitted

        // create an organization
        OrganizationDAO organizationDAO = new OrganizationDAO();
        organization = new Organization( "organization" );
        organizationDAO.insert( organization );

        // Create an application
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = new Application();
        application.setName( applicationName );
        application.setPublicId( applicationPublicId );
        application.setOrganizationId( organization.getId() );
        applicationDAO.insert( application );
        applicationId = application.getId();
        Assert.assertNotNull( applicationId );
    }

    @After
    public void cleanUp()
    {
        LabelDAO dao = new LabelDAO();
        List<Label> labels = dao.getByOwnerId( applicationId );
        for ( Label label : labels )
        {
            dao.delete( label );
        }
    }

    @Test
    public void testLabelWithSpaces()
        throws Exception
    {
        LabelDAO dao = new LabelDAO();
        Label label = new Label();
        label.setOwnerId( applicationId );
        label.setLabel( "My label" );

        // Insert
        try
        {
            dao.insert( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"The label text cannot contain spaces".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        // Update
        label.setLabel( "MyLabel" );
        dao.insert( label );
        label.setLabel( "My UpdatedLabel" );
        try
        {
            dao.update( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"The label text cannot contain spaces".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testLabelWithTabs()
        throws Exception
    {
        LabelDAO dao = new LabelDAO();
        Label label = new Label();
        label.setOwnerId( applicationId );
        label.setLabel( "My\tlabel" );

        // Insert
        try
        {
            dao.insert( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"The label text cannot contain tabs".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        // Update
        label.setLabel( "MyLabel" );
        dao.insert( label );
        label.setLabel( "My\tUpdatedLabel" );
        try
        {
            dao.update( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"The label text cannot contain tabs".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testLabelNull()
        throws Exception
    {
        LabelDAO dao = new LabelDAO();
        Label label = new Label();
        label.setOwnerId( applicationId );
        label.setLabel( null );

        // Insert
        try
        {
            dao.insert( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"The label text cannot be null or empty".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        // Update
        label.setLabel( "MyLabel" );
        dao.insert( label );
        label.setLabel( null );
        try
        {
            dao.update( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"The label text cannot be null or empty".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testLabelEmpty()
        throws Exception
    {
        LabelDAO dao = new LabelDAO();
        Label label = new Label();
        label.setOwnerId( applicationId );
        label.setLabel( " " );

        // Insert
        try
        {
            dao.insert( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"The label text cannot be null or empty".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        // Update
        label.setLabel( "MyLabel" );
        dao.insert( label );
        label.setLabel( " " );
        try
        {
            dao.update( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"The label text cannot be null or empty".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testSetColorBackToNull()
        throws Exception
    {
        LabelDAO dao = new LabelDAO();
        Label label = new Label();
        label.setOwnerId( applicationId );
        label.setLabel( "MyLabel" );
        label.setColor( Color.blue );
        dao.insert( label );
        Assert.assertNotNull( label.getId() );
        label = dao.getById( label.getId() );
        Assert.assertNotNull( label );
        assertLabel( applicationId, "MyLabel", Color.blue, label );

        // Update the color using a new Label instance. This is important because an instance that was not retrieved
        // from the db was never marked as attached/detached by openjpa.
        Label updatedLabel = new Label();
        updatedLabel.setId( label.getId() );
        updatedLabel.setOwnerId( label.getOwnerId() );
        updatedLabel.setLabel( label.getLabel() );
        updatedLabel.setColor( null );
        dao.update( updatedLabel );
        Assert.assertNull( updatedLabel.getColor() );
        updatedLabel = dao.getById( updatedLabel.getId() );
        Assert.assertNotNull( updatedLabel );
        Assert.assertNull( updatedLabel.getColor() );
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        LabelDAO dao = new LabelDAO();

        // Create
        Label label = new Label();
        label.setOwnerId( applicationId );
        label.setLabel( "MyLabel" );
        label.setColor( Color.blue );
        dao.insert( label );
        Assert.assertNotNull( label.getId() );

        label = dao.getById( label.getId() );
        Assert.assertNotNull( label );
        assertLabel( applicationId, "MyLabel", Color.blue, label );

        // Update
        label.setLabel( "MyUpdatedLabel" );
        dao.update( label );

        label = dao.getById( label.getId() );
        Assert.assertNotNull( label );
        assertLabel( applicationId, "MyUpdatedLabel", Color.blue, label );

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
        label.setOwnerId( applicationId );
        label.setLabel( "MyLabel" );
        label.setColor( Color.blue );
        labelDAO.insert( label );
        Assert.assertNotNull( label.getId() );

        ComponentLabel componentLabel = new ComponentLabel();
        componentLabel.setOwnerId( applicationId );
        componentLabel.setLabelId( label.getId() );
        componentLabel.setHash( "ababababab" );
        componentLabelDAO.insert( componentLabel );

        // Delete
        labelDAO.delete( label );

        label = labelDAO.getById( label.getId() );
        Assert.assertNull( label );
    }

    @Test
    public void testAddDuplicateLabel()
        throws Exception
    {
        LabelDAO labelDAO = new LabelDAO();

        // Add a label
        Label label = new Label();
        label.setOwnerId( applicationId );
        label.setLabel( "MyLabel" );
        label.setColor( Color.blue );
        labelDAO.insert( label );

        // Add another label with the same name
        label = new Label();
        label.setOwnerId( applicationId );
        label.setColor( Color.blue );
        label.setLabel( "MyLabel" );
        try
        {
            labelDAO.insert( label );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"A label with the same name already exists".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testUpdateDuplicateLabel()
        throws Exception
    {
        LabelDAO labelDAO = new LabelDAO();

        // Add a label
        Label label1 = new Label();
        label1.setOwnerId( applicationId );
        label1.setLabel( "MyLabel1" );
        label1.setColor( Color.blue );
        labelDAO.insert( label1 );

        // Add another label
        Label label2 = new Label();
        label2.setOwnerId( applicationId );
        label2.setColor( Color.blue );
        label2.setLabel( "MyLabel2" );
        labelDAO.insert( label2 );

        // Update without changing the name
        label2.setColor( Color.red );
        labelDAO.update( label2 );
        assertLabel( applicationId, "MyLabel2", Color.red, label2 );

        // Update with a conflicting name
        label2.setLabel( label1.getLabel() );
        try
        {
            labelDAO.update( label2 );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            if ( !"A label with the same name already exists".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testDuplicateLabelInApplication()
        throws Exception
    {
        LabelDAO labelDAO = new LabelDAO();

        Label label1 = new Label();
        label1.setOwnerId( applicationId );
        label1.setLabel( "MyLabel" );
        label1.setColor( Color.blue );
        labelDAO.insert( label1 );

        // direct insert of duplicate label
        try
        {
            Label label2 = new Label();
            label2.setOwnerId( organization.getId() );
            label2.setLabel( "MyLabel" );
            label2.setColor( Color.blue );
            labelDAO.insert( label2 );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            String expectedMessage = "A label with the same name already exists";
            if ( !expectedMessage.equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        // rename label to become a duplicate
        Label label2 = new Label();
        label2.setOwnerId( organization.getId() );
        label2.setLabel( "MyLabel2" );
        label2.setColor( Color.blue );
        labelDAO.insert( label2 );
        try
        {
            label2.setLabel( "MyLabel" );
            labelDAO.update( label2 );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            String expectedMessage = "A label with the same name already exists";
            if ( !expectedMessage.equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

    }

    @Test
    public void testDuplicateLabelInOrganization()
        throws Exception
    {
        LabelDAO labelDAO = new LabelDAO();

        Label label1 = new Label();
        label1.setOwnerId( organization.getId() );
        label1.setLabel( "MyLabel" );
        label1.setColor( Color.blue );
        labelDAO.insert( label1 );

        // direct insert of duplicate label
        try
        {
            Label label2 = new Label();
            label2.setOwnerId( applicationId );
            label2.setLabel( "MyLabel" );
            label2.setColor( Color.blue );
            labelDAO.insert( label2 );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            String expectedMessage = "A label with the same name already exists";
            if ( !expectedMessage.equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        // rename label to become a duplicate
        Label label2 = new Label();
        label2.setOwnerId( applicationId );
        label2.setLabel( "MyLabel2" );
        label2.setColor( Color.blue );
        labelDAO.insert( label2 );
        try
        {
            label2.setLabel( "MyLabel" );
            labelDAO.update( label2 );
            Assert.fail( "Expected InvalidLabelException" );
        }
        catch ( InvalidLabelException expected )
        {
            String expectedMessage = "A label with the same name already exists";
            if ( !expectedMessage.equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

    }

    private void assertLabel( String applicationId, String label, Color color, Label actual )
    {
        Assert.assertEquals( applicationId, actual.getOwnerId() );
        Assert.assertEquals( label, actual.getLabel() );
        Assert.assertEquals( label.toLowerCase( Locale.ENGLISH ), actual.getLabelLowercase() );
        Assert.assertEquals( color, actual.getColor() );
    }
}
