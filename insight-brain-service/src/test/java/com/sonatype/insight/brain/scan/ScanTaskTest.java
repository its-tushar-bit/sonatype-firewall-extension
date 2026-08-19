/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanTask.State;
import com.sonatype.insight.brain.scan.datastore.FileScanEntity;
import com.sonatype.insight.brain.scan.datastore.FileScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetryDataObfuscator;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.sonatype.insight.brain.testsupport.TempFolder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Also see {@link ScanTaskStateTest} and {@link ScanStateToTicketTranslatorTest}.
 */
public class ScanTaskTest
    extends AbstractDataTest
{
  private final Scanner scanner = mock(Scanner.class);

  private final ScanUploadService uploadService = mock(ScanUploadService.class);

  private final ScanPolicyEvaluator scanPolicyEvaluator = mock(ScanPolicyEvaluator.class);

  private final PolicyAlertNotifier notifier = mock(PolicyAlertNotifier.class);

  private final InsightWork work = mock(InsightWork.class);

  final FileCleaner fileCleaner = mock(FileCleaner.class);

  private final ProprietaryConfigService proprietaryConfigService = mock(ProprietaryConfigService.class);

  private final ScanPersistenceService scanPersistenceService = new FileScanPersistenceService(work, fileCleaner);

  private final TelemetryUtils telemetryUtils =
      new TelemetryUtils(new TelemetryDataObfuscator(mock(Configuration.class)));

  private ScanTask task;

  private PersistedScanTicketDAO persistedScanTicketDAO;

  private final Application app = newApp("public-app-id");

  private Stage stage = new Stage(Stage.ID_BUILD);

  private final ScanReceipt scanReceipt = new ScanReceipt();

  private File bundleFile;

  private String bundleFilename;

  private File scanFile;

  private FileScanEntity tmpScanEntity;

  @RegisterExtension
  public TempFolder tmpDir = new TempFolder();

  @BeforeEach
  public void init() throws Exception {
    persistedScanTicketDAO = daoFactory.createPersistedScanTicketDAO();
    task = new ScanTask(scanner, scanPolicyEvaluator, notifier, fileCleaner, proprietaryConfigService,
        uploadService, persistedScanTicketDAO, telemetryUtils, scanPersistenceService);

    scanReceipt.setScanId("scan-id");
    bundleFile = tmpDir.newFile("app.zip");
    bundleFilename = "test-app.zip";
    File scanDir = tmpDir.newFolder(app.getId());
    scanFile = new File(scanDir, "scan-" + scanReceipt.getScanId() + ".xml.gz");
    File file = new File(scanDir, "temp.xml.gz");
    file.createNewFile();
    tmpScanEntity = new FileScanEntity(file.toPath());
    when(work.getScanDir(eq(app.getId()))).thenReturn(scanDir);
    when(uploadService.upload(eq(tmpScanEntity), eq(app), anyString(), any(ClientScanType.class), eq(null), any(),
        any(), anyBoolean())).thenReturn(scanReceipt);
    ScanResult scanResult = new ScanResult(tmpScanEntity, false);
    when(scanner.scan(eq(bundleFile), eq(bundleFilename), eq(app.getId()), eq(null))).thenReturn(scanResult);
    when(uploadService.upload(any(ScanEntity.class), any(Application.class), any(), eq(ClientScanType.SONATYPE),
        any(), any(), any(), any(), anyBoolean())).thenReturn(scanReceipt);
  }

  private static Stage match(Stage stage) {
    return argThat(argument -> argument != null && stage.getStageTypeId().equals(argument.getStageTypeId()));
  }

  private Application newApp(String publicId) {
    Application app = new Application(publicId, "My App", null);
    app.setId("app-id");
    return app;
  }

  @Test
  public void stateForScheduledTaskIsPending() {
    assertThat(task.getState()).as("New task state").isEqualTo(State.PENDING);

    task.init(newApp("any"), new File("any"), "any", new Stage(Stage.ID_BUILD), false, null, null, false);
    assertThat(task.getState()).as("Initialized task state").isEqualTo(State.PENDING);
  }

  @Test
  public void savedApplicationBinaryIsScanned() throws IOException {
    task.init(app, bundleFile, bundleFilename, stage, false, null, null, false);
    persistedScanTicketDAO.insert(task.toPersistedScanTicket());

    assertThat(tmpScanEntity.path()).isRegularFile();
    assertThat(scanFile).doesNotExist();
    task.run();

    verify(scanner).scan(eq(bundleFile), eq(bundleFilename), eq(app.getId()), eq(null));
    assertThat(tmpScanEntity.path()).doesNotExist();
    assertThat(scanFile).isFile();
  }

  /**
   * The client will assemble a UI route to the functionality that displays the report. It needs the public app id and
   * the scan id to make this happen.
   * <p>
   * This is preferred over using the {@link UserInterfaceLinksResource} so that the UI state is not destroyed and
   * browser history is preserved. UserInterfaceLinksResource are stable links that redirect to the UI for rendering,
   * hence interrupt the app (reloading the page) and browser history.
   */
  @Test
  public void successfulTaskHasIdsForUiToRouteToReport() {
    task.init(app, bundleFile, bundleFilename, stage, false, null, null, false);
    persistedScanTicketDAO.insert(task.toPersistedScanTicket());
    task.run();

    assertThatTaskCompletedSuccessfully(task);

    assertThat(task.getError()).isNull();
    assertThat(task.getErrorId()).isNull();
    assertThat(task.getScanId()).isEqualTo(scanReceipt.getScanId());
    assertThat(task.getState()).isEqualTo(State.DONE);
  }

  @Test
  public void erorredTaskHasErrorMessage() throws IOException {
    task.init(app, bundleFile, bundleFilename, stage, false, null, null, false);

    when(scanner.scan(any(File.class), any(String.class), any(String.class), eq(null)))
        .thenThrow(RuntimeException.class);

    task.init(app, bundleFile, bundleFilename, stage, false, null, null, false);
    persistedScanTicketDAO.insert(task.toPersistedScanTicket());
    task.run();

    assertThatTaskCompletedUnsuccessfully(task);

    assertThat(task.getError()).isNotNull();
    assertThat(task.getErrorId()).isNotNull();
    assertThat(task.getScanId()).isNull();
    assertThat(task.getState()).isEqualTo(State.DONE);
  }

  @Test
  public void policyEvaluationConsidersStageParameter() throws IOException {
    stage = new Stage(Stage.ID_RELEASE);
    task.init(app, bundleFile, bundleFilename, stage, false, null, null, false);
    persistedScanTicketDAO.insert(task.toPersistedScanTicket());

    task.run();

    verify(scanPolicyEvaluator).evaluate(eq(app), eq(scanReceipt.getScanId()), match(stage),
        eq(ScanTriggerType.WEB_UI), eq(ClientScanType.SONATYPE), eq(false));
  }

  @Test
  public void erorredTaskDeletesTemporaryApplicationBinary() throws IOException {
    when(scanner.scan(any(File.class), any(String.class), any(String.class), eq(null)))
        .thenThrow(RuntimeException.class);

    File appBinary = new File("any");
    task.init(app, appBinary, bundleFilename, stage, false, null, null, false);
    persistedScanTicketDAO.insert(task.toPersistedScanTicket());
    task.run();

    assertThatTaskCompletedUnsuccessfully(task);
    verify(fileCleaner).delete(appBinary);
  }

  @Test
  public void sendsNotifications() throws Exception {
    task.init(app, bundleFile, bundleFilename, stage, true, null, null, false);
    persistedScanTicketDAO.insert(task.toPersistedScanTicket());

    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = new PolicyEvaluation();
    results.notifiableViolations = new ArrayList<>();
    results.allViolations = new ArrayList<>();
    when(scanPolicyEvaluator.evaluate(eq(app), eq(scanReceipt.getScanId()), match(stage),
        eq(ScanTriggerType.WEB_UI), eq(ClientScanType.SONATYPE), eq(false))).thenReturn(results);

    task.run();

    verify(notifier).sendNotifications(eq(app), same(results));
  }

  @Test
  public void testRun_processThirdPartyScanResults() throws Exception {
    File scanBinary = new File("any/path");
    FileScanEntity scanBinaryEntity = new FileScanEntity(scanBinary.toPath());
    ScanReceipt receipt = mock(ScanReceipt.class);
    when(uploadService.upload(scanBinaryEntity, app, stage.getStageTypeId(), ClientScanType.SONATYPE_THIRD_PARTY,
        null, null, null, false)).thenReturn(scanReceipt);

    task.init(app, scanBinary, bundleFilename, stage, false, "agent", "ui", false);
    persistedScanTicketDAO.insert(task.toPersistedScanTicket());
    when(scanner.scan(any(File.class), any(String.class), any(String.class), eq(null)))
        .thenReturn(new ScanResult(scanBinaryEntity, true));

    when(uploadService.upload(any(ScanEntity.class), eq(app), anyString(), any(ClientScanType.class), any(), any(),
        any(), any(), anyBoolean())).thenReturn(receipt);
    task.run();
    ArgumentCaptor<TelemetryData> arg = ArgumentCaptor.forClass(TelemetryData.class);
    verify(uploadService).upload(eq(scanBinaryEntity), eq(app), eq(stage.getStageTypeId()),
        eq(ClientScanType.SONATYPE_THIRD_PARTY), eq("agent"), arg.capture(), eq(null), eq(false));

    TelemetryData telemetryData = arg.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);
    assertThat(telemetryData.getAttributes()).contains(
        entry("application_id", HdsClientAnalytics.obfuscate(app.getId())),
        entry("real_application_id", HdsClientAnalytics.obfuscate(app.getId())),
        entry("stage_id", "build"), entry("source", "ui"), entry("user_agent", "agent"));
  }

  private void assertThatTaskCompletedSuccessfully(ScanTask task) {
    assertThat(task.getState()).isEqualTo(State.DONE);
    assertThat(task.getError()).isNull();
  }

  private void assertThatTaskCompletedUnsuccessfully(ScanTask task) {
    assertThat(task.getState()).isEqualTo(State.DONE);
    assertThat(task.getError()).isNotNull();
  }
}
