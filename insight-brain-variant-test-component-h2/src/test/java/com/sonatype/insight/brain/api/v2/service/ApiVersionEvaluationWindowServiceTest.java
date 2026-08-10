/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowsDTO;
import com.sonatype.insight.brain.dataaccess.configuration.VersionEvaluationWindowDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.VersionEvaluationWindow;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentH2Test
public class ApiVersionEvaluationWindowServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiVersionEvaluationWindowService service;

  @Inject
  private VersionEvaluationWindowDAO dao;

  @Test
  public void testGetVersionEvaluationWindows() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    tempEntity.newVersionEvaluationWindow(org.getId(), "context2", 20, 60);

    ApiVersionEvaluationWindowsDTO result = service.getVersionEvaluationWindows(org);

    assertThat(result).isNotNull();
    assertThat(result.versionEvaluationWindows()).hasSize(2);
    assertThat(result.versionEvaluationWindows())
        .extracting(ApiVersionEvaluationWindowDTO::contextId)
        .containsExactlyInAnyOrder("context1", "context2");
  }

  @Test
  public void testGetVersionEvaluationWindows_Empty() {
    Organization org = tempEntity.newOrganization();

    ApiVersionEvaluationWindowsDTO result = service.getVersionEvaluationWindows(org);

    assertThat(result).isNotNull();
    assertThat(result.versionEvaluationWindows()).isEmpty();
  }

  @Test
  public void testGetVersionEvaluationWindows_MultipleOrganizations() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    tempEntity.newVersionEvaluationWindow(org1.getId(), "context1", 10, 30);
    tempEntity.newVersionEvaluationWindow(org2.getId(), "context2", 20, 60);

    ApiVersionEvaluationWindowsDTO result1 = service.getVersionEvaluationWindows(org1);
    ApiVersionEvaluationWindowsDTO result2 = service.getVersionEvaluationWindows(org2);

    assertThat(result1.versionEvaluationWindows()).hasSize(1);
    assertThat(result1.versionEvaluationWindows().get(0).contextId()).isEqualTo("context1");
    assertThat(result2.versionEvaluationWindows()).hasSize(1);
    assertThat(result2.versionEvaluationWindows().get(0).contextId()).isEqualTo("context2");
  }

  @Test
  public void testSetVersionEvaluationWindow_Insert() {
    Organization org = tempEntity.newOrganization();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    service.setVersionEvaluationWindow(org, dto);

    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(org.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getContextId()).isEqualTo("context1");
    assertThat(stored.getMaxVersions()).isEqualTo(10);
    assertThat(stored.getMaxAgeInDays()).isEqualTo(30);
  }

  @Test
  public void testSetVersionEvaluationWindow_Update() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 20, 60);

    service.setVersionEvaluationWindow(org, dto);

    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(org.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getMaxVersions()).isEqualTo(20);
    assertThat(stored.getMaxAgeInDays()).isEqualTo(60);
  }

  @Test
  public void testSetVersionEvaluationWindow_BothNullValues_ThrowsException() {
    Organization org = tempEntity.newOrganization();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", null, null);

    assertThatThrownBy(() -> service.setVersionEvaluationWindow(org, dto))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("At least one of maxVersions or maxAgeInDays must be specified");
  }

  @Test
  public void testSetVersionEvaluationWindow_UpdateToOnlyMaxVersions() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 20, null);

    service.setVersionEvaluationWindow(org, dto);

    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(org.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getMaxVersions()).isEqualTo(20);
    assertThat(stored.getMaxAgeInDays()).isNull();
  }

  @Test
  public void testSetVersionEvaluationWindow_UpdateToOnlyMaxAgeInDays() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", null, 60);

    service.setVersionEvaluationWindow(org, dto);

    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(org.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getMaxVersions()).isNull();
    assertThat(stored.getMaxAgeInDays()).isEqualTo(60);
  }

  @Test
  public void testSetVersionEvaluationWindow_InsertWithOnlyMaxVersions() {
    Organization org = tempEntity.newOrganization();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, null);

    service.setVersionEvaluationWindow(org, dto);

    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(org.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getMaxVersions()).isEqualTo(10);
    assertThat(stored.getMaxAgeInDays()).isNull();
  }

  @Test
  public void testSetVersionEvaluationWindow_InsertWithOnlyMaxAgeInDays() {
    Organization org = tempEntity.newOrganization();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", null, 30);

    service.setVersionEvaluationWindow(org, dto);

    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(org.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getMaxVersions()).isNull();
    assertThat(stored.getMaxAgeInDays()).isEqualTo(30);
  }

  @Test
  public void testDeleteVersionEvaluationWindows_SpecificContext() {
    Organization org = tempEntity.newOrganization();
    VersionEvaluationWindow window1 = tempEntity.newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    VersionEvaluationWindow window2 = tempEntity.newVersionEvaluationWindow(org.getId(), "context2", 20, 60);

    service.deleteVersionEvaluationWindows(org, "context1");

    assertThat(dao.getById(window1.getId())).isNull();
    assertThat(dao.getById(window2.getId())).isNotNull();
  }

  @Test
  public void testDeleteVersionEvaluationWindows_AllContexts() {
    Organization org = tempEntity.newOrganization();
    VersionEvaluationWindow window1 = tempEntity.newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    VersionEvaluationWindow window2 = tempEntity.newVersionEvaluationWindow(org.getId(), "context2", 20, 60);

    service.deleteVersionEvaluationWindows(org, null);

    assertThat(dao.getById(window1.getId())).isNull();
    assertThat(dao.getById(window2.getId())).isNull();
  }

  @Test
  public void testDeleteVersionEvaluationWindows_DoesNotAffectOtherOwners() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    VersionEvaluationWindow window1 = tempEntity.newVersionEvaluationWindow(org1.getId(), "context1", 10, 30);
    VersionEvaluationWindow window2 = tempEntity.newVersionEvaluationWindow(org2.getId(), "context1", 20, 60);

    service.deleteVersionEvaluationWindows(org1, "context1");

    assertThat(dao.getById(window1.getId())).isNull();
    assertThat(dao.getById(window2.getId())).isNotNull();
  }
}
