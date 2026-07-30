/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentReleasedFromQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaivedPolicyViolationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.ProxyRepositoryPolicyViolationLogger;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseReason;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetryCreator;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;

/**
 * @since 1.78
 */
@Named
@Singleton
public class ApiComponentReleaseQuarantineService
{
  private static final Logger log = LoggerFactory.getLogger(ApiComponentReleaseQuarantineService.class);

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  private final ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator;

  private final PolicyDAO policyDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Inject
  public ApiComponentReleaseQuarantineService(
      RepositoryDAO repositoryDAO,
      ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator,
      ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator,
      PolicyDAO policyDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory)
  {
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.policyWaiverTelemetryCreator = policyWaiverTelemetryCreator;
    this.proxyRepositoryComponentTelemetryCreator = proxyRepositoryComponentTelemetryCreator;
    this.policyDAO = policyDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
  }

  public ApiComponentReleasedFromQuarantineDTO releaseQuarantineWithoutReEval(
      final String quarantineId,
      final String comment)
  {

    ProxyRepositoryComponent proxyRepositoryComponent = proxyRepositoryComponentDAO.getById(quarantineId);

    if (proxyRepositoryComponent == null) {
      throw new NotFoundException("Cannot find a component with quarantineId " + quarantineId + ".");
    }
    AuditData.get().setData("repositoryId", proxyRepositoryComponent.getRepositoryId());
    AuditData.get().setRepository(repositoryDAO.getById(proxyRepositoryComponent.getRepositoryId()));

    return releaseQuarantineWithoutReEval(proxyRepositoryComponent.getRepositoryId(), quarantineId, comment);
  }

  @Authorize(permission = Permission.WRITE)
  ApiComponentReleasedFromQuarantineDTO releaseQuarantineWithoutReEval(
      @AuthzContext(Key.REPOSITORY_ID) final String repositoryId,
      final String quarantineId,
      final String comment)
  {
    if (StringUtils.isBlank(comment)) {
      throw new BadRequestException("Comment has not been specified.");
    }

    ApiComponentReleasedFromQuarantineDTO componentReleasedFromQuarantineDTO =
        new ApiComponentReleasedFromQuarantineDTO();

    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      Repository repository = repositoryDAO.getById(tx, repositoryId);

      Date now = new Date();

      ProxyRepositoryComponent proxyRepositoryComponent = getRepositoryComponentByIdNotNull(tx, quarantineId);

      if (!proxyRepositoryComponent.isQuarantined()) {
        throw new BadRequestException(
            "Component with quarantineId " + quarantineId + " is not quarantined.");
      }

      List<ProxyRepositoryPolicyViolation> proxyRepositoryPolicyViolations = proxyRepositoryPolicyViolationDAO
          .getByRepositoryIdAndPathnameAndActionAndNotWaived(proxyRepositoryComponent.getRepositoryId(),
              proxyRepositoryComponent.getPathname(), Action.ID_FAIL);
      proxyRepositoryPolicyViolationDAO.loadConstraintFacts(proxyRepositoryPolicyViolations);

      List<PolicyWaiver> policyWaivers = new ArrayList<>();

      ProxyRepositoryPolicyViolationLogger policyViolationLogger =
          policyViolationLoggerFactory.newLogger(now, repository);

      for (ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation : proxyRepositoryPolicyViolations) {
        policyWaivers.add(waiveRepositoryViolation(tx, proxyRepositoryPolicyViolation, now, comment, repository));
        policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, proxyRepositoryPolicyViolation);
      }

      proxyRepositoryComponent.setUnquarantineTimeForManualRelease(now);
      proxyRepositoryComponentDAO.update(tx, proxyRepositoryComponent);

      log.debug(
          "releaseQuarantineWithoutReEval: Released component with quarantineId {} from quarantine and waived {} " +
              "repository policy violations.",
          quarantineId, proxyRepositoryPolicyViolations.size());
      tx.commit();
      policyViolationLogger.log();
      AuditData.get().setData("componentPathname", proxyRepositoryComponent.getPathname());
      AuditData.get().setComponentHash(proxyRepositoryComponent.getHash());

      componentReleasedFromQuarantineDTO.componentReleasedFromQuarantine =
          buildRepositoryComponentPolicyViolationDTO(proxyRepositoryComponent, proxyRepositoryPolicyViolations,
              policyWaivers);

