/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
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
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.RepositoryPolicyViolationLogger;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final PolicyViolationAdapter policyViolationAdapter;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  public ApiComponentReleaseQuarantineService(
      RepositoryDAO repositoryDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      PolicyViolationAdapter policyViolationAdapter,
      PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.policyViolationAdapter = policyViolationAdapter;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
  }

  @Authorize(permission = Permission.WRITE)
  public ApiComponentReleasedFromQuarantineDTO releaseQuarantineWithoutReEval(
      @AuthzContext(Key.REPOSITORY_ID) final String repositoryId,
      final String packageUrl,
      final String comment)
  {
    if (StringUtils.isEmpty(comment)) {
      throw new BadRequestException("Comment has not been specified.");
    }

    ApiComponentReleasedFromQuarantineDTO componentReleasedFromQuarantineDTO =
        new ApiComponentReleasedFromQuarantineDTO();

    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      Repository repository = repositoryDAO.getById(tx, repositoryId);

      Date now = new Date();

      RepositoryComponent repositoryComponent = getAndValidateRepositoryComponent(tx, repositoryId, packageUrl);

      List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
          .getByRepositoryIdAndPathnameAndActionAndNotWaived(repositoryId, repositoryComponent.getPathname(),
              Action.ID_FAIL);

      List<PolicyWaiver> policyWaivers = new ArrayList<>();

      RepositoryPolicyViolationLogger policyViolationLogger = policyViolationLoggerFactory.newLogger(now, repository);

      for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
        policyWaivers.add(waiveRepositoryViolation(tx, repositoryPolicyViolation, now, comment));
        policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, repositoryPolicyViolation);
      }

      repositoryComponent.setUnquarantineTime(now);
      repositoryComponentDAO.update(tx, repositoryComponent);

      log.debug("releaseQuarantineWithoutReEval: Released component with packageUrl {} from quarantine and waived {} " +
          "repository policy violations.", packageUrl, repositoryPolicyViolations.size());
      tx.commit();
      policyViolationLogger.log();
      AuditData.get().setData("componentPathname", repositoryComponent.getPathname());
      AuditData.get().setComponentHash(repositoryComponent.getHash());

      componentReleasedFromQuarantineDTO.componentReleasedFromQuarantine =
          buildRepositoryComponentPolicyViolationDTO(repositoryComponent, repositoryPolicyViolations, policyWaivers);
    }

    return componentReleasedFromQuarantineDTO;
  }

  private PolicyWaiver waiveRepositoryViolation(
      TransactionContext tx,
      RepositoryPolicyViolation repositoryPolicyViolation,
      Date now,
      String comment)
  {
    PolicyWaiver policyWaiver =
        new PolicyWaiver(repositoryPolicyViolation.getHash(), repositoryPolicyViolation.getPolicyId(),
            repositoryPolicyViolation.getRepositoryId(), comment);
    policyWaiver.setCreateTime(now);
    policyWaiver.setConstraintFactsJson(repositoryPolicyViolation.getConstraintFactsJson());

    new PolicyWaiverDAO().insert(tx, policyWaiver);

    repositoryPolicyViolation.setWaived(true);
    repositoryPolicyViolation.setPolicyWaiverId(policyWaiver.getId());
    repositoryPolicyViolation.setPolicyWaiverComment(policyWaiver.getComment());
    repositoryPolicyViolation.setWaiveTime(now);
    repositoryPolicyViolationDAO.update(tx, repositoryPolicyViolation);

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
    waivedPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
    waivedPolicyViolationDTO.constraintViolations = policyViolationAdapter.convert(policyViolation);

    ApiPolicyWaiverDTO policyWaiverDTO = new ApiPolicyWaiverDTO();

    policyWaiverDTO.isObsolete = false;
    policyWaiverDTO.policyWaiverId = policyWaiver.getId();
    policyWaiverDTO.comment = policyWaiver.getComment();
    policyWaiverDTO.createTime = policyWaiver.getCreateTime();

    waivedPolicyViolationDTO.policyWaiver = policyWaiverDTO;

    return waivedPolicyViolationDTO;
  }

  private RepositoryComponent getAndValidateRepositoryComponent(
      TransactionContext tx,
      String repositoryId,
      String packageUrl)
  {
    ComponentIdentifier purlComponentIdentifier = new PackageUrlIdentifier(packageUrl).ensureCompleteIdentifier();

    List<RepositoryComponent> repositoryComponentList = repositoryComponentDAO.getByRepositoryId(tx, repositoryId);
    RepositoryComponent repositoryComponent = repositoryComponentList.stream()
        .filter(component -> purlComponentIdentifier.equals(component.getComponentIdentifier())).findFirst()
        .orElseThrow(() -> new NotFoundException(
            "Cannot find a component with packageUrl " + packageUrl + " in repository with ID " + repositoryId + "."));

    if (!repositoryComponent.isQuarantined()) {
      throw new BadRequestException(
          "Component with packageUrl " + packageUrl + " in repository " + repositoryId + " is not quarantined.");
    }
    return repositoryComponent;
  }
}
