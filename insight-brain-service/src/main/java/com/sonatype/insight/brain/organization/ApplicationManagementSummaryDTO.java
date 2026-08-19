/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

  private String organizationName;

  private Map<String, PolicyEvaluation> policyEvaluations;

  private Map<String, PolicyEvaluationResult> policyEvaluationsResults;

  private ContactDTO contact;

  private boolean hasPendingSourceControlPolicyEvaluation;

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

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(final String organizationName) {
    this.organizationName = organizationName;
  }

  public Map<String, PolicyEvaluation> getPolicyEvaluations() {
    return (policyEvaluations != null) ? policyEvaluations : Collections.emptyMap();
  }

  public void setPolicyEvaluations(Map<String, PolicyEvaluation> policyEvaluations) {
    this.policyEvaluations = policyEvaluations;
  }

  public Map<String, PolicyEvaluationResult> getPolicyEvaluationsResults() {
    return (policyEvaluationsResults != null) ? policyEvaluationsResults : Collections.emptyMap();
  }

  public void setPolicyEvaluationsResults(Map<String, PolicyEvaluationResult> policyEvaluationsResults) {
    this.policyEvaluationsResults = policyEvaluationsResults;
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

  /**
   * @return true if there is source control evaluation source control event (new or in progress) for this
   *         application; false otherwise
   */
  public boolean getHasPendingSourceControlPolicyEvaluation() {
    return hasPendingSourceControlPolicyEvaluation;
  }

  public void setHasPendingSourceControlPolicyEvaluation(boolean hasPendingSourceControlPolicyEvaluation) {
    this.hasPendingSourceControlPolicyEvaluation = hasPendingSourceControlPolicyEvaluation;
  }

  @Override
  public String toString() {
    return "ApplicationManagementSummary [publicId=" + publicId + ", name=" + name + "]";
  }
}
