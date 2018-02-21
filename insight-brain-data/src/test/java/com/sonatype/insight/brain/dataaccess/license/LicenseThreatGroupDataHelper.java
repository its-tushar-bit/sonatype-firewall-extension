/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.io.IOException;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.json.store.JsonUtils;

public class LicenseThreatGroupDataHelper
{
  public static final int TEST_LICENSE_THREAT_GROUP_COUNT = 6;

  public static void createTestLicenseThreatGroups(TemporaryEntity tempEntity) {
    createTestLicenseThreatGroups(Organization.ROOT_ORGANIZATION_ID, tempEntity);
  }

  public static void createTestLicenseThreatGroups(final String ownerId, TemporaryEntity tempEntity) {
    TestLicenseThreatGroups testLTGs = TestLicenseThreatGroups.load();

    for (LicenseThreatGroup licenseThreatGroup : testLTGs.licenseThreatGroups) {
      tempEntity.newLicenseThreatGroup(licenseThreatGroup.getId(), ownerId, licenseThreatGroup.getName(),
          licenseThreatGroup.getThreatLevel());
    }

    for (LicenseThreatGroupLicense licenseThreatGroupLicense : testLTGs.licenseThreatGroupLicenses) {
      tempEntity.newLicenseThreatGroupLicense(ownerId, licenseThreatGroupLicense.getLicenseThreatGroupId(),
          licenseThreatGroupLicense.getLicenseId());
    }
  }

  private static class TestLicenseThreatGroups
  {
    public List<LicenseThreatGroup> licenseThreatGroups;

    public List<LicenseThreatGroupLicense> licenseThreatGroupLicenses;

    static TestLicenseThreatGroups load() {
      Class<TestLicenseThreatGroups> type = TestLicenseThreatGroups.class;
      try {
        return JsonUtils.parse(type.getResourceAsStream("/TestLicenseThreatGroups.json"), type);
      }
      catch (IOException e) {
        throw new IllegalStateException("Invalid LTG tests", e);
      }
    }
  }
}
