/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.product.license.CLMEnforcementPoint;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ComponentInfoResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testGetSelectableLicenses()
        throws Exception
    {
        String applicationPublicId = "ComponentInfoResourceTest";
        createApplication( applicationPublicId );

        String groupId = "g1";
        String artifactId = "a1";
        String version = "v1";
        ComponentDetails saasComponentDetails = new ComponentDetails( groupId, artifactId, version );

        // Verify that UNSPECIFIED is removed from the result
        saasComponentDetails.setDeclaredLicenses( toLicenseSet( "EPL-1.0", "UNSPECIFIED" ) );
        setSaasResponseForURI( getSaasComponentDetailsUrl( applicationPublicId, groupId, artifactId, version ),
                               JsonHelpers.asJson( saasComponentDetails ), 200 );
        Response response =
            RestAccess.get( getSelectableLicensesServiceURL( applicationPublicId, groupId, artifactId, version ) );
        assertResponseStatus( 200, response );
        License[] licenses = JsonHelpers.fromJson( response.getResponseBody(), License[].class );
        assertEquals( 1, licenses.length );
        assertEquals( "EPL-1.0", licenses[0].getLicenseId() );

        // Verify that a versionless license is resolved to versioned licenses
        saasComponentDetails.setDeclaredLicenses( toLicenseSet( "Apache-UNSPECIFIED" ) );
        setSaasResponseForURI( getSaasComponentDetailsUrl( applicationPublicId, groupId, artifactId, version ),
                               JsonHelpers.asJson( saasComponentDetails ), 200 );
        response =
            RestAccess.get( getSelectableLicensesServiceURL( applicationPublicId, groupId, artifactId, version ) );
        assertResponseStatus( 200, response );
        licenses = JsonHelpers.fromJson( response.getResponseBody(), License[].class );
        assertEquals( Arrays.asList( licenses ).toString(), 4, licenses.length );
        assertContainsLicenseId( "Apache-UNSPECIFIED", licenses );
        assertContainsLicenseId( "Apache-1.0", licenses );
        assertContainsLicenseId( "Apache-1.1", licenses );
        assertContainsLicenseId( "Apache-2.0", licenses );

        // Verify that declared and observed licenses are merged
        saasComponentDetails.setDeclaredLicenses( toLicenseSet( "Apache-2.0", "EPL-1.0" ) );
        saasComponentDetails.setObservedLicenses( toLicenseSet( "EPL-1.0", "GPL-2.0" ) );
        setSaasResponseForURI( getSaasComponentDetailsUrl( applicationPublicId, groupId, artifactId, version ),
                               JsonHelpers.asJson( saasComponentDetails ), 200 );
        response =
            RestAccess.get( getSelectableLicensesServiceURL( applicationPublicId, groupId, artifactId, version ) );
        assertResponseStatus( 200, response );
        licenses = JsonHelpers.fromJson( response.getResponseBody(), License[].class );
        assertEquals( Arrays.asList( licenses ).toString(), 3, licenses.length );
        assertContainsLicenseId( "Apache-2.0", licenses );
        assertContainsLicenseId( "EPL-1.0", licenses );
        assertContainsLicenseId( "GPL-2.0", licenses );
    }
    
    @Test
    public void testGetSelectableLicenses_Unlicensed()
        throws Exception
    {
        uninstallLicense();
        Response response = RestAccess.get( getSelectableLicensesServiceURL( "unlicensedappid", "ulg", "ula", "ulv" ) );
        assertResponseStatus( 402, response );
    }
    
    @Test
    public void testGetSelectableLicenses_EnforcementPointUnlicensed()
        throws Exception
    {
        //note this enforcement point should not apply to this request
        getLicenseManager().setEnforcementPoints( CLMEnforcementPoint.StageRelease );

        Response response = RestAccess.get( getSelectableLicensesServiceURL( "unlicensedappid", "ulg", "ula", "ulv" ) );
        assertResponseStatus( 402, response );
    }

    private void assertContainsLicenseId( String licenseId, License[] licenses )
    {
        for ( License license : licenses )
        {
            if ( licenseId.equals( license.getLicenseId() ) )
            {
                return;
            }
        }
        fail( "Expected license id " + licenseId );
    }

    private Set<License> toLicenseSet( String... licenseIds )
    {
        Set<License> result = new LinkedHashSet<License>();
        MultiLicenseDAO dao = new MultiLicenseDAO();
        for ( String licenseId : licenseIds )
        {
            MultiLicense multiLicense = dao.getByIdNotNull( licenseId );
            result.add( new License( multiLicense.getId(), multiLicense.getShortDisplayName() ) );
        }
        return result;
    }

    private String getSaasComponentDetailsUrl( String applicationPublicId, String g, String a, String v )
    {
        return "/rest/ide/component/details/" + applicationPublicId + "?groupId=" + g + "&artifactId=" + a
            + "&version=" + v;
    }

    private String getSelectableLicensesServiceURL( String applicationPublicId, String g, String a, String v )
    {
        return getServiceURL() + "/selectableLicenses/" + applicationPublicId + "?groupId=" + g + "&artifactId=" + a
            + "&version=" + v;
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ComponentInfoResource.SERVICE_PATH.replace( "{tool : ide|ci}", "ci" );
    }
}
