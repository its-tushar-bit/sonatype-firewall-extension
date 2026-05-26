/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryComponentDeleteService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sonatype.insight.brain.security.SecurityAspectControl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiRepositoryComponentServiceTest
{
  private static final String RM_INSTANCE_ID = "rm-instance-1";

  private static final String RM_INTERNAL_ID = "rm-internal-id";

  private static final String REPO_PUBLIC_ID = "hosted-repo-1";

  private static final String REPO_ID = "repo-internal-id";

  private static final String COMPONENT_ID_1 = "component-1";

  private static final String COMPONENT_ID_2 = "component-2";

  @Mock
  private RepositoryManagerDAO repositoryManagerDAO;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private RepositoryComponentDAO repositoryComponentDAO;

  @Mock
  private HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  @Mock
  private RepositoryComponentDeleteService repositoryComponentDeleteService;

  @Mock
  private TransactionContext transactionContext;

  @InjectMocks
  private ApiRepositoryComponentService service;

  private RepositoryComponent component1;

  private RepositoryComponent component2;

  @Before
  public void setup() {
    SecurityAspectControl.disableEnforcement();

    component1 = mock(RepositoryComponent.class);
    when(component1.getComponentId()).thenReturn(COMPONENT_ID_1);
    when(component1.getRepositoryId()).thenReturn(REPO_ID);

    component2 = mock(RepositoryComponent.class);
    when(component2.getComponentId()).thenReturn(COMPONENT_ID_2);
    when(component2.getRepositoryId()).thenReturn(REPO_ID);

    RepositoryManager rm = mock(RepositoryManager.class);
    when(rm.getId()).thenReturn(RM_INTERNAL_ID);
    when(repositoryManagerDAO.getByInstanceIdNotNull(RM_INSTANCE_ID)).thenReturn(rm);

    Repository repo = mock(Repository.class);
    when(repo.getRepositoryManagerId()).thenReturn(RM_INTERNAL_ID);
    when(repo.getRepositoryType()).thenReturn(RepositoryType.hosted);
    when(repositoryDAO.getByIdNotNull(REPO_ID)).thenReturn(repo);

    when(hostedComponentScanQueueDAO.createTransactionContext()).thenReturn(transactionContext);
    when(hostedComponentScanQueueDAO.hasInProgressByComponentIds(any(), any())).thenReturn(false);
  }

  @After
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
  }

  @Test
  public void testDeleteComponents_Success() {
    when(repositoryComponentDAO.getByIdNotNull(COMPONENT_ID_1)).thenReturn(component1);
    when(repositoryComponentDAO.getByIdNotNull(COMPONENT_ID_2)).thenReturn(component2);

    service.deleteComponents(RM_INSTANCE_ID, List.of(COMPONENT_ID_1, COMPONENT_ID_2));

    verify(hostedComponentScanQueueDAO).deletePendingByComponentIds(eq(transactionContext),
        eq(List.of(COMPONENT_ID_1, COMPONENT_ID_2)));
    verify(repositoryComponentDeleteService).deleteComponent(component1);
    verify(repositoryComponentDeleteService).deleteComponent(component2);
    verify(transactionContext, times(1)).begin();
    verify(transactionContext, times(1)).commit();
  }

  @Test
  public void testDeleteComponents_NullList() {
    service.deleteComponents(RM_INSTANCE_ID, null);

    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
    verify(hostedComponentScanQueueDAO, never()).deletePendingByComponentIds(any(), any());
  }

  @Test
  public void testDeleteComponents_EmptyList() {
    service.deleteComponents(RM_INSTANCE_ID, Collections.emptyList());

    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
    verify(hostedComponentScanQueueDAO, never()).deletePendingByComponentIds(any(), any());
  }

  @Test
  public void testDeleteComponents_NullElementsInList_Filtered() {
    when(repositoryComponentDAO.getByIdNotNull(COMPONENT_ID_1)).thenReturn(component1);

    service.deleteComponents(RM_INSTANCE_ID, Arrays.asList(COMPONENT_ID_1, null));

    verify(repositoryComponentDeleteService, times(1)).deleteComponent(component1);
    verify(hostedComponentScanQueueDAO).deletePendingByComponentIds(eq(transactionContext),
        eq(List.of(COMPONENT_ID_1)));
  }

  @Test
  public void testDeleteComponents_DuplicateIds_DeletedOnce() {
    when(repositoryComponentDAO.getByIdNotNull(COMPONENT_ID_1)).thenReturn(component1);

    service.deleteComponents(RM_INSTANCE_ID, List.of(COMPONENT_ID_1, COMPONENT_ID_1));

    verify(repositoryComponentDeleteService, times(1)).deleteComponent(component1);
    verify(hostedComponentScanQueueDAO).deletePendingByComponentIds(eq(transactionContext),
        eq(List.of(COMPONENT_ID_1)));
  }

  @Test
  public void testDeleteComponents_NonHostedComponent_ThrowsNotFound() {
    RepositoryComponent proxyComponent = mock(RepositoryComponent.class);
    when(proxyComponent.getRepositoryId()).thenReturn(REPO_ID);
    when(repositoryComponentDAO.getByIdNotNull(COMPONENT_ID_1)).thenReturn(proxyComponent);

    Repository proxyRepo = mock(Repository.class);
    when(proxyRepo.getRepositoryManagerId()).thenReturn(RM_INTERNAL_ID);
    when(proxyRepo.getRepositoryType()).thenReturn(RepositoryType.proxy);
    when(repositoryDAO.getByIdNotNull(REPO_ID)).thenReturn(proxyRepo);

    assertThatThrownBy(() -> service.deleteComponents(RM_INSTANCE_ID, List.of(COMPONENT_ID_1)))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("hosted repository");

    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
    verify(hostedComponentScanQueueDAO, never()).deletePendingByComponentIds(any(), any());
  }

  @Test
  public void testDeleteComponents_InvalidRM_ThrowsNotFound() {
    when(repositoryManagerDAO.getByInstanceIdNotNull("bad-rm"))
        .thenThrow(new NotFoundException("Cannot find a repository manager"));

    assertThatThrownBy(() -> service.deleteComponents("bad-rm", List.of(COMPONENT_ID_1)))
        .isInstanceOf(NotFoundException.class);

    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
  }

  @Test
  public void testDeleteComponents_ComponentBelongsToDifferentRM_ThrowsNotFound() {
    RepositoryManager otherRm = mock(RepositoryManager.class);
    when(otherRm.getId()).thenReturn("other-rm-internal-id");
    when(repositoryManagerDAO.getByInstanceIdNotNull("other-rm")).thenReturn(otherRm);

    when(repositoryComponentDAO.getByIdNotNull(COMPONENT_ID_1)).thenReturn(component1);

    assertThatThrownBy(() -> service.deleteComponents("other-rm", List.of(COMPONENT_ID_1)))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(COMPONENT_ID_1);

    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
    verify(hostedComponentScanQueueDAO, never()).deletePendingByComponentIds(any(), any());
  }

  @Test
  public void testDeleteComponents_ValidationFailsOnSecondComponent_NoComponentDeleted() {
    RepositoryManager otherRm = mock(RepositoryManager.class);
    when(otherRm.getId()).thenReturn("other-rm-internal-id");
    when(repositoryManagerDAO.getByInstanceIdNotNull("other-rm")).thenReturn(otherRm);

    // component1's repo belongs to RM_INTERNAL_ID, not "other-rm-internal-id" — fails on first component
    Repository otherRepo = mock(Repository.class);
    when(otherRepo.getRepositoryManagerId()).thenReturn(RM_INTERNAL_ID);

    when(repositoryComponentDAO.getByIdNotNull(COMPONENT_ID_1)).thenReturn(component1);
    when(repositoryDAO.getByIdNotNull(REPO_ID)).thenReturn(otherRepo);

    assertThatThrownBy(() -> service.deleteComponents("other-rm", List.of(COMPONENT_ID_1, COMPONENT_ID_2)))
        .isInstanceOf(NotFoundException.class);

    // Neither component should be deleted due to upfront validation
    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
    verify(hostedComponentScanQueueDAO, never()).deletePendingByComponentIds(any(), any());
  }

  @Test
  public void testDeleteRepositoryComponents_Success() {
    Repository hostedRepo = mock(Repository.class);
    when(hostedRepo.getId()).thenReturn(REPO_ID);
    when(hostedRepo.getPublicId()).thenReturn(REPO_PUBLIC_ID);
    when(hostedRepo.getRepositoryType()).thenReturn(RepositoryType.hosted);

    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, REPO_PUBLIC_ID))
        .thenReturn(hostedRepo);
    when(repositoryComponentDAO.getByRepositoryId(eq(transactionContext), eq(REPO_ID), eq(100), eq(0)))
        .thenReturn(List.of(component1, component2));

    service.deleteRepositoryComponents(RM_INSTANCE_ID, List.of(REPO_PUBLIC_ID));

    verify(hostedComponentScanQueueDAO).deletePendingByComponentIds(eq(transactionContext),
        eq(List.of(COMPONENT_ID_1, COMPONENT_ID_2)));
    verify(repositoryComponentDeleteService).deleteComponent(component1);
    verify(repositoryComponentDeleteService).deleteComponent(component2);
    verify(transactionContext, times(1)).begin();
    verify(transactionContext, times(1)).commit();
  }

  @Test
  public void testDeleteRepositoryComponents_MultipleBatches() {
    Repository hostedRepo = mock(Repository.class);
    when(hostedRepo.getId()).thenReturn(REPO_ID);
    when(hostedRepo.getPublicId()).thenReturn(REPO_PUBLIC_ID);
    when(hostedRepo.getRepositoryType()).thenReturn(RepositoryType.hosted);

    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, REPO_PUBLIC_ID))
        .thenReturn(hostedRepo);

    // Simulate 101 components: first fetch (offset=0) returns full batch of 100,
    // second fetch (offset=0 again, after deletion) returns the remaining 1, loop exits
    List<RepositoryComponent> fullBatch = Collections.nCopies(100, component1);
    when(repositoryComponentDAO.getByRepositoryId(eq(transactionContext), eq(REPO_ID), eq(100), eq(0)))
        .thenReturn(fullBatch)
        .thenReturn(List.of(component2));

    service.deleteRepositoryComponents(RM_INSTANCE_ID, List.of(REPO_PUBLIC_ID));

    verify(repositoryComponentDeleteService, times(100)).deleteComponent(component1);
    verify(repositoryComponentDeleteService, times(1)).deleteComponent(component2);
    verify(transactionContext, times(2)).begin();
    verify(transactionContext, times(2)).commit();
  }

  @Test
  public void testDeleteRepositoryComponents_MultipleRepos() {
    Repository repo1 = mock(Repository.class);
    when(repo1.getId()).thenReturn("repo-1");
    when(repo1.getPublicId()).thenReturn("pub-1");
    when(repo1.getRepositoryType()).thenReturn(RepositoryType.hosted);

    Repository repo2 = mock(Repository.class);
    when(repo2.getId()).thenReturn("repo-2");
    when(repo2.getPublicId()).thenReturn("pub-2");
    when(repo2.getRepositoryType()).thenReturn(RepositoryType.hosted);

    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, "pub-1"))
        .thenReturn(repo1);
    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, "pub-2"))
        .thenReturn(repo2);
    when(repositoryComponentDAO.getByRepositoryId(eq(transactionContext), eq("repo-1"), eq(100), eq(0)))
        .thenReturn(List.of(component1));
    when(repositoryComponentDAO.getByRepositoryId(eq(transactionContext), eq("repo-2"), eq(100), eq(0)))
        .thenReturn(List.of(component2));

    service.deleteRepositoryComponents(RM_INSTANCE_ID, List.of("pub-1", "pub-2"));

    verify(repositoryComponentDeleteService).deleteComponent(component1);
    verify(repositoryComponentDeleteService).deleteComponent(component2);
    verify(transactionContext, times(2)).begin();
    verify(transactionContext, times(2)).commit();
  }

  @Test
  public void testDeleteRepositoryComponents_ValidationFailsOnSecondRepo_NoRepoDeleted() {
    Repository hostedRepo = mock(Repository.class);
    when(hostedRepo.getRepositoryType()).thenReturn(RepositoryType.hosted);

    Repository proxyRepo = mock(Repository.class);
    when(proxyRepo.getRepositoryType()).thenReturn(RepositoryType.proxy);

    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, "pub-hosted"))
        .thenReturn(hostedRepo);
    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, "pub-proxy"))
        .thenReturn(proxyRepo);

    assertThatThrownBy(
        () -> service.deleteRepositoryComponents(RM_INSTANCE_ID, List.of("pub-hosted", "pub-proxy")))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("not a hosted repository");

    // Neither repo's components should be deleted due to upfront validation
    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
    verify(hostedComponentScanQueueDAO, never()).deletePendingByComponentIds(any(), any());
  }

  @Test
  public void testDeleteRepositoryComponents_NullList() {
    service.deleteRepositoryComponents(RM_INSTANCE_ID, null);

    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
    verify(hostedComponentScanQueueDAO, never()).deletePendingByComponentIds(any(), any());
  }

  @Test
  public void testDeleteRepositoryComponents_NullElementsInList_Filtered() {
    Repository hostedRepo = mock(Repository.class);
    when(hostedRepo.getId()).thenReturn(REPO_ID);
    when(hostedRepo.getPublicId()).thenReturn(REPO_PUBLIC_ID);
    when(hostedRepo.getRepositoryType()).thenReturn(RepositoryType.hosted);

    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, REPO_PUBLIC_ID))
        .thenReturn(hostedRepo);
    when(repositoryComponentDAO.getByRepositoryId(eq(transactionContext), eq(REPO_ID), eq(100), eq(0)))
        .thenReturn(List.of(component1));

    service.deleteRepositoryComponents(RM_INSTANCE_ID, Arrays.asList(REPO_PUBLIC_ID, null));

    verify(repositoryComponentDeleteService, times(1)).deleteComponent(component1);
  }

  @Test
  public void testDeleteRepositoryComponents_NotHosted_ThrowsNotFound() {
    Repository proxyRepo = mock(Repository.class);
    when(proxyRepo.getRepositoryType()).thenReturn(RepositoryType.proxy);

    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, REPO_PUBLIC_ID))
        .thenReturn(proxyRepo);

    assertThatThrownBy(() -> service.deleteRepositoryComponents(RM_INSTANCE_ID, List.of(REPO_PUBLIC_ID)))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("not a hosted repository");

    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
  }

  @Test
  public void testDeleteRepositoryComponents_EmptyRepo() {
    Repository hostedRepo = mock(Repository.class);
    when(hostedRepo.getId()).thenReturn(REPO_ID);
    when(hostedRepo.getRepositoryType()).thenReturn(RepositoryType.hosted);

    when(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(RM_INSTANCE_ID, REPO_PUBLIC_ID))
        .thenReturn(hostedRepo);
    when(repositoryComponentDAO.getByRepositoryId(eq(transactionContext), eq(REPO_ID), eq(100), eq(0)))
        .thenReturn(Collections.emptyList());

    service.deleteRepositoryComponents(RM_INSTANCE_ID, List.of(REPO_PUBLIC_ID));

    verify(repositoryComponentDeleteService, never()).deleteComponent(any());
  }
}
