/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.brain.model.sast.SastRemediation;
import com.sonatype.insight.brain.model.sast.SastScan;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SastScanDAOTest
    extends AbstractDbDAOTest
{
  private final SastScanDAO sastScanDAO = new SastScanDAO();

  private final SastFindingDAO sastFindingDAO = new SastFindingDAO();

  private final SastRemediationDAO sastRemediationDAO = new SastRemediationDAO();

  @Test
  public void testCRUD() {
    // Insert
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);
    assertThat(sastScan.getId()).isNotNull();

    // Get
    final SastScan result = sastScanDAO.getById(sastScan.getId());
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(sastScan.getId());
    assertThat(result.getApplicationId()).isEqualTo(application.getId());
    assertThat(result.getCreatedAt()).isNotNull();

    // Update not supported
    assertThatThrownBy(() -> sastScanDAO.update(sastScan))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The SastScan table does not support update operations");

    // Cascade delete via DeleteByApplicationId
    sastScanDAO.deleteByApplicationId(sastScan.getApplicationId());
    assertThat(sastScanDAO.getById(sastScan.getId())).isNull();
  }

  @Test
  public void testDeleteByApplicationId_CascadeToSastFindingAndSastRemediation() {
    // Insert
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);
    assertThat(sastScan.getId()).isNotNull();
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();

    final SastFinding sastFinding = new SastFinding();
    sastFinding.setSastScanId(sastScan.getId());
    sastFinding.setCoordinate("someCoordinate");
    sastFinding.setLineNumber(null);
    sastFinding.setCwe("someCWE");
    sastFinding.setSeverity(SastFindingSeverity.MEDIUM);
    sastFinding.setConfidence(SastFindingConfidence.MEDIUM);
    sastFinding.setRuleName("someRuleName");
    sastFinding.setDescription("someDescription");
    sastFindingDAO.insert(sastFinding);
    assertThat(sastFinding.getId()).isNotNull();
    assertThat(sastFindingDAO.getById(sastFinding.getId())).isNotNull();

    final SastRemediation sastRemediation = new SastRemediation(sastFinding.getId(), "someContent");
    sastRemediationDAO.insert(sastRemediation);
    assertThat(sastRemediation.getId()).isNotNull();
    assertThat(sastRemediationDAO.getById(sastRemediation.getId())).isNotNull();

    // Cascade delete via DeleteByApplicationId
    sastScanDAO.deleteByApplicationId(sastScan.getApplicationId());
    assertThat(sastScanDAO.getById(sastScan.getId())).isNull();
    assertThat(sastFindingDAO.getById(sastFinding.getId())).isNull();
    assertThat(sastRemediationDAO.getById(sastRemediation.getId())).isNull();
  }
}
