/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseThreatGroupValueTypeTest
    extends AbstractDataTest
{
  private Organization org;

  private Application app;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @BeforeEach
  public void setUp() {
    licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();

    org = tempEntity.newOrganization("orgName");
    app = tempEntity.newApplication("appName", "appId", org.getId());
    tempEntity.newLicenseThreatGroup(app.getId());
    tempEntity.newLicenseThreatGroup(org.getId());
    tempEntity.newLicenseThreatGroup(org.getParentOrganizationId());
  }

  @Test
  public void testGetAvailableValues_AppLevel() {
    LicenseThreatGroupValueType type = new LicenseThreatGroupValueType(app.getId(), licenseThreatGroupDAO);
    List<LicenseThreatGroup> ltgs = type.getAvailableValues();
    assertThat(ltgs).hasSize(4);
    assertThat(ltgs.get(3)).isEqualTo(LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP);
  }

  @Test
  public void testGetAvailableValues_OrgLevel() {
    LicenseThreatGroupValueType type = new LicenseThreatGroupValueType(org.getId(), licenseThreatGroupDAO);
    List<LicenseThreatGroup> ltgs = type.getAvailableValues();
    assertThat(ltgs).hasSize(3);
    assertThat(ltgs.get(2)).isEqualTo(LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP);
  }
}
