/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationCleanerTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationCleaner appCleaner;

  @Inject
  private ApplicationDAO appDAO;

  @Inject
  private InsightWork work;

  @Test
  public void testDelete_DeleteIconDirectory() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    File iconDir = new File(work.getApplicationIconDir(), app.getId());
    iconDir.mkdirs();
    new File(iconDir, "icon.png").createNewFile();

    try (TransactionContext tx = appDAO.createTransactionContext()) {
      tx.begin();
      appCleaner.delete(tx, app);
      tx.commit();
    }

    assertThat(iconDir).doesNotExist();
  }
}
