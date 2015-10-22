/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;

public class OrganizationSummaryViewTest
    extends AbstractSummaryViewTest
{

  private Organization organization;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    super.init(organization);
  }

  @Override
  @Test
  public void testReportLinks() {
    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldBe(empty);
  }
}
