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

import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates colour references for tags. See CLM-5299
 * 
 * @since 1.20
 */
@Named
public class TagColorMigrator
{
  private static final Logger log = LoggerFactory.getLogger(TagColorMigrator.class);

  static final String MARKER_FILE_NAME = "tagcolors-migrated";

  private final InsightWork insightWork;

  private final TagDAO tagDAO;


  @Inject
  public TagColorMigrator(InsightWork insightWork, TagDAO tagDAO) {
    this.insightWork = insightWork;
    this.tagDAO = tagDAO;
  }

  public void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Migrating tag colors data...");

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
      log.debug("Tag colors already migrated.");
      return;
    }

    int tagCount = 0;
    try (TransactionContext tx = tagDAO.createTransactionContext()) {
      tx.begin();

      for (Tag tag : tagDAO.getAll(tx)) {
        Color newColor = Color.getUpdatedColor(tag.getColor());

        if (newColor != tag.getColor()) {
          tagCount++;
          tag.setColor(newColor);
          tagDAO.update(tx, tag);
        }
      }

      tx.commit();

      markerFile.createNewFile();
    }

    log.info("Migrated {} tag colors in {} ms.", tagCount, System.currentTimeMillis() - start);
  }
}
