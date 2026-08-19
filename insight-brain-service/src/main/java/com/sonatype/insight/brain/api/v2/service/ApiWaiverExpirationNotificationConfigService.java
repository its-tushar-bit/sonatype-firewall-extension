/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverExpirationNotificationConfigDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.WaiverExpirationNotificationConfigDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.WaiverExpirationNotificationConfig;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for reading and writing waiver expiration notification configuration.
 * <p>
 * A missing row means "inherit from parent". The service walks the owner hierarchy to find
 * the effective configuration.
 */
@Named
@Singleton
public class ApiWaiverExpirationNotificationConfigService
{
  private static final Logger log = LoggerFactory.getLogger(ApiWaiverExpirationNotificationConfigService.class);

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  private static final Set<String> VALID_RECIPIENT_TYPES = Set.of("DIRECT", "ROLE", "BOTH");

  private final WaiverExpirationNotificationConfigDAO dao;

  private final OwnerDAO ownerDAO;

  private final ObjectMapper objectMapper;

  @Inject
  public ApiWaiverExpirationNotificationConfigService(
      final WaiverExpirationNotificationConfigDAO dao,
      final OwnerDAO ownerDAO,
      final ObjectMapper objectMapper)
  {
    this.dao = dao;
    this.ownerDAO = ownerDAO;
    this.objectMapper = objectMapper;
  }

  /**
   * Returns the effective notification config for an owner, walking up the hierarchy if the owner has no custom config.
   */
  public ApiWaiverExpirationNotificationConfigDTO getConfig(final String ownerId) {
    Optional<WaiverExpirationNotificationConfig> ownConfig = dao.findByOwnerId(ownerId);

    if (ownConfig.isPresent()) {
      ApiWaiverExpirationNotificationConfigDTO dto = toDTO(ownConfig.get());
      dto.setInheritConfig(false);
      return dto;
    }

    // No custom config — root org cannot inherit, so return empty custom config
    if (Organization.ROOT_ORGANIZATION_ID.equals(ownerId)) {
      ApiWaiverExpirationNotificationConfigDTO dto = emptyDTO();
      dto.setInheritConfig(false);
      return dto;
    }

    // Find effective config by walking hierarchy
    ApiWaiverExpirationNotificationConfigDTO dto = findEffectiveConfig(ownerId);
    dto.setInheritConfig(true);
    return dto;
  }

  /**
   * Saves the notification config for the given owner.
   * If {@code inheritConfig} is true, any existing custom config is deleted (reverts to inherit).
   */
  public void saveConfig(final String ownerId, final ApiWaiverExpirationNotificationConfigDTO dto) {
    if (dto.isInheritConfig()) {
      // Delete custom config so owner inherits from parent
      try (com.sonatype.insight.dataaccess.TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.deleteByOwnerId(tx, ownerId);
        tx.commit();
      }
      return;
    }

    validateNotificationDays(dto.getNotificationDays());
    validateRecipientType(dto.getRecipientType());
    validateDirectEmails(dto.getRecipientType(), dto.getDirectEmails());

    WaiverExpirationNotificationConfig entity = new WaiverExpirationNotificationConfig(
        ownerId,
        serializeNotificationDays(dto.getNotificationDays()),
        serializeNotificationsJson(dto));
    dao.save(entity);
  }

  private ApiWaiverExpirationNotificationConfigDTO findEffectiveConfig(final String ownerId) {
    List<String> ownerIds = ownerDAO.getOwnerIds(ownerId);
    if (ownerIds == null || ownerIds.size() <= 1) {
      // Owner not found or has no parents — fall through to root org fallback below
      Optional<WaiverExpirationNotificationConfig> rootConfig =
          dao.findByOwnerId(Organization.ROOT_ORGANIZATION_ID);
      return rootConfig.map(this::toDTO).orElseGet(this::emptyDTO);
    }
    // ownerIds[0] = self (already checked), walk [1..N] then root
    for (String parentId : ownerIds.subList(1, ownerIds.size())) {
      Optional<WaiverExpirationNotificationConfig> parentConfig = dao.findByOwnerId(parentId);
      if (parentConfig.isPresent()) {
        return toDTO(parentConfig.get());
      }
    }
    // Final fallback: root org
    Optional<WaiverExpirationNotificationConfig> rootConfig =
        dao.findByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    return rootConfig.map(this::toDTO).orElseGet(this::emptyDTO);
  }

