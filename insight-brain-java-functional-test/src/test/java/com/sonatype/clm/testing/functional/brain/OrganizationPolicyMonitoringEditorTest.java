/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.Test;

public class OrganizationPolicyMonitoringEditorTest
    extends AbstractPolicyMonitoringEditorTest
{
  private Organization organization;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    super.init(organization);
  }

  @Test
  @Override
  public void testEditMonitoredStage() {
    super.testEditMonitoredStage();
  }
}
