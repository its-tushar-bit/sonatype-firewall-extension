/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Collections;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

public class ApplicationManagementSummaryDTO
{
  private String id;

  private String publicId;

  private String name;

  private String organizationId;

  private Map<String, PolicyEvaluation> policyEvaluations;

  private Map<String, PolicyEvaluationResult> policyEvaluationsResults;

  private int scansCount;

  private ContactDTO contact;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public Map<String, PolicyEvaluation> getPolicyEvaluations() {
    return (policyEvaluations != null) ? policyEvaluations : Collections.<String, PolicyEvaluation>emptyMap();
  }

  public void setPolicyEvaluations(Map<String, PolicyEvaluation> policyEvaluations) {
    this.policyEvaluations = policyEvaluations;
  }

  public Map<String, PolicyEvaluationResult> getPolicyEvaluationsResults() {
    return (policyEvaluationsResults != null) ? policyEvaluationsResults : Collections
        .<String, PolicyEvaluationResult>emptyMap();
  }

  public void setPolicyEvaluationsResults(Map<String, PolicyEvaluationResult> policyEvaluationsResults) {
    this.policyEvaluationsResults = policyEvaluationsResults;
  }

  public int getScansCount() {
    return scansCount;
  }

  public void setScansCount(int scansCount) {
    this.scansCount = scansCount;
  }

  /**
   * Get the contact DTO for the application management summary DTO
   *
   * @return the contact DTO
   * @since 1.8
   */
  public ContactDTO getContact() {
    return contact;
  }

  /**
   * Set the contact DTO for the application management summary DTO
   *
   * @param contact the contact DTO
   * @since 1.8
   */
  public void setContact(final ContactDTO contact) {
    this.contact = contact;
  }

  @Override
  public String toString() {
    return "ApplicationManagementSummary [publicId=" + publicId + ", name=" + name + "]";
  }
}
