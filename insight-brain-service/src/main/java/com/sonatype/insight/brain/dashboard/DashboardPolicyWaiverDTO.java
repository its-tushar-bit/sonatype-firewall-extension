/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.utils.CsvWritable;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.Objects.nonNull;

/**
 * @since 1.147
 */
public class DashboardPolicyWaiverDTO
    implements CsvWritable
{
  public String id;

  public int threatLevel;

  public Date createTime;

  public Date expiryTime;

  public String policyId;

  public String policyName;

  public String ownerId;

  public String ownerName;

  public String ownerType;

  public ComponentMatcherStrategyForWaiver componentMatchStrategy;

  public String hash;

  public List<ConstraintFact> constraintFacts;

  public String comment;

  public String creatorId;

  public String creatorName;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public Boolean componentUpgradeAvailable;

  public Boolean isAutoWaiver = false;

  public boolean isExpireWhenRemediationAvailable = false;

  public PolicyWaiverReason policyWaiverReason;

  public boolean forContainerImage;

  @JsonProperty(access = Access.READ_ONLY)
  public ComponentDisplayName getDisplayName() {
    return this.componentIdentifier == null
        ? null
        : ComponentDisplayNameUtil.fromIdentifier(this.componentIdentifier.toComponentIdentifier());
  }

  static String getCsvHeader() {
    return "Waiver Id, Threat level, Created Date, Expiration Date, Policy Id, Policy Name, Policy Constraints, " +
        "Scope Type, Scope Id, Scope Name, Component Match Strategy, Component Hash, Component Name, " +
        "Upgrade, Created by Id, Created by Name,Comment, Is Auto Waiver, " +
        "Is Expire When Remediation Available Waiver, Waiver Reason Id, Waiver Reason Text";
  }

  public static String getComponentUpgradeAvailableValueCSVExport(Boolean isComponentUpgradeAvailable) {
    return (Boolean.TRUE.equals(isComponentUpgradeAvailable)) ? "Available" : "";
  }

  @Override
  public String toCsvLine() {
    final String createTimeCsv = formatDate(createTime);
    final String expiryTimeCsv = formatDate(expiryTime);
    final String constraintFactsJsonCsv = getConstraintFactsJsonCsv();
    final String componentHashCsv = StringUtils.defaultString(hash);
    final String displayNameCsv = ObjectUtils.defaultIfNull(getDisplayName(), "").toString();
    final String creatorIdCsv = StringUtils.defaultString(creatorId);
    final String creatorNameCsv =
        CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(StringUtils.defaultString(creatorName));
    final String waivedComponentUpgradeAvailableValueCsv =
        getComponentUpgradeAvailableValueCSVExport(componentUpgradeAvailable);

    final String policyWaiverReasonId;
    final String policyWaiverReasonText;
    if (nonNull(policyWaiverReason)) {
      policyWaiverReasonId = StringUtils.defaultString(policyWaiverReason.getId());
      policyWaiverReasonText = StringUtils.defaultString(policyWaiverReason.getReasonText());
    }
    else {
      policyWaiverReasonId = "";
      policyWaiverReasonText = "";
    }

    final String commentsCsv =
        CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(
            CsvWritable.escapeDoubleQuotes(StringUtils.defaultString(comment)));
    final String policyIdCsv = StringUtils.defaultString(policyId);
    final String policyNameCsv = StringUtils.defaultString(policyName);

    return CsvWritable.joiner.join(id, threatLevel, createTimeCsv, expiryTimeCsv, policyIdCsv, policyNameCsv,
        constraintFactsJsonCsv, ownerType, ownerId, ownerName, componentMatchStrategy, componentHashCsv, displayNameCsv,
        waivedComponentUpgradeAvailableValueCsv, creatorIdCsv, creatorNameCsv, commentsCsv, isAutoWaiver,
        isExpireWhenRemediationAvailable, policyWaiverReasonId, policyWaiverReasonText);
  }

  private String getConstraintFactsJsonCsv() {
    if (constraintFacts == null) {
      return "";
    }
    String constraintFactsJsonCsv = CsvWritable.escapeDoubleQuotes(JsonUtils.writeUnformatted(constraintFacts));
    return CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(constraintFactsJsonCsv);
  }
}
