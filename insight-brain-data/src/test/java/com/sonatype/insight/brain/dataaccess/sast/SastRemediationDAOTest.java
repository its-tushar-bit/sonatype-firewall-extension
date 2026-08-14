/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastRemediation;
import com.sonatype.insight.brain.model.sast.SastScan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SastRemediationDAOTest
    extends AbstractDbDAOTest
{
  private SastScanDAO sastScanDAO;

  private SastFindingDAO sastFindingDAO;

  private SastRemediationDAO sastRemediationDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    sastScanDAO = daoFactory.createSastScanDAO();
    sastFindingDAO = daoFactory.createSastFindingDAO();
    sastRemediationDAO = daoFactory.createSastRemediationDAO();
  }

  @Test
  public void testCRUD() {
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);
    assertThat(sastScan.getId()).isNotNull();
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();

    final SastFinding sastFinding = new SastFinding();
    sastFinding.setSastScanId(sastScan.getId());
    sastFinding.setCwe("cwe0");
    sastFinding.setConfidence(SastFindingConfidence.HIGH);
    sastFinding.setSeverity(SastFindingSeverity.MEDIUM);
    sastFinding.setDescription("someDescription0");
    sastFinding.setCoordinate("someCoordinate0");
    sastFinding.setLineNumber(323);
    sastFinding.setRuleName("someRuleName0");
    sastFindingDAO.insert(sastFinding);
    assertThat(sastFinding.getId()).isNotNull();
    assertThat(sastFindingDAO.getById(sastFinding.getId())).isNotNull();

    // Insert
    final SastRemediation sastRemediation = new SastRemediation(sastFinding.getId(), "someContent");
    sastRemediationDAO.insert(sastRemediation);
    assertThat(sastRemediation.getId()).isNotNull();
    assertThat(sastRemediationDAO.getById(sastRemediation.getId())).isNotNull();

    // GetAll
    final List<SastRemediation> results = sastRemediationDAO.getAll();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getId()).isEqualTo(sastRemediation.getId());

    // GetBySastFindingId
    final List<SastRemediation> results2 = sastRemediationDAO.getBySastFindingId(sastFinding.getId());
    assertThat(results2).hasSize(1);
    assertThat(results2.get(0).getId()).isEqualTo(sastRemediation.getId());

    // Get
    SastRemediation result = sastRemediationDAO.getById(sastRemediation.getId());
    assertThat(result).isNotNull();

    // Update not supported
    assertThatThrownBy(() -> sastRemediationDAO.update(result))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The SastRemediation table does not support update operations");

    // DeleteBySastFindingId
    sastRemediationDAO.deleteBySastFindingId(sastFinding.getId());
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();
    assertThat(sastFindingDAO.getById(sastFinding.getId())).isNotNull();
    assertThat(sastRemediationDAO.getById(result.getId())).isNull();
  }
}
