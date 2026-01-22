/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collections;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentCategoryList;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComponentCategoryUpdaterUnitTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private TaskScheduler mockTaskScheduler;

  private ComponentCategoryDAO componentCategoryDAO;

  @Inject
  private ComponentCategoryUpdater componentCategoryUpdater;

  @Override
  public void configure(Binder binder) {
    componentCategoryDAO = spy(daoFactory.createComponentCategoryDAO());

    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(ComponentCategoryDAO.class).toInstance(componentCategoryDAO);
    super.configure(binder);
  }

  @Test
  public void testDoUpdate_Schedules_LoadComponentCategories_OnAllOtherNodes() {
    ComponentCategoryList componentCategoryList = new ComponentCategoryList();
    componentCategoryList.setComponentCategories(Collections.emptyList());
    when(mockHdsClient.get(ComponentCategoryList.class, ComponentCategoryUpdater.HDS_COMPONENT_CATEGORY_PATH,
        null)).thenReturn(componentCategoryList);

    componentCategoryUpdater.doUpdate();

    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(componentCategoryUpdater);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ComponentCategoryUpdater.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() throws Exception {
    ComponentCategoryUpdater spyComponentCategoryUpdater = spy(componentCategoryUpdater);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spyComponentCategoryUpdater).doLoadComponentCategories();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spyComponentCategoryUpdater.execute(mock(JobExecutionContext.class));
    }

    verify(spyComponentCategoryUpdater).doLoadComponentCategories();
  }

  @Test
  public void testDoLoadComponentCategories() {
    componentCategoryUpdater.doLoadComponentCategories();

    verify(componentCategoryDAO).load();
  }
}
