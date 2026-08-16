/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.joda.time.DateTimeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

@ComponentH2Test
public class ScanFileCleanerTest
    extends AbstractComponentH2Test
{
  private static final long ONE_HOUR = DateTimeConstants.MILLIS_PER_HOUR;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  public LogOutput logOutput = new LogOutput(ScanFileCleaner.class);

  @Inject
  private ScanFileCleaner scanFileCleaner;

  @Inject
  private InsightWork insightWork;

  @Inject
  @Named("scanPersistenceService")
  private ScanPersistenceService scanPersistenceService;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @BeforeEach
  public void before() throws Exception {
    migrationTrackerDAO.deleteById(ScanFileCleaner.MARKER_ID);
    Files.deleteIfExists(scanFileCleaner.getObsoleteMarkerFile());
    applyBeanFieldOverride(ScanFileCleaner.class, "taskScheduler", taskSchedulerMock);
  }

  @Test
  public void testStartServer_NoMarker() {
    assertMarkerDoesNotExist();
    scanFileCleaner.register();

    verify(taskSchedulerMock).scheduleOneTimeTask(scanFileCleaner, LocalTime.of(23, 0));
  }

  @Test
  public void testStartServer_MarkerFile() throws Exception {
    assertMarkerDoesNotExist();
    insightWork.getWorkDir().toPath().resolve("obsoletescanfiles-cleaned").toFile().createNewFile();
    assertThat(scanFileCleaner.getObsoleteMarkerFile()).exists();

    scanFileCleaner.register();

    assertMarkerExists();
    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testStartServer_MarkerInDb() throws Exception {
    migrationTrackerDAO.insertTracker(ScanFileCleaner.MARKER_ID);
    assertMarkerExists();

    scanFileCleaner.start();

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testDeleteScanFiles_DeletesOnlyFilesOlderThanOneHour() throws Exception {
    assertMarkerDoesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path scanDir = insightWork.getScanDir(app.getId()).toPath();
    Files.createDirectories(scanDir);
    Path oldScanFile = Files.createFile(scanDir.resolve("old-file"));
    Files.setLastModifiedTime(oldScanFile, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));
    Path newScanFile = Files.createFile(scanDir.resolve("new-file"));
    Files.setLastModifiedTime(newScanFile, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR + 5000));
    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(scanDir)).containsExactly(newScanFile);

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_DoesNotDeleteScanFilesForLatestPolicyEvaluations() throws Exception {
    assertMarkerDoesNotExist();

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

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_HostedRepositoryComponent() throws Exception {
    assertMarkerDoesNotExist();

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    Path scanDir = insightWork.getScanDir(hrc.getId()).toPath();
    Files.createDirectories(scanDir);

    String oldScanId = "hrc-oldScanId";
    long oldScanTimestamp = System.currentTimeMillis() - 2 * ONE_HOUR;
    Path oldScanFile = insightWork.getScanFile(hrc.getId(), oldScanId).toPath();
    Files.createFile(oldScanFile);
    Files.setLastModifiedTime(oldScanFile, FileTime.fromMillis(oldScanTimestamp));

    String newScanId = "hrc-newScanId";
    long newScanTimestamp = System.currentTimeMillis() - ONE_HOUR - 1;
    tempEntity.newPolicyEvaluation(hrc.getId(), StageTypes.BUILD.getId(), newScanId, new Date(newScanTimestamp));
    Path newScanFile = insightWork.getScanFile(hrc.getId(), newScanId).toPath();
    Files.createFile(newScanFile);
    Files.setLastModifiedTime(newScanFile, FileTime.fromMillis(newScanTimestamp));

    assertThat(Files.list(scanDir)).containsExactlyInAnyOrder(oldScanFile, newScanFile);

    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(scanDir)).containsExactly(newScanFile);

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_HostedRepositoryComponents_BoundedEnumeration() throws Exception {
    assertMarkerDoesNotExist();

    applyBeanFieldOverride(ScanFileCleaner.class, "hrcPageSize", 2);

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc1 = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrc2 = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrc3 = tempEntity.newHostedRepositoryComponent(repository);

    for (HostedRepositoryComponent hrc : new HostedRepositoryComponent[]{hrc1, hrc2, hrc3}) {
      Path scanDir = insightWork.getScanDir(hrc.getId()).toPath();
      Files.createDirectories(scanDir);
      Path obsolete = Files.createFile(scanDir.resolve("obsolete"));
      Files.setLastModifiedTime(obsolete, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));
    }

    scanFileCleaner.deleteScanFiles();

    for (HostedRepositoryComponent hrc : new HostedRepositoryComponent[]{hrc1, hrc2, hrc3}) {
      assertThat(Files.list(insightWork.getScanDir(hrc.getId()).toPath())).isEmpty();
    }

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_HostedRepositoryComponents_PageBoundary_EachOwnerVisitedExactlyOnce() throws Exception {
    assertEachHostedRepositoryComponentVisitedExactlyOnce(2);
  }

  @Test
  public void testDeleteScanFiles_HostedRepositoryComponents_ExactFitPage_EachOwnerVisitedExactlyOnce() throws Exception {
    assertEachHostedRepositoryComponentVisitedExactlyOnce(3);
  }

  private void assertEachHostedRepositoryComponentVisitedExactlyOnce(int pageSize) throws Exception {
    applyBeanFieldOverride(ScanFileCleaner.class, "hrcPageSize", pageSize);

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc1 = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrc2 = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrc3 = tempEntity.newHostedRepositoryComponent(repository);

    ScanPersistenceService spyScanPersistenceService = spy(scanPersistenceService);
    applyBeanFieldOverride(ScanFileCleaner.class, "scanPersistenceService", spyScanPersistenceService);

    scanFileCleaner.deleteScanFiles();

    List<String> idsInAscendingOrder = Stream.of(hrc1, hrc2, hrc3)
        .map(HostedRepositoryComponent::getId)
        .sorted()
        .toList();
    InOrder inOrder = inOrder(spyScanPersistenceService);
    for (String hrcId : idsInAscendingOrder) {
      inOrder.verify(spyScanPersistenceService).allScanFilesFor(hrcId);
    }
  }

  @Test
  public void testDeleteScanFiles_ApplicationAndHostedRepositoryComponentInSameRun() throws Exception {
    assertMarkerDoesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path appScanDir = insightWork.getScanDir(app.getId()).toPath();
    Files.createDirectories(appScanDir);
    Path appObsolete = Files.createFile(appScanDir.resolve("app-obsolete"));
    Files.setLastModifiedTime(appObsolete, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    Path hrcScanDir = insightWork.getScanDir(hrc.getId()).toPath();
    Files.createDirectories(hrcScanDir);
    Path hrcObsolete = Files.createFile(hrcScanDir.resolve("hrc-obsolete"));
    Files.setLastModifiedTime(hrcObsolete, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));

    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(appScanDir)).isEmpty();
    assertThat(Files.list(hrcScanDir)).isEmpty();

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_NoHostedRepositoryComponents_AppPathStillPurges() throws Exception {
    assertMarkerDoesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path appScanDir = insightWork.getScanDir(app.getId()).toPath();
    Files.createDirectories(appScanDir);
    Path appObsolete = Files.createFile(appScanDir.resolve("app-obsolete"));
    Files.setLastModifiedTime(appObsolete, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));

    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(appScanDir)).isEmpty();

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_LogsWarningIfItCannotDeleteFile() throws Exception {
    assertMarkerDoesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path scanDir = insightWork.getScanDir(app.getId()).toPath();
    Path oldScanFile1 = scanDir.resolve("old-file1").toAbsolutePath();
    Path oldScanFile2 = scanDir.resolve("old-file2").toAbsolutePath();
    long oldTimestamp = System.currentTimeMillis() - ONE_HOUR - 1;

    ScanEntity oldScanEntity1 = mock(ScanEntity.class);
    when(oldScanEntity1.getLastModifiedTime()).thenReturn(oldTimestamp);
    when(oldScanEntity1.toString()).thenReturn(oldScanFile1.toString());

    ScanEntity oldScanEntity2 = mock(ScanEntity.class);
    when(oldScanEntity2.getLastModifiedTime()).thenReturn(oldTimestamp);
    when(oldScanEntity2.toString()).thenReturn(oldScanFile2.toString());

    ScanPersistenceService mockScanPersistenceService = mock(ScanPersistenceService.class);
    when(mockScanPersistenceService.allScanFilesFor(app.getId())).thenReturn(Stream.of(oldScanEntity1,
        oldScanEntity2));
    doThrow(new SecurityException("Test exception")).when(mockScanPersistenceService).deleteScan(oldScanEntity1);

    applyBeanFieldOverride(ScanFileCleaner.class, "scanPersistenceService", mockScanPersistenceService);

    scanFileCleaner.deleteScanFiles();

    verify(mockScanPersistenceService).deleteScan(oldScanEntity1);
    verify(mockScanPersistenceService).deleteScan(oldScanEntity2);
    assertThat(logOutput).atWarnLevel()
        .contains("Error deleting scan file '" + oldScanFile1
            + "': java.lang.SecurityException: Test exception");

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_LogsWarningIfItCannotAccessFile() throws Exception {
    assertMarkerDoesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    Path scanDir = insightWork.getScanDir(app.getId()).toPath();
    Path oldScanFile1 = scanDir.resolve("old-file1").toAbsolutePath();
    Path oldScanFile2 = scanDir.resolve("old-file2").toAbsolutePath();
    long oldTimestamp = System.currentTimeMillis() - ONE_HOUR - 1;

    ScanEntity oldScanEntity1 = mock(ScanEntity.class);
    when(oldScanEntity1.getLastModifiedTime()).thenThrow(new SecurityException("Test exception"));
    when(oldScanEntity1.toString()).thenReturn(oldScanFile1.toString());

    ScanEntity oldScanEntity2 = mock(ScanEntity.class);
    when(oldScanEntity2.getLastModifiedTime()).thenReturn(oldTimestamp);
    when(oldScanEntity2.toString()).thenReturn(oldScanFile2.toString());

    ScanPersistenceService mockScanPersistenceService = mock(ScanPersistenceService.class);
    when(mockScanPersistenceService.allScanFilesFor(app.getId())).thenReturn(Stream.of(oldScanEntity1,
        oldScanEntity2));

    applyBeanFieldOverride(ScanFileCleaner.class, "scanPersistenceService", mockScanPersistenceService);

    scanFileCleaner.deleteScanFiles();

    verify(mockScanPersistenceService, never()).deleteScan(oldScanEntity1);
    verify(mockScanPersistenceService).deleteScan(oldScanEntity2);
    assertThat(logOutput).atWarnLevel()
        .contains("Error accessing the last modified timestamp for scan file '"
            + oldScanFile1 + "': java.lang.SecurityException: Test exception");

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_ScanDirectoryDoesNotExist() {
    assertMarkerDoesNotExist();

    Application app = tempEntity.newApplicationWithParent();
    assertThat(insightWork.getScanDir(app.getId())).doesNotExist();

    scanFileCleaner.deleteScanFiles();

    assertMarkerExists();
  }

  @Test
  public void testDeleteScanFiles_HostedRepositoryComponent_ScanDirectoryDoesNotExist_OtherHrcStillProcessed() throws Exception {
    assertMarkerDoesNotExist();

    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrcNoDir = tempEntity.newHostedRepositoryComponent(repository);
    HostedRepositoryComponent hrcWithFile = tempEntity.newHostedRepositoryComponent(repository);

    assertThat(insightWork.getScanDir(hrcNoDir.getId())).doesNotExist();

    Path scanDir = insightWork.getScanDir(hrcWithFile.getId()).toPath();
    Files.createDirectories(scanDir);
    Path obsolete = Files.createFile(scanDir.resolve("obsolete"));
    Files.setLastModifiedTime(obsolete, FileTime.fromMillis(System.currentTimeMillis() - ONE_HOUR - 1));

    scanFileCleaner.deleteScanFiles();

    assertThat(Files.list(scanDir)).isEmpty();
    assertMarkerExists();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ScanFileCleaner.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() {
    ScanFileCleaner scanFileCleanerSpy = spy(scanFileCleaner);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(scanFileCleanerSpy).deleteScanFiles();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      scanFileCleanerSpy.execute(mock(JobExecutionContext.class));
    }

    verify(scanFileCleanerSpy).deleteScanFiles();
  }

  private void assertMarkerExists() {
    assertThat(migrationTrackerDAO.isTrackerPresent(ScanFileCleaner.MARKER_ID)).isTrue();
  }

  private void assertMarkerDoesNotExist() {
    assertThat(scanFileCleaner.getObsoleteMarkerFile()).doesNotExist();
    assertThat(migrationTrackerDAO.isTrackerPresent(ScanFileCleaner.MARKER_ID)).isFalse();
  }
}
