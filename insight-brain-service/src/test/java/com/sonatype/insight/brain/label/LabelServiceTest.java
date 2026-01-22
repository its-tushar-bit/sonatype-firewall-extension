/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.dataaccess.label.InvalidLabelException;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.LabelEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LabelServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LabelService labelService;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private LabelDAO labelDAO;

  @Test
  public void testAddUpdateAndDeleteLabelPostsEvents() throws Exception {
    TestEventHandler<LabelEvent> handler = new TestEventHandler<>(new CountDownLatch(1), LabelEvent.class);
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    ApiLabelDTO dto = new ApiLabelDTO("Label", "test label", "yellow");

    dto = labelService.addLabel(ORGANIZATION, organization.getId(), dto);
    Label created = labelDAO.getById(dto.id);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(CREATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().label.getId()).isEqualTo(created.getId());

    handler.setLatch(new CountDownLatch(1));

    labelService.updateLabel(ORGANIZATION, organization.getId(), dto);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().label.getId()).isEqualTo(created.getId());

    handler.setLatch(new CountDownLatch(1));

    labelService.deleteLabel(ORGANIZATION, organization.getId(), dto.id);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(DELETED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().label.getId()).isEqualTo(created.getId());

    eventBus.unregister(handler);
  }

  @Test
  public void testGetLabels_ExcludeInheritedLabels() {
    Organization myOrganization = tempEntity.newOrganization("My-Organization");
    Application application = tempEntity.newApplication(myOrganization.getId());
    String appId = application.getId();

    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newLabel(myOrganization.getId());
    Label label = tempEntity.newLabel(appId);

    List<ApiLabelDTO> labels = labelService.getLabels(APPLICATION, appId, /* inherit */ false);

    assertThat(labels.size()).isEqualTo(1);
    ApiLabelDTO apiLabelDTO = labels.get(0);
    assertThat(apiLabelDTO.id).isEqualTo(label.getId());
    assertThat(apiLabelDTO.label).isEqualTo(label.getLabel());
    assertThat(apiLabelDTO.description).isEqualTo(label.getDescription());
    assertThat(apiLabelDTO.color).isEqualTo(label.getColor().toValue());
    assertThat(apiLabelDTO.ownerId).isEqualTo(appId);
    assertThat(apiLabelDTO.ownerType).isEqualTo("APPLICATION");
  }

  @Test
  public void testGetLabels_IncludeInheritedLabels() {
    Organization organization = tempEntity.newOrganization("My-Organization");
    Application application = tempEntity.newApplication(organization.getId());

    Label rootOrgLabel = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Label orgLabel = tempEntity.newLabel(organization.getId());
    Label appLabel = tempEntity.newLabel(application.getId());

    List<ApiLabelDTO> labels = labelService.getLabels(APPLICATION, application.getId(), /* inherit */ true);

    assertThat(labels.size()).isEqualTo(3);

    ApiLabelDTO rootOrgDto = labels.stream().filter(dto -> dto.id.equals(rootOrgLabel.getId())).findAny().get();
    assertThat(rootOrgDto.id).isEqualTo(rootOrgLabel.getId());
    assertThat(rootOrgDto.label).isEqualTo(rootOrgLabel.getLabel());
    assertThat(rootOrgDto.description).isEqualTo(rootOrgLabel.getDescription());
    assertThat(rootOrgDto.color).isEqualTo(rootOrgLabel.getColor().toValue());
    assertThat(rootOrgDto.ownerId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(rootOrgDto.ownerType).isEqualTo("ORGANIZATION");

    ApiLabelDTO orgDto = labels.stream().filter(dto -> dto.id.equals(orgLabel.getId())).findAny().get();
    assertThat(orgDto.id).isEqualTo(orgLabel.getId());
    assertThat(orgDto.label).isEqualTo(orgLabel.getLabel());
    assertThat(orgDto.description).isEqualTo(orgLabel.getDescription());
    assertThat(orgDto.color).isEqualTo(orgLabel.getColor().toValue());
    assertThat(orgDto.ownerId).isEqualTo(organization.getId());
    assertThat(orgDto.ownerType).isEqualTo("ORGANIZATION");

    ApiLabelDTO appDto = labels.stream().filter(dto -> dto.id.equals(appLabel.getId())).findAny().get();
    assertThat(appDto.id).isEqualTo(appLabel.getId());
    assertThat(appDto.label).isEqualTo(appLabel.getLabel());
    assertThat(appDto.description).isEqualTo(appLabel.getDescription());
    assertThat(appDto.color).isEqualTo(appLabel.getColor().toValue());
    assertThat(appDto.ownerId).isEqualTo(application.getId());
    assertThat(appDto.ownerType).isEqualTo("APPLICATION");
  }

  @Test
  public void testGetLabels_GetApplicationLabelByPublicId() {
    Application application = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(application.getId());

    List<ApiLabelDTO> labels = labelService.getLabels(APPLICATION, application.getPublicId(), false);

    assertThat(labels.size()).isEqualTo(1);
    ApiLabelDTO apiLabelDTO = labels.get(0);
    assertThat(apiLabelDTO.id).isEqualTo(label.getId());
    assertThat(apiLabelDTO.label).isEqualTo(label.getLabel());
    assertThat(apiLabelDTO.description).isEqualTo(label.getDescription());
    assertThat(apiLabelDTO.color).isEqualTo(label.getColor().toValue());
    assertThat(apiLabelDTO.ownerId).isEqualTo(application.getId());
    assertThat(apiLabelDTO.ownerType).isEqualTo("APPLICATION");
  }

  @Test
  public void testAddLabel_WithId() {
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "description", "dark-blue");
    labelDTO.id = "id-on-demand";

    assertThatThrownBy(() -> labelService.addLabel(OwnerType.APPLICATION, appId, labelDTO))
        .isInstanceOf(BadRequestException.class).hasMessage("ID must be null when creating a Label.");
  }

  @Test
  public void testAddLabel_MissingLabel() {
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiLabelDTO labelDTO = new ApiLabelDTO(null, "description", "dark-blue");

    assertThatThrownBy(() -> labelService.addLabel(OwnerType.APPLICATION, appId, labelDTO))
        .isInstanceOf(InvalidNameException.class).hasMessage("Label name is required.");
  }

  @Test
  public void testAddLabel_WithOwnerId() {
    ApiLabelDTO apiLabelDTO = null;
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "description", "dark-blue");
    labelDTO.ownerId = appId;

    apiLabelDTO = labelService.addLabel(APPLICATION, appId, labelDTO);
    assertThat(apiLabelDTO.id).isNotNull();
    assertThat(apiLabelDTO.label).isEqualTo("MyLabel");
    assertThat(apiLabelDTO.description).isEqualTo("description");
    assertThat(apiLabelDTO.color).isEqualTo(Color.dark_blue.toValue());

    Label label = labelDAO.getById(apiLabelDTO.id);
    assertThat(label.getLabel()).isEqualTo("MyLabel");
    assertThat(label.getDescription()).isEqualTo("description");
    assertThat(label.getColor()).isEqualTo(Color.dark_blue);
  }

  @Test
  public void testAddLabel_WithOwnerIdMismatch() {
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "description", "dark-blue");
    labelDTO.ownerId = "ownerId-on-demand";

    assertThatThrownBy(() -> labelService.addLabel(OwnerType.APPLICATION, appId, labelDTO))
        .isInstanceOf(BadRequestException.class).hasMessage("Owner ID mismatch.");
  }

  @Test
  public void testAddLabel_ToNonExistingApplication() {
    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "DescriptionLabel", "dark-blue");
    assertThatThrownBy(() -> labelService.addLabel(OwnerType.APPLICATION, "no-app-with-this-id", labelDTO))
        .isInstanceOf(NotFoundException.class).hasMessage("Application with ID no-app-with-this-id does not exist.");
  }

  @Test
  public void testAddLabel_WithOwnerType() {
    ApiLabelDTO apiLabelDTO = null;
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "description", "dark-blue");
    labelDTO.ownerType = "APPLICATION";

    apiLabelDTO = labelService.addLabel(APPLICATION, appId, labelDTO);
    assertThat(apiLabelDTO.id).isNotNull();
    assertThat(apiLabelDTO.label).isEqualTo("MyLabel");
    assertThat(apiLabelDTO.description).isEqualTo("description");
    assertThat(apiLabelDTO.color).isEqualTo(Color.dark_blue.toValue());

    Label label = labelDAO.getById(apiLabelDTO.id);
    assertThat(label.getLabel()).isEqualTo("MyLabel");
    assertThat(label.getDescription()).isEqualTo("description");
    assertThat(label.getColor()).isEqualTo(Color.dark_blue);
  }

  @Test
  public void testAddLabel_WithOwnerTypeMismatch() {
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "description", "dark-blue");
    labelDTO.ownerType = "ORGANIZATION";

    assertThatThrownBy(() -> labelService.addLabel(OwnerType.APPLICATION, appId, labelDTO))
        .isInstanceOf(BadRequestException.class).hasMessage("Owner Type mismatch.");
  }

  @Test
  public void testAddLabel_MissingColor() {
    String appId = tempEntity.newApplicationWithParent().getId();
    ApiLabelDTO labelDTO = new ApiLabelDTO("Label Without Color", "Description", null);

    assertThatThrownBy(() -> labelService.addLabel(OwnerType.APPLICATION, appId, labelDTO))
        .isInstanceOf(InvalidLabelException.class).hasMessage("The label color must be assigned.");
  }

  @Test
  public void testAddLabel_InvalidColor() {
    String appId = tempEntity.newApplicationWithParent().getId();

    ApiLabelDTO labelDTO = new ApiLabelDTO("Label", "Description", "blakc");

    assertThatThrownBy(() -> labelService.addLabel(OwnerType.APPLICATION, appId, labelDTO))
        .isInstanceOf(BadRequestException.class).hasMessage("Unsupported color: blakc");
  }

  @Test
  public void testUpdateLabel() {
    String appId = tempEntity.newApplicationWithParent().getId();
    Label label = tempEntity.newLabel(appId);

    ApiLabelDTO apiLabelDTO = LabelService.toDTO(label, APPLICATION);
    apiLabelDTO.label = "NewLabel";
    apiLabelDTO.description = "NewDescription";
    apiLabelDTO.color = "light-blue";

    apiLabelDTO = labelService.updateLabel(APPLICATION, appId, apiLabelDTO);
    assertThat(apiLabelDTO.label).isEqualTo("NewLabel");
    assertThat(apiLabelDTO.description).isEqualTo("NewDescription");
    assertThat(apiLabelDTO.color).isEqualTo(Color.light_blue.toValue());
    assertThat(apiLabelDTO.ownerId).isEqualTo(appId);
    assertThat(apiLabelDTO.ownerType).isEqualTo("APPLICATION");

    label = labelDAO.getById(label.getId());
    assertThat(label.getLabel()).isEqualTo("NewLabel");
    assertThat(label.getDescription()).isEqualTo("NewDescription");
    assertThat(label.getColor()).isEqualTo(Color.light_blue);
  }

  @Test
  public void testUpdateLabel_UseApplicationPublicId() {
    Application application = tempEntity.newApplicationWithParent();
    String appId = application.getId();
    String appPublicId = application.getPublicId();
    Label label = tempEntity.newLabel(appId);

    ApiLabelDTO apiLabelDTO = LabelService.toDTO(label, APPLICATION);
    apiLabelDTO.label = "NewLabel";
    apiLabelDTO.description = "NewDescription";
    apiLabelDTO.color = "light-blue";

    apiLabelDTO = labelService.updateLabel(APPLICATION, appPublicId, apiLabelDTO);
    assertThat(apiLabelDTO.label).isEqualTo("NewLabel");
    assertThat(apiLabelDTO.description).isEqualTo("NewDescription");
    assertThat(apiLabelDTO.color).isEqualTo(Color.light_blue.toValue());
    assertThat(apiLabelDTO.ownerId).isEqualTo(appId);
    assertThat(apiLabelDTO.ownerType).isEqualTo("APPLICATION");

    label = labelDAO.getById(label.getId());
    assertThat(label.getLabel()).isEqualTo("NewLabel");
    assertThat(label.getDescription()).isEqualTo("NewDescription");
    assertThat(label.getColor()).isEqualTo(Color.light_blue);
  }

  @Test
  public void testUpdateLabel_MissingId() {
    String appId = tempEntity.newApplicationWithParent().getId();
    Label label = tempEntity.newLabel(appId);

    ApiLabelDTO apiLabelDTO = LabelService.toDTO(label, APPLICATION);
    apiLabelDTO.id = null;

    assertThatThrownBy(() -> labelService.updateLabel(OwnerType.APPLICATION, appId, apiLabelDTO))
        .isInstanceOf(NotFoundException.class).hasMessage("Label with ID null does not exist.");
  }

  @Test
  public void testUpdateLabel_WithOwnerIdMismatch() {
    String appId = tempEntity.newApplicationWithParent().getId();
    Label label = tempEntity.newLabel(appId);

    ApiLabelDTO apiLabelDTO = LabelService.toDTO(label, APPLICATION);
    apiLabelDTO.ownerId = tempEntity.newApplicationWithParent().getId();

    assertThatThrownBy(() -> labelService.updateLabel(OwnerType.APPLICATION, appId, apiLabelDTO))
        .isInstanceOf(BadRequestException.class).hasMessage("Owner ID mismatch.");
  }

  @Test
  public void testUpdateLabel_WithOwnerTypeMismatch() {
    String appId = tempEntity.newApplicationWithParent().getId();
    Label label = tempEntity.newLabel(appId);

    ApiLabelDTO apiLabelDTO = LabelService.toDTO(label, APPLICATION);
    apiLabelDTO.ownerType = "ORGANIZATION";

    assertThatThrownBy(() -> labelService.updateLabel(OwnerType.APPLICATION, appId, apiLabelDTO))
        .isInstanceOf(BadRequestException.class).hasMessage("Owner Type mismatch.");
  }

  @Test
  public void testDeleteLabel() {
    String appId = tempEntity.newApplicationWithParent().getId();
    Label label = tempEntity.newLabel(appId);

    labelService.deleteLabel(APPLICATION, appId, label.getId());
    assertThat(labelDAO.getById(label.getId())).isNull();
  }

  @Test
  public void testDeleteLabel_UseApplicationPublicId() {
    Application application = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(application.getId());

    labelService.deleteLabel(APPLICATION, application.getPublicId(), label.getId());
    assertThat(labelDAO.getById(label.getId())).isNull();
  }

  @Test
  public void testDeleteLabel_LabelDoesNotExist() {
    Application app = tempEntity.newApplicationWithParent();

    assertThatThrownBy(() -> labelService.deleteLabel(APPLICATION, app.getId(), "YettiId"))
        .isInstanceOf(NotFoundException.class).hasMessage("Label with ID YettiId does not exist.");
  }

  @Test
  public void testDeleteLabel_WithOwnerIdMismatch() {
    String appId = tempEntity.newApplicationWithParent().getId();
    Label label = tempEntity.newLabel(appId);
    String otherAppId = tempEntity.newApplicationWithParent().getId();

    assertThatThrownBy(() -> labelService.deleteLabel(OwnerType.APPLICATION, otherAppId, label.getId()))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a label with ID " + label.getId() + " for application ID " + otherAppId);
  }

  @Test
  public void testDeleteLabel_AppLabelUsedInPolicyCondition() {
    Application app = tempEntity.newApplicationWithParent();
    testDeleteLabel_InUseByPolicy(OwnerType.APPLICATION, app.getId(), app.getId(), null);
  }

  @Test
  public void testDeleteLabel_OrgLabelUsedInAppPolicyCondition() {
    Application app = tempEntity.newApplicationWithParent("appPublicId", "appName");
    testDeleteLabel_InUseByPolicy(OwnerType.ORGANIZATION, app.getOrganizationId(), app.getId(),
        "in application 'appName'");
  }

  @Test
  public void testDeleteLabel_OrgLabelUsedInGrandChildAppPolicyCondition() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    testDeleteLabel_InUseByPolicy(OwnerType.ORGANIZATION, org.getParentOrganizationId(), app.getId(),
        "in application 'appName'");
  }

  @Test
  public void testDeleteLabel_OrgLabelUsedInPolicyCondition() {
    Organization org = tempEntity.newOrganization();
    testDeleteLabel_InUseByPolicy(OwnerType.ORGANIZATION, org.getId(), org.getId(), null);
  }

  @Test
  public void testDeleteLabel_OrgLabelUsedInChildOrgPolicyCondition() {
    Organization org = tempEntity.newOrganization("orgName");
    testDeleteLabel_InUseByPolicy(OwnerType.ORGANIZATION, org.getParentOrganizationId(), org.getId(),
        "in organization 'orgName'");
  }

  private void testDeleteLabel_InUseByPolicy(
      OwnerType ownerType,
      String ownerId,
      String policyOwnerId,
      String policyLocation)
  {
    Label label = tempEntity.newLabel(ownerId);

    Policy policy = new Policy(null, "policyName");
    policy.setOwnerId(policyOwnerId);
    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.AND);
    constraint.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    String expectedErrorMessage =
        "Cannot delete the label because it is used in a condition for the 'policyName' policy";
    if (policyLocation != null) {
      expectedErrorMessage += " " + policyLocation;
    }
    assertThatThrownBy(() -> labelService.deleteLabel(ownerType, ownerId, label.getId()))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(expectedErrorMessage);

    assertThat(labelDAO.getById(label.getId())).isNotNull();
  }
}
