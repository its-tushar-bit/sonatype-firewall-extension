/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.List;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastRemediation;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SastFindingDAOTest
    extends AbstractDbDAOTest
{
  private SastScanDAO sastScanDAO;

  private SastFindingDAO sastFindingDAO;

  private SastRemediationDAO sastRemediationDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    sastScanDAO = daoFactory.createSastScanDAO();
    sastFindingDAO = daoFactory.createSastFindingDAO();
    sastRemediationDAO = daoFactory.createSastRemediationDAO();
  }

  @Test
  public void testInsert_invalidSeverityValue() {
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);
    IntStream.of(-1, SastFindingSeverity.getAll().size())
        .boxed()
        .map(severityId -> {
          final SastFinding sastFinding = new SastFinding();
          sastFinding.setSastScanId(sastScan.getId());
          sastFinding.setCwe("cwe");
          sastFinding.setConfidence(0);
          sastFinding.setSeverityId(severityId);
          sastFinding.setDescription("someDescription");
          sastFinding.setCoordinate("someCoordinate");
          sastFinding.setLineNumber(null);
          sastFinding.setRuleName("someRuleName");
          return sastFinding;
        })
        .forEach(sastFinding -> {
          assertThatThrownBy(() -> sastFindingDAO.insert(sastFinding))
              .isInstanceOf(BadRequestException.class)
              .hasMessage("Invalid id for SastFindingSeverity: " + sastFinding.getSeverityId());
        });
  }

  @Test
  public void testInsert_invalidConfidenceValue() {
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);
    IntStream.of(-1, SastFindingConfidence.values().length)
        .boxed()
        .map(confidence -> {
          final SastFinding sastFinding = new SastFinding();
          sastFinding.setSastScanId(sastScan.getId());
          sastFinding.setCwe("cwe");
          sastFinding.setConfidence(confidence);
          sastFinding.setSeverityId(0);
          sastFinding.setDescription("someDescription");
          sastFinding.setCoordinate("someCoordinate");
          sastFinding.setLineNumber(null);
          sastFinding.setRuleName("someRuleName");
          return sastFinding;
        })
        .forEach(sastFinding -> {
          assertThatThrownBy(() -> sastFindingDAO.insert(sastFinding))
              .isInstanceOf(BadRequestException.class)
              .hasMessage("The ordinal value '" + sastFinding.getConfidence()
                  + "' is outside the range [0, 3) for 'SastFindingConfidence'");
        });
  }

  @Test
  public void testCRUD() {
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);

    final SastFinding sastFinding = new SastFinding();
    sastFinding.setSastScanId(sastScan.getId());
    sastFinding.setCwe("cwe");
    sastFinding.setConfidence(SastFindingConfidence.MEDIUM);
    sastFinding.setSeverity(SastFindingSeverity.MEDIUM);
    sastFinding.setDescription("someDescription");
    sastFinding.setCoordinate("someCoordinate");
    sastFinding.setLineNumber(null);
    sastFinding.setRuleName("someRuleName");

    // Insert
    sastFindingDAO.insert(sastFinding);
    assertThat(sastFinding.getId()).isNotNull();

    // Create SastRemediation to test cascade delete
    final SastRemediation sastRemediation = new SastRemediation(sastFinding.getId(), "someContent");
    sastRemediationDAO.insert(sastRemediation);
    assertThat(sastRemediation.getId()).isNotNull();
    assertThat(sastRemediationDAO.getById(sastRemediation.getId())).isNotNull();

    // GetAll
    final List<SastFinding> results = sastFindingDAO.getAll();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getId()).isEqualTo(sastFinding.getId());

    // GetBySastScanId
    final List<SastFinding> results2 = sastFindingDAO.getBySastScanIdOrderBySeverityDesc(sastScan.getId());
    assertThat(results2).hasSize(1);
    assertThat(results2.get(0).getId()).isEqualTo(sastFinding.getId());

    // Get
    final SastFinding result = sastFindingDAO.getById(sastFinding.getId());
    assertThat(result).isNotNull();

    // Update not supported
    assertThatThrownBy(() -> sastFindingDAO.update(result))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The SastFinding table does not support update operations");

    // Cascade delete via DeleteBysastScanId
    sastFindingDAO.deleteBySastScanId(sastScan.getId());
    assertThat(sastRemediationDAO.getById(sastRemediation.getId())).isNull();
    assertThat(sastFindingDAO.getById(result.getId())).isNull();
  }

  @Test
  public void testDelete_CascadeToSastRemediation() {
    // Create SastScan
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);
    assertThat(sastScan.getId()).isNotNull();
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();

    // Create SastFinding to test cascade delete
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

    // Create SastRemediation to test cascade delete
    final SastRemediation sastRemediation = new SastRemediation(sastFinding.getId(), "someContent");
    sastRemediationDAO.insert(sastRemediation);
    assertThat(sastRemediation.getId()).isNotNull();
    assertThat(sastRemediationDAO.getById(sastRemediation.getId())).isNotNull();

    sastFindingDAO.deleteBySastScanId(sastScan.getId());
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();
    assertThat(sastFindingDAO.getById(sastFinding.getId())).isNull();
    assertThat(sastRemediationDAO.getById(sastRemediation.getId())).isNull();
  }

  @Test
  public void testGetBySastScanIdOrderBySeverityDesc_FindingsDescendBySeverity() {
    // Given a SastScan
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);

    // And 2 sastFindings with different severities
    final SastFinding sastFinding1 = new SastFinding();
    sastFinding1.setSastScanId(sastScan.getId());
    sastFinding1.setCwe("cwe1");
    sastFinding1.setConfidence(SastFindingConfidence.MEDIUM);
    sastFinding1.setSeverity(SastFindingSeverity.MEDIUM);
    sastFinding1.setDescription("someDescription1");
    sastFinding1.setCoordinate("someCoordinate1");
    sastFinding1.setLineNumber(null);
    sastFinding1.setRuleName("someRuleName1");

    final SastFinding sastFinding2 = new SastFinding();
    sastFinding2.setSastScanId(sastScan.getId());
    sastFinding2.setCwe("cwe2");
    sastFinding2.setConfidence(SastFindingConfidence.LOW);
    sastFinding2.setSeverity(SastFindingSeverity.HIGH);
    sastFinding2.setDescription("someDescription2");
    sastFinding2.setCoordinate("someCoordinate2");
    sastFinding2.setLineNumber(null);
    sastFinding2.setRuleName("someRuleName2");

    sastFindingDAO.insert(sastFinding1);
    sastFindingDAO.insert(sastFinding2);

    // When getBySastScanIdOrderBySeverityDesc is called
    final List<SastFinding> results = sastFindingDAO.getBySastScanIdOrderBySeverityDesc(sastScan.getId());

    // Then the findings should be sorted by descending severity
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getId()).isEqualTo(sastFinding2.getId());
    assertThat(results.get(0).getSeverityId()).isEqualTo(sastFinding2.getSeverityId());
    assertThat(results.get(1).getId()).isEqualTo(sastFinding1.getId());
    assertThat(results.get(1).getSeverityId()).isEqualTo(sastFinding1.getSeverityId());
  }
}
