/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.Repository;
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
    assertThat(result.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");

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
    assertThat(result.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");

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
    assertThat(result.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");

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
    assertThat(result.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");

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
}
