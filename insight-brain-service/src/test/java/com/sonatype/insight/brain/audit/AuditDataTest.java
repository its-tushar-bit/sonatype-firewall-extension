/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditDataTest
{
  @Captor
  private ArgumentCaptor<Function<AuditData, Void>> functionArgumentCaptor;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    AuditData.instance.remove();
  }

  @Test
  public void testGet_Initial() {
    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testSet_Null() {
    AuditData.set(null);

    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testSet_NoopAuditData() {
    AuditData.set(NoopAuditData.INSTANCE);

    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testSet_RecordingAuditData() {
    RecordingAuditData recordingAuditData = new RecordingAuditData(null, null);
    AuditData.set(recordingAuditData);

    assertThat(AuditData.get(), is(recordingAuditData));
  }

  @Test
  public void testSet_ProxyAuditData() {
    ProxyAuditData proxyAuditData = new ProxyAuditData(null);
    AuditData.set(proxyAuditData);

    assertThat(AuditData.get(), is(proxyAuditData));
  }

  @Test
  public void testSet_ReturnsPrevious() {
    RecordingAuditData recordingAuditData = new RecordingAuditData(null, null);

    assertThat(AuditData.set(recordingAuditData), is(NoopAuditData.INSTANCE));
    assertThat(AuditData.set(new ProxyAuditData(null)), is(recordingAuditData));
  }

  @Test
  public void testRecordSubEvent() {
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);
    when(auditData.forSubEvent(null, false)).thenReturn(auditData);

    try (AuditSession auditSession = auditData.recordSubEvent(null, false)) {
      assertThat(auditSession, is(notNullValue()));
      verify(auditData).forSubEvent(null, false);
    }

    verify(auditData).commit();
  }

  @Test
  public void testContinueAsync_Runnable() {
    String[] result = new String[1];
    Runnable runnable = () -> result[0] = "result";
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.continueAsync(runnable, runnableSubmitter());

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter, is(notNullValue()));
    wrappedTaskSubmitter.apply(auditData);
    assertThat(result[0], is("result"));
    verify(auditData, never()).setException(any());
    verify(auditData).commit();
    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testContinueAsync_Runnable_Throwable() {
    RuntimeException t = new RuntimeException("message");
    Runnable runnable = () -> {
      throw t;
    };
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.continueAsync(runnable, runnableSubmitter());

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter, is(notNullValue()));
    try {
      wrappedTaskSubmitter.apply(auditData);
    }
    catch (RuntimeException | Error e) {
      assertThat(e.getMessage(), is(t.getMessage()));
    }
    verify(auditData).setException(t);
    verify(auditData).commit();
    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testContinueAsync_Callable() {
    String[] result = new String[1];
    Callable<Void> callable = () -> {
      result[0] = "result";
      return null;
    };
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.continueAsync(callable, callableSubmitter());

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter, is(notNullValue()));
    wrappedTaskSubmitter.apply(auditData);
    assertThat(result[0], is("result"));
    verify(auditData, never()).setException(any());
    verify(auditData).commit();
    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testContinueAsync_Callable_Throwable() {
    Exception t = new RuntimeException("message");
    Callable<Void> callable = () -> {
      throw t;
    };
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.continueAsync(callable, callableSubmitter());

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter, is(notNullValue()));
    try {
      wrappedTaskSubmitter.apply(auditData);
    }
    catch (RuntimeException | Error e) {
      assertThat(e.getMessage(), is(t.getMessage()));
    }
    verify(auditData).setException(t);
    verify(auditData).commit();
    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testContinueAsync_Supplier() {
    String[] result = new String[1];
    Supplier<Void> supplier = () -> {
      result[0] = "result";
      return null;
    };
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.continueAsync(supplier, supplierSubmitter());

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter, is(notNullValue()));
    wrappedTaskSubmitter.apply(auditData);
    assertThat(result[0], is("result"));
    verify(auditData, never()).setException(any());
    verify(auditData).commit();
    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testContinueAsync_Supplier_Throwable() {
    RuntimeException t = new RuntimeException("message");
    Supplier<Void> supplier = () -> {
      throw t;
    };
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.continueAsync(supplier, supplierSubmitter());

    verify(auditData).continueAsync(functionArgumentCaptor.capture());
    Function<AuditData, Void> wrappedTaskSubmitter = functionArgumentCaptor.getValue();
    assertThat(wrappedTaskSubmitter, is(notNullValue()));
    try {
      wrappedTaskSubmitter.apply(auditData);
    }
    catch (RuntimeException | Error e) {
      assertThat(e.getMessage(), is(t.getMessage()));
    }
    verify(auditData).setException(t);
    verify(auditData).commit();
    assertThat(AuditData.get(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testSetApplication() {
    Application application = new Application();
    application.setId("appId");
    application.setPublicId("appPublicId");
    application.setName("appName");
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.setApplication(application);

    verify(auditData).addData("applicationId", application.getId());
    verify(auditData).addData("applicationPublicId", application.getPublicId());
    verify(auditData).addData("applicationName", application.getName());
  }

  @Test
  public void testSetApplication_Null_DoesNothing() {
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.setApplication(null);

    verify(auditData, never()).addData(anyString(), any());
  }

  @Test
  public void testSetOrganization() {
    Organization organization = new Organization();
    organization.setId("orgId");
    organization.setName("orgName");
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.setOrganization(organization);

    verify(auditData).addData("organizationId", organization.getId());
    verify(auditData).addData("organizationName", organization.getName());
  }

  @Test
  public void testSetOrganization_Null_DoesNothing() {
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.setOrganization(null);

    verify(auditData, never()).addData(anyString(), any());
  }

  @Test
  public void testSetRepository() {
    Repository repository = new Repository();
    repository.setId("repoId");
    repository.setPublicId("repoPublicId");
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.setRepository(repository);

    verify(auditData).addData("repositoryId", repository.getId());
    verify(auditData).addData("repositoryPublicId", repository.getPublicId());
  }

  @Test
  public void testSetRepository_Null_DoesNothing() {
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.setRepository(null);

    verify(auditData, never()).addData(anyString(), any());
  }

  @Test
  public void testSetRepositoryContainer() {
    AuditData auditData = mock(AuditData.class, Mockito.CALLS_REAL_METHODS);

    auditData.setRepositoryContainer();

    verify(auditData).addData("scope", "all-repositories");
  }

  private Function<Runnable, Void> runnableSubmitter() {
    return wrappedRunnable -> {
      wrappedRunnable.run();
      return null;
    };
  }

  private Function<Callable<Void>, Void> callableSubmitter() {
    return wrappedCallable -> {
      try {
        wrappedCallable.call();
      }
      catch (Exception e) {
        // do nothing
      }
      return null;
    };
  }

  private Function<Supplier<Void>, Void> supplierSubmitter() {
    return wrappedSupplier -> {
      wrappedSupplier.get();
      return null;
    };
  }
}
