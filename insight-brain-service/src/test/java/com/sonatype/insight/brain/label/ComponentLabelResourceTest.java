/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ComponentLabelResourceTest
    extends AbstractResourceTest
{
    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

    @Test
    public void testSetComponentLabels()
        throws Exception
    {
        // Create an application
        String appPublicId = "ComponentLabelResourceTest_AppId";
        Application application = new Application();
        application.setPublicId( appPublicId );
        applicationDAO.insert( application );

        String hash = "bababababa";
        Set<String> labels = toLabelSet( "LabelY", "LabelX" );
        Response response =
            RestAccess.put( getServiceURL( appPublicId ).replace( "{hash}", hash ), JsonHelpers.asJson( labels ) );
        assertResponseStatus( 204, response );

        List<ComponentLabel> componentLabels = componentLabelDAO.getByApplicationIdAndHash( application.getId(), hash );
        assertComponentLabels( labels, componentLabels );
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

    private String getServiceURL( final String appId )
    {
        return getRestBaseUrl() + ComponentLabelResource.SERVICE_PATH.replace( "{appId}", appId );
    }
}
