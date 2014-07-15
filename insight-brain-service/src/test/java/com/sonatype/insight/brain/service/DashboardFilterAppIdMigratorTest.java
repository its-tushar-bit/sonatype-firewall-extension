package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dashboard.DashboardFilterDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
    tempEntity.newDashboardFilter("foo",
        JsonUtils.format(createFilter(Collections.singletonList("testMissingApplication"))));
    dashboardFilterAppIdMigrator.migrate();
    
    DashboardFilterDTO filter = getByUser("foo");
    assertThat(filter, notNullValue());
    assertThat(filter.applicationFilters, is(empty()));
  }

  @Test
  public void testNullApplications() throws Exception {
    tempEntity.newDashboardFilter("foo", JsonUtils.format(createFilter(null)));
    dashboardFilterAppIdMigrator.migrate();

    DashboardFilterDTO filter = getByUser("foo");
    assertThat(filter, notNullValue());
    assertThat(filter.applicationFilters, is(nullValue()));
  }

  @Test
  public void testMigration() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testMigration");
    tempEntity.newDashboardFilter("foo", JsonUtils.format(createFilter(Collections.singletonList("testMigration"))));
    dashboardFilterAppIdMigrator.migrate();

    DashboardFilterDTO filter = getByUser("foo");
    assertThat(filter, notNullValue());
    assertThat(filter.applicationFilters, contains(app.getId()));
  }

  private DashboardFilterDTO createFilter(List<String> appIds) {
    DashboardFilterDTO filterDTO = new DashboardFilterDTO();
    filterDTO.applicationFilters = appIds;
    return filterDTO;
  }

  private DashboardFilterDTO getByUser(String user) throws IOException {
    DashboardFilter filter = dao.getByUsername(user);
    if (filter == null) {
      return null;
    }
    return JsonUtils.parse(filter.getFilter(), DashboardFilterDTO.class);
  }
}
