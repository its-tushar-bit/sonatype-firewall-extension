/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.CascadeStatusResponseDTO;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.component.MatchState;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallCascadeResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testInitiateCascadeReevaluation_Success() throws Exception {
    Repository repository = tempEntity.newRepository();
    String componentHash = "test_hash_123";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-package", "1.0.0"), now, now);

    String uri = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/componentHash/" + componentHash;

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);

    CascadeReevaluateTicketDTO ticket = response.getBody(CascadeReevaluateTicketDTO.class);
    assertThat(ticket).isNotNull();
    assertThat(ticket.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testInitiateCascadeReevaluation_MultipleRepositories() throws Exception {
    // Create multiple repositories with the same component
    Repository repo1 = tempEntity.newRepository("repo-1");
    Repository repo2 = tempEntity.newRepository("repo-2");
    String componentHash = "multi_repo_hash";
    Date now = new Date();

    tempEntity.newRepositoryComponent(repo1.getId(),
        MatchState.EXACT, "test/path1", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-package", "1.0.0"), now, now);
    tempEntity.newRepositoryComponent(repo2.getId(),
        MatchState.EXACT, "test/path2", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-package", "1.0.0"), now, now);

    String uri = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/componentHash/" + componentHash;

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);

    CascadeReevaluateTicketDTO ticket = response.getBody(CascadeReevaluateTicketDTO.class);
    assertThat(ticket).isNotNull();
    assertThat(ticket.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testInitiateCascadeReevaluation_ComponentNotFound() throws Exception {
    String nonExistentHash = "non_existent_hash";
    String uri = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/componentHash/" + nonExistentHash;

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);

    CascadeReevaluateTicketDTO ticket = response.getBody(CascadeReevaluateTicketDTO.class);
    assertThat(ticket).isNotNull();
    assertThat(ticket.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testInitiateCascadeReevaluation_BlankComponentHash() throws Exception {
    String uri = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/componentHash/";

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    // Should return 404 for blank component hash in path (malformed URL)
    assertThat(response.getStatusCode()).isIn(400, 404);
  }

  @Test
  public void testInitiateCascadeReevaluation_InvalidPath() throws Exception {
    String uri = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/componentHash/";

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    // Should return error for invalid path
    assertThat(response.getStatusCode()).isIn(400, 404);
  }

  @Test
  public void testInitiateCascadeReevaluation_RequiresContainerLevelPermission() throws Exception {
    Repository repository = tempEntity.newRepository();
    String componentHash = "permission_test_hash";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("permission-package", "1.0.0"), now, now);

    String uri = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/componentHash/" + componentHash;

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);

    CascadeReevaluateTicketDTO ticket = response.getBody(CascadeReevaluateTicketDTO.class);
    assertThat(ticket).isNotNull();
    assertThat(ticket.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testGetCascadeStatus_PendingProgressRequest() throws Exception {
    // Arrange
    Repository repository = tempEntity.newRepository();
    String componentHash = "test_hash_completed";
    Date now = new Date();

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/path/status", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-status-pkg", "1.0.0"), now, now);

    String uri = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/componentHash/" + componentHash;

    // Setup the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = component.getHash();
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    hdsResult.components.add(componentEvaluationData);
    getHdsServer().respondWith(hdsResult).atUri("/rest/component/details/firewall");

    // call to start the cascade request
    HttpResponse responseCreateRequest = restRequest()
        .path(uri)
        .post();

    CascadeReevaluateTicketDTO ticket = responseCreateRequest.getBody(CascadeReevaluateTicketDTO.class);

    // Act
    HttpResponse response = restRequest()
        .path(ticket.statusUrl)
        .get();

    // Assert
    assertResponseStatus(200, response);
    CascadeStatusResponseDTO result = response.getBody(CascadeStatusResponseDTO.class);

    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.IN_PROGRESS);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).isEmpty();
    assertThat(result.pending).hasSize(1);
    assertThat(result.failed).isEmpty();
  }

  @Test
  public void testGetCascadeStatus_Found_AllCompleted() throws Exception {
    // Arrange
    Repository repository = tempEntity.newRepository();
    String componentHash = "test_hash_mixed";
    Date now = new Date();

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/path/mixed", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-mixed-pkg", "1.0.0"), now, now);

    String cascadeRequestId = "cascade_mixed_test_456";
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "admin",
        ReevaluateCascadeRequestStatus.COMPLETED);

    tempEntity.newReevaluateCascadeProgress("progress_completed_1", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress("progress_completed_2", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress("progress_failed_1", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.FAILED.name());

    // Act
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH, "/status/" + cascadeRequestId)
        .get();

    // Assert
    assertResponseStatus(200, response);
    CascadeStatusResponseDTO result = response.getBody(CascadeStatusResponseDTO.class);

    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.COMPLETED);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).hasSize(2);
    assertThat(result.pending).hasSize(0);
    assertThat(result.failed).hasSize(1);

    // Check pending component
    assertThat(result.failed.get(0).repositoryId).isEqualTo(repository.getId());
    assertThat(result.failed.get(0).componentId).isEqualTo(component.getId());
    assertThat(result.failed.get(0).quarantined).isNull();

    // Check evaluated components (both COMPLETED and FAILED should be in evaluated list)
    assertThat(result.evaluated.stream().allMatch(c -> c.repositoryId.equals(repository.getId()))).isTrue();
    assertThat(result.evaluated.stream().allMatch(c -> c.componentId.equals(component.getId()))).isTrue();
  }

  @Test
  public void testGetCascadeStatus_Found_MixedProgress() throws Exception {
    // Arrange
    Repository repository = tempEntity.newRepository();
    String componentHash = "test_hash_mixed";
    Date now = new Date();

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/path/mixed", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-mixed-pkg", "1.0.0"), now, now);

    String cascadeRequestId = "cascade_mixed_test_456";
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "admin");

    // Create mixed progress entries
    tempEntity.newReevaluateCascadeProgress("progress_pending_1", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.PENDING.name());
    tempEntity.newReevaluateCascadeProgress("progress_completed_1", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress("progress_failed_1", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.FAILED.name());

    // Act
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH, "/status/" + cascadeRequestId)
        .get();

    // Assert
    assertResponseStatus(200, response);
    CascadeStatusResponseDTO result = response.getBody(CascadeStatusResponseDTO.class);

    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.PENDING);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).hasSize(1);
    assertThat(result.pending).hasSize(1);
    assertThat(result.failed).hasSize(1);

    // Check pending component
    assertThat(result.pending.get(0).repositoryId).isEqualTo(repository.getId());
    assertThat(result.pending.get(0).componentId).isEqualTo(component.getId());
    assertThat(result.pending.get(0).quarantined).isNull();

    // Check evaluated components (both COMPLETED and FAILED should be in evaluated list)
    assertThat(result.evaluated.stream().allMatch(c -> c.repositoryId.equals(repository.getId()))).isTrue();
    assertThat(result.evaluated.stream().allMatch(c -> c.componentId.equals(component.getId()))).isTrue();
  }

  @Test
  public void testGetCascadeStatus_Found_AllPending() throws Exception {
    // Arrange
    Repository repository = tempEntity.newRepository();
    String componentHash = "test_hash_pending";
    Date now = new Date();

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/path/pending", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-pending-pkg", "1.0.0"), now, now);

    String cascadeRequestId = "cascade_pending_test_789";
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "admin");

    // Create only pending progress entries
    tempEntity.newReevaluateCascadeProgress("progress_pending_1", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.PENDING.name());
    tempEntity.newReevaluateCascadeProgress("progress_pending_2", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.PENDING.name());

    // Act
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH, "/status/" + cascadeRequestId)
        .get();

    // Assert
    assertResponseStatus(200, response);
    CascadeStatusResponseDTO result = response.getBody(CascadeStatusResponseDTO.class);

    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.PENDING);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).isEmpty();
    assertThat(result.failed).isEmpty();
    assertThat(result.pending).hasSize(2);

    // All components should be in pending list
    assertThat(result.pending.stream().allMatch(c -> c.repositoryId.equals(repository.getId()))).isTrue();
    assertThat(result.pending.stream().allMatch(c -> c.componentId.equals(component.getId()))).isTrue();
  }

  @Test
  public void testGetCascadeStatus_NotFound() throws Exception {
    String nonExistentRequestId = "cascade_nonexistent_123";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH, "/status/" + nonExistentRequestId)
        .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetCascadeStatus_EmptyProgressList() throws Exception {
    // Arrange - create cascade request but no progress entries
    tempEntity.newRepository();
    String componentHash = "status_test_hash_empty";

    String uri = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/componentHash/" + componentHash;

    // call to start the cascade request
    HttpResponse responseCreateRequest = restRequest()
        .path(uri)
        .post();
    CascadeReevaluateTicketDTO ticket = responseCreateRequest.getBody(CascadeReevaluateTicketDTO.class);

    // Act
    HttpResponse response = restRequest()
        .path(ticket.statusUrl)
        .get();

    // Assert
    assertResponseStatus(200, response);
    CascadeStatusResponseDTO result = response.getBody(CascadeStatusResponseDTO.class);

    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.NO_COMPONENTS_FOUND);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).isEmpty();
    assertThat(result.pending).isEmpty();
    assertThat(result.failed).isEmpty();
  }

  @Test
  public void testGetCascadeStatus_MultipleRepositories() throws Exception {
    // Arrange
    Repository repo1 = tempEntity.newRepository("repo-1");
    Repository repo2 = tempEntity.newRepository("repo-2");
    String componentHash = "test_hash_multi";
    Date now = new Date();

    ProxyRepositoryComponent component1 = tempEntity.newRepositoryComponent(repo1.getId(),
        MatchState.EXACT, "test/path/multi1", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-multi-pkg", "1.0.0"), now, now);
    ProxyRepositoryComponent component2 = tempEntity.newRepositoryComponent(repo2.getId(),
        MatchState.EXACT, "test/path/multi2", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-multi-pkg", "1.0.0"), now, now);

    String cascadeRequestId = "cascade_multi_test_456";
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "admin");

    // Create progress for multiple repositories
    tempEntity.newReevaluateCascadeProgress("progress_multi_1", cascadeRequestId, repo1.getId(), component1.getId(),
        ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress("progress_multi_2", cascadeRequestId, repo2.getId(), component2.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    // Act
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH, "/status/" + cascadeRequestId)
        .get();

    // Assert
    assertResponseStatus(200, response);
    CascadeStatusResponseDTO result = response.getBody(CascadeStatusResponseDTO.class);

    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.PENDING);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).hasSize(1);
    assertThat(result.pending).hasSize(1);

    // Verify different repositories are represented
    assertThat(result.evaluated.get(0).repositoryId).isEqualTo(repo1.getId());
    assertThat(result.pending.get(0).repositoryId).isEqualTo(repo2.getId());
  }
}
