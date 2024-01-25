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
import com.sonatype.insight.brain.model.sast.SastPullRequestComment;
import com.sonatype.insight.brain.model.sast.SastRemediation;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.sast.SastScmScanContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SastScanDAOTest
    extends AbstractDbDAOTest
{
  private SastScanDAO sastScanDAO;

  private SastFindingDAO sastFindingDAO;

  private SastRemediationDAO sastRemediationDAO;

  private SastScmScanContextDAO sastScmScanContextDAO;

  private SastPullRequestCommentDAO sastPullRequestCommentDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    sastScanDAO = daoFactory.createSastScanDAO();
    sastFindingDAO = daoFactory.createSastFindingDAO();
    sastRemediationDAO = daoFactory.createSastRemediationDAO();
    sastScmScanContextDAO = daoFactory.createSastScmScanContextDAO();
    sastPullRequestCommentDAO = daoFactory.createSastPullRequestCommentDAO();
  }

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
  public void testDeleteByApplicationId_CascadeToSastFinding() {
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

    // Cascade delete via DeleteByApplicationId
    sastScanDAO.deleteByApplicationId(sastScan.getApplicationId());
    assertThat(sastScanDAO.getById(sastScan.getId())).isNull();
    assertThat(sastFindingDAO.getById(sastFinding.getId())).isNull();
  }

  @Test
  public void testDeleteByApplicationId_CascadeToSastRemediation() {
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
    assertThat(sastRemediationDAO.getById(sastRemediation.getId())).isNull();
  }

  @Test
  public void testDeleteByApplicationId_CascadeToSastScmScanContext() {
    // Insert
    final SastScmScanContext sastScmScanContext = new SastScmScanContext("testBranch", "testCommitHash");
    sastScmScanContextDAO.insert(sastScmScanContext);
    assertThat(sastScmScanContext.getId()).isNotNull();
    assertThat(sastScmScanContextDAO.getById(sastScmScanContext.getId())).isNotNull();

    final SastScan sastScan = new SastScan(application.getId(), sastScmScanContext.getId());
    sastScanDAO.insert(sastScan);
    assertThat(sastScan.getId()).isNotNull();
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();

    // Cascade delete via DeleteByApplicationId
    sastScanDAO.deleteByApplicationId(sastScan.getApplicationId());
    assertThat(sastScanDAO.getById(sastScan.getId())).isNull();
    assertThat(sastScmScanContextDAO.getById(sastScmScanContext.getId())).isNull();
  }

  @Test
  public void testDeleteByApplicationId_CascadeToSastPullRequestComment() {
    // Insert
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);
    assertThat(sastScan.getId()).isNotNull();
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();

    final SastPullRequestComment sastPullRequestComment = new SastPullRequestComment(
        sastScan.getId(),
        "https://github.com/sonatype/insight-brain/pull/10894",
        "commit-hash",
        "content-hash",
        "discussion_r1450570374"
    );

    sastPullRequestCommentDAO.insert(sastPullRequestComment);
    String sastPullRequestCommentId = sastPullRequestComment.getId();
    assertThat(sastPullRequestCommentId).isNotNull();

    // Cascade delete via DeleteByApplicationId
    sastScanDAO.deleteByApplicationId(sastScan.getApplicationId());
    assertThat(sastScanDAO.getById(sastScan.getId())).isNull();
    assertThat(sastPullRequestCommentDAO.getById(sastPullRequestCommentId)).isNull();
  }
}