      proxyRepositoryComponentTelemetryCreator
          .sendRepositoryComponentTelemetry(proxyRepositoryComponent, proxyRepositoryPolicyViolations,
              repository.getRepositoryManagerId(), repository.getPublicId(),
              RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE,
              ReleaseQuarantineType.MANUAL, ReleaseReason.WAIVED.getDescription(), Collections.emptyList());
    }

    return componentReleasedFromQuarantineDTO;
  }

  private PolicyWaiver waiveRepositoryViolation(
      TransactionContext tx,
      ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation,
      Date now,
      String comment,
      Repository repository)
  {
    String componentPurl = PackageUrlIdentifier.toPackageUrl(proxyRepositoryPolicyViolation.getComponentIdentifier());
    PolicyWaiver policyWaiver =
        new PolicyWaiver(proxyRepositoryPolicyViolation.getHash(), proxyRepositoryPolicyViolation.getPolicyId(),
            proxyRepositoryPolicyViolation.getRepositoryId(), componentPurl, EXACT_COMPONENT, comment);
    policyWaiver.setCreateTime(now);
    policyWaiver.setConstraintFactsJson(proxyRepositoryPolicyViolation.getConstraintFactsJson());

    policyWaiverDAO.insert(tx, policyWaiver);

    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CREATE_WAIVER, false)) {
      auditPolicyWaiver(policyWaiver, repository);
    }

    proxyRepositoryPolicyViolation.setWaived(true);
    proxyRepositoryPolicyViolation.setPolicyWaiverId(policyWaiver.getId());
    proxyRepositoryPolicyViolation.setPolicyWaiverComment(policyWaiver.getComment());
    proxyRepositoryPolicyViolation.setWaiveTime(now);
    proxyRepositoryPolicyViolationDAO.update(tx, proxyRepositoryPolicyViolation);

    policyWaiverTelemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, proxyRepositoryPolicyViolation);

    return policyWaiver;
  }

  private ApiRepositoryComponentPolicyViolationDTO buildRepositoryComponentPolicyViolationDTO(
      ProxyRepositoryComponent proxyRepositoryComponent,
      List<ProxyRepositoryPolicyViolation> proxyRepositoryPolicyViolations,
      List<PolicyWaiver> policyWaivers)
  {
    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        new ApiRepositoryComponentPolicyViolationDTO();
    repositoryComponentPolicyViolationDTO.component = buildRepositoryComponentDTO(proxyRepositoryComponent);
    repositoryComponentPolicyViolationDTO.waivedPolicyViolations = proxyRepositoryPolicyViolations.stream()
        .map(
            policyViolation -> buildWaivedPolicyViolationDTO(policyViolation,
                policyWaivers.stream()
                    .filter(waiver -> waiver.getHash().equals(policyViolation.getHash()))
                    .findFirst()
                    .get()))
        .collect(Collectors.toList());

    return repositoryComponentPolicyViolationDTO;
  }

  private ApiRepositoryComponentDTO buildRepositoryComponentDTO(ProxyRepositoryComponent proxyRepositoryComponent) {
    ApiRepositoryComponentDTO repositoryComponentDTO = new ApiRepositoryComponentDTO();
    ComponentIdentifier componentIdentifier = proxyRepositoryComponent.getComponentIdentifier();
    repositoryComponentDTO.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    repositoryComponentDTO.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    repositoryComponentDTO.displayName = proxyRepositoryComponent.getDisplayName();
    repositoryComponentDTO.hash = proxyRepositoryComponent.getHash();
    repositoryComponentDTO.proprietary = null;
    repositoryComponentDTO.quarantineTime = proxyRepositoryComponent.getQuarantineTime();
    repositoryComponentDTO.quarantineReleaseTime = proxyRepositoryComponent.getUnquarantineTime();

    return repositoryComponentDTO;
  }

  private ApiWaivedPolicyViolationDTO buildWaivedPolicyViolationDTO(
      ProxyRepositoryPolicyViolation policyViolation,
      PolicyWaiver policyWaiver)
  {
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = new ApiWaivedPolicyViolationDTO();
    waivedPolicyViolationDTO.policyId = policyViolation.getPolicyId();
    waivedPolicyViolationDTO.policyName = policyViolation.getPolicyName();
    waivedPolicyViolationDTO.policyViolationId = policyViolation.getId();
    waivedPolicyViolationDTO.openTime = policyViolation.getOpenTime();
    waivedPolicyViolationDTO.waiveTime = policyViolation.getWaiveTime();
    waivedPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
    waivedPolicyViolationDTO.constraintViolations = PolicyViolationAdapter.convert(policyViolation);

    // policy waiver reason is null here because we do not create waivers with reasons during
    // quarantine release
    ApiPolicyWaiverDTO policyWaiverDTO = ApiPolicyWaiverDTO.toDto(policyWaiver, null, null);
    policyWaiverDTO.isObsolete = false;

    waivedPolicyViolationDTO.policyWaiver = policyWaiverDTO;

    return waivedPolicyViolationDTO;
  }

  private ProxyRepositoryComponent getRepositoryComponentByIdNotNull(
      TransactionContext tx,
      String quarantineId)
  {
    ProxyRepositoryComponent proxyRepositoryComponent = proxyRepositoryComponentDAO.getById(tx, quarantineId);

    if (proxyRepositoryComponent == null) {
      throw new NotFoundException("Cannot find a component with quarantineId " + quarantineId + ".");
    }
    return proxyRepositoryComponent;
  }

  private void auditPolicyWaiver(PolicyWaiver policyWaiver, Repository repository) {
    AuditData.get()
        .setRepository(repository)
        .setData("policyWaiverId", policyWaiver.getId())
        .setPolicy(policyDAO.getByIdNotNull(policyWaiver.getPolicyId()))
        .setComment(policyWaiver.getComment())
        .setComponentHash(policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() != null) {
      AuditData.get()
          .setData("policyConstraints",
              policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }
}
