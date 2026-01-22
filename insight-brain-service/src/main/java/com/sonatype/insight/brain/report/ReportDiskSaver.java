/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TVFS;
import io.dropwizard.servlets.tasks.Task;
import org.apache.commons.io.FileUtils;
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes legacy report artifacts from all report.zip files on disk.
 * <p>
 * To trigger it using curl:<br/>
 * {@code curl -X POST http://localhost:8071/tasks/reduceReportZipSize}
 *
 * @since 1.168.0
 */
@Named
@DisallowConcurrentExecution
public class ReportDiskSaver
    extends Task
{
  public static final String NAME = "ReportDiskSaver";

  private static final List<String> toBeRemoved = ImmutableList.of(
      "appcheck.js",
      "detail.rptdesign",
      "flag_white.png",
      "header-columns-bg.gif",
      "lib.min.css",
      "popularity.png",
      "protovis-tipsy.js",
      "red_arrow.png",
      "report.css",
      "sort-asc.gif",
      "yellow_arrow.png",
      "artifactInfoIcon.png",
      "dirty.gif",
      "glyphicons-halflings.png",
      "history.png",
      "lib.min.js",
      "protovis.min.js",
      "protovis-xpan.js",
      "release-header-bg.png",
      "report.js",
      "sort-desc.gif",
      "collapse.gif",
      "expand.gif",
      "glyphicons-halflings-white.png",
      "insight-slick-grid.merged.min.js",
      "orange_arrow.png",
      "protovis-msie.min.js",
      "release-tooltip.png",
      "slick.grid-2.0.merged.min.js",
      "ui-icons_888888_256x240.png",
      "public/bg-score-critical.png",
      "public/bg-score-moderate.png",
      "public/bg-score-severe.png",
      "public/blue.png",
      "public/coord-unknown.png",
      "public/grey.png",
      "public/orange.png",
      "public/security-icon_16x16.png",
      "public/yellow.png",
      "public/bg-score-ignore.png",
      "public/bg-score-none.png",
      "public/bg-score-unspecified.png",
      "public/coord-component.png",
      "public/glypyicons-halfligns-icon-info-sign.png",
      "public/license-icon_16x16.png",
      "public/red.png",
      "public/sonatype.png"
  );

  private static final Logger log = LoggerFactory.getLogger(ReportDiskSaver.class);

  private static final int errorThreshold = 100;

  private final InsightWork insightWork;

  private final InsightConfig insightConfig;

  @Inject
  public ReportDiskSaver(InsightWork insightWork, InsightConfig insightConfig) {
    super("reduceReportZipSize");
    this.insightWork = insightWork;
    this.insightConfig = insightConfig;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    if (insightConfig.getStorage() != null &&
        insightConfig.getStorage().getType() == DataStoreType.S3) {
      throw new UnsupportedOperationException(
          "Report zip minification is only needed for legacy reports using local file storage.");
    }
    log.debug("Starting report zips minifying");
    minifyReports();
  }

  private void minifyReports() {
    minifyReports(insightWork.getReportDir());
  }

  @VisibleForTesting
  void minifyReports(File reportDir) {
    log.info("Minifying report zips");
    AtomicInteger errorCount = new AtomicInteger();
    AtomicInteger processed = new AtomicInteger();
    AtomicLong spaceSaved = new AtomicLong();

    try (Stream<Path> stream = Files.walk(reportDir.toPath())) {
      if (stream != null) {
        stream
            .filter(path -> path.toFile().getName().equals("report.zip"))
            .forEach(path -> {
              long saved = spaceSaved.addAndGet(minifyReport(path.toFile(), errorCount));
              int count = processed.incrementAndGet();
              if (count % 1000 == 0) {
                log.debug("Minified {} report zips so far; space saved: {} MB", count, saved / 1_000_000);
              }
            });
      }
    }
    catch (IOException e) {
      log.error("Cannot traverse: {}", reportDir);
    }
    finally {
      log.info("Minified {} report zips; total space saved: {} MB; error count: {}",
          processed.get(), spaceSaved.get() / 1_000_000, errorCount.get());
    }
  }

  private long minifyReport(File file, final AtomicInteger errorCount) {
    // create working copy
    File tmpZip = new File(file.getParentFile(), "tmp.zip");
    try {
      FileUtils.copyFile(file, tmpZip);
    }
    catch (IOException e) {
      if (errorCount.incrementAndGet() < errorThreshold) {
        log.debug("Cannot copy {} due to: {}", file, e.getMessage());
      }
      return 0;
    }

    // remove files from the working copy
    try {
      for (String zipEntry : toBeRemoved) {
        TFile entry = new TFile(tmpZip, zipEntry);
        if (entry.exists()) {
          entry.rm();
        }
      }
      TVFS.umount();
    }
    catch (IOException e) {
      if (errorCount.incrementAndGet() < errorThreshold) {
        log.debug("Error processing {} due to: {}", file, e.getMessage());
      }
      return 0;
    }

    long spaceSaved = file.length() - tmpZip.length();

    // replace the original file with the updated working copy
    try {
      FileUtils.forceDelete(file);
      FileUtils.moveFile(tmpZip, file);
    }
    catch (IOException e) {
      if (errorCount.incrementAndGet() < errorThreshold) {
        log.debug("Cannot replace {} due to: {}", file, e.getMessage());
      }
      return 0;
    }
    return spaceSaved;
  }
}
