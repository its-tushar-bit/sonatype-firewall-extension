/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.json.store.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyViolationLogger
{
  public static final String POLICY_VIOLATION_LOGGER_NAME = "com.sonatype.insight.policy.violation";

  private static final Logger POLICY_VIOLATION_LOGGER = LoggerFactory.getLogger(POLICY_VIOLATION_LOGGER_NAME);

  private static final ObjectMapper POLICY_VIOLATION_OBJECT_MAPPER = new ObjectMapper();

  private final boolean enabled;

  private List<PolicyViolationData> policyViolationData = new LinkedList<>();

  private Organization organization;

  private Application application;

  PolicyViolationLogger(boolean licensed, Application application) {
    enabled = licensed && POLICY_VIOLATION_LOGGER.isInfoEnabled();
    if (enabled) {
      this.application = application;
      organization = new OrganizationDAO().getById(application.getOrganizationId());
    }
  }

  public void add(PolicyViolationLogEvent policyViolationLogEvent, PolicyViolation policyViolation) {
    if (enabled) {
      policyViolationData.add(new PolicyViolationData(policyViolationLogEvent, policyViolation));
    }
  }

  public void log() {
    policyViolationData.forEach(policyViolationData -> POLICY_VIOLATION_LOGGER.info(toString(policyViolationData)));
    policyViolationData = new LinkedList<>();
  }

  private String toString(PolicyViolationData policyViolationData) {
    try {
      return POLICY_VIOLATION_OBJECT_MAPPER.writeValueAsString(
          createPolicyViolationLogDTO(policyViolationData.policyViolationLogEvent, organization, application,
              policyViolationData.policyViolation));
    }
    catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static PolicyViolationLogDTO createPolicyViolationLogDTO(PolicyViolationLogEvent policyViolationLogEvent,
                                                                   Organization organization,
                                                                   Application application,
                                                                   PolicyViolation policyViolation)
  {
    PolicyViolationLogDTO policyViolationLogDTO = new PolicyViolationLogDTO();
    policyViolationLogDTO.eventType = policyViolationLogEvent.name().toLowerCase(Locale.ROOT);
    policyViolationLogDTO.eventTimestamp = ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(policyViolation.getOpenTime().getTime()), ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    policyViolationLogDTO.policyViolationId = policyViolation.getId();
    policyViolationLogDTO.policyId = policyViolation.getPolicyId();
    policyViolationLogDTO.policyName = policyViolation.getPolicyName();
    policyViolationLogDTO.policyThreatCategory = policyViolation.getThreatCategory().getName();
    policyViolationLogDTO.policyThreatLevel = policyViolation.getThreatLevel();
    policyViolationLogDTO.stageTypeId = policyViolation.getStageTypeId();
    policyViolationLogDTO.stagePolicyAction =
        policyViolation.getActionTypeId() == null ? "none" : policyViolation.getActionTypeId();
    policyViolationLogDTO.applicationId = policyViolation.getApplicationId();
    policyViolationLogDTO.applicationPublicId = application.getPublicId();
    policyViolationLogDTO.applicationName = application.getName();
    policyViolationLogDTO.organizationId = application.getOrganizationId();
    policyViolationLogDTO.organizationName = organization.getName();
    policyViolationLogDTO.componentIdentifier = policyViolation.getComponentIdentifier();
    policyViolationLogDTO.componentHash = policyViolation.getHash();
    return policyViolationLogDTO;
  }

  private static class PolicyViolationData
  {
    public PolicyViolationLogEvent policyViolationLogEvent;

    public PolicyViolation policyViolation;

    public PolicyViolationData(PolicyViolationLogEvent policyViolationLogEvent, PolicyViolation policyViolation) {
      this.policyViolationLogEvent = policyViolationLogEvent;
      this.policyViolation = policyViolation;
    }
  }
}
