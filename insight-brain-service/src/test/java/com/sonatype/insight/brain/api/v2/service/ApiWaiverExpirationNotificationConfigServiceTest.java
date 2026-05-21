/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverExpirationNotificationConfigDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.WaiverExpirationNotificationConfigDAO;
import com.sonatype.insight.brain.model.configuration.WaiverExpirationNotificationConfig;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiWaiverExpirationNotificationConfigServiceTest
{
  private ApiWaiverExpirationNotificationConfigService service;

  private WaiverExpirationNotificationConfigDAO dao;

  @Before
  public void setUp() {
    dao = mock(WaiverExpirationNotificationConfigDAO.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    ObjectMapper objectMapper = new ObjectMapper();
    service = new ApiWaiverExpirationNotificationConfigService(dao, ownerDAO, objectMapper);
  }

  // ---- validateDirectEmails ----

  @Test
  public void saveConfig_rejectsInvalidEmailFormat() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(List.of("notanemail"));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("Invalid email address");
  }

  @Test
  public void saveConfig_rejectsMissingAtSign() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(List.of("foo.bar.com"));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("Invalid email address");
  }

  @Test
  public void saveConfig_rejectsMissingDomain() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(List.of("foo@"));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("Invalid email address");
  }

  @Test
  public void saveConfig_rejectsEmailWithSpaces() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(List.of("foo @bar.com"));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("Invalid email address");
  }

  @Test
  public void saveConfig_rejectsOneInvalidEmailAmongMultiple() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(Arrays.asList("valid@example.com", "bademail"));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("Invalid email address");
  }

  @Test
  public void saveConfig_acceptsValidEmailAddresses() throws Exception {
    // Should not throw — we just check it makes it past validation
    // (dao.save will be a no-op on the mock, so this is purely a validation test)
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(Arrays.asList("alice@example.com", "bob.smith@corp.org"));
    dto.setRoleIds(Collections.emptyList());

    // No exception expected
    service.saveConfig("owner-1", dto);
  }

  @Test
  public void saveConfig_skipsEmailValidationWhenRecipientTypeIsRole() {
    // When recipient type is ROLE, directEmails are irrelevant — no validation should run
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("ROLE");
    dto.setDirectEmails(List.of("notanemail"));
    dto.setRoleIds(List.of("role-1"));

    // No exception expected — ROLE type doesn't use directEmails
    service.saveConfig("owner-1", dto);
  }

  @Test
  public void saveConfig_validatesEmailsWhenRecipientTypeIsBoth() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("BOTH");
    dto.setDirectEmails(List.of("bademail"));
    dto.setRoleIds(List.of("role-1"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("Invalid email address");
  }

  // ---- validateNotificationDays ----

  @Test
  public void saveConfig_rejectsNullNotificationDays() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(null);
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(List.of("alice@example.com"));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("At least one notification day threshold is required");
  }

  @Test
  public void saveConfig_rejectsEmptyNotificationDays() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(Collections.emptyList());
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(List.of("alice@example.com"));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("At least one notification day threshold is required");
  }

  @Test
  public void saveConfig_rejectsEmptyDirectEmailsWhenRecipientTypeIsDirect() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(Collections.emptyList());
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("At least one recipient");
  }

  @Test
  public void saveConfig_rejectsNullDirectEmailsWhenRecipientTypeIsDirect() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(null);
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("At least one recipient");
  }

  @Test
  public void saveConfig_rejectsEmptyDirectEmailsWhenRecipientTypeIsBoth() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("BOTH");
    dto.setDirectEmails(Collections.emptyList());
    dto.setRoleIds(List.of("role-1"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("At least one recipient");
  }

  // ---- validateRecipientType ----

  @Test
  public void saveConfig_rejectsNullRecipientType() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType(null);
    dto.setDirectEmails(Collections.emptyList());
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("recipientType");
  }

  @Test
  public void saveConfig_rejectsInvalidRecipientType() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("INVALID");
    dto.setDirectEmails(Collections.emptyList());
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("recipientType");
  }

  @Test
  public void saveConfig_rejectsNotificationDayAbove365() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(366));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(List.of("alice@example.com"));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("between 1 and 365");
  }

  @Test
  public void saveConfig_accepts365AsNotificationDay() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(365));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(List.of("alice@example.com"));
    dto.setRoleIds(Collections.emptyList());

    // No exception expected — 365 is the maximum allowed value
    service.saveConfig("owner-1", dto);
  }

  @Test
  public void saveConfig_rejectsBlankEmailInDirectEmailsList() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(Arrays.asList("alice@example.com", ""));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("must not be blank");
  }

  @Test
  public void saveConfig_rejectsNullEmailInDirectEmailsList() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setInheritConfig(false);
    dto.setNotificationDays(List.of(7));
    dto.setRecipientType("DIRECT");
    dto.setDirectEmails(Arrays.asList("alice@example.com", null));
    dto.setRoleIds(Collections.emptyList());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.saveConfig("owner-1", dto))
        .withMessageContaining("must not be blank");
  }

  // ---- deserializeNotificationDays (corrupt DB data) ----

  @Test
  public void getConfig_toleratesCorruptNotificationDaysInDatabase() {
    WaiverExpirationNotificationConfig entity = new WaiverExpirationNotificationConfig(
        "owner-1", "7,abc", null);
    when(dao.findByOwnerId("owner-1")).thenReturn(Optional.of(entity));

    // Should not throw — corrupt entry is skipped, valid entry is returned
    ApiWaiverExpirationNotificationConfigDTO result = service.getConfig("owner-1");
    assertThat(result.getNotificationDays()).containsExactly(7);
  }
}
