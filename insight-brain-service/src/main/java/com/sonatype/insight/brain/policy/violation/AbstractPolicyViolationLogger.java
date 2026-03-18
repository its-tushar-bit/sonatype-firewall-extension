/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.security.CurrentUser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.sonatype.insight.brain.tenancy.AllTenantsJob.tenantUtil;

/**
 * @since 1.60
 */
public abstract class AbstractPolicyViolationLogger<T extends AbstractPolicyViolation>
{
  public static final String POLICY_VIOLATION_LOGGER_NAME = "com.sonatype.insight.policy.violation";

  private static final Logger POLICY_VIOLATION_LOGGER = LoggerFactory.getLogger(POLICY_VIOLATION_LOGGER_NAME);

  private static final ObjectMapper POLICY_VIOLATION_OBJECT_MAPPER = new ObjectMapper();

  private final boolean enabled;

  private final CurrentUser currentUser;

  /**
   * This timestamp will be used for all events logged by this logger instance.
   */
  private String formattedLogTimestamp;

  private List<PolicyViolationData<T>> policyViolationData = new LinkedList<>();

  protected AbstractPolicyViolationLogger(boolean licensed, Date logTimestamp, CurrentUser currentUser) {
    enabled = licensed && POLICY_VIOLATION_LOGGER.isInfoEnabled();
    formattedLogTimestamp =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(logTimestamp.getTime()), ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    this.currentUser = currentUser;
  }

  public void add(PolicyViolationLogEvent policyViolationLogEvent, T policyViolation) {
    if (enabled) {
      policyViolationData.add(new PolicyViolationData<>(policyViolationLogEvent, policyViolation));
    }
  }

  public void log() {
    policyViolationData.forEach(policyViolationData -> POLICY_VIOLATION_LOGGER.info(toString(policyViolationData)));
    policyViolationData = new LinkedList<>();
  }

  public void logClearEvent() {
    add(PolicyViolationLogEvent.CLEAR, null);
    log();
  }

  private String toString(PolicyViolationData<T> policyViolationData) {
    try {
      return POLICY_VIOLATION_OBJECT_MAPPER.writeValueAsString(createPolicyViolationLogDTO(policyViolationData));
    }
    catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected PolicyViolationLogDTO createPolicyViolationLogDTO(PolicyViolationData<T> policyViolationData) {
    T policyViolation = policyViolationData.policyViolation;

    PolicyViolationLogDTO policyViolationLogDTO = new PolicyViolationLogDTO();
    policyViolationLogDTO.userName = currentUser.getUsernameOrSystem();
    policyViolationLogDTO.eventType = policyViolationData.policyViolationLogEvent.name().toLowerCase(Locale.ROOT);
    policyViolationLogDTO.eventTimestamp = formattedLogTimestamp;
    if (policyViolation != null) {
      policyViolationLogDTO.policyId = policyViolation.getPolicyId();
      policyViolationLogDTO.policyName = policyViolation.getPolicyName();
      policyViolationLogDTO.policyThreatCategory = policyViolation.getThreatCategory().getName();
      policyViolationLogDTO.policyThreatLevel = policyViolation.getThreatLevel();
      if (shouldIncludeStagePolicyAction(policyViolationData.policyViolationLogEvent, policyViolation)) {
        policyViolationLogDTO.stagePolicyAction =
            policyViolation.getActionTypeId() == null ? "none" : policyViolation.getActionTypeId();
      }
      policyViolationLogDTO.policyConditionTriggers = policyViolation.getConstraintFacts()
          .stream()
          .flatMap(constraintFact -> constraintFact.getConditionFacts().stream())
          .map(ConditionFact::getReason)
          .distinct()
          .map(this::createPolicyConditionTriggerDTO)
          .collect(Collectors.toList());
      policyViolationLogDTO.componentIdentifier = policyViolation.getComponentIdentifier();
      policyViolationLogDTO.componentHash = policyViolation.getHash();
    }
    if (tenantUtil.isMultiTenant()) {
      policyViolationLogDTO.tenant = MDC.get("tenant");
    }
    return policyViolationLogDTO;
  }

  private PolicyConditionTriggerDTO createPolicyConditionTriggerDTO(String reason) {
    PolicyConditionTriggerDTO policyConditionTriggerDTO = new PolicyConditionTriggerDTO();
    policyConditionTriggerDTO.reason = reason;
    return policyConditionTriggerDTO;
  }

  protected boolean shouldIncludeStagePolicyAction(
      PolicyViolationLogEvent policyViolationLogEvent,
      @SuppressWarnings("unused") T policyViolation)
  {
    return PolicyViolationLogEvent.CREATE.equals(policyViolationLogEvent);
  }

  protected static class PolicyViolationData<T extends AbstractPolicyViolation>
  {
    public final PolicyViolationLogEvent policyViolationLogEvent;

    public final T policyViolation;

    public PolicyViolationData(
        PolicyViolationLogEvent policyViolationLogEvent,
        T policyViolation)
    {
      this.policyViolationLogEvent = policyViolationLogEvent;
      this.policyViolation = policyViolation;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }
}