  private ApiWaiverExpirationNotificationConfigDTO toDTO(final WaiverExpirationNotificationConfig entity) {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setNotificationDays(deserializeNotificationDays(entity.getNotificationDays()));

    if (entity.getNotificationsJson() != null && !entity.getNotificationsJson().isEmpty()) {
      try {
        NotificationsJson notificationsJson =
            objectMapper.readValue(entity.getNotificationsJson(), NotificationsJson.class);
        dto.setRecipientType(notificationsJson.recipientType);
        dto.setDirectEmails(notificationsJson.directEmails != null
            ? notificationsJson.directEmails
            : Collections.emptyList());
        dto.setRoleIds(notificationsJson.roleIds != null
            ? notificationsJson.roleIds
            : Collections.emptyList());
      }
      catch (JsonProcessingException e) {
        log.warn("Failed to deserialize notificationsJson for owner {} — returning empty config",
            entity.getOwnerId(), e);
        dto.setRecipientType("DIRECT");
        dto.setDirectEmails(Collections.emptyList());
        dto.setRoleIds(Collections.emptyList());
      }
    }
    else {
      dto.setRecipientType("DIRECT");
      dto.setDirectEmails(Collections.emptyList());
      dto.setRoleIds(Collections.emptyList());
    }
    return dto;
  }

  private ApiWaiverExpirationNotificationConfigDTO emptyDTO() {
    ApiWaiverExpirationNotificationConfigDTO dto = new ApiWaiverExpirationNotificationConfigDTO();
    dto.setNotificationDays(Collections.emptyList());
    dto.setDirectEmails(Collections.emptyList());
    dto.setRoleIds(Collections.emptyList());
    return dto;
  }

  private List<Integer> deserializeNotificationDays(final String notificationDays) {
    if (notificationDays == null || notificationDays.trim().isEmpty()) {
      return Collections.emptyList();
    }
    List<Integer> result = new java.util.ArrayList<>();
    for (String token : notificationDays.split(",")) {
      String trimmed = token.trim();
      if (!trimmed.isEmpty()) {
        try {
          result.add(Integer.parseInt(trimmed));
        }
        catch (NumberFormatException e) {
          log.warn("Skipping corrupt notification_days entry '{}' — not a valid integer", trimmed);
        }
      }
    }
    return result;
  }

  private String serializeNotificationDays(final List<Integer> notificationDays) {
    if (notificationDays == null || notificationDays.isEmpty()) {
      return null;
    }
    return notificationDays.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  private String serializeNotificationsJson(final ApiWaiverExpirationNotificationConfigDTO dto) {
    try {
      NotificationsJson notificationsJson = new NotificationsJson();
      notificationsJson.recipientType = dto.getRecipientType();
      notificationsJson.directEmails = dto.getDirectEmails() != null
          ? dto.getDirectEmails()
          : Collections.emptyList();
      notificationsJson.roleIds = dto.getRoleIds() != null
          ? dto.getRoleIds()
          : Collections.emptyList();
      return objectMapper.writeValueAsString(notificationsJson);
    }
    catch (JsonProcessingException e) {
      throw new BadRequestException("Failed to serialize notification config: " + e.getMessage());
    }
  }

  private void validateNotificationDays(final List<Integer> notificationDays) {
    if (notificationDays == null || notificationDays.isEmpty()) {
      throw new BadRequestException("At least one notification day threshold is required.");
    }
    if (notificationDays.size() > 3) {
      throw new BadRequestException("A maximum of 3 notification day thresholds are allowed.");
    }
    for (Integer day : notificationDays) {
      if (day == null || day < 1 || day > 365) {
        throw new BadRequestException("Each notification day threshold must be between 1 and 365.");
      }
    }
    if (notificationDays.size() != new HashSet<>(notificationDays).size()) {
      throw new BadRequestException("Notification day thresholds must be unique.");
    }
  }

  private void validateRecipientType(final String recipientType) {
    if (recipientType == null || !VALID_RECIPIENT_TYPES.contains(recipientType)) {
      throw new BadRequestException(
          "recipientType is required and must be one of: DIRECT, ROLE, BOTH.");
    }
  }

  private void validateDirectEmails(final String recipientType, final List<String> directEmails) {
    if (!"DIRECT".equals(recipientType) && !"BOTH".equals(recipientType)) {
      return;
    }
    if (directEmails == null || directEmails.isEmpty()) {
      throw new BadRequestException(
          "At least one recipient (email address or role) is required.");
    }
    for (String email : directEmails) {
      if (email == null || email.trim().isEmpty()) {
        throw new BadRequestException("Email address must not be blank.");
      }
      if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
        throw new BadRequestException("Invalid email address: '" + email + "'.");
      }
    }
  }

  /**
   * Internal JSON structure for notifications_json column.
   */
  static class NotificationsJson
  {
    public String recipientType;

    public List<String> directEmails;

    public List<String> roleIds;
  }
}
