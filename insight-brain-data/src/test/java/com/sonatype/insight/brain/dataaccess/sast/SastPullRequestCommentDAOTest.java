/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.sast;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.sast.SastPullRequestComment;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SastPullRequestCommentDAOTest
    extends AbstractDbDAOTest
{
  private SastPullRequestCommentDAO sastPullRequestCommentDAO;

  private SastScanDAO sastScanDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    sastPullRequestCommentDAO = daoFactory.createSastPullRequestCommentDAO();
    sastScanDAO = daoFactory.createSastScanDAO();
  }

  @Test
  public void testCRUD() {
    final SastPullRequestComment sastPullRequestComment = getSastPullRequestComment();

    // Insert
    sastPullRequestCommentDAO.insert(sastPullRequestComment);
    String sastPullRequestCommentId = sastPullRequestComment.getId();
    assertThat(sastPullRequestCommentId).isNotNull();

    // Get
    final SastPullRequestComment result = sastPullRequestCommentDAO.getById(sastPullRequestCommentId);
    assertThat(result)
        .isNotNull()
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(sastPullRequestComment);

    // Update
    Date newLastUpdatedAt = new Date();
    sastPullRequestComment.setContentHash("new-content");
    sastPullRequestComment.setLastUpdatedAt(newLastUpdatedAt);

    sastPullRequestCommentDAO.update(sastPullRequestComment);

    final SastPullRequestComment result2 = sastPullRequestCommentDAO.getById(sastPullRequestCommentId);
    assertThat(result2).isNotNull();
    assertThat(result2.getContentHash()).isEqualTo("new-content");
    assertThat(result2.getLastUpdatedAt()).isEqualTo(newLastUpdatedAt);

    // Delete
    sastPullRequestCommentDAO.delete(sastPullRequestComment);
    assertThat(sastPullRequestCommentDAO.getById(sastPullRequestCommentId)).isNull();
  }

  @Test
  public void testGetByPullRequestUrl_DoesNotExist() {
    assertThat(sastPullRequestCommentDAO.getByPullRequestUrl("void")).isNull();
  }

  @Test
  public void testGetByPullRequestUrl_Exist() {
    final SastPullRequestComment sastPullRequestComment = getSastPullRequestComment();
    sastPullRequestCommentDAO.insert(sastPullRequestComment);
    assertThat(sastPullRequestCommentDAO.getByPullRequestUrl(sastPullRequestComment.getPullRequestUrl())).isNotNull();
  }

  @Test
  public void testGetBySastScanId_DoesNotExist() {
    try (TransactionContext tx = sastPullRequestCommentDAO.createTransactionContext()) {
      SastPullRequestComment byPullRequestUrl =
          sastPullRequestCommentDAO.getBySastScanId(tx, "void");
      assertThat(byPullRequestUrl).isNull();
    }
  }

  @Test
  public void testGetBySastScanId_Exist() {
    final SastPullRequestComment sastPullRequestComment = getSastPullRequestComment();

    sastPullRequestCommentDAO.insert(sastPullRequestComment);
    try (TransactionContext tx = sastPullRequestCommentDAO.createTransactionContext()) {
      SastPullRequestComment getBySastScanId =
          sastPullRequestCommentDAO.getBySastScanId(tx, sastPullRequestComment.getSastScanId());
      assertThat(getBySastScanId).isNotNull();
    }
  }

  private SastPullRequestComment getSastPullRequestComment() {
    final SastScan sastScan = new SastScan(application.getId());
    sastScanDAO.insert(sastScan);
    assertThat(sastScan.getId()).isNotNull();
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();

    return new SastPullRequestComment(
        sastScan.getId(),
        "https://github.com/sonatype/insight-brain/pull/10894",
        "commit-hash",
        "content-hash",
        "discussion_r1450570374",
        0);
  }
}
