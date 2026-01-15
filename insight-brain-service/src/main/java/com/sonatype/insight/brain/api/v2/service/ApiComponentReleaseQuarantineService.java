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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.RepositoryPolicyViolationLogger;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseReason;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
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

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  private final RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  private final PolicyDAO policyDAO ;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Inject
  public ApiComponentReleaseQuarantineService(
      RepositoryDAO repositoryDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator,
      RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator,
      PolicyDAO policyDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory)
  {
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.policyWaiverTelemetryCreator = policyWaiverTelemetryCreator;
    this.repositoryComponentTelemetryCreator = repositoryComponentTelemetryCreator;
    this.policyDAO = policyDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
  }

  public ApiComponentReleasedFromQuarantineDTO releaseQuarantineWithoutReEval(
      final String quarantineId,
      final String comment)
  {

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(quarantineId);

    if (repositoryComponent == null) {
      throw new NotFoundException("Cannot find a component with quarantineId " + quarantineId + ".");
    }
    AuditData.get().setData("repositoryId", repositoryComponent.getRepositoryId());
    AuditData.get().setRepository(repositoryDAO.getById(repositoryComponent.getRepositoryId()));

    return releaseQuarantineWithoutReEval(repositoryComponent.getRepositoryId(), quarantineId, comment);
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

    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      Repository repository = repositoryDAO.getById(tx, repositoryId);

      Date now = new Date();

      RepositoryComponent repositoryComponent = getRepositoryComponentByIdNotNull(tx, quarantineId);

      if (!repositoryComponent.isQuarantined()) {
        throw new BadRequestException(
            "Component with quarantineId " + quarantineId + " is not quarantined.");
      }

      List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
          .getByRepositoryIdAndPathnameAndActionAndNotWaived(repositoryComponent.getRepositoryId(),
              repositoryComponent.getPathname(), Action.ID_FAIL);
      repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);

      List<PolicyWaiver> policyWaivers = new ArrayList<>();

      RepositoryPolicyViolationLogger policyViolationLogger = policyViolationLoggerFactory.newLogger(now, repository);

      for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
        policyWaivers.add(waiveRepositoryViolation(tx, repositoryPolicyViolation, now, comment, repository));
        policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, repositoryPolicyViolation);
      }

      repositoryComponent.setUnquarantineTimeForManualRelease(now);
      repositoryComponentDAO.update(tx, repositoryComponent);

      log.debug(
          "releaseQuarantineWithoutReEval: Released component with quarantineId {} from quarantine and waived {} " +
              "repository policy violations.", quarantineId, repositoryPolicyViolations.size());
      tx.commit();
      policyViolationLogger.log();
      AuditData.get().setData("componentPathname", repositoryComponent.getPathname());
      AuditData.get().setComponentHash(repositoryComponent.getHash());

      componentReleasedFromQuarantineDTO.componentReleasedFromQuarantine =
          buildRepositoryComponentPolicyViolationDTO(repositoryComponent, repositoryPolicyViolations, policyWaivers);

      repositoryComponentTelemetryCreator
          .sendRepositoryComponentTelemetry(repositoryComponent, repositoryPolicyViolations,
              repository.getRepositoryManagerId(), repository.getPublicId(),
              RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE,
              ReleaseQuarantineType.MANUAL, ReleaseReason.WAIVED.getDescription(), Collections.emptyList());
    }

    return componentReleasedFromQuarantineDTO;
  }

  private PolicyWaiver waiveRepositoryViolation(
      TransactionContext tx,
      RepositoryPolicyViolation repositoryPolicyViolation,
      Date now,
      String comment, Repository repository)
  {
    String componentPurl = PackageUrlIdentifier.toPackageUrl(repositoryPolicyViolation.getComponentIdentifier());
    PolicyWaiver policyWaiver =
        new PolicyWaiver(repositoryPolicyViolation.getHash(), repositoryPolicyViolation.getPolicyId(),
            repositoryPolicyViolation.getRepositoryId(), componentPurl, EXACT_COMPONENT, comment);
    policyWaiver.setCreateTime(now);
    policyWaiver.setConstraintFactsJson(repositoryPolicyViolation.getConstraintFactsJson());

    policyWaiverDAO.insert(tx, policyWaiver);

    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CREATE_WAIVER, false)) {
      auditPolicyWaiver(policyWaiver, repository);
    }

    repositoryPolicyViolation.setWaived(true);
    repositoryPolicyViolation.setPolicyWaiverId(policyWaiver.getId());
    repositoryPolicyViolation.setPolicyWaiverComment(policyWaiver.getComment());
    repositoryPolicyViolation.setWaiveTime(now);
    repositoryPolicyViolationDAO.update(tx, repositoryPolicyViolation);

    policyWaiverTelemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, repositoryPolicyViolation);

    return policyWaiver;
  }

  private ApiRepositoryComponentPolicyViolationDTO buildRepositoryComponentPolicyViolationDTO(
      RepositoryComponent repositoryComponent,
      List<RepositoryPolicyViolation> repositoryPolicyViolations,
      List<PolicyWaiver> policyWaivers)
  {
    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        new ApiRepositoryComponentPolicyViolationDTO();
    repositoryComponentPolicyViolationDTO.component = buildRepositoryComponentDTO(repositoryComponent);
    repositoryComponentPolicyViolationDTO.waivedPolicyViolations = repositoryPolicyViolations.stream().map(
        policyViolation -> buildWaivedPolicyViolationDTO(policyViolation,
            policyWaivers.stream().filter(waiver -> waiver.getHash().equals(policyViolation.getHash())).findFirst()
                .get())).collect(Collectors.toList());

    return repositoryComponentPolicyViolationDTO;
  }

  private ApiRepositoryComponentDTO buildRepositoryComponentDTO(RepositoryComponent repositoryComponent) {
    ApiRepositoryComponentDTO repositoryComponentDTO = new ApiRepositoryComponentDTO();
    ComponentIdentifier componentIdentifier = repositoryComponent.getComponentIdentifier();
    repositoryComponentDTO.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    repositoryComponentDTO.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    repositoryComponentDTO.displayName = repositoryComponent.getDisplayName();
    repositoryComponentDTO.hash = repositoryComponent.getHash();
    repositoryComponentDTO.proprietary = null;
    repositoryComponentDTO.quarantineTime = repositoryComponent.getQuarantineTime();
    repositoryComponentDTO.quarantineReleaseTime = repositoryComponent.getUnquarantineTime();

    return repositoryComponentDTO;
  }

  private ApiWaivedPolicyViolationDTO buildWaivedPolicyViolationDTO(
      RepositoryPolicyViolation policyViolation,
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

  private RepositoryComponent getRepositoryComponentByIdNotNull(
      TransactionContext tx,
      String quarantineId)
  {
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(tx, quarantineId);

    if (repositoryComponent == null) {
      throw new NotFoundException("Cannot find a component with quarantineId " + quarantineId + ".");
    }
    return repositoryComponent;
  }

  private void auditPolicyWaiver(PolicyWaiver policyWaiver, Repository repository) {
    AuditData.get().setRepository(repository)
        .setData("policyWaiverId", policyWaiver.getId())
        .setPolicy(policyDAO.getByIdNotNull(policyWaiver.getPolicyId()))
        .setComment(policyWaiver.getComment())
        .setComponentHash(policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() != null) {
      AuditData.get().setData("policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }
}
