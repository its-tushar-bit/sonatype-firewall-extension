/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DashboardService
{

  private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

  private static GenericVersionScheme versionScheme = new GenericVersionScheme();

  private ApplicationDAO applicationDAO;

  private ApplicationService applicationService;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyViolationAdapter policyViolationAdapter;

  private PolicyViolationDAO policyViolationDAO;

  @Inject
  public DashboardService(ApplicationDAO applicationDAO, ApplicationService applicationService,
      PolicyEvaluationDAO policyEvaluationDAO, PolicyViolationAdapter policyViolationAdapter,
      PolicyViolationDAO policyViolationDAO)
  {
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationAdapter = policyViolationAdapter;
    this.policyViolationDAO = policyViolationDAO;
  }

  /**
   * @param applicationPublicIds A list of application public ids to get policy violations.
   * @param stageTypeId The stage to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param violationFilter A filter for violations, defaults to accept all violations.
   * @return A list of {@link PolicyViolationDTO}s for the provided application public ids.
   * @throws BadRequestException Thrown if the list of application public ids is null, empty, the stage type id is
   *           unknown, or the first element is an empty string.
   * @throws com.sonatype.insight.error.exception.NotFoundException Thrown if one of the provided application ids does
   *           not match an existing application.
   * @throws org.apache.shiro.authz.UnauthenticatedException Thrown if the user has not logged in.
   * @throws org.apache.shiro.authz.UnauthorizedException Thrown if the user is not authorized to read one of the
   *           applications provided.
   */
  public List<PolicyViolationDTO> getPolicyViolationsByApplicationIds(List<String> applicationPublicIds,
      @Nullable String stageTypeId, @Nullable Predicate<PolicyViolation> violationFilter)
  {
    StageType stage = getStageType(stageTypeId);
    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    // The first item being an empty string occurs when someone GETs with a query parameter that has no value (i.e.
    // ?applicationPublicIds&stageId=release).
    if (applicationPublicIds == null || applicationPublicIds.isEmpty() || applicationPublicIds.get(0).isEmpty()) {
      throw new BadRequestException("Unable to get policy violations for null or empty application public IDs.");
    }

    for (String applicationPublicId : applicationPublicIds) {
      // getPolicyViolations is handling the read authentication for each application public Id.
      policyViolationDTOs
          .addAll(getPolicyViolationsByApplicationId(applicationPublicId, stage, violationFilter, false));
    }

    return sort(policyViolationDTOs);
  }

  /**
   * @param stageTypeId The stage to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param violationFilter A filter for violations, defaults to accept all violations.
   * @return A list of {@link PolicyViolationDTO}s for all applications with read permissions.
   * @throws BadRequestException Thrown if the stageTypeId does not match a known {@link StageType}.
   */
  public List<PolicyViolationDTO> getPolicyViolations(@Nullable String stageTypeId,
      @Nullable Predicate<PolicyViolation> violationFilter)
  {
    StageType stage = getStageType(stageTypeId);
    List<Application> applications = applicationService.getApplicationsWithReadPermission();

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();
    for (Application application : applications) {
      PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
          stage.getId());

      if (policyEvaluation != null) {
        List<PolicyViolation> filteredViolations = filter(
            policyViolationDAO.getByEvaluationId(policyEvaluation.getId()), violationFilter);

        policyViolationDTOs.addAll(policyViolationAdapter.createPolicyViolationDTOs(application, filteredViolations));
      }
    }

    return sort(policyViolationDTOs);
  }

  List<PolicyViolation> filter(List<PolicyViolation> violations, Predicate<PolicyViolation> violationFilter) {
    if (violationFilter == null || violations == null || violations.isEmpty()) {
      return violations;
    }

    return Lists.newArrayList(Collections2.filter(violations, violationFilter));
  }

  /**
   * @return Sort by threat level (descending), policy name, application name, and then coordinates.
   */
  List<PolicyViolationDTO> sort(List<PolicyViolationDTO> dtos) {
    Collections.sort(dtos, new Comparator<PolicyViolationDTO>()
    {

      @Override
      public int compare(PolicyViolationDTO v1, PolicyViolationDTO v2) {
        int result = v2.threatLevel - v1.threatLevel;
        if (result != 0) {
          return result;
        }

        result = v1.policyName.compareToIgnoreCase(v2.policyName);
        if (result != 0) {
          return result;
        }

        result = v1.applicationName.compareToIgnoreCase(v2.applicationName);
        if (result != 0) {
          return result;
        }

        return compareCoordinates(v1, v2);
      }

    });
    return dtos;
  }

  private int compareCoordinates(PolicyViolationDTO v1, PolicyViolationDTO v2) {
    int result = 0;

    // Null elements are infinitely large.
    if (v1.groupId == null && v2.groupId != null) {
      return 1;
    }
    else if (v1.groupId != null && v2.groupId == null) {
      return -1;
    }
    else if (v1.groupId != null && v2.groupId != null) {
      result = v1.groupId.compareToIgnoreCase(v2.groupId);
      if (result != 0) {
        return result;
      }
    }

    if (v1.artifactId == null && v2.artifactId != null) {
      return 1;
    }
    else if (v1.artifactId != null && v2.artifactId == null) {
      return -1;
    }
    else if (v1.artifactId != null && v2.artifactId != null) {
      result = v1.artifactId.compareToIgnoreCase(v2.artifactId);
      if (result != 0) {
        return result;
      }
    }

    if (v1.version == null && v2.version != null) {
      return 1;
    }
    else if (v1.version != null && v2.version == null) {
      return -1;
    }
    else if (v1.version != null && v2.version != null) {
      try {
        Version parsedVersion1 = versionScheme.parseVersion(v1.version);
        Version parsedVersion2 = versionScheme.parseVersion(v2.version);
        return parsedVersion1.compareTo(parsedVersion2);
      }
      catch (InvalidVersionSpecificationException e) {
        log.error(
            "Unable to parse policy violation versions for policy violations with IDs {} {} and versions {} {}, defaulting to string comparison.",
            v1.id, v2.id, v1.version, v2.version, e);
      }
      return v1.version.compareToIgnoreCase(v2.version);
    }

    return result;
  }

  @Authorize(permission = Permission.READ)
  protected List<PolicyViolationDTO> getPolicyViolationsByApplicationId(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId, StageType stage,
      @Nullable Predicate<PolicyViolation> violationFilter, boolean sort)
  {
    if (StringUtils.isBlank(applicationPublicId)) {
      throw new BadRequestException("Unable to get policy violations for null or empty application public id.");
    }

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
        stage.getId());

    if (policyEvaluation == null) {
      return Lists.newArrayList();
    }

    List<PolicyViolation> filteredViolations = filter(policyViolationDAO.getByEvaluationId(policyEvaluation.getId()),
        violationFilter);

    List<PolicyViolationDTO> result = policyViolationAdapter.createPolicyViolationDTOs(application, filteredViolations);
    if (sort) {
      return sort(result);
    }

    return result;
  }

  private StageType getStageType(String stageTypeId) {
    if (stageTypeId == null) {
      return StageTypes.getById(BuildStageType.ID);
    }

    StageType stage = StageTypes.getById(stageTypeId);
    if (stage == null) {
      throw new BadRequestException("Unknown stage type: " + stageTypeId + ".");
    }

    return stage;
  }
}
