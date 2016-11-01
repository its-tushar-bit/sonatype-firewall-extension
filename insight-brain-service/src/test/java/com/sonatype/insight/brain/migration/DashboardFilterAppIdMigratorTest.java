/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dashboard.DashboardFilterDTO;
import com.sonatype.insight.brain.dashboard.NamedDashboardFilterDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;

public class DashboardFilterAppIdMigratorTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private File sonatypeWork;

  private InsightConfig insightConfig;

  private InsightWork insightWork;

  private DashboardFilterAppIdMigrator dashboardFilterAppIdMigrator;

  private DashboardFilterDAO dao = new DashboardFilterDAO();

  @Before
  public void setup() throws IOException {
    sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
    dashboardFilterAppIdMigrator = new DashboardFilterAppIdMigrator(insightWork);
  }

  @Test
  public void testMissingApplication() throws Exception {
    String filterName = "";
    tempEntity.newDashboardFilter("foo", filterName,
        JsonUtils.format(createFilter(Collections.singletonList("testMissingApplication"))));
    dashboardFilterAppIdMigrator.migrate();

    List<NamedDashboardFilterDTO> filter = getByUser("foo");
    assertThat(filter, is(not(empty())));
    assertThat(filter.get(0).filter.applicationFilters, is(empty()));
  }

  @Test
  public void testNullApplications() throws Exception {
    String filterName = "";
    tempEntity.newDashboardFilter("foo", filterName, JsonUtils.format(createFilter(null)));
    dashboardFilterAppIdMigrator.migrate();

    List<NamedDashboardFilterDTO> filter = getByUser("foo");
    assertThat(filter, is(not(empty())));
    assertThat(filter.get(0).filter.applicationFilters, is(nullValue()));
  }

  @Test
  public void testMigration() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testMigration");
    String filterName = "";
    tempEntity.newDashboardFilter("foo", filterName,
        JsonUtils.format(createFilter(Collections.singletonList("testMigration"))));
    dashboardFilterAppIdMigrator.migrate();

    List<NamedDashboardFilterDTO> filter = getByUser("foo");
    assertThat(filter, is(not(empty())));
    assertThat(filter.get(0).filter.applicationFilters, contains(app.getId()));
  }

  private DashboardFilterDTO createFilter(List<String> appIds) {
    DashboardFilterDTO filterDTO = new DashboardFilterDTO();
    filterDTO.applicationFilters = appIds;
    return filterDTO;
  }

  private List<NamedDashboardFilterDTO> getByUser(String user) throws IOException {
    List<DashboardFilter> filters = dao.getByUsername(user);
    if (filters.isEmpty()) {
      return null;
    }

    List<NamedDashboardFilterDTO> filterDTOs = new ArrayList<>();
    for (DashboardFilter filter : filters) {
      NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
      namedDashboardFilterDTO.filter = JsonUtils.parse(filter.getFilter(), DashboardFilterDTO.class);
      namedDashboardFilterDTO.name = filter.getName();

      filterDTOs.add(namedDashboardFilterDTO);
    }

    return filterDTOs;
  }
}
