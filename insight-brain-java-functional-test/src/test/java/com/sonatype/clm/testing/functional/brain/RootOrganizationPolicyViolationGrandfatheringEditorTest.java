/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.After;
import org.junit.Before;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

public class RootOrganizationPolicyViolationGrandfatheringEditorTest
    extends AbstractPolicyViolationGrandfatheringEditorTest
{
  private Organization organization;

  @Before
  public void init() {
    organization = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
    super.init(organization);
  }

  // Must manually reset grandfathering props since root organization is not
  // part of the organization collection in TemporaryEntity.
  @After
  public void resetRootOrganizationPolicyViolationGrandfathering() {
    OrganizationDAO orgDAO = new OrganizationDAO();
    Organization rootOrg = orgDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);
    rootOrg.setPolicyViolationGrandfatheringEnabled(null);
    rootOrg.setAllowPolicyViolationGrandfatheringOverride(true);
    orgDAO.update(rootOrg);
  }
}
