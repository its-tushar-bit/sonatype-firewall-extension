/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.saas.ScanUploader;
import com.sonatype.insight.brain.scan.ScanTask.State;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Also see {@link ScanTaskStateTest} and {@link ScanStateToTicketTranslatorTest}.
 */
public class ScanTaskTest
{
  private static class StageMatcher
      extends BaseMatcher<Stage>
  {
    private final Stage stage;

    public StageMatcher(Stage stage) {
      this.stage = stage;
    }

    @Override
    public boolean matches(Object item) {
      return item != null && stage.getStageTypeId().equals(((Stage) item).getStageTypeId());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(stage.getStageTypeId());
    }
  }

  private static Stage match(Stage stage) {
    return argThat(new StageMatcher(stage));
  }

  private Application newApp(String publicId) {
    Application app = new Application(publicId, "My App", null);
    app.setId("uuid");
    return app;
  }

  @Test
  public void stateForScheduledTaskIsPending() {
    ScanTask task = new ScanTask(null, null, null, null);
    assertThat("New task state", task.getState(), equalTo(State.PENDING));

    task.init(newApp("any"), new File("any"), new Stage(Stage.ID_BUILD), false);
    assertThat("Initialized task state", task.getState(), equalTo(State.PENDING));
  }

  @Test
  public void savedApplicationBinaryIsScanned() throws IOException {
    Scanner scanner = mock(Scanner.class);
    ScanTask task = new ScanTask(scanner, null, null, null);

    File appBinaryLocation = new File("app-binary-location");
    task.init(newApp("public-app-id"), appBinaryLocation, new Stage(Stage.ID_BUILD), false);

    task.run();

    verify(scanner).scan(appBinaryLocation);
  }

  /**
   * The client will assemble a UI route to the functionality that displays the report.  It needs the public app id and
   * the scan id to make this happen.
   *
   * This is preferred over using the {@link UserInterfaceLinksResource} so that the UI state is not destroyed and
   * browser history is preserved.  UserInterfaceLinksResource are stable links that redirect to the UI for rendering,
   * hence interrupt the app (reloading the page) and browser history.
   */
  @Test
  public void successfulTaskHasTicketWithIdsForUiToRouteToReport() throws IOException {
    Scanner scanner = mock(Scanner.class);
    ScanUploader uploader = mock(ScanUploader.class);
    PolicyEvaluationUtils evaluator = mock(PolicyEvaluationUtils.class);
    PolicyAlertNotifier notifier = mock(PolicyAlertNotifier.class);
    ScanTask task = new ScanTask(scanner, uploader, evaluator, notifier);

    ScanReceipt scanReciept = mock(ScanReceipt.class);
    when(scanReciept.getScanId()).thenReturn("expected-scan-id");

    when(uploader.upload((File) any(), anyString(), anyString())).thenReturn(scanReciept);

    task.init(newApp("expected-public-app-id"), new File("any-file"), new Stage(Stage.ID_BUILD), false);
    task.run();

    ScanTicket ticket = task.getTicket();
    assertThat("Ticket has no error", ticket.error, is(nullValue()));
    assertThat("Ticket has public app id", ticket.applicationPublicId, is("expected-public-app-id"));
    assertThat("Ticket has scan id", ticket.scanId, is("expected-scan-id"));
    assertThat("Final ticket step", ticket.currentStep, is(ticket.totalSteps));
    assertThat("Final ticket step text", ticket.currentStepName, is("Done"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void erorredTaskHasTicketWithErrorMessage() throws IOException {
    Scanner scanner = mock(Scanner.class);
    ScanTask task = new ScanTask(scanner, null, null, null);
    task.init(newApp("any"), new File("any"), new Stage(Stage.ID_BUILD), false);

    when(scanner.scan((File) any())).thenThrow(RuntimeException.class);

    task.run();

    ScanTicket ticket = task.getTicket();
    assertThat("Ticket has error", ticket.error, is(notNullValue()));
    assertThat("Ticket has no scan id", ticket.scanId, is(nullValue()));
    assertThat("Final ticket step", ticket.currentStep, is(ticket.totalSteps));
    assertThat("Final ticket step text", ticket.currentStepName, is("Done"));
  }

  @Test
  public void policyEvaluationConsidersStageParameter() throws IOException {
    Scanner scanner = mock(Scanner.class);
    ScanUploader uploader = mock(ScanUploader.class);
    PolicyEvaluationUtils evaluator = mock(PolicyEvaluationUtils.class);
    PolicyAlertNotifier notifier = mock(PolicyAlertNotifier.class);
    ScanTask task = new ScanTask(scanner, uploader, evaluator, notifier);

    ScanReceipt scanReciept = mock(ScanReceipt.class);
    when(scanReciept.getScanId()).thenReturn("scan-id");
    when(uploader.upload((File) any(), eq("app-id"), anyString())).thenReturn(scanReciept);

    Stage stage = new Stage(Stage.ID_RELEASE);
    task.init(newApp("app-id"), new File("any-file"), stage, false);

    task.run();

    verify(evaluator).evaluate(eq("app-id"), eq("scan-id"), match(stage));
  }

  @Test
  public void sendsNotifications() throws IOException {
    Scanner scanner = mock(Scanner.class);
    ScanUploader uploader = mock(ScanUploader.class);
    PolicyEvaluationUtils evaluator = mock(PolicyEvaluationUtils.class);
    PolicyAlertNotifier notifier = mock(PolicyAlertNotifier.class);
    ScanTask task = new ScanTask(scanner, uploader, evaluator, notifier);

    Application app = newApp("app-id");
    Stage stage = new Stage(Stage.ID_RELEASE);
    task.init(app, new File("any-file"), stage, true);

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scan-id");
    when(uploader.upload((File) any(), eq(app.getPublicId()), anyString())).thenReturn(scanReceipt);

    List<PolicyAlert> oldAlerts = new ArrayList<>();
    List<PolicyAlert> newAlerts = new ArrayList<>();
    when(evaluator.findLastPrimaryPolicyAlerts(eq(app.getPublicId()), eq(app.getId()), match(stage))).thenReturn(
        oldAlerts);

    PolicyEvaluationResult evalResult = new PolicyEvaluationResult();
    evalResult.setAlerts(newAlerts);
    when(evaluator.evaluate(eq(app.getPublicId()), eq(scanReceipt.getScanId()), match(stage))).thenReturn(evalResult);

    task.run();

    verify(notifier).sendNotifications(eq(app.getPublicId()), eq(app.getId()), eq("scan-id"), match(stage),
        same(newAlerts), same(oldAlerts));
  }
}
