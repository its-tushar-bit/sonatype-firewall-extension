/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryResultsDetailsDto
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryResultsDetailsDto.class);

  public Integer threatLevel;

  public String policyName;

  public String repositoryManagerId;

  public String repositoryId;

  public String componentDisplayText;

  public String pathname;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String hash;

  public String matchStateId;

  public Date lastEvaluationTime;

  public Date quarantineTime;

  public Boolean waived;

  /**
   * The constraints that caused the violation, matching Lifecycle's policythreats.json structure.
   *
   * @since 1.202
   */
  public List<PolicyThreats.PolicyConstraint> constraints;

  /**
   * The policy violation ID for enabling direct waiver operations from the results view.
   *
   * @since 1.203
   */
  public String policyViolationId;

  public RepositoryResultsDetailsDto() {
  }

  public RepositoryResultsDetailsDto(final RepositoryResultsDetails details) {
    ComponentIdentifier componentIdentifierFromJson = ComponentIdentifierAdapter
        .formatAndJsonToComponentIdentifier(details.componentIdFormat, details.componentIdCoordinatesJson);

    this.threatLevel = details.policyThreatLevel;
    this.policyName = details.policyName;
    this.repositoryManagerId = details.repositoryManagerId;
    this.repositoryId = details.repositoryId;
    this.componentDisplayText = details.componentDisplayName;
    this.pathname = details.pathname;
    this.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifierFromJson);
    this.hash = details.hash;
    this.matchStateId = details.matchStateId;
    this.lastEvaluationTime = details.lastEvaluationTime;
    this.quarantineTime = details.quarantineTime;
    this.waived = details.waived;
    this.constraints = parseConstraintFactsJson(details.constraintFactsJson);
    this.policyViolationId = details.policyViolationId;
  }

  private static List<PolicyThreats.PolicyConstraint> parseConstraintFactsJson(String constraintFactsJson) {
    if (constraintFactsJson == null || constraintFactsJson.isEmpty()) {
      return Collections.emptyList();
    }

    try {
      List<ConstraintFact> constraintFacts = Arrays.asList(
          JsonUtils.parse(constraintFactsJson, ConstraintFact[].class));
      return PolicyThreatsAdapter.toPolicyThreatsPolicyConstraints(constraintFacts);
    }
    catch (IOException e) {
      log.debug("Failed to parse constraint_facts_json", e);
      return Collections.emptyList();
    }
  }
}
