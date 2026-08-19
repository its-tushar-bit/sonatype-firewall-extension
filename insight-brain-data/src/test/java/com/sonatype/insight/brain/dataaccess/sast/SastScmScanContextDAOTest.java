/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.sast;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.sast.SastScmScanContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SastScmScanContextDAOTest
    extends AbstractDbDAOTest
{
  private SastScmScanContextDAO sastScmScanContextDAO;

  private SastScanDAO sastScanDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    sastScmScanContextDAO = daoFactory.createSastScmScanContextDAO();
    sastScanDAO = daoFactory.createSastScanDAO();
  }

  @Test
  public void testCRUD() {
    // Insert
    final SastScmScanContext sastScmScanContext =
        new SastScmScanContext("testBranch", "testCommitHash");
    sastScmScanContextDAO.insert(sastScmScanContext);
    assertThat(sastScmScanContext.getId()).isNotNull();

    // Get
    final SastScmScanContext result = sastScmScanContextDAO.getById(sastScmScanContext.getId());
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(sastScmScanContext.getId());
    assertThat(result.getBranchName()).isEqualTo(sastScmScanContext.getBranchName());
    assertThat(result.getCommitHash()).isEqualTo(sastScmScanContext.getCommitHash());
    assertThat(result.getCreatedAt()).isNotNull();

    // Update not supported
    assertThatThrownBy(() -> sastScmScanContextDAO.update(sastScmScanContext))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The SastScmScanContext table does not support update operations");

    // Delete and set FK in sast_scan to null
    final SastScan sastScan = new SastScan(tempEntity.newApplicationWithParent().getId(), sastScmScanContext.getId());
    sastScanDAO.insert(sastScan);
    assertThat(sastScan.getSastScmScanContextId())
        .isEqualTo(sastScmScanContext.getId());

    sastScmScanContextDAO.delete(sastScmScanContext);
    assertThat(sastScmScanContextDAO.getById(sastScmScanContext.getId())).isNull();
    assertThat(sastScanDAO.getById(sastScan.getId()).getSastScmScanContextId()).isNull();
  }

  @Test
  public void testInsert_InvalidBranchName() {
    SastScmScanContext sastScmScanContext = new SastScmScanContext("/testBranch", "testCommitHash");
    assertThatThrownBy(() -> {
      sastScmScanContextDAO.insert(sastScmScanContext);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }
}
