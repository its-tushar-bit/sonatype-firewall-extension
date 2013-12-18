/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.InputStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ScanServiceTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Inject
  private ScanService scanService;

  private Application app;

  @Before
  public void init() {
    app = tempEntity.newApplication(tempEntity.newOrganization().getId());
  }

  @Test
  public void testScanBinary() throws Exception {
    InputStream appBundle = getClass().getResourceAsStream("/ScannerTest/app01.zip");
    ScanTicket ticket = scanService.scanBinary(app.getPublicId(), appBundle);
    assertThat(ticket, is(notNullValue()));
    assertThat(ticket.ticketId, is(notNullValue()));
  }
}
