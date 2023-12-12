/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.experimental.sast.SastScanRequestDTO.SastFindingRequestDTO;
import com.sonatype.insight.brain.api.experimental.sast.SastScanRequestDTO.SastRemediationRequestDTO;
import com.sonatype.insight.brain.api.experimental.sast.SastScanResponseDTO.SastFindingResponseDTO;
import com.sonatype.insight.brain.api.experimental.sast.SastScanResponseDTO.SastRemediationResponseDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastRemediationDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.brain.model.sast.SastRemediation;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.nonNull;

@Named
class ApiSastScanService
{
  private final SastScanDAO sastScanDAO;

  private final SastFindingDAO sastFindingDAO;

  private final SastRemediationDAO sastRemediationDAO;

  @Inject
  public ApiSastScanService(
      final SastScanDAO sastScanDAO,
      final SastFindingDAO sastFindingDAO,
      final SastRemediationDAO sastRemediationDAO)
  {
    this.sastScanDAO = sastScanDAO;
    this.sastFindingDAO = sastFindingDAO;
    this.sastRemediationDAO = sastRemediationDAO;
  }

  @Authorize(permission = Permission.READ)
  SastScanResponseDTO getSastScan(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String sastScanId)
  {
    final SastScan sastScan = sastScanDAO.getByIdNotNull(sastScanId);
    validateSastScanAssociatedWithApplication(applicationPublicId, sastScan);
    return toSastScanDTO(sastScan);
  }

  @Authorize(permission = Permission.WRITE)
  SastScanResponseDTO createSastScan(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final SastScanRequestDTO sastScanRequestDTO)
  {
    final String applicationId = IdUtils.getInternalOwnerId(OwnerType.APPLICATION, applicationPublicId);
    try (final TransactionContext tx = sastScanDAO.createTransactionContext()) {
      tx.begin();
      final SastScan sastScan = persistSastScan(tx, applicationId);
      if (nonNull(sastScanRequestDTO.findings)) {
        sastScanRequestDTO.findings.forEach(
            sastFindingRequestDTO -> createSastFinding(tx, sastScan.getId(), sastFindingRequestDTO));
      }
      tx.commit();
      auditSastScanId(sastScan.getId());
      return toSastScanDTO(sastScan);
    }
  }

  private void validateSastScanAssociatedWithApplication(final String applicationPublicId, final SastScan sastScan) {
    final String requestedApplicationId = IdUtils.getInternalOwnerId(OwnerType.APPLICATION, applicationPublicId);
    if (!requestedApplicationId.equals(sastScan.getApplicationId())) {
      throw new NotFoundException("Could not find SastScan");
    }
  }

  private SastScan persistSastScan(final TransactionContext tx, final String applicationId) {
    final SastScan sastScan = new SastScan(applicationId);
    sastScanDAO.insert(tx, sastScan);
    return sastScan;
  }

  private void createSastFinding(
      final TransactionContext tx,
      final String sastScanId,
      final SastFindingRequestDTO sastFindingRequestDTO)
  {
    final SastFinding sastFinding = persistSastFinding(tx, sastScanId, sastFindingRequestDTO);
    if (nonNull(sastFindingRequestDTO.remediations)) {
      sastFindingRequestDTO.remediations
          .forEach(sastRemediationResponseDTO -> {
            persistSastRemediation(tx, sastFinding.getId(), sastRemediationResponseDTO);
          });
    }
  }

  private SastFinding persistSastFinding(
      final TransactionContext tx,
      final String sastScanId,
      final SastFindingRequestDTO sastFindingRequestDTO)
  {
    final SastFinding sastFinding = fromSastFindingDTO(sastScanId, sastFindingRequestDTO);
    sastFindingDAO.insert(tx, sastFinding);
    return sastFinding;
  }

  private void persistSastRemediation(
      final TransactionContext tx,
      final String sastFindingId,
      final SastRemediationRequestDTO sastRemediationRequestDTO)
  {
    sastRemediationDAO.insert(tx, fromSastRemediationDTO(sastFindingId, sastRemediationRequestDTO));
  }

  private SastScanResponseDTO toSastScanDTO(final SastScan sastScan) {
    final SastScanResponseDTO sastScanResponseDTO = new SastScanResponseDTO();
    sastScanResponseDTO.sastScanId = sastScan.getId();
    sastScanResponseDTO.createdAt = sastScan.getCreatedAt();
    sastScanResponseDTO.findings = toSastFindingDTOs(sastScan.getId());
    return sastScanResponseDTO;
  }

