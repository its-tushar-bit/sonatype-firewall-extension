/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.saas.ScanUploader;
import com.sonatype.insight.brain.scan.ScanTask.State;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Also see {@link ScanTaskStateTest} and {@link ScanStateToTicketTranslatorTest}.
 */
public class ScanTaskTest
{
  @Test
  public void stateForScheduledTaskIsPending() {
    ScanTask task = new ScanTask(null, null, null);
    assertThat("New task state", task.getState(), equalTo(State.PENDING));

    task.init("any", new File("any"));
    assertThat("Initialized task state", task.getState(), equalTo(State.PENDING));
  }

  @Test
  public void savedApplicationBinaryIsScanned() throws IOException {
    Scanner scanner = mock(Scanner.class);
    ScanTask task = new ScanTask(scanner, null, null);

    File appBinaryLocation = new File("app-binary-location");
    task.init("public-app-id", appBinaryLocation);

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
    ScanTask task = new ScanTask(scanner, uploader, evaluator);

    ScanReceipt scanReciept = mock(ScanReceipt.class);
    when(scanReciept.getScanId()).thenReturn("expected-scan-id");

    when(uploader.upload((File) any(), anyString(), anyString())).thenReturn(scanReciept);

    task.init("expected-public-app-id", new File("any-file"));
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
    ScanTask task = new ScanTask(scanner, null, null);

    when(scanner.scan((File) any())).thenThrow(RuntimeException.class);

    task.run();

    ScanTicket ticket = task.getTicket();
    assertThat("Ticket has error", ticket.error, is(notNullValue()));
    assertThat("Ticket has no scan id", ticket.scanId, is(nullValue()));
    assertThat("Final ticket step", ticket.currentStep, is(ticket.totalSteps));
    assertThat("Final ticket step text", ticket.currentStepName, is("Done"));
  }
}
