/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentCategoryList;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.dataaccess.TransactionContext;
import jakarta.inject.Inject;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

@ComponentH2Test
public class ComponentCategoryUpdaterUnitTest
    extends AbstractComponentH2Test
{
  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private ComponentCategoryDAO componentCategoryDAO;

  @Inject
  private ComponentCategoryUpdater componentCategoryUpdater;

  @Test
  public void testDoUpdate_Schedules_LoadComponentCategories_OnAllOtherNodes() {
    ComponentCategoryList componentCategoryList = new ComponentCategoryList();
    componentCategoryList.setComponentCategories(Collections.emptyList());
    TransactionContext transactionContext = mock(TransactionContext.class);
    when(mockHdsClient.get(ComponentCategoryList.class, ComponentCategoryUpdater.HDS_COMPONENT_CATEGORY_PATH,
        null)).thenReturn(componentCategoryList);
    when(componentCategoryDAO.createTransactionContext()).thenReturn(transactionContext);

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
