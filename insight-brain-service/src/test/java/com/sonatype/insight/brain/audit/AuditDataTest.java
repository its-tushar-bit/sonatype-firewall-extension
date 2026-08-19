/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.lang.annotation.ElementType;
import java.util.function.Function;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuditDataTest
{
  @RegisterExtension
  public TestAuditSession testAuditSession = new TestAuditSession();

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private AuditData auditData;

  @Captor
  private ArgumentCaptor<Function<AuditData, Void>> functionArgumentCaptor;

  @Test
  public void testGet_Initial() {
    assertThat(AuditData.get()).isEqualTo(NoopAuditData.INSTANCE);
  }

  @Test
  public void testGet_Current() {
    testAuditSession.set(auditData);
    assertThat(AuditData.get()).isEqualTo(auditData);
  }

  @Test
  public void testRecordSubEvent() {
    AuditData subAuditData = mock(AuditData.class);
    when(auditData.forSubEvent(null, false, false)).thenReturn(subAuditData);

    try (AuditSession auditSession = auditData.recordSubEvent(null, false)) {
      assertThat(auditSession).isNotNull();
      verify(auditData).forSubEvent(null, false, false);
    }

    verify(subAuditData).commit();
  }

  @Test
  public void testContinueAsync_Executor_Runnable() {
    String[] result = new String[1];
    Runnable runnable = () -> result[0] = "result";

    auditData.continueAsync(Runnable::run, runnable);

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter).isNotNull();
    wrappedTaskSubmitter.apply(auditData);
    assertThat(result[0]).isEqualTo("result");
    verify(auditData, never()).setException(any());
    verify(auditData).commit();
    assertThat(AuditData.get()).isEqualTo(NoopAuditData.INSTANCE);
  }

  @Test
  public void testContinueAsync_Runnable() {
    String[] result = new String[1];
    Runnable runnable = () -> result[0] = "result";

    auditData.continueAsync(runnable, runnableSubmitter());

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter).isNotNull();
    wrappedTaskSubmitter.apply(auditData);
    assertThat(result[0]).isEqualTo("result");
    verify(auditData, never()).setException(any());
    verify(auditData).commit();
    assertThat(AuditData.get()).isEqualTo(NoopAuditData.INSTANCE);
  }

  @Test
  public void testContinueAsync_Runnable_Throwable() {
    RuntimeException t = new RuntimeException("message");
    Runnable runnable = () -> {
      throw t;
    };

    auditData.continueAsync(runnable, runnableSubmitter());

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter).isNotNull();
    wrappedTaskSubmitter.apply(auditData);
    verify(auditData).setException(t);
    verify(auditData).commit();
    assertThat(AuditData.get()).isEqualTo(NoopAuditData.INSTANCE);
  }

  @Test
  public void testSetEnum_NonNullValue() {
    auditData.setEnum("key", ElementType.ANNOTATION_TYPE);
    verify(auditData).setData("key", "annotation-type");
  }

  @Test
  public void testSetEnum_NullValue() {
    auditData.setEnum("key", null);
    verify(auditData).setData("key", null);
  }

  @Test
  public void testSetApplication() {
    Application application = new Application();
    application.setId("appId");
    application.setPublicId("appPublicId");
    application.setName("appName");

    auditData.setApplication(application);

    verify(auditData).setData("applicationId", application.getId());
    verify(auditData).setData("applicationPublicId", application.getPublicId());
    verify(auditData).setData("applicationName", application.getName());
  }

  @Test
  public void testSetApplication_Null_DoesNothing() {
    auditData.setApplication(null);

    verify(auditData, never()).setData(anyString(), any());
  }

  @Test
  public void testSetOrganization() {
    Organization organization = new Organization();
    organization.setId("orgId");
    organization.setName("orgName");

    auditData.setOrganization(organization);

    verify(auditData).setData("organizationId", organization.getId());
    verify(auditData).setData("organizationName", organization.getName());
  }

  @Test
  public void testSetOrganization_Null_DoesNothing() {
    auditData.setOrganization(null);

    verify(auditData, never()).setData(anyString(), any());
  }

  @Test
  public void testSetRepository() {
    Repository repository = new Repository();
    repository.setId("repoId");
    repository.setPublicId("repoPublicId");

    auditData.setRepository(repository);

    verify(auditData).setData("repositoryId", repository.getId());
    verify(auditData).setData("repositoryPublicId", repository.getPublicId());
  }

  @Test
  public void testSetRepository_Null_DoesNothing() {
    auditData.setRepository(null);

    verify(auditData, never()).setData(anyString(), any());
  }

  @Test
  public void testSetRepositoryContainer() {
    auditData.setRepositoryContainer();

    verify(auditData).setData("scope", "all-repositories");
  }

  @Test
  public void testSetGlobal() {
    auditData.setGlobal();

    verify(auditData).setData("scope", "global");
  }

  @Test
  public void testSetRepositoryManager() {
    RepositoryManager repositoryManager = new RepositoryManager();
    auditData.setRepositoryManager(repositoryManager);

    verify(auditData).setData("repositoryManagerId", repositoryManager.getId());
    verify(auditData).setData("repositoryManagerInstanceId", repositoryManager.getInstanceId());
    verify(auditData).setData("repositoryManagerName", repositoryManager.getName());
  }

  private Function<Runnable, Void> runnableSubmitter() {
    return wrappedRunnable -> {
      wrappedRunnable.run();
      return null;
    };
  }
}
