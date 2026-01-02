/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LabelValueTypeTest
    extends AbstractDataTest
{
  private Organization org;

  private Application app;

  private LabelDAO labelDAO;

  @Before
  public void setUp() {
    labelDAO = daoFactory.createLabelDAO();
    org = tempEntity.newOrganization("orgName");
    app = tempEntity.newApplication("appName", "appId", org.getId());
    tempEntity.newLabel(app.getId(), "appLabel");
  }

  @Test
  public void testGetAvailableValues_AppLevel() {
    LabelValueType type = new LabelValueType(app.getId(), labelDAO);
    List<Label> labels = type.getAvailableValues();
    assertThat(labels).hasSize(1);
  }

  @Test
  public void testGetAvailableValues_OrgLevel() {
    LabelValueType type = new LabelValueType(org.getId(), labelDAO);
    List<Label> labels = type.getAvailableValues();
    assertThat(labels).isEmpty();
  }
}
