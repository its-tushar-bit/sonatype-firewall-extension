/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates colour references for labels. See CLM-5299
 * 
 * @since 1.20
 */
@Named
public class LabelColorMigrator
{
  private static final Logger log = LoggerFactory.getLogger(LabelColorMigrator.class);

  static final String MARKER_FILE_NAME = "labelcolors-migrated";

  private final InsightWork insightWork;

  private LabelDAO labelDAO;

  @Inject
  public LabelColorMigrator(InsightWork insightWork, LabelDAO labelDAO) {
    this.insightWork = insightWork;
    this.labelDAO = labelDAO;
  }

  public void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Migrating label colors data...");

    File workDir = insightWork.getWorkDir();
    log.debug("Work directory {}", workDir.getAbsolutePath());
    File markerFile = new File(workDir, MARKER_FILE_NAME);
    if (!workDir.exists()) {
      // Nothing to migrate
      log.info("Work directory {} does not exist yet.", workDir.getAbsolutePath());
      workDir.mkdirs();
      markerFile.createNewFile();
      return;
    }

    if (markerFile.exists()) {
      log.debug("Label colors already migrated.");
      return;
    }

    int labelCount = 0;
    try (TransactionContext tx = labelDAO.createTransactionContext()) {
      tx.begin();

      for (Label label : labelDAO.getAll(tx)) {
        Color newColor = Color.getUpdatedColor(label.getColor());

        if (newColor != label.getColor()) {
          labelCount++;
          label.setColor(newColor);
          labelDAO.update(tx, label);
        }
      }

      tx.commit();

      markerFile.createNewFile();
    }

    log.info("Migrated {} label colors in {} ms.", labelCount, System.currentTimeMillis() - start);
  }
}
