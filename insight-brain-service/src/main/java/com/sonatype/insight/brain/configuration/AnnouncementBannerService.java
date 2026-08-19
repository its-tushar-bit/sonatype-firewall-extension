/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.AnnouncementBannerDAO;
import com.sonatype.insight.brain.model.configuration.AnnouncementBanner;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * Service for the deployment-global announcement banner. The write path is intentionally unprotected by Shiro:
 * its only caller is the MTIQ {@code @MtiqAdminEndpoint} resource, which is network- and JWT-protected. On-prem
 * never compiles in that resource, so {@link #updateBanner} has no caller there.
 */
@Named
public class AnnouncementBannerService
{
  // Ordered so the validation error message reads consistently across JVM runs (Set.of iteration order isn't
  // guaranteed). Declaration order matches the severity escalation shown in the UI.
  static final List<String> VALID_SEVERITIES = List.of("info", "warning", "critical");

  private final AnnouncementBannerDAO dao;

  @Inject
  public AnnouncementBannerService(final AnnouncementBannerDAO dao) {
    this.dao = dao;
  }

  public AnnouncementBanner getBanner() {
    AnnouncementBanner banner = dao.get();
    return banner != null ? banner : disabledDefault();
  }

  public AnnouncementBanner updateBanner(final AnnouncementBanner input) {
    if (input == null) {
      throw new BadRequestException("Announcement banner payload is required.");
    }
    normalizeAndValidate(input);
    // DAO mutates input with final id and updatedAt, so it's authoritative post-write; returning it avoids a
    // DB round-trip and the read-after-write race it would open against a concurrent writer.
    dao.update(input);
    return input;
  }

  private void normalizeAndValidate(final AnnouncementBanner banner) {
    if (banner.getSeverity() == null) {
      // Column is NOT NULL DEFAULT 'info' but the default only fires on INSERT. Coerce here to preserve the
      // invariant on UPDATE.
      banner.setSeverity("info");
    }
    else if (!VALID_SEVERITIES.contains(banner.getSeverity())) {
      throw new BadRequestException("severity must be one of " + VALID_SEVERITIES);
    }
    if (banner.isEnabled()) {
      if (StringUtils.isBlank(banner.getWindowId())) {
        banner.setWindowId(UUID.randomUUID().toString());
      }
      if (StringUtils.isBlank(banner.getMessage())) {
        throw new BadRequestException("message is required when enabled=true");
      }
      // An enabled banner must carry a fully-specified window; a half-specified window (one date missing)
      // would be silently accepted as open-ended, which is almost never what the operator intended.
      if (banner.getDisplayFrom() == null || banner.getDisplayUntil() == null) {
        throw new BadRequestException(
            "displayFrom and displayUntil are both required when enabled=true");
      }
      if (banner.getDisplayFrom().isAfter(banner.getDisplayUntil())) {
        throw new BadRequestException("displayFrom must be <= displayUntil");
      }
    }
  }

  private AnnouncementBanner disabledDefault() {
    AnnouncementBanner banner = new AnnouncementBanner();
    banner.setId(AnnouncementBannerDAO.SINGLETON_ENTITY_ID);
    banner.setEnabled(false);
    banner.setSeverity("info");
    return banner;
  }
}
