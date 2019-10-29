/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApplicationCloneServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationCloneService appCloneService;

  @Test
  public void testCloneApplication_SourceApplicationDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      appCloneService.cloneApplication("AppDoesNotExistId", "clonedAppName", "clonedAppPublicId");
    }).withMessage("Could not find an application with ID AppDoesNotExistId.");
  }

  @Test
  public void testCloneApplication_Application() {
    String clonedAppName = "clonedAppName";
    String clonedAppPublicId = "clonedAppPublicId";
    String contactUsername = "testuser";
    Application sourceApp = tempEntity.newApplicationWithParent();
    sourceApp.setContactInternalName(contactUsername);
    // The application cloning is supposed to disable grandfathering for the cloned app.
    // So we set it to true in the source application in order to verify
    // that is not copied to the cloned application.
    sourceApp.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(sourceApp);

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), clonedAppName, clonedAppPublicId);

    // Assert the returned app DTO.
    assertThat(clonedAppDTO.organizationId).isEqualTo(sourceApp.getOrganizationId());
    assertThat(clonedAppDTO.name).isEqualTo(clonedAppName);
    assertThat(clonedAppDTO.publicId).isEqualTo(clonedAppPublicId);
    assertThat(clonedAppDTO.contactUserName).isEqualTo(contactUsername);
    assertThat(clonedAppDTO.applicationTags).isEmpty();
    
    // Assert the app stored in the db.
    Application clonedApp = new ApplicationDAO().getByIdNotNull(clonedAppDTO.id);
    assertThat(clonedApp.getOrganizationId()).isEqualTo(sourceApp.getOrganizationId());
    assertThat(clonedApp.getName()).isEqualTo(clonedAppName);
    assertThat(clonedApp.getPublicId()).isEqualTo(clonedAppPublicId);
    assertThat(clonedApp.getContactInternalName()).isEqualTo(contactUsername);
    assertThat(clonedApp.isPolicyViolationGrandfatheringEnabled()).isFalse();
  }

  @Test
  public void testCloneApplication_DuplicateApplicationName() {
    String clonedAppName = "clonedAppName";
    Application sourceApp = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent("appPublicId", clonedAppName);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      appCloneService.cloneApplication(sourceApp.getId(), clonedAppName, "clonedAppPublicId");
    }).withMessage("An application with name '" + clonedAppName + "' already exists.");
  }

  @Test
  public void testCloneApplication_DuplicateApplicationPublicId() {
    String clonedAppPublicId = "clonedAppPublicId";
    Application sourceApp = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent(clonedAppPublicId, "aAppName");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", clonedAppPublicId);
    }).withMessage("An application with public ID '" + clonedAppPublicId + "' already exists.");
  }
}
