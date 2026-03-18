/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scm.event;

import java.io.UncheckedIOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract base logger for source control events. To provide structured JSON logging for SCM operations.
 */
public abstract class AbstractSourceControlEventLogger
{
  public static final String SCM_EVENT_LOGGER_NAME = "com.sonatype.insight.brain.scm.event";

  private static final Logger SCM_EVENT_LOGGER = getLogger(SCM_EVENT_LOGGER_NAME);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final TenantUtil TENANT_UTIL = new TenantUtil();

  private final boolean enabled;

  private final CurrentUser currentUser;

  private final Application application;

  private final Organization organization;

  private final GitRepositoryInfo gitRepositoryInfo;

  private final String formattedLogTimestamp;

  private final List<SourceControlEventData> eventData = new LinkedList<>();

  protected AbstractSourceControlEventLogger(
      final Date logTimestamp,
      final Application application,
      final Organization organization,
      final GitRepositoryInfo gitRepositoryInfo,
      final CurrentUser currentUser)
  {
    this.enabled = TENANT_UTIL.isMultiTenant() && SCM_EVENT_LOGGER.isInfoEnabled();
    this.formattedLogTimestamp = ofInstant(
        ofEpochMilli(logTimestamp.getTime()), systemDefault()).format(ISO_OFFSET_DATE_TIME);
    this.application = application;
    this.organization = organization;
    this.gitRepositoryInfo = gitRepositoryInfo;
    this.currentUser = currentUser;
  }

  public void add(final SourceControlEventType eventType, final SourceControlEventData data) {
    if (enabled) {
      data.eventType = eventType;
      eventData.add(data);
    }
  }

  public void log() {
    eventData.forEach(data -> SCM_EVENT_LOGGER.info(toString(data)));
    eventData.clear();
  }

  protected SourceControlEventLogDTO createSourceControlEventLogDTO(final SourceControlEventData data) {
    SourceControlEventLogDTO dto = new SourceControlEventLogDTO();
    dto.userName = currentUser.getUsernameOrSystem();
    dto.eventType = data.eventType.name().toLowerCase(Locale.ROOT);
    dto.eventTimestamp = formattedLogTimestamp;

    if (application != null) {
      dto.applicationId = application.getId();
      dto.applicationPublicId = application.getPublicId();
      dto.applicationName = application.getName();
    }

    if (organization != null) {
      dto.organizationId = organization.getId();
      dto.organizationName = organization.getName();
    }

    if (gitRepositoryInfo != null && gitRepositoryInfo.getProvider() != null) {
      dto.scmProvider = gitRepositoryInfo.getProvider().name();
      dto.repositoryUrl = gitRepositoryInfo.getRepositoryUrl();
    }

    if (data.pullRequestNumber != null) {
      dto.pullRequestNumber = data.pullRequestNumber;
    }

    if (data.violationsAppeared != null) {
      dto.violationsAppeared = data.violationsAppeared;
    }

    if (data.violationsCleared != null) {
      dto.violationsCleared = data.violationsCleared;
    }

    if (data.errorMessage != null) {
      dto.errorMessage = data.errorMessage;
    }

    dto.tenant = MDC.get("tenant");

    return dto;
  }

  public boolean isEnabled() {
    return enabled;
  }

  private String toString(final SourceControlEventData data) {
    try {
      return OBJECT_MAPPER.writeValueAsString(createSourceControlEventLogDTO(data));
    }
    catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static class SourceControlEventData
  {
    public SourceControlEventType eventType;

    public String pullRequestNumber;

    public Integer violationsAppeared;

    public Integer violationsCleared;

    public String errorMessage;

    public static SourceControlEventData forComment(
        final String pullRequestNumber,
        final Integer violationsAppeared,
        final Integer violationsCleared)
    {
      SourceControlEventData data = new SourceControlEventData();
      data.pullRequestNumber = pullRequestNumber;
      data.violationsAppeared = violationsAppeared;
      data.violationsCleared = violationsCleared;
      return data;
    }

    public static SourceControlEventData forPullRequest(final String pullRequestNumber) {
      SourceControlEventData data = new SourceControlEventData();
      data.pullRequestNumber = pullRequestNumber;
      return data;
    }

    public static SourceControlEventData forError(final String errorMessage) {
      SourceControlEventData data = new SourceControlEventData();
      data.errorMessage = errorMessage;
      return data;
    }
  }
}
