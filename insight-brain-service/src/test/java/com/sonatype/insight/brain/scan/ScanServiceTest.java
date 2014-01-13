/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ScanServiceTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Inject
  private ScanService scanService;

  private Application app;

  private InputStream getBundle(String name) {
    return getClass().getResourceAsStream("/ScannerTest/" + name);
  }

  @Before
  public void init() {
    app = tempEntity.newApplication(tempEntity.newOrganization().getId());
  }

  @Test
  public void testScanBinary() throws Exception {
    InputStream appBundle = getBundle("app01.zip");
    ScanTicket ticket = scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(Stage.ID_BUILD),
        false);
    assertThat(ticket, is(notNullValue()));
    assertThat(ticket.ticketId, is(notNullValue()));
  }

  @Test
  public void testSaveBinary_KeepsOriginalFileExtensionForArchiveDetectionPurposes() throws Exception {
    File file = ScanService.saveBinary(getBundle("app01.zip"), "app.tar.gz");
    file.delete();
    assertThat(file.getName(), endsWith(".tar.gz"));
  }

  @Test
  public void testGetTicket() throws IOException {
    InputStream appBundle = getBundle("app01.zip");
    ScanTicket originalTicket = scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(
        Stage.ID_BUILD), false);

    ScanTicket statusTicket = scanService.getTicket(app.getPublicId(), originalTicket.ticketId);
    assertThat(statusTicket.ticketId, is(originalTicket.ticketId));
  }

  @Test
  public void testGetTicketNotFound() {
    try {
      scanService.getTicket(app.getPublicId(), "unknown-ticket");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), containsString("unknown-ticket"));
    }
  }

  /**
   * Simulates what the UI will do, but without any pausing.  Don't really know the value of this other than having an
   * integrated test of the actual task execution.
   */
  @Test(timeout=15 * 1000)
  public void testGetTicketUntilTaskComplete() throws IOException {
    InputStream appBundle = getBundle("app01.zip");
    ScanTicket originalTicket = scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(
        Stage.ID_BUILD), false);

    ScanTicket statusTicket = originalTicket;
    while (statusTicket.currentStep != statusTicket.totalSteps) {
      statusTicket = scanService.getTicket(app.getPublicId(), originalTicket.ticketId);
    }
  }

  @Test
  public void testFailEarlyOnInvalidStage() throws Exception {
    InputStream appBundle = getBundle("app01.zip");
    try {
      scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage("invalid-stage-id"), false);
      fail("Should have reject invalid stage");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), containsString("invalid-stage-id"));
    }
  }
}
