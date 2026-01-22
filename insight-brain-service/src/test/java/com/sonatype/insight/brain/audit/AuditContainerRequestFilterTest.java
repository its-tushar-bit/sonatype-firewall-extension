/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditContainerRequestFilterTest
    extends AbstractDataTest
{
  private static final String BAD_ID = "badId";

  @Rule
  public TestAuditSession testAuditSession = new TestAuditSession();

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private ResourceInfo mockResourceInfo;

  @Mock
  private ContainerRequestContext mockContainerRequestContext;

  @Mock
  private UriInfo mockUriInfo;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private AuditData mockAuditData;

  @Captor
  private ArgumentCaptor<Owner> ownerArgumentCaptor;

  private AuditContainerRequestFilter auditContainerRequestFilter;

  private MultivaluedMap<String, String> pathParameters;

  private Organization organization;

  private Application application;

  private Repository repository;

  private RepositoryManager repositoryManager;

  @Before
  public void before() {
    lenient().when(mockContainerRequestContext.getUriInfo()).thenReturn(mockUriInfo);
    pathParameters = new MultivaluedHashMap<>();
    lenient().when(mockUriInfo.getPathParameters()).thenReturn(pathParameters);
    testAuditSession.set(mockAuditData);

    ApplicationDAO applicationDAO = daoFactory.createApplicationDAO();
    OrganizationDAO organizationDAO = daoFactory.createOrganizationDAO();
    RepositoryDAO repositoryDAO = daoFactory.createRepositoryDAO();
    RepositoryManagerDAO repositoryManagerDAO = daoFactory.createRepositoryManagerDAO();
    auditContainerRequestFilter = new AuditContainerRequestFilter(applicationDAO, organizationDAO, repositoryDAO,
        repositoryManagerDAO, () -> mockResourceInfo);
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, TemporaryEntity.uuid());
  }

  @Test
  public void testFilter_NullMethod_DoesNothing() {
    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, never()).setEvent(any());
  }

  @Test
  public void testFilter_AuditedMethod_SetsEvent() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setEvent(AuditEvent.AUTHENTICATION_FAILURE);
  }

  @Test
  public void testFilter_NonAuditedMethod_DoesNothing() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("notAudited"));

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, never()).setEvent(any());
  }

  @Test
  public void testFilter_AuditedMethod_Guice_SetsEvent() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTestGuice$$.class.getMethod("audited"));

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setEvent(AuditEvent.AUTHENTICATION_FAILURE);
  }

  @Test(expected = IllegalStateException.class)
  public void testFilter_NoAuditedMethod_Guice_ThrowsException() throws Exception {
    when(mockResourceInfo.getResourceMethod())
        .thenReturn(AuditedAnnotationTestGuice$$.class.getMethod("onlyInAuditedAnnotationTestGuice$$"));

    auditContainerRequestFilter.filter(mockContainerRequestContext);
  }

  @Test
  public void testFilter_ApplicationId_SetsApplication() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("applicationId", application.getId());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setApplicationId(application.getId());
    verify(mockAuditData).setApplication((Application) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(application.getId());
  }

  @Test
  public void testFilter_BadApplicationId_SetsApplicationId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("applicationId", BAD_ID);

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setApplicationId(BAD_ID);
    verify(mockAuditData).setApplication(null);
  }

  @Test
  public void testFilter_ApplicationPublicId_SetsApplication() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("applicationPublicId", application.getPublicId());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setApplicationPublicId(application.getPublicId());
    verify(mockAuditData).setApplication((Application) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(application.getId());
  }

  @Test
  public void testFilter_BadApplicationPublicId_SetsApplicationPublicId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("applicationPublicId", BAD_ID);

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setApplicationPublicId(BAD_ID);
    verify(mockAuditData).setApplication(null);
  }

  @Test
  public void testFilter_OrganizationId_SetsOrganization() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("organizationId", organization.getId());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setOrganizationId(organization.getId());
    verify(mockAuditData).setOrganization((Organization) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(organization.getId());
  }

  @Test
  public void testFilter_BadOrganizationId_SetsOrganizationId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("organizationId", BAD_ID);

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setOrganizationId(BAD_ID);
    verify(mockAuditData).setOrganization(null);
  }

  @Test
  public void testFilter_RepositoryId_SetsRepository() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("repositoryId", repository.getId());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setRepositoryId(repository.getId());
    verify(mockAuditData).setRepository((Repository) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(repository.getId());
  }

  @Test
  public void testFilter_BadRepositoryId_SetsRepositoryId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("repositoryId", BAD_ID);

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setRepositoryId(BAD_ID);
    verify(mockAuditData).setRepository(null);
  }

  @Test
  public void testFilter_RepositoryPublicId_SetsRepository() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("repositoryPublicId", repository.getPublicId());
    pathParameters.add("repositoryManagerInstanceId", repositoryManager.getInstanceId());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setRepositoryPublicId(repository.getPublicId());
    verify(mockAuditData).setRepository((Repository) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(repository.getId());
  }

  @Test
  public void testFilter_BadRepositoryPublicId_SetsRepositoryPublicId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("repositoryPublicId", BAD_ID);
    pathParameters.add("repositoryManagerInstanceId", repositoryManager.getInstanceId());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setRepositoryPublicId(BAD_ID);
    verify(mockAuditData).setRepository(null);
  }

  @Test
  public void testFilter_ApplicationOwnerIdAndType_SetsApplicationFromInternalId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", application.getId());
    pathParameters.add("ownerType", OwnerType.APPLICATION.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setApplication((Application) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(application.getId());
  }

  @Test
  public void testFilter_RepositoryManagerId_SetsRepositoryManager() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("repositoryManagerId", repositoryManager.getId());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setRepositoryManagerId(repositoryManager.getId());
    verify(mockAuditData).setRepositoryManager((RepositoryManager) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(repositoryManager.getId());
  }

  @Test
  public void testFilter_BadRepositoryManagerId_SetsRepositoryManagerId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("repositoryManagerId", BAD_ID);

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setRepositoryManagerId(BAD_ID);
    verify(mockAuditData).setRepositoryManager(null);
  }

  @Test
  public void testFilter_ApplicationOwnerIdAndType_SetsApplicationFromPublicId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", application.getPublicId());
    pathParameters.add("ownerType", OwnerType.APPLICATION.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setApplicationPublicId(application.getPublicId());
    verify(mockAuditData).setApplication((Application) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(application.getId());
  }

  @Test
  public void testFilter_ApplicationInternalOwnerIdAndType_SetsApplication() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("internalOwnerId", application.getId());
    pathParameters.add("ownerType", OwnerType.APPLICATION.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setApplicationId(application.getId());
    verify(mockAuditData).setApplication((Application) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(application.getId());
  }

  @Test
  public void testFilter_BadApplicationOwnerIdAndType_SetsApplicationId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", BAD_ID);
    pathParameters.add("ownerType", OwnerType.APPLICATION.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setApplicationPublicId(BAD_ID);
    verify(mockAuditData).setApplication(null);
  }

  @Test
  public void testFilter_OrganizationOwnerIdAndType_SetsOrganization() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", organization.getId());
    pathParameters.add("ownerType", OwnerType.ORGANIZATION.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setOrganizationId(organization.getId());
    verify(mockAuditData).setOrganization((Organization) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(organization.getId());
  }

  @Test
  public void testFilter_OrganizationInternalOwnerIdAndType_SetsOrganization() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("internalOwnerId", organization.getId());
    pathParameters.add("ownerType", OwnerType.ORGANIZATION.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setOrganizationId(organization.getId());
    verify(mockAuditData).setOrganization((Organization) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(organization.getId());
  }

  @Test
  public void testFilter_BadOrganizationOwnerIdAndType_SetsOrganizationId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", BAD_ID);
    pathParameters.add("ownerType", OwnerType.ORGANIZATION.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setOrganizationId(BAD_ID);
    verify(mockAuditData).setOrganization(null);
  }

  @Test
  public void testFilter_RepositoryOwnerIdAndType_SetsRepository() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", repository.getId());
    pathParameters.add("ownerType", OwnerType.REPOSITORY.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setRepositoryId(repository.getId());
    verify(mockAuditData).setRepository((Repository) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(repository.getId());
  }

  @Test
  public void testFilter_RepositoryInternalOwnerIdAndType_SetsRepository() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("internalOwnerId", repository.getId());
    pathParameters.add("ownerType", OwnerType.REPOSITORY.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setRepositoryId(repository.getId());
    verify(mockAuditData).setRepository((Repository) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(repository.getId());
  }

  @Test
  public void testFilter_BadRepositoryOwnerIdAndType_SetsRepositoryId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", BAD_ID);
    pathParameters.add("ownerType", OwnerType.REPOSITORY.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setRepositoryId(BAD_ID);
    verify(mockAuditData).setRepository(null);
  }

  @Test
  public void testFilter_AnyIdAndRepositoryContainerOwnerType_SetsRepositoryContainer() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", "anyId");
    pathParameters.add("ownerType", OwnerType.REPOSITORY_CONTAINER.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setRepositoryContainer();
  }

  @Test
  public void testFilter_JustRepositoryContainerOwnerType_SetsRepositoryContainer() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerType", OwnerType.REPOSITORY_CONTAINER.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setRepositoryContainer();
  }

  @Test
  public void testFilter_AnyIdAndGlobalOwnerType_SetsGlobal() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", "anyId");
    pathParameters.add("ownerType", OwnerType.GLOBAL.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setGlobal();
  }

  @Test
  public void testFilter_JustGlobalOwnerType_SetsGlobal() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerType", OwnerType.GLOBAL.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setGlobal();
  }

  @Test
  public void testFilter_JustOwnerId_DoesNothing() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", "ownerId");

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, never()).setData(anyString(), any());
  }

  @Test
  public void testFilter_JustApplicationOwnerType_DoesNothing() throws Exception {
    testFilter_JustOwnerType_DoesNothing(OwnerType.APPLICATION.toString());
  }

  @Test
  public void testFilter_JustOrganizationOwnerType_DoesNothing() throws Exception {
    testFilter_JustOwnerType_DoesNothing(OwnerType.ORGANIZATION.toString());
  }

  @Test
  public void testFilter_JustRepositoryOwnerType_DoesNothing() throws Exception {
    testFilter_JustOwnerType_DoesNothing(OwnerType.REPOSITORY.toString());
  }

  private void testFilter_JustOwnerType_DoesNothing(String ownerType) throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerType", ownerType);

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, never()).setData(anyString(), any());
  }

  @Test
  public void testFilter_OwnerIdAndBadType_DoesNothing() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", application.getId());
    pathParameters.add("ownerType", "unknown");

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, never()).setData(anyString(), any());
  }

  @Test
  public void testPriority_IsPresent() {
    Priority priority = AuditContainerRequestFilter.class.getAnnotation(Priority.class);

    assertThat(priority.value()).isLessThan(Priorities.AUTHENTICATION);
  }

  @Test
  public void testFilter_RepositoryManagerOwnerIdAndType_SetsRepositoryManager() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", repositoryManager.getId());
    pathParameters.add("ownerType", OwnerType.REPOSITORY_MANAGER.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData, atLeastOnce()).setRepositoryManagerId(repositoryManager.getId());
    verify(mockAuditData).setRepositoryManager((RepositoryManager) ownerArgumentCaptor.capture());
    assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(repositoryManager.getId());
  }

  @Test
  public void testFilter_BadRepositoryManagerOwnerIdAndType_SetsRepositoryManagerId() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));
    pathParameters.add("ownerId", BAD_ID);
    pathParameters.add("ownerType", OwnerType.REPOSITORY_MANAGER.toString());

    auditContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockAuditData).setRepositoryManagerId(BAD_ID);
    verify(mockAuditData).setRepositoryManager(null);
  }

  private static class AuditedAnnotationTest
  {
    @Audited(value = AuditEvent.AUTHENTICATION_FAILURE)
    public void audited() {
    }

    @SuppressWarnings("unused")
    public void notAudited() {
    }
  }

  @SuppressWarnings("checkstyle:TypeName")
  private static class AuditedAnnotationTestGuice$$
      extends AuditedAnnotationTest
  {
    @Override
    public void audited() {
    }

    @SuppressWarnings({"unused", "checkstyle:MethodName"})
    public void onlyInAuditedAnnotationTestGuice$$() {
    }

    @Override
    public void notAudited() {
    }
  }
}
