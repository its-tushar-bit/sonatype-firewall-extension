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

import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetrics;
import com.sonatype.insight.brain.service.AbstractComponentTest;
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

public class SuccessMetricsServiceTest
    extends AbstractComponentTest
{
  private final SuccessMetricsDAO successMetricsDAO = new SuccessMetricsDAO();

  @Inject
  private SuccessMetricsService successMetricsService;

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
  public void testCreateSuccessMetricsForCurrentUser() throws Exception
  {
    SuccessMetricsScopeDTO scopeDTO = new SuccessMetricsScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId())), new HashSet<>(Arrays.asList(org.getId())));
    SuccessMetricsDTO dto = new SuccessMetricsDTO("Metrics1", scopeDTO);
    SuccessMetricsDTO actualDto = successMetricsService.createSuccessMetricsForCurrentUser(dto);

    SuccessMetrics actual = successMetricsDAO.getById(actualDto.id);
    assertSuccessMetrics(actual, USERNAME, "Metrics1", "metrics1", scopeDTO);

    //cleanup
    successMetricsDAO.delete(actual);
  }

  @Test
  public void testCreateSuccessMetricsForCurrentUser_NullAppsAndOrgs() throws Exception
  {
    SuccessMetricsScopeDTO scopeDTO = new SuccessMetricsScopeDTO(null, null);
    SuccessMetricsDTO dto = new SuccessMetricsDTO("Metrics1", scopeDTO);
    SuccessMetricsDTO actualDto = successMetricsService.createSuccessMetricsForCurrentUser(dto);

    SuccessMetrics actual = successMetricsDAO.getById(actualDto.id);
    assertSuccessMetrics(actual, USERNAME, "Metrics1", "metrics1", scopeDTO);

    //cleanup
    successMetricsDAO.delete(actual);
  }

  @Test
  public void testCreateSuccessMetricsForCurrentUser_EmptyAppsAndOrgs() throws Exception
  {
    SuccessMetricsScopeDTO scopeDTO = new SuccessMetricsScopeDTO(Collections.<String>emptySet(),
        Collections.<String>emptySet());
    SuccessMetricsDTO dto = new SuccessMetricsDTO("Metrics1", scopeDTO);
    SuccessMetricsDTO actualDto = successMetricsService.createSuccessMetricsForCurrentUser(dto);

    SuccessMetrics actual = successMetricsDAO.getById(actualDto.id);
    assertSuccessMetrics(actual, USERNAME, "Metrics1", "metrics1", scopeDTO);

    //cleanup
    successMetricsDAO.delete(actual);
  }

  @Test
  public void testGetSuccessMetricsForCurrentUser() throws Exception {
    String metricsName1 = "Metrics1";
    String metricsName2 = "Metrics2";
    SuccessMetricsScopeDTO scopeDTO1 = new SuccessMetricsScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId())), new HashSet<>(Arrays.asList(org.getId())));
    SuccessMetricsScopeDTO scopeDTO2 = new SuccessMetricsScopeDTO(new HashSet<>(Arrays.asList(app1.getId())),
        new HashSet<>(Arrays.asList(org.getId())));

    SuccessMetricsDTO dto1 = new SuccessMetricsDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetrics(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    SuccessMetricsDTO dto2 = new SuccessMetricsDTO(metricsName2, scopeDTO2);
    tempEntity.newSuccessMetrics(USERNAME, metricsName2, JsonUtils.format(dto2.scope));

    tempEntity.newSuccessMetrics("wrong_name", metricsName2, JsonUtils.format(dto2.scope));

    List<SuccessMetricsDTO> actual = successMetricsService.getSuccessMetricsForCurrentUser();

    assertThat(actual, hasSize(2));
    assertSuccessMetricsDTO(actual.get(0), metricsName1, scopeDTO1);
    assertSuccessMetricsDTO(actual.get(1), metricsName2, scopeDTO2);
  }

  @Test
  public void testGetSuccessMetricsForCurrentUser_NullAppsAndOrgs() throws Exception {
    String metricsName1 = "Metrics1";
    SuccessMetricsScopeDTO scopeDTO1 = new SuccessMetricsScopeDTO(null, null);

    SuccessMetricsDTO dto1 = new SuccessMetricsDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetrics(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    List<SuccessMetricsDTO> actual = successMetricsService.getSuccessMetricsForCurrentUser();

    assertThat(actual, hasSize(1));
    assertSuccessMetricsDTO(actual.get(0), metricsName1, scopeDTO1);
  }

  @Test
  public void testGetSuccessMetricsForCurrentUser_EmptyApp() throws Exception {
    String metricsName1 = "Metrics1";
    SuccessMetricsScopeDTO scopeDTO1 = new SuccessMetricsScopeDTO(Collections.<String>emptySet(),
        new HashSet<>(Arrays.asList(org.getId())));

    SuccessMetricsDTO dto1 = new SuccessMetricsDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetrics(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    List<SuccessMetricsDTO> actual = successMetricsService.getSuccessMetricsForCurrentUser();

    assertThat(actual, hasSize(1));
    assertSuccessMetricsDTO(actual.get(0), metricsName1, scopeDTO1);
  }

  @Test
  public void testGetSuccessMetricsForCurrentUser_EmptyOrg() throws Exception {
    String metricsName1 = "Metrics1";
    SuccessMetricsScopeDTO scopeDTO1 = new SuccessMetricsScopeDTO(new HashSet<>(Arrays.asList(app1.getId())),
        Collections.<String>emptySet());

    SuccessMetricsDTO dto1 = new SuccessMetricsDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetrics(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    List<SuccessMetricsDTO> actual = successMetricsService.getSuccessMetricsForCurrentUser();

    assertThat(actual, hasSize(1));
    assertSuccessMetricsDTO(actual.get(0), metricsName1, scopeDTO1);
  }

  @Test
  public void testGetSuccessMetricsForCurrentUser_EmptyAppsAndOrgs() throws Exception {
    String metricsName1 = "Metrics1";
    SuccessMetricsScopeDTO scopeDTO1 = new SuccessMetricsScopeDTO(Collections.<String>emptySet(),
        Collections.<String>emptySet());

    SuccessMetricsDTO dto1 = new SuccessMetricsDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetrics(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    List<SuccessMetricsDTO> actual = successMetricsService.getSuccessMetricsForCurrentUser();

    assertThat(actual, hasSize(1));
    assertSuccessMetricsDTO(actual.get(0), metricsName1, scopeDTO1);
  }

  @Test
  public void testDeleteSuccessMetricsForCurrentUser() {
    String metricsName1 = "Metrics1";
    String metricsName2 = "Metrics2";
    SuccessMetricsScopeDTO scopeDTO1 = new SuccessMetricsScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId())), new HashSet<>(Arrays.asList(org.getId())));
    SuccessMetricsScopeDTO scopeDTO2 = new SuccessMetricsScopeDTO(new HashSet<>(Arrays.asList(app1.getId())),
        new HashSet<>(Arrays.asList(org.getId())));

    SuccessMetricsDTO dto1 = new SuccessMetricsDTO(metricsName1, scopeDTO1);
    tempEntity.newSuccessMetrics(USERNAME, metricsName1, JsonUtils.format(dto1.scope));

    SuccessMetricsDTO dto2 = new SuccessMetricsDTO(metricsName2, scopeDTO2);
    SuccessMetrics metrics2 = tempEntity.newSuccessMetrics(USERNAME, metricsName2, JsonUtils.format(dto2.scope));

    successMetricsService.deleteSuccessMetricsForCurrentUser(metrics2.getId());

    List<SuccessMetrics> actual = successMetricsDAO.getByUsername(USERNAME);

    assertThat(actual, hasSize(1));
    assertSuccessMetrics(actual.get(0), USERNAME, metricsName1, "metrics1", scopeDTO1);
  }

  @Test
  public void testDeleteSuccessMetricsForCurrentUser_WrongUser() {
    User anotherUser = tempEntity.newUser("Another_User");

    String metricsName1 = "Metrics1";
    SuccessMetricsScopeDTO scopeDTO1 = new SuccessMetricsScopeDTO(
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId())), new HashSet<>(Arrays.asList(org.getId())));

    SuccessMetricsDTO dto1 = new SuccessMetricsDTO(metricsName1, scopeDTO1);
    // create a record belonging to another user
    SuccessMetrics metrics1 = tempEntity
        .newSuccessMetrics(anotherUser.getUsername(), metricsName1, JsonUtils.format(dto1.scope));

    // try to delete other user's record
    try {
      successMetricsService.deleteSuccessMetricsForCurrentUser(metrics1.getId());
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a success metrics with id " + metrics1.getId() + " for user id " + USERNAME + ".",
          expected.getMessage());
    }
  }

  @Test
  public void testDeleteSuccessMetricsForCurrentUser_NotFound() {
    try {
      successMetricsService.deleteSuccessMetricsForCurrentUser("not_found");
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a success metrics with id not_found for user id " + USERNAME + ".",
          expected.getMessage());
    }
  }

  private void assertSuccessMetrics(SuccessMetrics actual,
                                    String username,
                                    String name,
                                    String nameLowercaseNoWhitespace,
                                    SuccessMetricsScopeDTO scopeDTO)
  {
    assertThat(actual.getId(), is(notNullValue()));
    assertThat(actual.getUsername(), is(username));
    assertThat(actual.getName(), is(name));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(nameLowercaseNoWhitespace));
    assertThat(actual.getScopeJson(), is(JsonUtils.format(scopeDTO)));
  }

  private void assertSuccessMetricsDTO(SuccessMetricsDTO actual,
                                       String name,
                                       SuccessMetricsScopeDTO scopeDTO)
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
