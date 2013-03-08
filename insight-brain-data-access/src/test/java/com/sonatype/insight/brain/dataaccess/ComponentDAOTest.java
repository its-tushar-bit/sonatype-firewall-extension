/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sonatype.clm.dto.model.MatchedComponent;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

public class ComponentDAOTest
    extends AbstractDbDAOTest
{

    private static final String COMP_HASH = "12345678901234567890";

    private ComponentDAO componentDAO = new ComponentDAO();

    private LabelDAO labelDAO = new LabelDAO();

    private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

    private com.sonatype.insight.brain.model.component.SecurityVulnerability newSV( String refId, String source,
                                                                                    Float severity,
                                                                                    SecurityVulnerabilityStatus status )
    {
        com.sonatype.insight.brain.model.component.SecurityVulnerability sv =
            new com.sonatype.insight.brain.model.component.SecurityVulnerability( source, refId, severity );
        sv.setStatus( status );
        return sv;
    }

    private void assertSecurityVulnerabilities( List<com.sonatype.insight.brain.model.component.SecurityVulnerability> actual,
                                                com.sonatype.insight.brain.model.component.SecurityVulnerability... expected )
    {
        assertEquals( expected.length, actual.size() );
        for ( int i = 0, n = expected.length; i < n; i++ )
        {
            assertSecurityVulnerability( expected[i], actual.get( i ) );
        }
    }

    private void assertSecurityVulnerability( com.sonatype.insight.brain.model.component.SecurityVulnerability expected,
                                              com.sonatype.insight.brain.model.component.SecurityVulnerability actual )
    {
        assertEquals( expected.getRefId(), actual.getRefId() );
        assertEquals( expected.getSource(), actual.getSource() );
        assertEquals( expected.getSeverity(), actual.getSeverity() );
        assertEquals( expected.getStatus(), actual.getStatus() );
    }

    private void assertLicenseThreatGroups( Set<LicenseThreatGroup> actual, String... expected )
    {
        Set<String> actualNames = new TreeSet<String>();
        for ( LicenseThreatGroup group : actual )
        {
            actualNames.add( group.getName() );
        }
        assertEquals( new TreeSet<String>( Arrays.asList( expected ) ), actualNames );
    }

    @Before
    public void init()
    {
        Label label = new Label( applicationId, "red", null );
        labelDAO.insert( label );
        componentLabelDAO.insert( new ComponentLabel( applicationId, label.getId(), COMP_HASH ) );
    }

    @After
    public void exit()
    {
        List<ComponentLabel> componentLabels = componentLabelDAO.getByApplicationIdAndHash( applicationId, COMP_HASH );
        for ( ComponentLabel componentLabel : componentLabels )
        {
            componentLabelDAO.delete( componentLabel );
        }

        List<Label> labels = labelDAO.getByApplicationId( applicationId );
        for ( Label label : labels )
        {
            labelDAO.delete( label );
        }
    }

    @Test
    public void testGetComponent_ForIDE()
    {
        MatchedComponent info = new MatchedComponent();
        info.setHash( COMP_HASH );
        info.setGroupId( "gid" );
        info.setArtifactId( "aid" );
        info.setVersion( "1.2.3" );
        info.setMatchState( "similar" );
        info.setCatalogDate( System.currentTimeMillis() );
        info.setRelativePopularity( 42 );
        info.addDeclaredLicenseId( "Apache-2.0" );
        info.addObservedLicenseId( "MIT" );
        info.addSecurityThreat( new SecurityVulnerability( "12345", "osvdb", 4f ) );
        Component comp = componentDAO.getComponent( applicationId, info, null, null );
        assertNotNull( comp );
        assertEquals( info.getHash(), comp.getHash() );
        assertEquals( info.getGroupId(), comp.getGroupId() );
        assertEquals( info.getArtifactId(), comp.getArtifactId() );
        assertEquals( info.getVersion(), comp.getVersion() );
        assertEquals( info.getMatchState(), comp.getMatchState().getId() );
        assertEquals( info.getCatalogDate(), comp.getCatalogDate() );
        assertEquals( info.getRelativePopularity(), new Integer( comp.getRelativePopularity() ) );
        assertEquals( info.getDeclaredLicenseIds(), comp.getDeclaredLicenseIds() );
        assertEquals( info.getObservedLicenseIds(), comp.getObservedLicenseIds() );
        assertEquals( Collections.emptySet(), comp.getOverriddenLicenseIds() );
        assertLicenseThreatGroups( comp.getLicenseThreatGroups(), "Liberal" );
        assertSecurityVulnerabilities( comp.getSecurityVulnerabilities(),
                                       newSV( "12345", "osvdb", 4f, SecurityVulnerabilityStatus.OPEN ) );
        assertEquals( 1, comp.getLabelIds().size() );
    }

}
