/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.CascadeStatusResponseDTO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiFirewallCascadeServiceTest extends AbstractComponentTest
{
  @Inject
  private ApiFirewallCascadeService cascadeService;

  @Inject
  private ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAO;

  @Inject
  private ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testInitiateCascadeReevaluation_Success() {
    // Arrange
    Repository repository = tempEntity.newRepository("test-repo-cascade");
    String componentHash = "cascade_test_hash";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/component/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-cascade-pkg", "1.0.0"), now, now);

    // Act
    CascadeReevaluateTicketDTO result = cascadeService.initiateCascadeReevaluation(componentHash);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");

    // Verify cascade request was created in database
    List<ReevaluateCascadeRequest> cascadeRequests = reevaluateCascadeRequestDAO.getByComponentHash(componentHash);
    assertThat(cascadeRequests).hasSize(1);

    ReevaluateCascadeRequest cascadeRequest = cascadeRequests.get(0);
    assertThat(cascadeRequest.getComponentReferenceHash()).isEqualTo(componentHash);
    assertThat(cascadeRequest.getCreatedByUsername()).isEqualTo("testuser");
  }

  @Test
  public void testInitiateCascadeReevaluation_MultipleRepositories() {
    // Arrange - Create multiple repositories with the same component
    Repository repo1 = tempEntity.newRepository("test-repo-1");
    Repository repo2 = tempEntity.newRepository("test-repo-2");
    String componentHash = "multi_repo_hash";
    Date now = new Date();

    // Add same component to both repositories
    tempEntity.newRepositoryComponent(repo1.getId(),
        MatchState.EXACT, "test/component/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-pkg", "1.0.0"), now, now);
    tempEntity.newRepositoryComponent(repo2.getId(),
        MatchState.EXACT, "test/component/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-pkg", "1.0.0"), now, now);

    // Act
    CascadeReevaluateTicketDTO result = cascadeService.initiateCascadeReevaluation(componentHash);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");

    // Verify cascade request was created
    List<ReevaluateCascadeRequest> cascadeRequests = reevaluateCascadeRequestDAO.getByComponentHash(componentHash);
    assertThat(cascadeRequests).hasSize(1);

    ReevaluateCascadeRequest cascadeRequest = cascadeRequests.get(0);
    assertThat(cascadeRequest.getComponentReferenceHash()).isEqualTo(componentHash);
  }

  @Test
  public void testInitiateCascadeReevaluation_ComponentNotFound() {
    // Arrange
    String nonExistentHash = "non_existent_hash";

    CascadeReevaluateTicketDTO result = cascadeService.initiateCascadeReevaluation(nonExistentHash);

    // Assert - Request is created successfully, task will set NO_COMPONENTS_FOUND status
    assertThat(result).isNotNull();
    assertThat(result.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");

    List<ReevaluateCascadeRequest> cascadeRequests = reevaluateCascadeRequestDAO.getByComponentHash(nonExistentHash);
    assertThat(cascadeRequests).hasSize(1);

    ReevaluateCascadeRequest cascadeRequest = cascadeRequests.get(0);
    assertThat(cascadeRequest.getComponentReferenceHash()).isEqualTo(nonExistentHash);
  }

  @Test
  public void testInitiateCascadeReevaluation_BlankComponentHash() {
    // Act & Assert
    assertThatThrownBy(() -> cascadeService.initiateCascadeReevaluation(""))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Component hash is required");
  }

  @Test
  public void testInitiateCascadeReevaluation_NullComponentHash() {
    // Act & Assert
    assertThatThrownBy(() -> cascadeService.initiateCascadeReevaluation(null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Component hash is required");
  }

  @Test
  public void testInitiateCascadeReevaluation_ValidatesCascadeRequestCreation() {
    // Arrange
    Repository repository = tempEntity.newRepository("test-repo-validation");
    String componentHash = "validation_test_hash";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/component/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("validation-pkg", "1.0.0"), now, now);

    // Ensure no cascade requests exist initially
    List<ReevaluateCascadeRequest> initialRequests = reevaluateCascadeRequestDAO.getByComponentHash(componentHash);
    assertThat(initialRequests).isEmpty();

    // Act
    CascadeReevaluateTicketDTO result = cascadeService.initiateCascadeReevaluation(componentHash);

    // Assert DTO response
    assertThat(result).isNotNull();
    assertThat(result.statusUrl).startsWith(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/");

    // Assert cascade request was properly created
    List<ReevaluateCascadeRequest> createdRequests = reevaluateCascadeRequestDAO.getByComponentHash(componentHash);
    assertThat(createdRequests).hasSize(1);

    ReevaluateCascadeRequest request = createdRequests.get(0);
    assertThat(request.getComponentReferenceHash()).isEqualTo(componentHash);
    assertThat(request.getCreatedAt()).isNotNull();
    assertThat(request.getCreatedByUsername()).isEqualTo("testuser");
  }

  @Test
  public void testInitiateCascadeReevaluation_InvalidLicense_MissingFirewallFeature() {
    // Arrange
    Repository repository = tempEntity.newRepository("test-repo-license");
    String componentHash = "license_test_hash";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/component/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("license-test-pkg", "1.0.0"), now, now);

    // Mock license to not have FIREWALL_AUTO_UNQUARANTINE feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    // Act & Assert
    assertThatThrownBy(() -> cascadeService.initiateCascadeReevaluation(componentHash))
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void testInitiateCascadeReevaluation_InvalidLicense_MissingReleaseIntegrityFeature() {
    // Arrange
    Repository repository = tempEntity.newRepository("test-repo-license2");
    String componentHash = "license_test_hash2";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/component/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("license-test-pkg2", "1.0.0"), now, now);

    // Mock license to not have RELEASE_INTEGRITY feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    // Act & Assert
    assertThatThrownBy(() -> cascadeService.initiateCascadeReevaluation(componentHash))
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void testInitiateCascadeReevaluation_InvalidLicense_MissingBothFeatures() {
    // Arrange
    Repository repository = tempEntity.newRepository("test-repo-license3");
    String componentHash = "license_test_hash3";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/component/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("license-test-pkg3", "1.0.0"), now, now);

    // Mock license to not have either required feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    // Act & Assert
    assertThatThrownBy(() -> cascadeService.initiateCascadeReevaluation(componentHash))
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void testGetCascadeStatus_AllCompleted() {
    // Arrange
    Repository repository = tempEntity.newRepository("test-repo-status");
    String componentHash = "status_test_hash";
    Date now = new Date();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/status/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-status-pkg", "1.0.0"), now, now);

    String cascadeRequestId = "cascade_status_test";
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "testuser",
        ReevaluateCascadeRequestStatus.COMPLETED);

    // Create completed progress entries
    tempEntity.newReevaluateCascadeProgress("progress_completed_1", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress("progress_completed_2", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.COMPLETED.name());

    // Act
    CascadeStatusResponseDTO result = cascadeService.getCascadeStatus(cascadeRequestId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.COMPLETED);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).hasSize(2);
    assertThat(result.pending).isEmpty();
    assertThat(result.failed).isEmpty();

    // Verify evaluated component details
    assertThat(result.evaluated.get(0).repositoryId).isEqualTo(repository.getId());
    assertThat(result.evaluated.get(0).componentId).isEqualTo(component.getId());
    assertThat(result.evaluated.get(0).repositoryManagerId).isEqualTo(repository.getRepositoryManagerId());
  }

  @Test
  public void testGetCascadeStatus_MixedProgress() {
    // Arrange
    Repository repository = tempEntity.newRepository("test-repo-mixed");
    String componentHash = "mixed_test_hash";
    Date now = new Date();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/mixed/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-mixed-pkg", "1.0.0"), now, now);

    String cascadeRequestId = "cascade_mixed_test";
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "testuser");

    // Create mixed progress entries
    ReevaluateCascadeProgress progressPending =
        tempEntity.newReevaluateCascadeProgress("progress_pending", cascadeRequestId, repository.getId(),
            component.getId(),
            ReevaluateCascadeProgressStatus.PENDING.name());
    progressPending.setQuarantined(false);
    reevaluateCascadeProgressDAO.update(progressPending);

    ReevaluateCascadeProgress progressCompleted =
        tempEntity.newReevaluateCascadeProgress("progress_completed", cascadeRequestId, repository.getId(),
            component.getId(), ReevaluateCascadeProgressStatus.COMPLETED.name());
    progressCompleted.setQuarantined(true);
    reevaluateCascadeProgressDAO.update(progressCompleted);

    ReevaluateCascadeProgress progressFailed =
        tempEntity.newReevaluateCascadeProgress("progress_failed", cascadeRequestId, repository.getId(),
            component.getId(),
            ReevaluateCascadeProgressStatus.FAILED.name());
    progressFailed.setQuarantined(false);
    reevaluateCascadeProgressDAO.update(progressFailed);

    // Act
    CascadeStatusResponseDTO result = cascadeService.getCascadeStatus(cascadeRequestId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.PENDING);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).hasSize(1);
    assertThat(result.failed).hasSize(1);
    assertThat(result.pending).hasSize(1);

    // Verify pending component has quarantine field
    assertThat(result.pending.get(0).repositoryId).isEqualTo(repository.getId());
    assertThat(result.pending.get(0).componentId).isEqualTo(component.getId());
    assertThat(result.pending.get(0).quarantined).isEqualTo(false);

    // Verify completed component has quarantine field
    assertThat(result.evaluated.get(0).repositoryId).isEqualTo(repository.getId());
    assertThat(result.evaluated.get(0).componentId).isEqualTo(component.getId());
    assertThat(result.evaluated.get(0).quarantined).isEqualTo(true);

    // Verify failed component does NOT have quarantine field (should be null)
    assertThat(result.failed.get(0).repositoryId).isEqualTo(repository.getId());
    assertThat(result.failed.get(0).componentId).isEqualTo(component.getId());
    assertThat(result.failed.get(0).quarantined).isNull();
  }

  @Test
  public void testGetCascadeStatus_AllPending() {
    // Arrange
    Repository repository = tempEntity.newRepository("test-repo-pending");
    String componentHash = "pending_test_hash";
    Date now = new Date();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/pending/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-pending-pkg", "1.0.0"), now, now);

    String cascadeRequestId = "cascade_pending_test";
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "testuser");

    // Create only pending progress entries
    tempEntity.newReevaluateCascadeProgress("progress_pending_1", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.PENDING.name());
    tempEntity.newReevaluateCascadeProgress("progress_pending_2", cascadeRequestId, repository.getId(),
        component.getId(), ReevaluateCascadeProgressStatus.PENDING.name());

    // Act
    CascadeStatusResponseDTO result = cascadeService.getCascadeStatus(cascadeRequestId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.PENDING);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).isEmpty();
    assertThat(result.failed).isEmpty();
    assertThat(result.pending).hasSize(2);

    // All should be in pending list
    assertThat(result.pending.stream().allMatch(c ->
        c.repositoryId.equals(repository.getId()) && c.componentId.equals(component.getId()))).isTrue();
  }

  @Test
  public void testGetCascadeStatus_EmptyProgressList() {
    // Arrange - create cascade request but no progress entries
    tempEntity.newRepository("test-repo-empty");
    String componentHash = "empty_test_hash";
    String cascadeRequestId = "cascade_empty_test";

    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "testuser");
    // No progress entries created

    // Act
    CascadeStatusResponseDTO result = cascadeService.getCascadeStatus(cascadeRequestId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.PENDING);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).isEmpty();
    assertThat(result.pending).isEmpty();
    assertThat(result.failed).isEmpty();
  }

  @Test
  public void testGetCascadeStatus_RequestNotFound() {
    // Act & Assert
    String nonExistentRequestId = "nonexistent_cascade_request";

    assertThatThrownBy(() -> cascadeService.getCascadeStatus(nonExistentRequestId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Cascade request not found: " + nonExistentRequestId);
  }

  @Test
  public void testGetCascadeStatus_MultipleRepositories() {
    // Arrange
    Repository repo1 = tempEntity.newRepository("test-repo-multi-1");
    Repository repo2 = tempEntity.newRepository("test-repo-multi-2");
    String componentHash = "multi_repo_test_hash";
    Date now = new Date();

    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repo1.getId(),
        MatchState.EXACT, "test/multi1/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-multi-pkg", "1.0.0"), now, now);
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo2.getId(),
        MatchState.EXACT, "test/multi2/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-multi-pkg", "1.0.0"), now, now);

    String cascadeRequestId = "cascade_multi_repo_test";
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, componentHash, "testuser"); // ALL scope

    // Create progress for multiple repositories
    tempEntity.newReevaluateCascadeProgress("progress_repo1", cascadeRequestId, repo1.getId(), component1.getId(),
        ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress("progress_repo2", cascadeRequestId, repo2.getId(), component2.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    // Act
    CascadeStatusResponseDTO result = cascadeService.getCascadeStatus(cascadeRequestId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.status).isEqualTo(ReevaluateCascadeRequestStatus.PENDING);
    assertThat(result.referenceComponentHash).isEqualTo(componentHash);
    assertThat(result.evaluated).hasSize(1);
    assertThat(result.pending).hasSize(1);
    assertThat(result.failed).isEmpty();

    // Verify different repositories are represented
    assertThat(result.evaluated.get(0).repositoryId).isEqualTo(repo1.getId());
    assertThat(result.evaluated.get(0).repositoryManagerId).isEqualTo(repo1.getRepositoryManagerId());
    assertThat(result.pending.get(0).repositoryId).isEqualTo(repo2.getId());
    assertThat(result.pending.get(0).repositoryManagerId).isEqualTo(repo2.getRepositoryManagerId());
    assertThat(result.failed).isEmpty();
  }
}
