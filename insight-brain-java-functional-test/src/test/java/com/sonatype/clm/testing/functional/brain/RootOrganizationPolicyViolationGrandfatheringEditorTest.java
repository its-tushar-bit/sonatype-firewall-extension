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

public class RootOrganizationPolicyViolationGrandfatheringEditorTest
    extends AbstractPolicyViolationGrandfatheringEditorTest
{
  private Boolean grandfatheringEnabled;

  private boolean grandfatheringOverrideEnabled;

  @Before
  public void init() {
    Organization rootOrg = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);

    // Save the root org grandfathering settings so we can restore them after the tests.
    grandfatheringEnabled = rootOrg.isPolicyViolationGrandfatheringEnabled();
    grandfatheringOverrideEnabled = rootOrg.isAllowPolicyViolationGrandfatheringOverride();

    super.init(rootOrg);
  }

  @After
  public void restoreRootOrganizationPolicyViolationGrandfatheringSettings() {
    OrganizationDAO orgDAO = new OrganizationDAO();
    Organization rootOrg = orgDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setPolicyViolationGrandfatheringEnabled(grandfatheringEnabled);
    rootOrg.setAllowPolicyViolationGrandfatheringOverride(grandfatheringOverrideEnabled);
    orgDAO.update(rootOrg);
  }
}