  private List<SastFindingResponseDTO> toSastFindingDTOs(final String sastScanId) {
    return sastFindingDAO.getBySastScanIdOrderBySeverityDesc(sastScanId)
        .stream()
        .map(this::toSastFindingResponseDTO)
        .collect(toImmutableList());
  }

  private SastFindingResponseDTO toSastFindingResponseDTO(final SastFinding finding) {
    final SastFindingResponseDTO findingDTO = new SastFindingResponseDTO();
    findingDTO.sastFindingId = finding.getId();
    findingDTO.confidence = finding.getConfidenceEnum().name();
    findingDTO.severity = finding.getSeverityEnum().name();

    try {
      findingDTO.coordinate = jsonStringToMap(finding.getCoordinate());
    }
    catch (JsonProcessingException e) {
      throw new InternalServerException("failed to create coordinate json object", e);
    }

    findingDTO.cwe = finding.getCwe();
    findingDTO.description = finding.getDescription();
    findingDTO.lineNumber = finding.getLineNumber();
    findingDTO.ruleName = finding.getRuleName();
    findingDTO.remediations = toSastRemediationResponseDTOs(finding.getId());
    return findingDTO;
  }

  private List<SastRemediationResponseDTO> toSastRemediationResponseDTOs(final String sastFindingId) {
    return sastRemediationDAO.getBySastFindingId(sastFindingId)
        .stream()
        .map(this::toSastRemediationResponseDTO)
        .collect(toImmutableList());
  }

  private SastRemediationResponseDTO toSastRemediationResponseDTO(final SastRemediation remediation) {
    final SastRemediationResponseDTO remediationDTO = new SastRemediationResponseDTO();
    remediationDTO.sastRemediationId = remediation.getId();
    remediationDTO.content = remediation.getContent();
    return remediationDTO;
  }

  private SastFinding fromSastFindingDTO(final String sastScanId, final SastFindingRequestDTO sastFindingRequestDTO) {
    final SastFinding sastFinding = new SastFinding();
    sastFinding.setSastScanId(sastScanId);
    sastFinding.setSeverity(validateSastFindingSeverityText(sastFindingRequestDTO.severity));
    sastFinding.setConfidence(validateSastFindingConfidenceText(sastFindingRequestDTO.confidence));

    try {
      sastFinding.setCoordinate(mapToJsonString(sastFindingRequestDTO.coordinate));
    }
    catch (JsonProcessingException e) {
      throw new BadRequestException("Could not process coordinate", e);
    }

    sastFinding.setRuleName(sastFindingRequestDTO.ruleName);
    sastFinding.setLineNumber(sastFindingRequestDTO.lineNumber);
    sastFinding.setDescription(sastFindingRequestDTO.description);
    sastFinding.setCwe(sastFindingRequestDTO.cwe);
    return sastFinding;
  }

  private SastRemediation fromSastRemediationDTO(
      final String sastFindingId,
      final SastRemediationRequestDTO sastRemediationRequestDTO)
  {
    return new SastRemediation(sastFindingId, sastRemediationRequestDTO.content);
  }

  private SastFindingSeverity validateSastFindingSeverityText(final String sastFindingSeverity) {
    return validateEnumValue(SastFindingSeverity.class, sastFindingSeverity);
  }

  private SastFindingConfidence validateSastFindingConfidenceText(final String sastFindingConfidence) {
    return validateEnumValue(SastFindingConfidence.class, sastFindingConfidence);
  }

  private <E extends Enum<E>> E validateEnumValue(final Class<E> enumClass, final String text) {
    try {
      return Enum.valueOf(enumClass, text);
    }
    catch (NullPointerException | IllegalArgumentException ex) {
      throw new BadRequestException("Invalid value for " + enumClass.getSimpleName(), ex);
    }
  }

  private void auditSastScanId(final String sastScanId) {
    AuditData.get().setData("sastScanId", sastScanId);
  }

  private static Map<String, Object> jsonStringToMap(final String jsonString) throws JsonProcessingException {
    final ObjectMapper mapper = configureObjectMapper(new ObjectMapper());
    final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>(){};
    return mapper.readValue(jsonString, typeRef);
  }

  private static String mapToJsonString(final Map<String, Object> map) throws JsonProcessingException {
    final ObjectMapper mapper = configureObjectMapper(new ObjectMapper());
    return mapper.writeValueAsString(map);
  }

  private static ObjectMapper configureObjectMapper(ObjectMapper mapper) {
    mapper.registerModule(new JodaModule());
    mapper.registerModule(new ParameterNamesModule());
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }
}
