/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.Test;

public class RootOrganizationPolicyViolationGrandfatheringEditorTest
    extends AbstractPolicyViolationGrandfatheringEditorTest
{
  @Before
  public void init() {
    Organization rootOrg = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
    super.init(rootOrg);
  }

  @Test
  @Override
  public void testPolicyViolationGrandfatheringConfiguration_Editable() {
    super.testPolicyViolationGrandfatheringConfiguration_Editable();
  }
}
