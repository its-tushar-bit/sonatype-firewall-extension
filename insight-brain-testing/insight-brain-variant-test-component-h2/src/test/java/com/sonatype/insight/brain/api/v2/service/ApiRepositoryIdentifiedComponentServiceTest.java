/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

@ComponentH2Test
public class ApiRepositoryIdentifiedComponentServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiRepositoryIdentifiedComponentService repositoryIdentifiedComponentService;

  @Inject
  private RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  @Inject
  private RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @AfterEach
  public void after() {
    repositoryIdentifiedComponentCache.getLoadingCache().invalidateAll();
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_TwoOrMoreNotNull() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("hash",
            componentIdentifier, PackageUrlIdentifier.toPackageUrl(componentIdentifier)))
        .withMessageContaining("Only one of either hash or componentIdentifier or packageUrl must be specified.");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("hash",
            componentIdentifier, null))
        .withMessageContaining("Only one of either hash or componentIdentifier or packageUrl must be specified.");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
            componentIdentifier, PackageUrlIdentifier.toPackageUrl(componentIdentifier)))
        .withMessageContaining("Only one of either hash or componentIdentifier or packageUrl must be specified.");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("hash",
            null, PackageUrlIdentifier.toPackageUrl(componentIdentifier)))
        .withMessageContaining("Only one of either hash or componentIdentifier or packageUrl must be specified.");
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_AllNull() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, null))
        .withMessageContaining("You must specify one of either hash or componentIdentifier or packageUrl.");
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Hash_OnlyInDatabase() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 = tempEntity.newRepositoryIdentifiedComponent();

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getHash(),
        null, null);

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getHash)
        .containsExactly(repositoryIdentifiedComponent2.getHash());
    verifyScheduledTask(repositoryIdentifiedComponent1.getHash(), null);
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Hash_OnlyInMemory() {
    repositoryIdentifiedComponentCache.getLoadingCache()
        .asMap()
        .put("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    repositoryIdentifiedComponentCache.getLoadingCache()
        .asMap()
        .put("hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("hash1", null, null);

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().keySet()).containsExactly("hash2");
    verifyScheduledTask("hash1", null);
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Hash() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 = tempEntity.newRepositoryIdentifiedComponent();
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent1.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent2.getHash());

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getHash(),
        null, null);

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getHash)
        .containsExactly(repositoryIdentifiedComponent2.getHash());
    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().keySet()).containsExactly(
        repositoryIdentifiedComponent2.getHash());
    verifyScheduledTask(repositoryIdentifiedComponent1.getHash(), null);
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier_OnlyInDatabase() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    tempEntity.newRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getComponentIdentifier());
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 = tempEntity.newRepositoryIdentifiedComponent();

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
        repositoryIdentifiedComponent1.getComponentIdentifier(), null);

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getComponentIdentifier)
        .containsExactly(repositoryIdentifiedComponent3.getComponentIdentifier());
    verifyScheduledTask(null, repositoryIdentifiedComponent1.getComponentIdentifier());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier_OnlyInMemory() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash1", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash2", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash3", componentIdentifier2);

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, componentIdentifier1, null);

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        componentIdentifier2);
    verifyScheduledTask(null, componentIdentifier1);
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 =
        tempEntity.newRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getComponentIdentifier());
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 = tempEntity.newRepositoryIdentifiedComponent();
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent1.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent2.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent3.getHash());

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
        repositoryIdentifiedComponent1.getComponentIdentifier(), null);

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getComponentIdentifier)
        .containsExactly(repositoryIdentifiedComponent3.getComponentIdentifier());
    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        repositoryIdentifiedComponent3.getComponentIdentifier());
    verifyScheduledTask(null, repositoryIdentifiedComponent1.getComponentIdentifier());
  }

  @Test
  public void testBadPurl() {
    assertThatExceptionOfType(InvalidPackageURLException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, "badPurl"));
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Purl_OnlyInDatabase() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    tempEntity.newRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getComponentIdentifier());
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 = tempEntity.newRepositoryIdentifiedComponent();

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null,
        PackageUrlIdentifier.toPackageUrl(repositoryIdentifiedComponent1.getComponentIdentifier()));

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getComponentIdentifier)
        .containsExactly(repositoryIdentifiedComponent3.getComponentIdentifier());
    verifyScheduledTask(null, repositoryIdentifiedComponent1.getComponentIdentifier());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Purl_OnlyInMemory() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash1", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash2", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash3", componentIdentifier2);

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null,
        PackageUrlIdentifier.toPackageUrl(componentIdentifier1));

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        componentIdentifier2);
    verifyScheduledTask(null, componentIdentifier1);
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Purl() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 =
        tempEntity.newRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getComponentIdentifier());
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 = tempEntity.newRepositoryIdentifiedComponent();
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent1.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent2.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent3.getHash());

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
        null, PackageUrlIdentifier.toPackageUrl(repositoryIdentifiedComponent1.getComponentIdentifier()));

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getComponentIdentifier)
        .containsExactly(repositoryIdentifiedComponent3.getComponentIdentifier());
    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        repositoryIdentifiedComponent3.getComponentIdentifier());
    verifyScheduledTask(null, repositoryIdentifiedComponent1.getComponentIdentifier());
  }

  @Test
  public void testDeleteAllRepositoryIdentifiedComponents_OnlyInDatabase() {
    tempEntity.newRepositoryIdentifiedComponent();
    tempEntity.newRepositoryIdentifiedComponent();

    repositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents();

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getHash).isEmpty();

    verifyScheduledTaskClearAll();
  }

  @Test
  public void testClearAllCache_OnlyInMemory() {
    repositoryIdentifiedComponentCache.getLoadingCache()
        .asMap()
        .put("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    repositoryIdentifiedComponentCache.getLoadingCache()
        .asMap()
        .put("hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));

    repositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents();

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().keySet()).isEmpty();
    verifyScheduledTaskClearAll();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ApiRepositoryIdentifiedComponentService.class)
        .build()
        .isConcurrentExectionDisallowed()).isFalse();
  }

  @Test
  public void testExecute_Hash() throws Exception {
    repositoryIdentifiedComponentCache.getLoadingCache()
        .asMap()
        .put("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    repositoryIdentifiedComponentCache.getLoadingCache()
        .asMap()
        .put("hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));
    ApiRepositoryIdentifiedComponentService spyRepositoryIdentifiedComponentService =
        spy(repositoryIdentifiedComponentService);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return invocationOnMock.callRealMethod();
    }).when(spyRepositoryIdentifiedComponentService).deleteRepositoryIdentifiedComponentFromMemory(any(), any());
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobDataMap.put(ApiRepositoryIdentifiedComponentService.TASK_PARAM_HASH, "hash1");

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spyRepositoryIdentifiedComponentService.execute(mockJobExecutionContext);
    }

    verify(spyRepositoryIdentifiedComponentService).deleteRepositoryIdentifiedComponentFromMemory("hash1", null);
    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().keySet()).containsExactly("hash2");
    verifyNoInteractions(mockTaskScheduler);
  }

  @Test
  public void testExecute_ComponentIdentifier() throws Exception {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    repositoryIdentifiedComponentCache.getLoadingCache()
        .asMap()
        .put("hash1", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache()
        .asMap()
        .put("hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));
    ApiRepositoryIdentifiedComponentService spyRepositoryIdentifiedComponentService =
        spy(repositoryIdentifiedComponentService);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return invocationOnMock.callRealMethod();
    }).when(spyRepositoryIdentifiedComponentService).deleteRepositoryIdentifiedComponentFromMemory(any(), any());
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobDataMap.put(ApiRepositoryIdentifiedComponentService.TASK_PARAM_COMPONENT_IDENTIFIER,
        JsonUtils.writeUnformatted(componentIdentifier1));

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spyRepositoryIdentifiedComponentService.execute(mockJobExecutionContext);
    }

    verify(spyRepositoryIdentifiedComponentService).deleteRepositoryIdentifiedComponentFromMemory(null,
        componentIdentifier1);
    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().keySet()).containsExactly("hash2");
    verifyNoInteractions(mockTaskScheduler);
  }

  private void verifyScheduledTaskClearAll() {
    Map<String, String> expectedParameters = new HashMap<>();
    expectedParameters.put(ApiRepositoryIdentifiedComponentService.TASK_PARAM_CLEAR_ALL, "true");
    verify(mockTaskScheduler)
        .scheduleOneTimeTaskForAllOtherNodes(repositoryIdentifiedComponentService, expectedParameters);
  }

  private void verifyScheduledTask(String hash, ComponentIdentifier componentIdentifier) {
    Map<String, String> expectedParameters = new HashMap<>();
    if (hash != null) {
      expectedParameters.put(ApiRepositoryIdentifiedComponentService.TASK_PARAM_HASH, hash);
    }
    if (componentIdentifier != null) {
      expectedParameters.put(ApiRepositoryIdentifiedComponentService.TASK_PARAM_COMPONENT_IDENTIFIER,
          JsonUtils.writeUnformatted(componentIdentifier));
    }
    verify(mockTaskScheduler)
        .scheduleOneTimeTaskForAllOtherNodes(repositoryIdentifiedComponentService, expectedParameters);
  }
}
