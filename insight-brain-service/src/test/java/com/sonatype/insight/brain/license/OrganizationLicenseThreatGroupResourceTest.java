/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import org.junit.Test;

import com.sonatype.insight.brain.model.Organization;

public class OrganizationLicenseThreatGroupResourceTest
    extends AbstractLicenseThreatGroupResourceTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        Organization organization = createOrganization( "testCRUD-Organization", true /* createLicenseThreatGroups */);
        testCRUD( organization.getId(), organization.getId() );
    }

    @Test
    public void testDelete_OwnerIdMismatch()
        throws Exception
    {
        Organization organization1 =
            createOrganization( "testDeleteOwnerIdMismatch1", false /* createLicenseThreatGroups */);
        Organization organization2 =
            createOrganization( "testDeleteOwnerIdMismatch2", false /* createLicenseThreatGroups */);
        testDelete_OwnerIdMismatch( organization1.getId(), organization1.getId(), organization2.getId(),
                                    organization2.getId() );
    }

    @Override
    protected String getOwnerType()
    {
        return "organization";
    }
}
