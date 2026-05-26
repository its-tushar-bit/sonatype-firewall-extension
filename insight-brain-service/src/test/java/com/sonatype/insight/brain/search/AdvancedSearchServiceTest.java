/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import jakarta.inject.Inject;
import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdvancedSearchServiceTest
    extends AbstractComponentTest
{
  @Inject
  private AdvancedSearchService advancedSearchService;

  @Inject
  private IndexService indexService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private TaskScheduler taskScheduler;

  @Inject
  private SystemConfigurationPropertyDAO dao;

  @Inject
  private QuartzJobSchedulingService quartzJobSchedulingService;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Before
  public void resetSearchIndexState() throws Exception {
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(quartzJobSchedulingService);
    taskScheduler.createScheduler();
    taskScheduler.clear();
    FileUtils.deleteDirectory(insightWork.getSearchIndexDir());
  }

  @Test
  public void testSetStatus_AdvancedSearchConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> advancedSearchService.setStatus(new AdvancedSearchStatusDTO()))
        .withMessage("advanced-search-configuration feature is disabled.");
  }

  @Test
  public void testSetStatus_EnableSearch() {
    AdvancedSearchStatusDTO statusDTO = new AdvancedSearchStatusDTO();
    statusDTO.isEnabled = true;
    advancedSearchService.setStatus(statusDTO);
    assertThat(isAdvancedSearchEnabled()).isTrue();
  }

  @Test
  public void testSetStatus_DisabledSearch() {
    // Given Advanced Search is in enabled state..
    dao.update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));

    AdvancedSearchStatusDTO statusDTO = new AdvancedSearchStatusDTO();
    statusDTO.isEnabled = false;

    advancedSearchService.setStatus(statusDTO);
    assertThat(isAdvancedSearchEnabled()).isFalse();
  }

  @Test
  public void testGetStatus_AdvancedSearchConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> advancedSearchService.getStatus())
        .withMessage("advanced-search-configuration feature is disabled.");
  }

  @Test
  public void testGetStatus_SearchDisabled() {
    taskScheduler.createScheduler();
    AdvancedSearchStatusDTO status = advancedSearchService.getStatus();
    assertThat(status.isEnabled).isFalse();
  }

  @Test
  public void testGetStatus_SearchEnabled() {
    taskScheduler.createScheduler();
    // Given Advanced Search is in enabled state..
    dao.update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    AdvancedSearchStatusDTO status = advancedSearchService.getStatus();
    assertThat(status.isEnabled).isTrue();
  }

  @Test
  public void testGetStatus_NoIndex_NullLastIndexTime() {
    taskScheduler.createScheduler();
    assertThat(advancedSearchService.getStatus().lastIndexTime).isNull();
    assertThat(insightWork.getSearchIndexDir()).doesNotExist();
  }

  @Test
  public void testGetStatus_Index_HasLastIndexTime() throws Exception {
    indexService.createSearchIndex();
    taskScheduler.createScheduler();
    File segmentFile = Arrays.stream(insightWork.getSearchIndexDir().listFiles())
        .filter(file -> file.getName().startsWith("segment"))
        .findFirst()
        .get();
    long firstIndexTime = segmentFile.lastModified();
    assertThat(advancedSearchService.getStatus().lastIndexTime).isEqualTo(firstIndexTime);
    indexService.createSearchIndex();
    segmentFile = Arrays.stream(insightWork.getSearchIndexDir().listFiles())
        .filter(file -> file.getName().startsWith("segment"))
        .findFirst()
        .get();
    segmentFile.setLastModified(segmentFile.lastModified() + 1000); // Ensure the next index time is different
    long secondIndexTime = segmentFile.lastModified();
    assertThat(secondIndexTime).isGreaterThan(firstIndexTime);
    assertThat(advancedSearchService.getStatus().lastIndexTime).isEqualTo(secondIndexTime);
  }

  @Test
  public void testGetStatus_FullIndexNotTriggered() {
    taskScheduler.createScheduler();

    assertThat(advancedSearchService.getStatus().isFullIndexTriggered).isFalse();
  }

  @Test
  public void testGetStatus_FullIndexTriggered() {
    taskScheduler.createScheduler();
    indexService.register();

    assertThat(advancedSearchService.getStatus().isFullIndexTriggered).isFalse();

    indexService.createIndexAsync();

    await().pollDelay(Duration.ZERO)
        .pollInterval(Duration.ofMillis(10))
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(() -> assertThat(advancedSearchService.getStatus().isFullIndexTriggered).isTrue());

    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(quartzJobSchedulingService);
  }

  private boolean isAdvancedSearchEnabled() {
    return Boolean.parseBoolean(dao.getByName(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED).getValue());
  }
}
