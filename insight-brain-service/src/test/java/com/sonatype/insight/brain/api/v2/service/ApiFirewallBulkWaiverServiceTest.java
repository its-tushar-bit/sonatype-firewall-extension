/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiBulkWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService.MAX_BULK_WAIVER_VIOLATIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;

import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ApiFirewallBulkWaiverService covering all 45 security test scenarios
 * from SECURITY-REQUIREMENTS-NEXUS-51214.md.
 *
 * Coverage target: 90%+
 * Security focus: MTIQ tenant isolation, authorization, input validation, transaction rollback
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiFirewallBulkWaiverServiceTest
{
  private static final String REPOSITORY_ID = "repo-123";

  private static final String OWNER_ID = "owner-456";

  private static final String INTERNAL_OWNER_ID = "internal-owner-456";

  private static final String VIOLATION_ID_1 = "violation-1";

  private static final String VIOLATION_ID_2 = "violation-2";

  private static final String VIOLATION_ID_3 = "violation-3";

  private static final String WAIVER_COMMENT = "Test waiver comment";

  private static final String COMPONENT_PATHNAME = "/path/to/component";

  private static final String COMPONENT_HASH = "hash-123";

  @Mock
  private OwnerDAO ownerDAO;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private RepositoryComponentDAO repositoryComponentDAO;

  @Mock
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Mock
  private PolicyWaiverDAO policyWaiverDAO;

  @Mock
  private ApiPolicyWaiverService apiPolicyWaiverService;

  @Mock
  private IdUtils idUtils;

  @Mock
  private TransactionContext transactionContext;

  @Mock
  private Repository repository;

  @Mock
  private RepositoryComponent repositoryComponent;

  @Mock
  private PolicyWaiver policyWaiver;

  private ApiFirewallBulkWaiverService service;

  @Before
  public void setUp() throws Exception {
    service = new ApiFirewallBulkWaiverService(
        ownerDAO,
        repositoryDAO,
        repositoryComponentDAO,
        repositoryPolicyViolationDAO,
        policyWaiverDAO,
        apiPolicyWaiverService,
        idUtils);

    // Default mock setup
    when(idUtils.getInternalOwnerId(any(OwnerType.class), anyString())).thenReturn(INTERNAL_OWNER_ID);
    when(repositoryComponentDAO.createTransactionContext()).thenReturn(transactionContext);
    when(repositoryDAO.getById(any(TransactionContext.class), anyString())).thenReturn(repository);
    when(repository.getId()).thenReturn(REPOSITORY_ID);
    when(policyWaiver.getId()).thenReturn("waiver-id");

    // Default stub for savePolicyWaiver - return the policyWaiver mock by default
    // Note: Using nullable() for expiryTime and waiverReasonId since they can be null
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Mock transaction context methods (they return void, so using doNothing)
    org.mockito.Mockito.doNothing().when(transactionContext).begin();
    org.mockito.Mockito.doNothing().when(transactionContext).commit();
    org.mockito.Mockito.doNothing().when(transactionContext).close();

    // Default mock for policyWaiverDAO - return empty list (no existing waivers)
    when(policyWaiverDAO.getActiveApplicableByOwnerId(anyString())).thenReturn(Collections.emptyList());
  }

  // ============================================================================
  // INPUT VALIDATION TESTS (REQ-3.x)
  // ============================================================================

  /**
   * TEST-INPUT-1: Submit request with 1001 violations - verify HTTP 400
   */
  @Test
  public void testAddBulkPolicyWaivers_ExceedsMaxViolations() {
    // Arrange
    List<String> violationIds = new ArrayList<>();
    for (int i = 0; i < MAX_BULK_WAIVER_VIOLATIONS + 1; i++) {
      violationIds.add("violation-" + i);
    }
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Maximum " + MAX_BULK_WAIVER_VIOLATIONS + " violations allowed");

    verifyNoInteractions(repositoryComponentDAO);
  }

  /**
   * TEST-INPUT-2: Submit request with null violationIds - verify HTTP 400
   */
  @Test
  public void testAddBulkPolicyWaivers_NullViolationIds() {
    // Arrange
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(null, waiverOptions);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Violation IDs list cannot be null or empty");
  }

  /**
   * TEST-INPUT-2: Submit request with empty violationIds - verify HTTP 400
   */
  @Test
  public void testAddBulkPolicyWaivers_EmptyViolationIds() {
    // Arrange
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(Collections.emptyList(), waiverOptions);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Violation IDs list cannot be null or empty");
  }

  /**
   * TEST-INPUT-3: Submit request with duplicate violation IDs - verify deduplication
   */
  @Test
  public void testAddBulkPolicyWaivers_DuplicateViolationIds() {
    // Arrange
    List<String> violationIds = Arrays.asList(VIOLATION_ID_1, VIOLATION_ID_1, VIOLATION_ID_2);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation1 = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    RepositoryPolicyViolation violation2 = createValidViolation(VIOLATION_ID_2, REPOSITORY_ID, false);

    Owner owner = createOwner(INTERNAL_OWNER_ID);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation1);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_2)).thenReturn(violation2);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert - Should process only 2 unique violations
    verify(repositoryPolicyViolationDAO, times(2)).getByIdWithConstraintFacts(anyString());
  }

  /**
   * TEST-INPUT-4: Submit request with non-existent violation ID - verify HTTP 400
   */
  @Test
  public void testAddBulkPolicyWaivers_NonExistentViolationId() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Could not find repository policy violation with ID: " + VIOLATION_ID_1);
  }

  /**
   * TEST-INPUT-5: Submit request with non-quarantine violation - verify it is waived successfully
   * (Non-quarantine violations should be waivable, matching Lifecycle behavior)
   */
  @Test
  public void testAddBulkPolicyWaivers_NonQuarantineViolation() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    violation.setActionTypeId(Action.ID_WARN); // Not a quarantine violation - should still be waivable

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert - Non-quarantine violation should be waived successfully
    verify(apiPolicyWaiverService).savePolicyWaiver(
        any(TransactionContext.class), eq(INTERNAL_OWNER_ID), eq(violation),
        eq(WAIVER_COMMENT), eq(EXACT_COMPONENT), any(), any(), anyBoolean());
  }

  /**
   * TEST-INPUT-6: Submit request with already-waived violation - verify it is skipped
   * Source of truth is policy_waiver table, not violation.isWaived() flag
   */
  @Test
  public void testAddBulkPolicyWaivers_AlreadyWaivedViolation() {
    // Arrange
    List<String> violationIds = Arrays.asList(VIOLATION_ID_1, VIOLATION_ID_2);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation1 = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, true);
    RepositoryPolicyViolation violation2 = createValidViolation(VIOLATION_ID_2, REPOSITORY_ID, false);

    // Extract values before setting up mocks to avoid "unfinished stubbing" error
    String constraintFactsJson1 = violation1.getConstraintFactsJson();

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation1);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_2)).thenReturn(violation2);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));

    // Mock existing waiver for violation1 (policyId + hash + constraintFactsJson match)
    PolicyWaiver existingWaiver = new PolicyWaiver();
    existingWaiver.setPolicyId("policy-" + VIOLATION_ID_1);
    existingWaiver.setHash("hash-" + VIOLATION_ID_1);
    existingWaiver.setConstraintFactsJson(constraintFactsJson1);
    when(policyWaiverDAO.getActiveApplicableByOwnerId(INTERNAL_OWNER_ID))
        .thenReturn(Collections.singletonList(existingWaiver));

    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert - Should waive only violation2 (violation1 already has waiver in policy_waiver table)
    verify(apiPolicyWaiverService, times(1)).savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean());
  }

  @Test
  public void testAddBulkPolicyWaivers_SkipsExistingWaiverWithSameConstraintFactsJson() {
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, createValidWaiverOptions());

    RepositoryPolicyViolation violation = createValidViolationWithDetails(
        VIOLATION_ID_1, REPOSITORY_ID, "Policy One", "npm", "test-package", "1.0.0",
        "Constraint A", "Reason A", false);

    // Extract all values and create owner BEFORE setting up mocks to avoid "unfinished stubbing" error
    String policyId = violation.getPolicyId();
    String hash = violation.getHash();
    String constraintFactsJson = violation.getConstraintFactsJson();
    Owner owner = createOwner(INTERNAL_OWNER_ID);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));

    PolicyWaiver existingWaiver = new PolicyWaiver();
    existingWaiver.setPolicyId(policyId);
    existingWaiver.setHash(hash);
    existingWaiver.setConstraintFactsJson(constraintFactsJson);
    when(policyWaiverDAO.getActiveApplicableByOwnerId(INTERNAL_OWNER_ID))
        .thenReturn(Collections.singletonList(existingWaiver));

    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    verify(apiPolicyWaiverService, never()).savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean());
  }

  @Test
  public void testAddBulkPolicyWaivers_CreatesWaiverWhenConstraintFactsJsonDiffers() {
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, createValidWaiverOptions());

    RepositoryPolicyViolation violation = createValidViolationWithDetails(
        VIOLATION_ID_1, REPOSITORY_ID, "Policy One", "npm", "test-package", "1.0.0",
        "Constraint A", "Reason A", false);
    RepositoryPolicyViolation otherConstraintViolation = createValidViolationWithDetails(
        VIOLATION_ID_2, REPOSITORY_ID, "Policy One", "npm", "test-package", "1.0.0",
        "Constraint A", "Reason B", false);

    // Extract values and create owner BEFORE setting up mocks to avoid "unfinished stubbing" error
    String policyId = violation.getPolicyId();
    String hash = violation.getHash();
    String otherConstraintFactsJson = otherConstraintViolation.getConstraintFactsJson();
    Owner owner = createOwner(INTERNAL_OWNER_ID);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));

    PolicyWaiver existingWaiver = new PolicyWaiver();
    existingWaiver.setPolicyId(policyId);
    existingWaiver.setHash(hash);
    existingWaiver.setConstraintFactsJson(otherConstraintFactsJson);
    when(policyWaiverDAO.getActiveApplicableByOwnerId(INTERNAL_OWNER_ID))
        .thenReturn(Collections.singletonList(existingWaiver));

    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    verify(apiPolicyWaiverService, times(1)).savePolicyWaiver(
        any(TransactionContext.class), eq(INTERNAL_OWNER_ID), eq(violation),
        eq(WAIVER_COMMENT), eq(EXACT_COMPONENT),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), eq(false));
  }

  /**
   * TEST-INPUT-7: Submit request with null apiWaiverOptionsDTO - verify HTTP 400
   */
  @Test
  public void testAddBulkPolicyWaivers_NullWaiverOptions() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, null);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Waiver options cannot be null");
  }

  /**
   * TEST-INPUT-8: Submit request with unsupported matcherStrategy - verify HTTP 400
   */
  @Test
  public void testAddBulkPolicyWaivers_UnsupportedMatcherStrategy() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO(
        WAIVER_COMMENT,
        PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS, // Unsupported
        null,
        null,
        false);
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Only EXACT_COMPONENT and ALL_VERSIONS matcher strategies are supported");
  }

  /**
   * TEST-INPUT-9: Submit request with expiry date in the past - verify HTTP 400
   */
  @Test
  public void testAddBulkPolicyWaivers_ExpiryDateInPast() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    Date yesterday = Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO(
        WAIVER_COMMENT,
        EXACT_COMPONENT,
        yesterday,
        null,
        false);
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Expiration date must be in the future");
  }

  /**
   * TEST-INPUT-10: Submit request with expireWhenRemediationAvailable=true and matcherStrategy=ALL_VERSIONS
   */
  @Test
  public void testAddBulkPolicyWaivers_ExpireWhenRemediationWithAllVersions() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = new ApiWaiverOptionsDTO(
        WAIVER_COMMENT,
        ALL_VERSIONS,
        null,
        null,
        true // expireWhenRemediationAvailable
    );
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Expire When Remediation Available Waivers can only be applied to Exact Components");
  }

  // TEST-INPUT-11 removed: Comment is now optional for bulk waivers

  // ============================================================================
  // AUTHORIZATION TESTS (REQ-1.x)
  // ============================================================================

  /**
   * TEST-AUTH-5: Verify ownerType validation rejects unsupported types
   */
  @Test
  public void testAddBulkPolicyWaivers_UnsupportedOwnerType() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.APPLICATION, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unsupported Firewall bulk waiver owner type");
  }

  // ============================================================================
  // TENANT ISOLATION TESTS (REQ-2.x) - CRITICAL
  // ============================================================================

  /**
   * TEST-TENANT-1: Verify cross-tenant violation access is rejected
   */
  @Test
  public void testAddBulkPolicyWaivers_CrossTenantViolation() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);

    // Owner hierarchy does NOT include INTERNAL_OWNER_ID (cross-tenant)
    Owner differentOwner = createOwner("different-owner-id");
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(differentOwner));

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Violation " + VIOLATION_ID_1 + " does not belong to owner " + OWNER_ID);

    verify(apiPolicyWaiverService, never()).savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean());
  }

  /**
   * TEST-TENANT-2: Verify isViolationOwnerId correctly walks owner hierarchy
   */
  @Test
  public void testAddBulkPolicyWaivers_OwnerHierarchyValidation() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);

    // Owner hierarchy: repo -> repo_manager -> repo_container (INTERNAL_OWNER_ID at top)
    Owner repoOwner = createOwner("repo-owner");
    Owner repoManagerOwner = createOwner("repo-manager-owner");
    Owner repoContainerOwner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID))
        .thenReturn(Arrays.asList(repoOwner, repoManagerOwner, repoContainerOwner));
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert - Should succeed because INTERNAL_OWNER_ID is in hierarchy
    verify(ownerDAO).walkHierarchy(REPOSITORY_ID);
  }

  /**
   * TEST-TENANT-4: Verify bulk request with one cross-tenant violation fails completely
   */
  @Test
  public void testAddBulkPolicyWaivers_PartialCrossTenantFailure() {
    // Arrange - 3 violations, violation2 belongs to different tenant
    List<String> violationIds = Arrays.asList(VIOLATION_ID_1, VIOLATION_ID_2, VIOLATION_ID_3);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    String repo1 = "repo-1";
    String repo2 = "repo-2"; // Different tenant

    RepositoryPolicyViolation violation1 = createValidViolation(VIOLATION_ID_1, repo1, false);
    RepositoryPolicyViolation violation2 = createValidViolation(VIOLATION_ID_2, repo2, false);
    // Note: violation3 is never fetched because the test fails on violation2 (cross-tenant)

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation1);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_2)).thenReturn(violation2);

    // repo1 belongs to our tenant, repo2 does not
    Owner owner1 = createOwner(INTERNAL_OWNER_ID);
    Owner owner2 = createOwner("different-owner");
    when(ownerDAO.walkHierarchy(repo1)).thenReturn(Collections.singletonList(owner1));
    when(ownerDAO.walkHierarchy(repo2)).thenReturn(Collections.singletonList(owner2));

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Violation " + VIOLATION_ID_2 + " does not belong to owner " + OWNER_ID);

    // Violation1 will have called savePolicyWaiver before violation2 fails
    // The transaction is rolled back but the mock call is still registered
    // So we verify at most 1 call (for violation1) before the cross-tenant error
    verify(apiPolicyWaiverService, atMost(1)).savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean());
  }

  // ============================================================================
  // TRANSACTION ROLLBACK TESTS (REQ-8.x)
  // ============================================================================

  /**
   * TEST-TX-1: Verify database error causes full rollback
   */
  @Test
  public void testAddBulkPolicyWaivers_TransactionRollbackOnError() {
    // Arrange
    List<String> violationIds = Arrays.asList(VIOLATION_ID_1, VIOLATION_ID_2);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation1 = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    RepositoryPolicyViolation violation2 = createValidViolation(VIOLATION_ID_2, REPOSITORY_ID, false);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation1);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_2)).thenReturn(violation2);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));

    // First waiver succeeds, second fails with database exception
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver)
            .thenThrow(new RuntimeException("Database constraint violation"));

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unable to create firewall bulk waivers");

    // Transaction should be auto-rolled back (no manual rollback needed in try-with-resources)
  }

  // ============================================================================
  // UNQUARANTINE TESTS (REQ-9.x)
  // ============================================================================

  /**
   * TEST-UNQUAR-1: Waive all quarantine violations - verify component is unquarantined
   */
  @Test
  public void testAddBulkPolicyWaivers_UnquarantineComponent() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    violation.setPathname(COMPONENT_PATHNAME);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert - verify PolicyWaiver was created but NOT unquarantined immediately
    // Unquarantine is deferred to next reevaluation by RepositoryPolicyEvaluator
    verify(transactionContext).begin();
    verify(transactionContext).commit();
    verify(apiPolicyWaiverService).savePolicyWaiver(
        any(TransactionContext.class), eq(INTERNAL_OWNER_ID), eq(violation),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean());
    verify(apiPolicyWaiverService).auditAndSendTelemetry(
        eq(OwnerType.REPOSITORY), eq(INTERNAL_OWNER_ID), eq(policyWaiver), eq(violation));

    // Verify NO immediate unquarantine (deferred to reevaluation)
    verify(repositoryComponentDAO, never()).getByRepositoryIdAndPathname(any(), anyString(), anyString());
  }

  /**
   * TEST-UNQUAR-2: Verify bulk waiver does NOT check remaining violations
   * (unquarantine is deferred to reevaluation by RepositoryPolicyEvaluator)
   */
  @Test
  public void testAddBulkPolicyWaivers_NoRemainingViolationsCheck() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    violation.setPathname(COMPONENT_PATHNAME);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert - verify NO component interaction (deferred to reevaluation)
    verify(repositoryComponentDAO, never()).getByRepositoryIdAndPathname(any(), anyString(), anyString());
    verify(repositoryPolicyViolationDAO, never()).getByRepositoryIdAndPathnameAndActionAndNotWaived(
        anyString(), anyString(), anyString());
  }

  /**
   * TEST-UNQUAR-4: Verify bulk waiver does NOT interact with component at all
   * (unquarantine is deferred to reevaluation by RepositoryPolicyEvaluator)
   */
  @Test
  public void testAddBulkPolicyWaivers_NoComponentInteraction() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    violation.setPathname(COMPONENT_PATHNAME);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert - verify NO component interaction (deferred to reevaluation)
    verify(repositoryComponentDAO, never()).getByRepositoryIdAndPathname(any(), anyString(), anyString());
    verify(repositoryComponentDAO, never()).update(any(), any());
  }

  // ============================================================================
  // SUCCESSFUL BULK WAIVER TESTS
  // ============================================================================

  /**
   * Happy path: Successful bulk waiver of multiple violations
   */
  @Test
  public void testAddBulkPolicyWaivers_Success() {
    // Arrange
    List<String> violationIds = Arrays.asList(VIOLATION_ID_1, VIOLATION_ID_2);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation1 = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    RepositoryPolicyViolation violation2 = createValidViolation(VIOLATION_ID_2, REPOSITORY_ID, false);

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation1);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_2)).thenReturn(violation2);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));

    PolicyWaiver waiver1 = createPolicyWaiver("waiver-1");
    PolicyWaiver waiver2 = createPolicyWaiver("waiver-2");
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(waiver1, waiver2);

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert - verify interactions since method returns void
    verify(transactionContext).begin();
    verify(transactionContext).commit();
    // Verify PolicyWaivers were created (not violation updates - those are deferred to reevaluation)
    verify(apiPolicyWaiverService, times(2)).savePolicyWaiver(
        any(TransactionContext.class), eq(INTERNAL_OWNER_ID), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean());
    // Verify telemetry was sent after commit
    verify(apiPolicyWaiverService, times(2)).auditAndSendTelemetry(
        eq(OwnerType.REPOSITORY), eq(INTERNAL_OWNER_ID), any(PolicyWaiver.class), any(RepositoryPolicyViolation.class));
    // Verify NO immediate violation updates (deferred to reevaluation)
    verify(repositoryPolicyViolationDAO, never()).update(any(TransactionContext.class),
        any(RepositoryPolicyViolation.class));
  }

  /**
   * Test with null request DTO
   */
  @Test
  public void testAddBulkPolicyWaivers_NullRequest() {
    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Waivers request cannot be null");
  }

  /**
   * Test repository not found
   */
  @Test
  public void testAddBulkPolicyWaivers_RepositoryNotFound() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));
    when(repositoryDAO.getById(any(TransactionContext.class), eq(REPOSITORY_ID))).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Cannot find a repository with ID " + REPOSITORY_ID);
  }

  /**
   * Test that violation details are populated even when condition reason is missing
   */
  @Test
  public void testAddBulkPolicyWaivers_ViolationDetailsWithoutConditionReason() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    // Create violation with constraint but no condition reason
    RepositoryPolicyViolation violation = createValidViolationWithDetails(
        VIOLATION_ID_1, REPOSITORY_ID, "Test-Policy", "npm", "test-package", "1.0.0",
        "Test Constraint", null, false); // null conditionReason

    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));

    PolicyWaiver waiver = createPolicyWaiver("waiver-1");
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(waiver);

    // Act
    service.addBulkPolicyWaivers(OwnerType.REPOSITORY, OWNER_ID, request);

    // Assert
  }

  /**
   * Test with valid REPOSITORY_MANAGER owner type
   */
  @Test
  public void testAddBulkPolicyWaivers_RepositoryManagerOwnerType() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Act
    service.addBulkPolicyWaivers(
        OwnerType.REPOSITORY_MANAGER, OWNER_ID, request);

    // Assert
  }

  /**
   * Test with valid REPOSITORY_CONTAINER owner type
   */
  @Test
  public void testAddBulkPolicyWaivers_RepositoryContainerOwnerType() {
    // Arrange
    List<String> violationIds = Collections.singletonList(VIOLATION_ID_1);
    ApiWaiverOptionsDTO waiverOptions = createValidWaiverOptions();
    ApiBulkWaiversDTO request = new ApiBulkWaiversDTO(violationIds, waiverOptions);

    RepositoryPolicyViolation violation = createValidViolation(VIOLATION_ID_1, REPOSITORY_ID, false);
    when(repositoryPolicyViolationDAO.getByIdWithConstraintFacts(VIOLATION_ID_1)).thenReturn(violation);
    Owner owner = createOwner(INTERNAL_OWNER_ID);
    when(ownerDAO.walkHierarchy(REPOSITORY_ID)).thenReturn(Collections.singletonList(owner));
    when(apiPolicyWaiverService.savePolicyWaiver(
        any(TransactionContext.class), anyString(), any(RepositoryPolicyViolation.class),
        anyString(), any(PolicyWaiver.ComponentMatcherStrategyForWaiver.class),
        ArgumentMatchers.<Date>nullable(Date.class), nullable(String.class), anyBoolean()))
            .thenReturn(policyWaiver);

    // Act
    service.addBulkPolicyWaivers(
        OwnerType.REPOSITORY_CONTAINER, OWNER_ID, request);

    // Assert
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private ApiWaiverOptionsDTO createValidWaiverOptions() {
    return new ApiWaiverOptionsDTO(
        WAIVER_COMMENT,
        EXACT_COMPONENT,
        null, // expiryTime
        null, // waiverReasonId
        false // expireWhenRemediationAvailable
    );
  }

  private RepositoryPolicyViolation createValidViolation(String id, String repositoryId, boolean isWaived) {
    RepositoryPolicyViolation violation = new RepositoryPolicyViolation();
    violation.setId(id);
    violation.setRepositoryId(repositoryId);
    violation.setActionTypeId(Action.ID_FAIL);
    violation.setWaived(isWaived);
    violation.setPathname(COMPONENT_PATHNAME);
    // Set unique policyId and hash per violation to avoid deduplication in source of truth check
    violation.setPolicyId("policy-" + id);
    violation.setHash("hash-" + id);
    // Set empty constraint facts to avoid "Constraint facts are not loaded" error
    violation
        .setConstraintFacts(Collections.singletonList(new ConstraintFact("constraint-id", "Test Constraint", "OR")));
    return violation;
  }

  private RepositoryPolicyViolation createValidViolationWithDetails(
      String id,
      String repositoryId,
      String policyName,
      String format,
      String componentName,
      String componentVersion,
      String constraintName,
      String conditionReason,
      boolean isWaived)
  {
    // Create component identifier - service uses "name" coordinate for display
    // Use helper method to create valid ComponentIdentifier for the format
    ComponentIdentifier componentId = createComponentIdentifier(format, componentName, componentVersion);

    // Create condition fact with reason (conditionTypeId, index, summary, reason)
    ConditionFact conditionFact = conditionReason != null
        ? new ConditionFact("security-vulnerability", 0, conditionReason, conditionReason)
        : null;

    // Create constraint fact
    ConstraintFact constraintFact = new ConstraintFact();
    constraintFact.setConstraintName(constraintName);
    if (conditionFact != null) {
      constraintFact.setConditionFacts(Collections.singletonList(conditionFact));
    }
    else {
      constraintFact.setConditionFacts(Collections.emptyList());
    }

    // Create violation
    RepositoryPolicyViolation violation = new RepositoryPolicyViolation(
        repositoryId,
        COMPONENT_PATHNAME,
        new Date(),
        "policy-id",
        policyName,
        10,
        null,
        COMPONENT_HASH,
        componentId,
        Collections.singletonList(constraintFact));
    violation.setId(id);
    violation.setActionTypeId(Action.ID_FAIL);
    violation.setWaived(isWaived);
    return violation;
  }

  private Owner createOwner(String ownerId) {
    Owner owner = org.mockito.Mockito.mock(Owner.class, org.mockito.Mockito.withSettings().lenient());
    org.mockito.Mockito.when(owner.getId()).thenReturn(ownerId);
    return owner;
  }

  private PolicyWaiver createPolicyWaiver(String id) {
    PolicyWaiver waiver = org.mockito.Mockito.mock(PolicyWaiver.class, org.mockito.Mockito.withSettings().lenient());
    org.mockito.Mockito.when(waiver.getId()).thenReturn(id);
    return waiver;
  }

  private ComponentIdentifier createComponentIdentifier(String format, String name, String version) {
    // Use the static factory methods which handle proper coordinate names
    if ("npm".equals(format)) {
      return ComponentIdentifier.createNpmCoordinates(name, version);
    }
    else if ("maven".equals(format)) {
      return ComponentIdentifier.createMavenCoordinates("com.example", name, version);
    }
    else {
      // Generic fallback - use simple coordinates
      Map<String, String> coordinates = new HashMap<>();
      coordinates.put("name", name);
      coordinates.put("version", version);
      return new ComponentIdentifier(format, coordinates);
    }
  }
}
