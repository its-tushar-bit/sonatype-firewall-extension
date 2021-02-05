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
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluationTriggerType;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanTask.State;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanService;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
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
{
  private Scanner scanner = mock(Scanner.class);

  private ScanUploader uploader = mock(ScanUploader.class);

  private ScanPolicyEvaluator scanPolicyEvaluator = mock(ScanPolicyEvaluator.class);

  private PolicyAlertNotifier notifier = mock(PolicyAlertNotifier.class);

  private InsightWork work = mock(InsightWork.class);

  FileCleaner fileCleaner = mock(FileCleaner.class);

  private ProprietaryConfigService proprietaryConfigService = mock(ProprietaryConfigService.class);

  private ThirdPartyScanService thirdPartyScanService = mock(ThirdPartyScanService.class);

  private ScanTask task =
      new ScanTask(scanner, uploader, scanPolicyEvaluator, notifier, work, fileCleaner, proprietaryConfigService,
           thirdPartyScanService, new PersistedScanTicketDAO());

  private Application app = newApp("public-app-id");

  private Stage stage = new Stage(Stage.ID_BUILD);

  private ScanReceipt scanReceipt = new ScanReceipt();

  private File bundleFile;

  private String bundleFilename;

  private File scanDir;

  private File scanFile;

  private File tmpScanFile;

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Before
  public void init() throws Exception {
    scanReceipt.setScanId("scan-id");
    bundleFile = tmpDir.newFile("app.zip");
    bundleFilename = "test-app.zip";
    scanDir = tmpDir.newFolder(app.getId());
    scanFile = new File(scanDir, "scan-" + scanReceipt.getScanId() + ".xml.gz");
    tmpScanFile = new File(scanDir, "temp.xml.gz");
    tmpScanFile.createNewFile();
    when(work.getScanDir(eq(app.getId()))).thenReturn(scanDir);
    when(work.getScanFile(eq(app.getId()), eq(scanReceipt.getScanId()))).thenReturn(scanFile);

    when(uploader.upload(eq(tmpScanFile), eq(app), anyString(), eq(null))).thenReturn(scanReceipt);
    ScanResult scanResult = new ScanResult(tmpScanFile, false);
    when(scanner.scan(eq(bundleFile), eq(bundleFilename), eq(scanDir), eq(null))).thenReturn(scanResult);
    when(thirdPartyScanService.filterAndUpload(any(File.class), eq(app), any(String.class), eq(null),
        any(TelemetryData.class))) //
        .thenReturn(scanReceipt);
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

    task.init(newApp("any"), new File("any"), "any", new Stage(Stage.ID_BUILD), false, null, null);
    assertThat(task.getState()).as("Initialized task state").isEqualTo(State.PENDING);
  }

  @Test
  public void savedApplicationBinaryIsScanned() throws IOException {
    task.init(app, bundleFile, bundleFilename, stage, false, null, null);

    assertThat(tmpScanFile).isFile();
    assertThat(scanFile).doesNotExist();
    task.run();

    verify(scanner).scan(eq(bundleFile), eq(bundleFilename), eq(scanDir), eq(null));
    assertThat(tmpScanFile).doesNotExist();
    assertThat(scanFile).isFile();
  }

  /**
   * The client will assemble a UI route to the functionality that displays the report. It needs the public app id and
   * the scan id to make this happen.
   * 
   * This is preferred over using the {@link UserInterfaceLinksResource} so that the UI state is not destroyed and
   * browser history is preserved. UserInterfaceLinksResource are stable links that redirect to the UI for rendering,
   * hence interrupt the app (reloading the page) and browser history.
   */
  @Test
  public void successfulTaskHasIdsForUiToRouteToReport() {
    task.init(app, bundleFile, bundleFilename, stage, false, null, null);
    task.run();

    assertThatTaskCompletedSuccessfully(task);

    assertThat(task.getError()).isNull();
    assertThat(task.getErrorId()).isNull();
    assertThat(task.getScanId()).isEqualTo(scanReceipt.getScanId());
    assertThat(task.getState()).isEqualTo(State.DONE);
  }

  @Test
  public void erorredTaskHasErrorMessage() throws IOException {
    task.init(app, bundleFile, bundleFilename, stage, false, null, null);

    when(scanner.scan(any(File.class), any(String.class), any(File.class), eq(null)))
        .thenThrow(RuntimeException.class);

    task.init(app, bundleFile, bundleFilename, stage, false, null, null);
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
    task.init(app, bundleFile, bundleFilename, stage, false, null, null);

    task.run();

    verify(scanPolicyEvaluator).evaluate(eq(app), eq(scanReceipt.getScanId()), match(stage),
        eq(PolicyEvaluationTriggerType.WEB_UI));
  }

  @Test
  public void erorredTaskDeletesTemporaryApplicationBinary() throws IOException {
    when(scanner.scan(any(File.class), any(String.class), any(File.class), eq(null)))
        .thenThrow(RuntimeException.class);

    File appBinary = new File("any");
    task.init(app, appBinary, bundleFilename, stage, false, null, null);
    task.run();

    assertThatTaskCompletedUnsuccessfully(task);
    verify(fileCleaner).delete(appBinary);
  }

  @Test
  public void sendsNotifications() throws Exception {
    task.init(app, bundleFile, bundleFilename, stage, true, null, null);

    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = new PolicyEvaluation();
    results.notifiableViolations = new ArrayList<>();
    results.allViolations = new ArrayList<>();
    when(scanPolicyEvaluator.evaluate(eq(app), eq(scanReceipt.getScanId()), match(stage),
        eq(PolicyEvaluationTriggerType.WEB_UI))).thenReturn(results);

    task.run();

    verify(notifier).sendNotifications(eq(app), same(results));
  }

  @Test
  public void testRun_processThirdPartyScanResults() throws Exception {
    File scanBinary = new File("any");
    when(thirdPartyScanService.filterAndUpload(scanBinary, app, stage.getStageTypeId(), null, null))
        .thenReturn(scanReceipt);
    task.init(app, scanBinary, bundleFilename, stage, false, "agent", "ui");
    when(scanner.scan(any(File.class), any(String.class), any(File.class), eq(null)))
        .thenReturn(new ScanResult(scanBinary, true));

    when(uploader.upload(any(File.class), eq(app), anyString(), eq(null))).thenReturn(scanReceipt);
    task.run();
    ArgumentCaptor<TelemetryData> arg = ArgumentCaptor.forClass(TelemetryData.class);
    verify(thirdPartyScanService).filterAndUpload(eq(scanBinary), eq(app), eq(stage.getStageTypeId()), eq(null),
        arg.capture());

    TelemetryData telemetryData = arg.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);
    assertThat(telemetryData.getAttributes())
        .contains(entry("application_id", "public-app-id"), entry("stage_id", "build"), entry("source", "ui"),
            entry("user_agent", "agent"));
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
