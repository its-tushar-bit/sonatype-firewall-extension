/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationError;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationError.MoveOrganizationValidationErrorType;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationWarning;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationWarning.MoveOrganizationValidationWarningType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.ConflictException;

import org.junit.Test;

import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class MoveOrganizationServiceTest
    extends AbstractComponentTest
{
  @Inject
  public OrganizationDAO organizationDAO;

  @Inject
  public PolicyDAO policyDAO;

  @Inject
  public LabelDAO labelDAO;

  @Inject
  public ApplicationDAO applicationDAO;

  @Inject
  public MoveOrganizationService moveOrganizationService;

  @Inject
  private AsyncEventBus eventBus;

  private final boolean failEarlyOnError = false;

  @Test
  public void testMoveOrganization() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(moveOrganizationResponseDTO.warnings).isEmpty();
    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_WithChildren() {
    Organization sourceOrg = tempEntity.newOrganization();
    Organization childOrg = tempEntity.newOrganization(sourceOrg);
    Organization destOrg = tempEntity.newOrganization();
    // Sanity checks
    assertThat(organizationDAO.getById(sourceOrg.getId()).getParentOrganizationId())
        .isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(organizationDAO.getById(childOrg.getId()).getParentOrganizationId()).isEqualTo(sourceOrg.getId());

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(sourceOrg.getId(), destOrg.getId(), failEarlyOnError);
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(moveOrganizationResponseDTO.warnings).isEmpty();
    assertThat(organizationDAO.getById(sourceOrg.getId()).getParentOrganizationId()).isEqualTo(destOrg.getId());
  }

  @Test
  public void testMoveOrganization_PostEvents() throws InterruptedException {
    TestEventHandler<OwnerEvent> handler = new TestEventHandler<>(new CountDownLatch(1), OwnerEvent.class);
    eventBus.register(handler);

    try {
      List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
      Organization organization = tempEntity.newOrganization();
      MoveOrganizationResponseDTO moveOrganizationResponseDTO = moveOrganizationService
          .moveOrganization(organizations.get(0).getId(), organization.getId(), failEarlyOnError);
      assertThat(moveOrganizationResponseDTO).isNotNull();
      assertThat(moveOrganizationResponseDTO.errors).isEmpty();
      assertThat(organizationDAO.getById(organizations.get(0).getId()).getParentOrganizationId())
          .isEqualTo(organization.getId());
      assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
      assertThat(handler.getEvent().action).isEqualTo(UPDATED);
      assertThat(handler.getEvent().ownerId).isEqualTo(organizations.get(0).getId());
      assertThat(handler.getEvent().owner.getId()).isEqualTo(organizations.get(0).getId());
      assertThat(handler.getEvent().owner.getParentOwnerId()).isEqualTo(organization.getId());
    }
    finally {
      eventBus.unregister(handler);
    }
  }

  @Test
  public void testMoveOrganization_NewParentIsSameAsOldParent() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(organizations.get(0).getId(), organizations.get(1).getId(),
                true))
        .withMessage(String.format("New parent org %s is already set and in use as the parent of org %s",
            organizations.get(1).getName(), organizations.get(0).getName()));

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizations.get(0).getId(), organizations.get(1).getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.PARENT_HIERARCHY);

    assertThat(validationError.message)
        .isEqualTo(String.format("New parent org %s is already set and in use as the parent of org %s",
            organizations.get(1).getName(), organizations.get(0).getName()));

    assertThat(organizationDAO.getById(organizations.get(0).getId()).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_InvalidParentHierarchy() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 4, 0);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(organizations.get(2).getId(), organizations.get(0).getId(),
                true))
        .withMessage(String.format("The parent org cannot be a child of the current org",
            organizations.get(0).getName(), organizations.get(2).getName()));

    boolean failEarlyOnError = false;
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizations.get(2).getId(), organizations.get(0).getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.PARENT_HIERARCHY);

    assertThat(validationError.message)
        .isEqualTo(String.format("The parent org cannot be a child of the current org",
            organizations.get(0).getName(), organizations.get(2).getName()));

    assertThat(organizationDAO.getById(organizations.get(0).getId()).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_TagsAreMissingInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Application application = tempEntity.newApplication(movedOrganizationId);
    Tag tag = tempEntity.newTag(organizations.get(1).getId());
    Tag tag2 = tempEntity.newTag(organizations.get(1).getId());
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    tempEntity.newApplicationTag(application.getId(), tag2.getId());

    // create a tag on source hierarchy but do not apply to the app.
    // This would not be a validation failure since it is not applied to the tag
    tempEntity.newTag(organizations.get(1).getId());

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), true))
        .withMessageContaining("Missing application categories for new parent org " + organization.getName())
        .withMessageContaining(tag.getName())
        .withMessageContaining(tag2.getName());

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.TAG);

    /*
      should not assert the entire message to avoid potentially flaky test
      since the tag order is not always guaranteed and also not important.
    */
    assertThat(validationError.message)
        .contains("Missing application categories for new parent org " + organization.getName())
        .contains(tag.getName())
        .contains(tag2.getName());

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_NoTagsAreMissingInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    tempEntity.newTag(organizations.get(1).getId());
    String movedOrganizationId = organizations.get(0).getId();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);
    assertThat(moveOrganizationResponseDTO).isNotNull();
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_LabelAreMissingInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();

    Label label = tempEntity.newLabel(organizations.get(1).getId());
    Application application = tempEntity.newApplication(movedOrganizationId);
    Condition condition = new Condition(LabelConditionType.ID, "is", label.getId());
    tempEntity.newPolicy(application.getId(), "NewPol", condition);

    Label label1 = tempEntity.newLabel(organizations.get(2).getId());
    Application application1 = tempEntity.newApplication(movedOrganizationId);
    Condition condition1 = new Condition(LabelConditionType.ID, "is", label1.getId());
    tempEntity.newPolicy(application1.getId(), "NewPol1", condition1);

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), true))
        .withMessageContaining("Missing labels for new parent org " + organization.getName())
        .withMessageContaining(label.getLabel())
        .withMessageContaining(label1.getLabel());

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.LABEL);

    /*
      should not assert the entire message to avoid potentially flaky test
      since the label order is not always guaranteed and also not important.
    */

    assertThat(validationError.message)
        .contains("Missing labels for new parent org " + organization.getName())
        .contains(label.getLabel())
        .contains(label1.getLabel());

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_NoLabelsAreMissingInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    tempEntity.newLabel(organizations.get(1).getId());
    String movedOrganizationId = organizations.get(0).getId();
    moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);
    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_LTGAreMissingInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Application application = tempEntity.newApplication(movedOrganizationId);
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(organizations.get(1).getId());
    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId());
    tempEntity.newPolicy(application.getId(), "PolName", condition);

    LicenseThreatGroup ltg1 = tempEntity.newLicenseThreatGroup(organizations.get(2).getId());
    Condition condition1 = new Condition(LicenseThreatGroupConditionType.ID, "is", ltg1.getId());
    tempEntity.newPolicy(application.getId(), "PolName1", condition1);

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), true))
        .withMessageContaining("Missing license threat groups for new parent org " + organization.getName())
        .withMessageContaining(ltg.getName())
        .withMessageContaining(ltg1.getName());

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.LICENSE_THREAT_GROUP);

    /*
      should not assert the entire message to avoid potentially flaky test
      since the ltg order is not always guaranteed and also not important.
    */

    assertThat(validationError.message)
        .contains("Missing license threat groups for new parent org " + organization.getName())
        .contains(ltg.getName())
        .contains(ltg1.getName());

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_PoliciesAreMissingInTheNewParents_SingleCommonAncestor() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    Organization organization = tempEntity.newOrganization();

    Policy policy1 = tempEntity.newPolicy(organizations.get(1));
    Policy policy2 = tempEntity.newPolicy(organizations.get(2));
    tempEntity.newPolicy(organization);

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(organizations.get(0).getId(), organization.getId(), true))
        .withMessageContaining("Missing org policies for new parent org " + organization.getName())
        .withMessageContaining(policy2.getName())
        .withMessageContaining(policy1.getName());

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizations.get(0).getId(), organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.POLICY);

    /*
      should not assert the entire message to avoid potentially flaky test
      since the policy order is not always guaranteed and also not important.
    */

    assertThat(validationError.message)
        .contains("Missing org policies for new parent org " + organization.getName())
        .contains(policy2.getName())
        .contains(policy1.getName());

    assertThat(organizationDAO.getById(organizations.get(0).getId()).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  /*
  Test using an entity that is a common parent of both (source and destination) hierarchies and sitting under root.
  This test indirectly tests if removal of the common parent duplication is working properly
  It is also a use case where source and destination org have a common parent that is different from the root org.
  */
  @Test
  public void testMoveOrganization_PoliciesAreMissingInTheNewParents() {
    List<Organization> sourceOrgHierarchy = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    List<Organization> destinationOrgHierarchy = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);

    Policy policy1 = tempEntity.newPolicy(sourceOrgHierarchy.get(1));
    tempEntity.newPolicy(destinationOrgHierarchy.get(0));

    Organization rootChild = tempEntity.newOrganization();
    sourceOrgHierarchy.get(2).setParentOrganizationId(rootChild.getParentOrganizationId());
    organizationDAO.update(sourceOrgHierarchy.get(2));
    destinationOrgHierarchy.get(2).setParentOrganizationId(rootChild.getParentOrganizationId());
    organizationDAO.update(destinationOrgHierarchy.get(2));

    // this policy should be ignored as the rootChild is a common parent.
    tempEntity.newPolicy(rootChild);

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(sourceOrgHierarchy.get(0).getId(),
                destinationOrgHierarchy.get(1).getId(), true))
        .withMessageContaining("Missing org policies for new parent org " + destinationOrgHierarchy.get(1).getName())
        .withMessageContaining(policy1.getName());

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(sourceOrgHierarchy.get(0).getId(),
            destinationOrgHierarchy.get(1).getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.POLICY);
    assertThat(validationError.message)
        .contains("Missing org policies for new parent org " + destinationOrgHierarchy.get(1).getName())
        .contains(policy1.getName());

    assertThat(organizationDAO.getById(sourceOrgHierarchy.get(0).getId()).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(sourceOrgHierarchy.get(1).getId());
  }

  @Test
  public void testMoveOrganization_PolicyDoesntExistInNewParent() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Policy policy = tempEntity.newPolicy(organizations.get(1).getId());

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), true))
        .withMessageContaining("Missing org policies for new parent org " + organization.getName())
        .withMessageContaining(policy.getName());

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);
    assertThat(moveOrganizationResponseDTO.errors.get(0).type).isEqualTo(
        MoveOrganizationValidationErrorType.POLICY);

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_LicenseOverrideComponentsDoesntEqualsByCoordinatesOfFirstParent() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Application application = tempEntity.newApplication(movedOrganizationId);
    ComponentIdentifier appComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    tempEntity.newLicenseOverride(application.getId(), appComponentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "GPL-2.0");

    ComponentIdentifier orgComponentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(organizations.get(1).getId(), orgComponentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isNotEmpty().hasSize(1);
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();

    ValidationWarning validationWarning =
        moveOrganizationResponseDTO.warnings.get(0);
    assertThat(validationWarning.type).isEqualTo(MoveOrganizationValidationWarningType.LICENSE_OVERRIDE);
    assertThat(validationWarning.message).isEqualTo(
        String.format("New parent org %s does not inherit the same license overrides as old parent org %s",
            organization.getName(), organizations.get(1).getName()));

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Organization should have moved as license override issues are considered a warning and not an error.")
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_LicenseOverrideComponentsDoesntEqualsByCoordinatesOfSecondParent() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Application application = tempEntity.newApplication(movedOrganizationId);
    ComponentIdentifier appComponentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    tempEntity.newLicenseOverride(application.getId(), appComponentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    ComponentIdentifier orgComponentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(organizations.get(1).getId(), orgComponentIdentifier1,
        LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    ComponentIdentifier orgComponentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(organizations.get(2).getId(), orgComponentIdentifier2,
        LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isNotEmpty().hasSize(1);
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();

    ValidationWarning validationWarning =
        moveOrganizationResponseDTO.warnings.get(0);
    assertThat(validationWarning.type).isEqualTo(MoveOrganizationValidationWarningType.LICENSE_OVERRIDE);
    assertThat(validationWarning.message).isEqualTo(
        String.format("New parent org %s does not inherit the same license overrides as old parent org %s",
            organization.getName(), organizations.get(1).getName()));

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Organization should have moved as license override issues are considered a warning and not an error.")
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_LicenseOverrideComponentsDoesntEqualsByCoordinatesOfSecondParent_FailEarlyOnError() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Application application = tempEntity.newApplication(movedOrganizationId);
    ComponentIdentifier appComponentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    tempEntity.newLicenseOverride(application.getId(), appComponentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    ComponentIdentifier orgComponentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(organizations.get(1).getId(), orgComponentIdentifier1,
        LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    ComponentIdentifier orgComponentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(organizations.get(2).getId(), orgComponentIdentifier2,
        LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isNotEmpty().hasSize(1);
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();

    ValidationWarning validationWarning =
        moveOrganizationResponseDTO.warnings.get(0);
    assertThat(validationWarning.type).isEqualTo(MoveOrganizationValidationWarningType.LICENSE_OVERRIDE);
    assertThat(validationWarning.message).isEqualTo(
        String.format("New parent org %s does not inherit the same license overrides as old parent org %s",
            organization.getName(), organizations.get(1).getName()));

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Organization should have moved as license override issues are considered a warning and not an error.")
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_LicenseOverrideComponentsDoesntEqualsByComponentIdentifierType() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Application application = tempEntity.newApplication(movedOrganizationId);
    ComponentIdentifier appComponentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    tempEntity.newLicenseOverride(application.getId(), appComponentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    ComponentIdentifier orgComponentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(organizations.get(1).getId(), orgComponentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isNotEmpty().hasSize(1);
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();

    ValidationWarning validationWarning =
        moveOrganizationResponseDTO.warnings.get(0);
    assertThat(validationWarning.type).isEqualTo(MoveOrganizationValidationWarningType.LICENSE_OVERRIDE);
    assertThat(validationWarning.message).isEqualTo(
        String.format("New parent org %s does not inherit the same license overrides as old parent org %s",
            organization.getName(), organizations.get(1).getName()));

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Organization should have moved as license override issues are considered a warning and not an error.")
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_TagsAreDuplicatedInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Tag tag = tempEntity.newTag(movedOrganizationId, "TagName");
    tempEntity.newTag(organization.getId(), "TagName");

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), true))
        .withMessageContaining(
            "The following application categories already exist on new parent " + organization.getName())
        .withMessageContaining(tag.getName());

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.TAG);
    assertThat(validationError.message)
        .contains("The following application categories already exist on new parent " + organization.getName())
        .contains(tag.getName());

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_TagsAreNotDuplicatedInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    tempEntity.newTag(movedOrganizationId, "TagName");
    tempEntity.newTag(organization.getId(), "NewTagName");
    moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);
    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_rootIsCommonAncestor() {
    Organization movedOrganization = tempEntity.newOrganization();
    Organization destinationOrg = tempEntity.newOrganization();

    tempEntity.newTag(destinationOrg.getId(), "TagName");

    moveOrganizationService.moveOrganization(movedOrganization.getId(), destinationOrg.getId(), failEarlyOnError);
    assertThat(organizationDAO.getById(movedOrganization.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrg.getId());
  }

  @Test
  public void testMoveOrganization_LabelsAreDuplicatedInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    tempEntity.newLabel(movedOrganizationId, "LabelText");
    tempEntity.newLabel(organization.getId(), "LabelText");

    // apply a label on a child application.
    Application application = tempEntity.newApplication(movedOrganizationId);
    tempEntity.newLabel(application.getId(), "ApplicationLabelDuplicatedOnOrg");
    tempEntity.newLabel(organization.getId(), "ApplicationLabelDuplicatedOnOrg");

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), true))
        .withMessageContaining("The following labels already exist on new parent " + organization.getName())
        .withMessageContaining("LabelText")
        .withMessageContaining("ApplicationLabelDuplicatedOnOrg");

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.LABEL);
    assertThat(validationError.message).contains(
            "The following labels already exist on new parent " + organization.getName())
        .contains("ApplicationLabelDuplicatedOnOrg")
        .contains("LabelText");

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_LabelsAreNotDuplicatedInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();

    Application application = tempEntity.newApplication(movedOrganizationId);
    tempEntity.newLabel(application.getId(), "ApplicationLabelText");

    tempEntity.newLabel(movedOrganizationId, "LabelText");
    tempEntity.newLabel(organization.getId(), "NewLabelText");

    moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_LTGsAreDuplicatedInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    tempEntity.newLicenseThreatGroup(movedOrganizationId, "Ltg1", 1);
    tempEntity.newLicenseThreatGroup(organization.getId(), "Ltg1", 1);

    // add duplicated ltg on application and new parent
    Application application = tempEntity.newApplication(movedOrganizationId);
    tempEntity.newLicenseThreatGroup(application.getId(), "Ltg2", 3);
    tempEntity.newLicenseThreatGroup(organization.getId(), "Ltg2", 3);

    // sample ltg that is not in conflict anywhere.
    tempEntity.newLicenseThreatGroup(application.getId(), "Ltg3", 5);

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), true))
        .withMessageContaining(
            "The following license threat groups already exist on new parent " + organization.getName())
        .withMessageContaining("Ltg1")
        .withMessageContaining("Ltg2");

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.LICENSE_THREAT_GROUP);
    assertThat(validationError.message)
        .contains("The following license threat groups already exist on new parent " + organization.getName())
        .contains("Ltg1")
        .contains("Ltg2");

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_LTGsAreNotDuplicatedInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    tempEntity.newLicenseThreatGroup(movedOrganizationId, "Ltg1", 1);
    tempEntity.newLicenseThreatGroup(organization.getId(), "NewLtg1", 1);

    Application application = tempEntity.newApplication(movedOrganizationId);
    tempEntity.newLicenseThreatGroup(application.getId(), "Ltg2", 5);

    // add a ltg on the application which is a child of moved org's parent org.
    // this ltg should not conflict anywhere.
    Application application1 = tempEntity.newApplication(organizations.get(1).getId());
    tempEntity.newLicenseThreatGroup(application1.getId(), "Ltg-not-in-conflict", 2);

    moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_PoliciesAreDuplicatedInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    tempEntity.newPolicy(organizations.get(0).getId(), "PolName");
    tempEntity.newPolicy(organization.getId(), "PolName");

    Application application1 = tempEntity.newApplication(organizations.get(0).getId());

    // create an application and policy on the parent of the org being moved.
    // This policy should not be in the conflict
    Application application2 = tempEntity.newApplication(organizations.get(1).getId());

    tempEntity.newPolicy(application1.getId(), "PolName1");
    tempEntity.newPolicy(organization.getId(), "PolName1");
    tempEntity.newPolicy(application2.getId(), "Policy-not-in-conflict-anywhere");

    // fail early case
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(organizations.get(0).getId(), organization.getId(), true))
        .withMessageContaining("The following policies already exist on new parent " + organization.getName())
        .withMessageContaining("PolName")
        .withMessageContaining("PolName1");

    // non fail early case
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizations.get(0).getId(), organization.getId(), failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);

    ValidationError validationError =
        moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.POLICY);
    assertThat(validationError.message)
        .contains("The following policies already exist on new parent " + organization.getName())
        .contains("PolName")
        .contains("PolName1");

    assertThat(organizationDAO.getById(organizations.get(0).getId()).getParentOrganizationId())
        .as("Org should not have moved as there are validation errors")
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testMoveOrganization_PoliciesAreNotDuplicatedInTheNewParents() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    tempEntity.newPolicy(movedOrganizationId, "PolName");
    tempEntity.newPolicy(organization.getId(), "NewPolName");

    moveOrganizationService.moveOrganization(movedOrganizationId, organization.getId(), failEarlyOnError);

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .isEqualTo(organization.getId());
  }

  @Test
  public void testMoveOrganization_PolicyWaiversDefinedForOldInheritedPoliciesAtSelf_issuesWarning() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    Policy policyAtMiddleParent = tempEntity.newPolicy(organizationToMove.getParentOrganizationId());
    tempEntity.newWaiver(policyAtMiddleParent.getId(), organizationToMove.getId());

    Organization destinationOrganization = tempEntity.newOrganization();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).hasSize(1);
    ValidationWarning validationWarning = moveOrganizationResponseDTO.warnings.get(0);
    assertThat(validationWarning.type).isEqualTo(MoveOrganizationValidationWarningType.POLICY_WAIVER);
    assertThat(validationWarning.message).isEqualTo(ValidationWarning.POLICY_WAIVER_MSG);

    assertThat(moveOrganizationResponseDTO.errors).hasSize(1);
    ValidationError validationError = moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.POLICY);
  }

  @Test
  public void testMoveOrganization_PolicyWaiversDefinedForOldInheritedPoliciesAtSelf_NoWarningWhenFailFastActive() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    Policy policyAtMiddleParent = tempEntity.newPolicy(organizationToMove.getParentOrganizationId());
    tempEntity.newWaiver(policyAtMiddleParent.getId(), organizationToMove.getId());

    Organization destinationOrganization = tempEntity.newOrganization();
    // fail early case ignores warnings as it throws an exception immediately
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
                true))
        .withMessageContaining("Missing org policies for new parent org " + destinationOrganization.getName());
  }

  @Test
  public void testMoveOrganization_PolicyWaiversDefinedForOldInheritedPoliciesAtChild_issuesWarning() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    Policy policyAtMiddleParent = tempEntity.newPolicy(organizationToMove.getParentOrganizationId());
    Application childApplication = applicationDAO.getByOrganizationId(organizationToMove.getId()).get(0);
    tempEntity.newWaiver(policyAtMiddleParent.getId(), childApplication.getId());

    Organization destinationOrganization = tempEntity.newOrganization();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).hasSize(1);
    ValidationWarning validationWarning = moveOrganizationResponseDTO.warnings.get(0);
    assertThat(validationWarning.type).isEqualTo(MoveOrganizationValidationWarningType.POLICY_WAIVER);
    assertThat(validationWarning.message).isEqualTo(ValidationWarning.POLICY_WAIVER_MSG);

    assertThat(moveOrganizationResponseDTO.errors).hasSize(1);
    ValidationError validationError = moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.POLICY);
  }

  @Test
  public void testMoveOrganization_PolicyWaiversDefinedForOldInheritedPoliciesAtChild_NoWarningWhenFailFastActive() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    Policy policyAtMiddleParent = tempEntity.newPolicy(organizationToMove.getParentOrganizationId());
    Application childApplication = applicationDAO.getByOrganizationId(organizationToMove.getId()).get(0);
    tempEntity.newWaiver(policyAtMiddleParent.getId(), childApplication.getId());

    Organization destinationOrganization = tempEntity.newOrganization();
    // fail early case ignores warnings as it throws an exception immediately
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
                true))
        .withMessageContaining("Missing org policies for new parent org " + destinationOrganization.getName());
  }

  @Test
  public void testMoveOrganization_PolicyWaiverDefinedForOldInheritedPolicyAtOldParent() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    Policy policyAtMiddleParent = tempEntity.newPolicy(organizationToMove.getParentOrganizationId());
    tempEntity.newWaiver(policyAtMiddleParent.getId(), organizationToMove.getParentOrganizationId());

    Organization destinationOrganization = tempEntity.newOrganization();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isEmpty();
    assertThat(moveOrganizationResponseDTO.errors).hasSize(1);
    ValidationError validationError = moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.POLICY);
  }

  @Test
  public void testMoveOrganization_PolicyWaiverNotDefinedForOldInheritedPolicies() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    Policy policyAtOrganization = tempEntity.newPolicy(organizationToMove.getId());
    tempEntity.newWaiver(policyAtOrganization.getId(), organizationToMove.getId());

    Organization destinationOrganization = tempEntity.newOrganization();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isEmpty();
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrganization.getId());
  }

  @Test
  public void testMoveOrganization_NoPolicyWaiversDefined() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    tempEntity.newPolicy(organizationToMove.getParentOrganizationId());
    tempEntity.newPolicy(organizationToMove.getId());

    Organization destinationOrganization = tempEntity.newOrganization();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isEmpty();

    assertThat(moveOrganizationResponseDTO.errors).hasSize(1);
    ValidationError validationError = moveOrganizationResponseDTO.errors.get(0);
    assertThat(validationError.type).isEqualTo(MoveOrganizationValidationErrorType.POLICY);
  }

  @Test
  public void testMoveOrganization_PolicyMonitoringDefinedAtTheMovedOrgLevel() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    tempEntity.newPolicyMonitoring(organizationToMove.getId(), Stage.ID_RELEASE);

    Organization destinationOrganization = tempEntity.newOrganization();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isEmpty();
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrganization.getId());
  }

  @Test
  public void testMoveOrganization_PolicyMonitoringNotDefinedForMovedOrgAtAnyLevel() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);

    Organization destinationOrganization = tempEntity.newOrganization();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isEmpty();
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrganization.getId());
  }

  @Test
  public void testMoveOrganization_InheritedPolicyMonitoringDefinedAtNewParentWithMatchingStageToOldParent() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    tempEntity.newPolicyMonitoring(organizationToMove.getParentOrganizationId(), Stage.ID_RELEASE);

    Organization destinationOrganization = tempEntity.newOrganization();
    tempEntity.newPolicyMonitoring(destinationOrganization.getParentOrganizationId(), Stage.ID_RELEASE);

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).isEmpty();
    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrganization.getId());
  }

  @Test
  public void testMoveOrganization_InheritedPolicyMonitoringDefinedAtNewParentWithDifferentStage_issuesWarning() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    tempEntity.newPolicyMonitoring(organizationToMove.getParentOrganizationId(), Stage.ID_RELEASE);

    Organization destinationOrganization = tempEntity.newOrganization();
    tempEntity.newPolicyMonitoring(destinationOrganization.getParentOrganizationId(), Stage.ID_OPERATE);

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).hasSize(1);
    ValidationWarning validationWarning = moveOrganizationResponseDTO.warnings.get(0);
    assertThat(validationWarning.type).isEqualTo(MoveOrganizationValidationWarningType.POLICY_MONITORING);
    assertThat(validationWarning.message).isEqualTo(ValidationWarning.POLICY_MONITORING_DIFFERENT_MSG);

    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrganization.getId());
  }

  @Test
  public void testMoveOrganization_InheritedPolicyMonitoringNotFoundAtNewParent_issuesWarning() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 1);
    Organization organizationToMove = organizations.get(0);
    tempEntity.newPolicyMonitoring(organizationToMove.getParentOrganizationId(), Stage.ID_RELEASE);

    Organization destinationOrganization = tempEntity.newOrganization();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.warnings).hasSize(1);
    ValidationWarning validationWarning = moveOrganizationResponseDTO.warnings.get(0);
    assertThat(validationWarning.type).isEqualTo(MoveOrganizationValidationWarningType.POLICY_MONITORING);
    assertThat(validationWarning.message).isEqualTo(ValidationWarning.POLICY_MONITORING_MISSING_MSG);

    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrganization.getId());
  }

  @Test
  public void testMoveOrganization_fixConflict_PolicyNonExistent() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization destinationOrganization = tempEntity.newOrganization(Organization.ROOT_ORGANIZATION_ID);
    Organization organizationToMove = organizations.get(0);
    Policy missingPolicyInDestination = tempEntity.newPolicy(organizations.get(1).getId(), "Policy in middle parent");

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);
    assertThat(moveOrganizationResponseDTO.errors.get(0).type).isEqualTo(
        MoveOrganizationValidationErrorType.POLICY);

    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(organizations.get(1).getId());

    // "Move" policy from immediate parent to common parent
    policyDAO.delete(missingPolicyInDestination);
    missingPolicyInDestination.setOwnerId(destinationOrganization.getParentOwnerId());
    tempEntity.newPolicy(missingPolicyInDestination);

    moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrganization.getId());
  }

  @Test
  public void testMoveOrganization_fixConflict_PolicyIsDuplicated() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization destinationOrganization = tempEntity.newOrganization(Organization.ROOT_ORGANIZATION_ID);
    Organization organizationToMove = organizations.get(0);
    Policy originalPolicyInMiddleParent = tempEntity.newPolicy(organizations.get(1).getId(), "Conflicting policy");
    Policy duplicatedPolicyInDestination = tempEntity.newPolicy(destinationOrganization.getId(), "Conflicting policy");

    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty().hasSize(1);
    assertThat(moveOrganizationResponseDTO.errors.get(0).type).isEqualTo(
        MoveOrganizationValidationErrorType.POLICY);

    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(organizations.get(1).getId());

    // "Move" policy from immediate parent to common parent and remove conflicting policies
    policyDAO.delete(originalPolicyInMiddleParent);
    policyDAO.delete(duplicatedPolicyInDestination);
    originalPolicyInMiddleParent.setOwnerId(destinationOrganization.getParentOwnerId());
    tempEntity.newPolicy(originalPolicyInMiddleParent);

    moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(organizationToMove.getId(), destinationOrganization.getId(),
            failEarlyOnError);

    assertThat(moveOrganizationResponseDTO.errors).isEmpty();
    assertThat(organizationDAO.getById(organizationToMove.getId()).getParentOrganizationId())
        .isEqualTo(destinationOrganization.getId());
  }

  @Test
  public void testGetMoveOrganizationErrorsForExport() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Application application = tempEntity.newApplication(movedOrganizationId);
    Tag tag = tempEntity.newTag(organizations.get(1).getId());
    Tag tag2 = tempEntity.newTag(organizations.get(1).getId());
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    tempEntity.newApplicationTag(application.getId(), tag2.getId());

    // create a tag on source hierarchy but do not apply to the app.
    // This would not be a validation failure since it is not applied to the tag
    tempEntity.newTag(organizations.get(1).getId());

    Label label = tempEntity.newLabel(organizations.get(1).getId());
    Condition condition = new Condition(LabelConditionType.ID, "is", label.getId());
    tempEntity.newPolicy(application.getId(), "NewPol", condition);

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(organizations.get(1).getId());
    Condition ltgCondition = new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId());
    tempEntity.newPolicy(application.getId(), "PolName", ltgCondition);

    List<ValidationError> validationErrors =
        moveOrganizationService.getMoveOrganizationErrors(movedOrganizationId, organization.getId());

    assertThat(validationErrors)
        .isNotEmpty()
        .as("As there should be only one line item for each error category")
        .hasSize(3);

    assertThat(validationErrors.get(0).type).isEqualTo(MoveOrganizationValidationErrorType.TAG);
    assertThat(validationErrors.get(0).message)
        .as("All the individual validation failures for each error category should be part of a single message")
        .contains("Missing application categories for new parent org " + organization.getName())
        .contains(tag.getName())
        .contains(tag2.getName());

    assertThat(validationErrors.get(1).type).isEqualTo(MoveOrganizationValidationErrorType.LABEL);
    assertThat(validationErrors.get(1).message)
        .as("All the individual validation failures for each error category should be part of a single message")
        .contains("Missing labels for new parent org " + organization.getName())
        .contains(label.getLabel());

    assertThat(validationErrors.get(2).type).isEqualTo(MoveOrganizationValidationErrorType.LICENSE_THREAT_GROUP);
    assertThat(validationErrors.get(2).message)
        .as("All the individual validation failures for each error category should be part of a single message")
        .contains("Missing license threat groups for new parent org " + organization.getName())
        .contains(ltg.getName());
  }

  @Test
  public void testGetMoveOrganizationErrorsForExport_WarningsShouldNotBeIncluded() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();
    Application application = tempEntity.newApplication(movedOrganizationId);
    Tag tag = tempEntity.newTag(organizations.get(1).getId());
    Tag tag2 = tempEntity.newTag(organizations.get(1).getId());
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    tempEntity.newApplicationTag(application.getId(), tag2.getId());

    // create warning scenario
    ComponentIdentifier appComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    tempEntity.newLicenseOverride(application.getId(), appComponentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        "GPL-2.0");

    ComponentIdentifier orgComponentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(organizations.get(1).getId(), orgComponentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN,
        "GLWTPL");

    List<ValidationError> validationErrors =
        moveOrganizationService.getMoveOrganizationErrors(movedOrganizationId, organization.getId());

    assertThat(validationErrors)
        .isNotEmpty()
        .hasSize(1);
  }

  @Test
  public void testGetMoveOrganizationErrorsForExport_NoErrors() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    String movedOrganizationId = organizations.get(0).getId();

    List<ValidationError> validationErrors =
        moveOrganizationService.getMoveOrganizationErrors(movedOrganizationId, organization.getId());

    assertThat(validationErrors).isEmpty();

    assertThat(organizationDAO.getById(movedOrganizationId).getParentOrganizationId())
        .isEqualTo(organizations.get(1).getId());
  }

  @Test
  public void testGetDestinationOrganizations_RootOrgHasNoDestinations() {
    tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    assertThat(organizationDAO.getAll()).hasSize(3);

    List<Organization> destinationOrganizations =
        moveOrganizationService.getDestinationOrganizations(Organization.ROOT_ORGANIZATION_ID);

    assertThat(destinationOrganizations).isEmpty();
  }

  @Test
  public void testGetDestinationOrganizations_NotSelfNorCurrentParent() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 5, 0);
    assertThat(organizationDAO.getAll()).hasSize(6);

    Organization leafOrganization = organizations.get(0);
    List<Organization> destinationOrganizations =
        moveOrganizationService.getDestinationOrganizations(leafOrganization.getId());

    assertThat(destinationOrganizations).hasSize(4);
    assertThat(destinationOrganizations.stream().map(Organization::getId)).doesNotContain(leafOrganization.getId(),
        leafOrganization.getParentOrganizationId());
  }

  @Test
  public void testGetDestinationOrganizations_NotChildOfSelf() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 6, 0);
    assertThat(organizationDAO.getAll()).hasSize(7);

    Organization organizationWithChildren = organizations.get(3);
    List<Organization> destinationOrganizations =
        moveOrganizationService.getDestinationOrganizations(organizationWithChildren.getId());

    assertThat(destinationOrganizations).hasSize(2);

    String[] childrenOrganizationIds =
        new String[]{organizations.get(0).getId(), organizations.get(1).getId(), organizations.get(2).getId()};
    assertThat(destinationOrganizations.stream().map(Organization::getId)).doesNotContain(childrenOrganizationIds);
    assertThat(destinationOrganizations.stream().map(Organization::getId)).containsOnly(organizations.get(5).getId(),
        Organization.ROOT_ORGANIZATION_ID);
  }
}
