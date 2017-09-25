/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SuccessMetricsReportServiceTest
    extends AbstractComponentTest
{
  private final SuccessMetricsReportDAO successMetricsReportDAO = new SuccessMetricsReportDAO();

  @Inject
  private SuccessMetricsReportService successMetricsReportService;

  private Organization org;

  private Application app1;

  private Application app2;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    tempEntity.newUser(USERNAME);
  }

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser() throws Exception {
    SuccessMetricsReportScopeDTO scopeDTO = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId())), new HashSet<>(Arrays.asList(org.getId())));
    SuccessMetricsReportDTO dto = new SuccessMetricsReportDTO("Metrics1", scopeDTO);
    SuccessMetricsReportDTO actualDto = successMetricsReportService.createSuccessMetricsReportForCurrentUser(dto);

    SuccessMetricsReport actual = successMetricsReportDAO.getById(actualDto.id);
    assertSuccessMetrics(actual, USERNAME, "Metrics1", "metrics1", scopeDTO);

    // cleanup
    successMetricsReportDAO.delete(actual);
  }

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser_NullAppsAndOrgs() throws Exception {
    SuccessMetricsReportScopeDTO scopeDTO = new SuccessMetricsReportScopeDTO(null, null);
    SuccessMetricsReportDTO dto = new SuccessMetricsReportDTO("Metrics1", scopeDTO);
    SuccessMetricsReportDTO actualDto = successMetricsReportService.createSuccessMetricsReportForCurrentUser(dto);

    SuccessMetricsReport actual = successMetricsReportDAO.getById(actualDto.id);
    assertSuccessMetrics(actual, USERNAME, "Metrics1", "metrics1", scopeDTO);

    // cleanup
    successMetricsReportDAO.delete(actual);
  }

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser_NullScope() throws Exception {
    SuccessMetricsReportScopeDTO scopeDTO = null;
    SuccessMetricsReportDTO dto = new SuccessMetricsReportDTO("Metrics1", scopeDTO);
    try {
      successMetricsReportService.createSuccessMetricsReportForCurrentUser(dto);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("Scope cannot be null or missing.", expected.getMessage());
    }
  }

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser_EmptyAppsAndOrgs() throws Exception {
    SuccessMetricsReportScopeDTO scopeDTO = new SuccessMetricsReportScopeDTO(Collections.<String> emptySet(),
        Collections.<String> emptySet());
    SuccessMetricsReportDTO dto = new SuccessMetricsReportDTO("Metrics1", scopeDTO);
    SuccessMetricsReportDTO actualDto = successMetricsReportService.createSuccessMetricsReportForCurrentUser(dto);

    SuccessMetricsReport actual = successMetricsReportDAO.getById(actualDto.id);
    assertSuccessMetrics(actual, USERNAME, "Metrics1", "metrics1", scopeDTO);

    //cleanup
    successMetricsReportDAO.delete(actual);
  }

  @Test
  public void testGetSuccessMetricsReportsForCurrentUser() throws Exception {
    String metricsName1 = "Metrics1";
    String metricsName2 = "Metrics2";
    SuccessMetricsReportScopeDTO scopeDTO1 = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId())), new HashSet<>(Arrays.asList(org.getId())));
    SuccessMetricsReportScopeDTO scopeDTO2 = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId())), new HashSet<>(Arrays.asList(org.getId())));

    SuccessMetricsReportDTO dto1 = new SuccessMetricsReportDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetricsReport(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    SuccessMetricsReportDTO dto2 = new SuccessMetricsReportDTO(metricsName2, scopeDTO2);
    tempEntity.newSuccessMetricsReport(USERNAME, metricsName2, JsonUtils.format(dto2.scope));

    tempEntity.newSuccessMetricsReport("wrong_name", metricsName2, JsonUtils.format(dto2.scope));

    List<SuccessMetricsReportDTO> actual = successMetricsReportService.getSuccessMetricsReportsForCurrentUser();

    assertThat(actual, hasSize(2));
    assertSuccessMetricsReportDTO(actual.get(0), metricsName1, scopeDTO1);
    assertSuccessMetricsReportDTO(actual.get(1), metricsName2, scopeDTO2);
  }

  @Test
  public void testGetSuccessMetricsReportsForCurrentUser_NullAppsAndOrgs() throws Exception {
    String metricsName1 = "Metrics1";
    SuccessMetricsReportScopeDTO scopeDTO1 = new SuccessMetricsReportScopeDTO(null, null);

    SuccessMetricsReportDTO dto1 = new SuccessMetricsReportDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetricsReport(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    List<SuccessMetricsReportDTO> actual = successMetricsReportService.getSuccessMetricsReportsForCurrentUser();

    assertThat(actual, hasSize(1));
    assertSuccessMetricsReportDTO(actual.get(0), metricsName1, scopeDTO1);
  }

  @Test
  public void testGetSuccessMetricsReportsForCurrentUser_EmptyApp() throws Exception {
    String metricsName1 = "Metrics1";
    SuccessMetricsReportScopeDTO scopeDTO1 = new SuccessMetricsReportScopeDTO(Collections.<String> emptySet(),
        new HashSet<>(Arrays.asList(org.getId())));

    SuccessMetricsReportDTO dto1 = new SuccessMetricsReportDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetricsReport(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    List<SuccessMetricsReportDTO> actual = successMetricsReportService.getSuccessMetricsReportsForCurrentUser();

    assertThat(actual, hasSize(1));
    assertSuccessMetricsReportDTO(actual.get(0), metricsName1, scopeDTO1);
  }

  @Test
  public void testGetSuccessMetricsReportsForCurrentUser_EmptyOrg() throws Exception {
    String metricsName1 = "Metrics1";
    SuccessMetricsReportScopeDTO scopeDTO1 = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId())), Collections.<String> emptySet());

    SuccessMetricsReportDTO dto1 = new SuccessMetricsReportDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetricsReport(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    List<SuccessMetricsReportDTO> actual = successMetricsReportService.getSuccessMetricsReportsForCurrentUser();

    assertThat(actual, hasSize(1));
    assertSuccessMetricsReportDTO(actual.get(0), metricsName1, scopeDTO1);
  }

  @Test
  public void testGetSuccessMetricsReportsForCurrentUser_EmptyAppsAndOrgs() throws Exception {
    String metricsName1 = "Metrics1";
    SuccessMetricsReportScopeDTO scopeDTO1 = new SuccessMetricsReportScopeDTO(Collections.<String> emptySet(),
        Collections.<String> emptySet());

    SuccessMetricsReportDTO dto1 = new SuccessMetricsReportDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetricsReport(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    List<SuccessMetricsReportDTO> actual = successMetricsReportService.getSuccessMetricsReportsForCurrentUser();

    assertThat(actual, hasSize(1));
    assertSuccessMetricsReportDTO(actual.get(0), metricsName1, scopeDTO1);
  }

  @Test
  public void testDeleteSuccessMetricsReportForCurrentUser() {
    String metricsName1 = "Metrics1";
    String metricsName2 = "Metrics2";
    SuccessMetricsReportScopeDTO scopeDTO1 = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId())), new HashSet<>(Arrays.asList(org.getId())));
    SuccessMetricsReportScopeDTO scopeDTO2 = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId())), new HashSet<>(Arrays.asList(org.getId())));

    SuccessMetricsReportDTO dto1 = new SuccessMetricsReportDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetricsReport(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    SuccessMetricsReportDTO dto2 = new SuccessMetricsReportDTO(metricsName2, scopeDTO2);
    SuccessMetricsReport metrics2 = tempEntity.newSuccessMetricsReport(USERNAME, metricsName2,
        JsonUtils.format(dto2.scope));

    successMetricsReportService.deleteSuccessMetricsReportForCurrentUser(metrics2.getId());

    List<SuccessMetricsReport> actual = successMetricsReportDAO.getByUsername(USERNAME);

    assertThat(actual, hasSize(1));
    assertSuccessMetrics(actual.get(0), USERNAME, metricsName1, "metrics1", scopeDTO1);
  }

  @Test
  public void testDeleteSuccessMetricsReportForCurrentUser_WrongUser() {
    User anotherUser = tempEntity.newUser("Another_User");

    String metricsName1 = "Metrics1";
    SuccessMetricsReportScopeDTO scopeDTO1 = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId())), new HashSet<>(Arrays.asList(org.getId())));

    SuccessMetricsReportDTO dto1 = new SuccessMetricsReportDTO(metricsName1, scopeDTO1);
    // create a record belonging to another user
    SuccessMetricsReport metrics1 = tempEntity.newSuccessMetricsReport(anotherUser.getUsername(), metricsName1,
        JsonUtils.format(dto1.scope));

    // try to delete other user's record
    try {
      successMetricsReportService.deleteSuccessMetricsReportForCurrentUser(metrics1.getId());
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a success metrics report with id " + metrics1.getId() + " for user id " + USERNAME + ".",
          expected.getMessage());
    }
  }

  @Test
  public void testDeleteSuccessMetricsReportForCurrentUser_NotFound() {
    try {
      successMetricsReportService.deleteSuccessMetricsReportForCurrentUser("not_found");
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a success metrics report with id not_found for user id " + USERNAME + ".",
          expected.getMessage());
    }
  }

  private void assertSuccessMetrics(SuccessMetricsReport actual,
                                    String username,
                                    String name,
                                    String nameLowercaseNoWhitespace,
                                    SuccessMetricsReportScopeDTO scopeDTO)
  {
    assertThat(actual.getId(), is(notNullValue()));
    assertThat(actual.getUsername(), is(username));
    assertThat(actual.getName(), is(name));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(nameLowercaseNoWhitespace));
    assertThat(actual.getScopeJson(), is(JsonUtils.format(scopeDTO)));
  }

  private void assertSuccessMetricsReportDTO(SuccessMetricsReportDTO actual,
                                             String name,
                                             SuccessMetricsReportScopeDTO scopeDTO)
  {
    assertThat(actual.id, is(notNullValue()));
    assertThat(actual.name, is(name));
    if (actual.scope.applicationIds != null) {
      assertThat(actual.scope.applicationIds,
          containsInAnyOrder(scopeDTO.applicationIds.toArray(new String[scopeDTO.applicationIds.size()])));
    }
    else {
      assertThat(actual.scope.applicationIds, is(scopeDTO.applicationIds));
    }
    if (actual.scope.organizationIds != null) {
      assertThat(actual.scope.organizationIds,
          containsInAnyOrder(scopeDTO.organizationIds.toArray(new String[scopeDTO.organizationIds.size()])));
    }
    else {
      assertThat(actual.scope.organizationIds, is(scopeDTO.organizationIds));
    }
  }
}
