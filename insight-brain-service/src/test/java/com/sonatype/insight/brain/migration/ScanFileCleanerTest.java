/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.Permission;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.test.LogOutput;

import org.joda.time.DateTimeConstants;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScanFileCleanerTest
    extends AbstractComponentTest
{
  private static final SecurityManager ORIGINAL_SECURITY_MANAGER = System.getSecurityManager();

  private static final long ONE_HOUR = DateTimeConstants.MILLIS_PER_HOUR;

  @Rule
  public LogOutput logOutput = new LogOutput(ScanFileCleaner.class);

  @Inject
  private ScanFileCleaner scanFileCleaner;

  @Inject
  private InsightWork insightWork;

  @After
  public void resetSecurityManager() {
    System.setSecurityManager(ORIGINAL_SECURITY_MANAGER);
  }

  @Test
  public void testStartServer() {
    scanFileCleaner = spy(scanFileCleaner);
    ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);
    when(scanFileCleaner.newExecutor()).thenReturn(mockExecutor);
    scanFileCleaner.start();
    verify(mockExecutor).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testStopServer() {
    scanFileCleaner = spy(scanFileCleaner);
    ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);
    when(scanFileCleaner.newExecutor()).thenReturn(mockExecutor);
    scanFileCleaner.start();
    scanFileCleaner.stop();
    verify(mockExecutor).shutdown();
  }

  @Test
  public void testDeleteScanFiles_DeletesOnlyFilesOlderThanOneHour() throws Exception {
    assertThat(scanFileCleaner.getMarkerFile()).doesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path scanDir = insightWork.getScanDir(app.getId()).toPath();
    Files.createDirectories(scanDir);
    Path oldScanFile = Files.createFile(scanDir.resolve("old-file"));
    Files.setLastModifiedTime(oldScanFile, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));
    Path newScanFile = Files.createFile(scanDir.resolve("new-file"));
    Files.setLastModifiedTime(newScanFile, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR + 5000));
    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(scanDir)).containsExactly(newScanFile);

    assertThat(scanFileCleaner.getMarkerFile()).isRegularFile();
  }

  @Test
  public void testDeleteScanFiles_DoesNotDeleteScanFilesForLatestPolicyEvaluations() throws Exception {
    assertThat(scanFileCleaner.getMarkerFile()).doesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path scanDir = insightWork.getScanDir(app.getId()).toPath();
    Files.createDirectories(scanDir);

    // Create two policy evaluations for two scans older than one hour.
    // Only the first scan should be deleted.
    String oldScanId = "oldScanId";
    long oldScanTimestamp = System.currentTimeMillis() - 2 * ONE_HOUR;
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), oldScanId, new Date(oldScanTimestamp));
    Path oldScanFile = insightWork.getScanFile(app.getId(), oldScanId).toPath();
    Files.createFile(oldScanFile);
    Files.setLastModifiedTime(oldScanFile, FileTime.fromMillis(oldScanTimestamp));
    String newScanId = "newScanId";
    long newScanTimestamp = System.currentTimeMillis() - ONE_HOUR - 1;
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), newScanId, new Date(newScanTimestamp));
    Path newScanFile = insightWork.getScanFile(app.getId(), newScanId).toPath();
    Files.createFile(newScanFile);
    Files.setLastModifiedTime(newScanFile, FileTime.fromMillis(newScanTimestamp));

    assertThat(Files.list(scanDir)).containsExactlyInAnyOrder(oldScanFile, newScanFile);

    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(scanDir)).containsExactly(newScanFile);

    assertThat(scanFileCleaner.getMarkerFile()).isRegularFile();
  }

  @Test
  public void testDeleteScanFiles_LogsWarningIfItCannotDeleteFile() throws Exception {
    assertThat(scanFileCleaner.getMarkerFile()).doesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path scanDir = insightWork.getScanDir(app.getId()).toPath();
    Files.createDirectories(scanDir);

    // Create two scan files older than one hour and configure a security manager that doesn't allow the deletion of one
    // of the files.
    Path oldScanFile1 = Files.createFile(scanDir.resolve("old-file1"));
    Files.setLastModifiedTime(oldScanFile1, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));
    Path oldScanFile2 = Files.createFile(scanDir.resolve("old-file2"));
    Files.setLastModifiedTime(oldScanFile2, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));

    System.setSecurityManager(new SecurityManager()
    {
      @Override
      public void checkDelete(String file) {
        if (file.contains("old-file1")) {
          throw new SecurityException("Test exception");
        }
      }

      @Override
      public void checkPermission(Permission perm) {
      }

      @Override
      public void checkPermission(Permission perm, Object context) {
      }
    });

    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(scanDir)).containsExactly(oldScanFile1);
    assertThat(logOutput).atWarnLevel()
        .contains("Error deleting scan file '" + oldScanFile1.toAbsolutePath() + "'. Error: Test exception");

    assertThat(scanFileCleaner.getMarkerFile()).isRegularFile();
  }

  @Test
  public void testDeleteScanFiles_LogsWarningIfItCannotAccessFile() throws Exception {
    assertThat(scanFileCleaner.getMarkerFile()).doesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path scanDir = insightWork.getScanDir(app.getId()).toPath();
    Files.createDirectories(scanDir);

    // Create two scan files older than one hour and configure a security manager that doesn't allow access to one
    // of the files.
    Path oldScanFile1 = Files.createFile(scanDir.resolve("old-file1"));
    Files.setLastModifiedTime(oldScanFile1, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));
    Path oldScanFile2 = Files.createFile(scanDir.resolve("old-file2"));
    Files.setLastModifiedTime(oldScanFile2, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));

    System.setSecurityManager(new SecurityManager()
    {
      @Override
      public void checkRead(String file) {
        if (file.contains("old-file1")) {
          throw new SecurityException("Test exception");
        }
      }

      @Override
      public void checkPermission(Permission perm) {
      }

      @Override
      public void checkPermission(Permission perm, Object context) {
      }
    });

    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(scanDir)).containsExactly(oldScanFile1);
    assertThat(logOutput).atWarnLevel().contains("Error accessing the last modified timestamp for scan file '"
        + oldScanFile1.toAbsolutePath() + "'. Error: Test exception");

    assertThat(scanFileCleaner.getMarkerFile()).isRegularFile();
  }

  @Test
  public void testDeleteScanFiles_ScanDirectoryDoesNotExist() throws Exception {
    assertThat(scanFileCleaner.getMarkerFile()).doesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    assertThat(insightWork.getScanDir(app.getId())).doesNotExist();

    scanFileCleaner.deleteScanFiles();

    assertThat(scanFileCleaner.getMarkerFile()).isRegularFile();
  }
}
