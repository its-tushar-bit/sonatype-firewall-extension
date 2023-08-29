/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScanServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ScanService scanService;

  private PersistedScanTicketDAO persistedScanTicketDAO = new PersistedScanTicketDAO();

  private InputStream getBundle() {
    return new ByteArrayInputStream(new byte[0]);
  }

  private Set<String> existingPersistedScanTicketIds;

  @Before
  public void before() {
    existingPersistedScanTicketIds =
        persistedScanTicketDAO.getAll().stream().map(PersistedScanTicket::getId).collect(Collectors.toSet());
  }

  @After
  public void after() {
    persistedScanTicketDAO.getAll().forEach(persistedScanTicket -> {
      if (!existingPersistedScanTicketIds.contains(persistedScanTicket.getId())) {
        persistedScanTicketDAO.delete(persistedScanTicket);
      }
    });
  }

  @Test(expected = UnauthenticatedException.class)
  public void testScanBinary_Anon() throws Exception {
    scanService.scanBinary(app.getPublicId(), getBundle(), "app.zip", new Stage(Stage.ID_BUILD), false, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testScanBinary_Unauthorized() throws Exception {
    login();
    scanService.scanBinary(app.getPublicId(), getBundle(), "app.zip", new Stage(Stage.ID_BUILD), false, null, null);
  }

  @Test
  public void testScanBinary_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    scanService.scanBinary(app.getPublicId(), getBundle(), "app.zip", new Stage(Stage.ID_BUILD), false, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetTicket_Anon() {
    scanService.getTicket("any-app-id", "any-ticket-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTicket_Unauthorized() {
    login();
    scanService.getTicket(app.getPublicId(), "any-ticket-id");
  }

  @Test
  public void testGetTicket_Authorized() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    try {
      scanService.getTicket(app.getPublicId(), "any-ticket-id");
    }
    catch (NotFoundException irrelevant) {
      // Expected but irrelevant for this test.
    }
  }
}
