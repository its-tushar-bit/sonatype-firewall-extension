/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.variant.AbstractBrainInjectedH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class DashboardPolicyWaiverDTOTest
    extends AbstractBrainInjectedH2Test
{
  private final DateTimeFormatter csvDateFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(TimeZone.getTimeZone("UTC").toZoneId());

  private final Instant testNormalizedTime = Instant.now();

  @Test
  public void testGetCSVHeaders() {
    final String expectedHeaders = "Waiver Id, Threat level, Created Date, Expiration Date, Policy Id, Policy Name, " +
        "Policy Constraints, Scope Type, Scope Id, Scope Name, Component Match Strategy, Component Hash, " +
        "Component Name, Upgrade, Created by Id, Created by Name,Comment, Is Auto Waiver, " +
        "Is Expire When Remediation Available Waiver, Waiver Reason Id, Waiver Reason Text";
    String actualHeaders = DashboardPolicyWaiverDTO.getCsvHeader();
    assertThat(actualHeaders).isEqualTo(expectedHeaders);
  }

  @Test
  public void testToCsvLine_TurnsOptionalNullFieldsToEmptyString() {
    String dtoAsCSV = getTestDto().toCsvLine();
    StringBuilder expectedLine = new StringBuilder();
    expectedLine.append("waiverId");
    addJoiner(expectedLine);
    expectedLine.append("7");
    addJoiner(expectedLine);
    expectedLine.append(csvDateFormatter.format(testNormalizedTime));
    addJoiner(expectedLine);
    expectedLine.append(""); /* expiry time */
    addJoiner(expectedLine);
    expectedLine.append("policyId");
    addJoiner(expectedLine);
    expectedLine.append("test policy");
    addJoiner(expectedLine);
    addJoiner(expectedLine);
    expectedLine.append("organization");
    addJoiner(expectedLine);
    expectedLine.append("ownerId");
    addJoiner(expectedLine);
    expectedLine.append("ownerName");
    addJoiner(expectedLine);
    expectedLine.append("ALL_COMPONENTS");
    addJoiner(expectedLine);
    expectedLine.append("hash");
    addJoiner(expectedLine);
    addJoiner(expectedLine);
    expectedLine.append("Available");
    addJoiner(expectedLine);
    expectedLine.append("admin");
    addJoiner(expectedLine);
    expectedLine.append("Admin User");
    addJoiner(expectedLine);
    expectedLine.append(""); /* comments */
    addJoiner(expectedLine);
    expectedLine.append("false"); /* is auto waiver */
    addJoiner(expectedLine);
    expectedLine.append("false"); /* is auto expiry waiver */
    addJoiner(expectedLine);
    expectedLine.append(""); /* waiver reason id */
    addJoiner(expectedLine);
    expectedLine.append(""); /* waiver reason text */

    assertThat(dtoAsCSV).isEqualTo(expectedLine.toString());
  }

  @Test
  public void testToCsvLine_TurnsNullFieldsToEmptyString() {
    // here we are checking that fields added without a default value into the waiver table can also be exported
    DashboardPolicyWaiverDTO testDto = getTestDto();
    testDto.creatorId = null;
    testDto.creatorName = null;
    testDto.componentUpgradeAvailable = null;

    String dtoAsCSV = testDto.toCsvLine();
    StringBuilder expectedLine = new StringBuilder();
    expectedLine.append("waiverId");
    addJoiner(expectedLine);
    expectedLine.append("7");
    addJoiner(expectedLine);
    expectedLine.append(csvDateFormatter.format(testNormalizedTime));
    addJoiner(expectedLine);
    expectedLine.append(""); /* expiry time */
    addJoiner(expectedLine);
    expectedLine.append("policyId");
    addJoiner(expectedLine);
    expectedLine.append("test policy");
    addJoiner(expectedLine);
    addJoiner(expectedLine);
    expectedLine.append("organization");
    addJoiner(expectedLine);
    expectedLine.append("ownerId");
    addJoiner(expectedLine);
    expectedLine.append("ownerName");
    addJoiner(expectedLine);
    expectedLine.append("ALL_COMPONENTS");
    addJoiner(expectedLine);
    expectedLine.append("hash");
    addJoiner(expectedLine);
    expectedLine.append(""); /* component name */
    addJoiner(expectedLine);
    expectedLine.append("");
    addJoiner(expectedLine);
    expectedLine.append(""); /* creator Id */
    addJoiner(expectedLine);
    expectedLine.append(""); /* creator name */
    addJoiner(expectedLine);
    expectedLine.append(""); /* comments */
    addJoiner(expectedLine);
    expectedLine.append("false"); /* is auto waiver */
    addJoiner(expectedLine);
    expectedLine.append("false"); /* is auto expiry waiver */
    addJoiner(expectedLine);
    expectedLine.append(""); /* waiver reason id */
    addJoiner(expectedLine);
    expectedLine.append(""); /* waiver reason text */

    assertThat(dtoAsCSV).isEqualTo(expectedLine.toString());
  }

  @Test
  public void testToCsvLine_EscapesCommaAndQuotesInConstraints() {
    DashboardPolicyWaiverDTO testDto = getTestDto();

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact =
        new ConditionFact(ConditionTypes.SecurityVulnerabilityStatusConditionType.getId(), 0, "summary,of,condition",
            "reason",
            triggerReference);
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint, name", "operator", conditionFact);

    testDto.constraintFacts = Collections.singletonList(constraintFact);

    final String expectedConstraintStringInitial = "\"[{\"\"constraintId\"\":\"\"constraint id\"\",";
    final String expectedConstraintStringEnding =
        "{\"\"value\"\":\"\"vulnerability-1\"\",\"\"type\"\":\"\"SECURITY_VULNERABILITY_REFID\"\"}," +
            "\"\"triggerJson\"\":null}]}]\"";

    assertThat(testDto.toCsvLine()).contains(expectedConstraintStringInitial);
    assertThat(testDto.toCsvLine()).contains(expectedConstraintStringEnding);
  }

  @Test
  public void testToCsvLine_EscapesCommaInCreator() {
    DashboardPolicyWaiverDTO testDto = getTestDto();
    testDto.creatorName = "Juan Camilo de la Rosa, e Ibanez";

    final int creatorNameColumnIndex = 15;
    final String[] splittedCsv = testDto.toCsvLine().split(",");
    final String creatorNameFullField =
        splittedCsv[creatorNameColumnIndex] + "," + splittedCsv[creatorNameColumnIndex + 1];
    assertThat(creatorNameFullField).isEqualTo("\"Juan Camilo de la Rosa, e Ibanez\"");
  }

  @Test
  public void testToCsvLine_EscapesCommaInComments() {
    DashboardPolicyWaiverDTO testDto = getTestDto();
    testDto.comment = "comment that includes, comma, and comma";
    final int commentColumnIndex = 16;
    final String[] splittedCsv = testDto.toCsvLine().split(",");
    final String fullCommentsWithCommas =
        splittedCsv[commentColumnIndex] + "," + splittedCsv[commentColumnIndex + 1] + "," +
            splittedCsv[commentColumnIndex + 2];
    assertThat(fullCommentsWithCommas).isEqualTo("\"comment that includes, comma, and comma\"");
  }

  @Test
  public void testToCsvLine_EscapesNewLinesInComments() {
    DashboardPolicyWaiverDTO testDto = getTestDto();
    testDto.comment = "comment that includes new line \n and return \r\n";

    final int commentColumnIndex = 16;
    assertThat(testDto.toCsvLine().split(",")[commentColumnIndex]).isEqualTo(
        "\"comment that includes new line \n and return \r\n\"");
  }

  @Test
  public void testToCsvLine_EscapesQuotesInComments() {
    DashboardPolicyWaiverDTO testDto = getTestDto();
    testDto.comment = "comment that includes \"Quotes\"";

    final int commentColumnIndex = 16;
    assertThat(testDto.toCsvLine().split(",")[commentColumnIndex]).isEqualTo(
        "\"comment that includes \"\"Quotes\"\"\"");
  }

  @Test
  public void testToCsvLine_FormatsCreatedTime() {
    DashboardPolicyWaiverDTO testDto = getTestDto();
    Date createdDate = Date.from(testNormalizedTime.minus(Duration.ofDays(4)));
    testDto.createTime = createdDate;

    final int createdTimeColumnIndex = 2;
    String expectedDate = csvDateFormatter.format(createdDate.toInstant());
    assertThat(testDto.toCsvLine().split(",")[createdTimeColumnIndex]).isEqualTo(expectedDate);
  }

  @Test
  public void testToCsvLine_FormatsExpiryTime() {
    DashboardPolicyWaiverDTO testDto = getTestDto();
    Date expirationDate = Date.from(testNormalizedTime.plus(Duration.ofDays(15)));
    testDto.expiryTime = expirationDate;

    final int expirationTimeColumnIndex = 3;
    String expectedDate = csvDateFormatter.format(expirationDate.toInstant());
    assertThat(testDto.toCsvLine().split(",")[expirationTimeColumnIndex]).isEqualTo(expectedDate);
  }

  @Test
  public void testGetComponentUpgradeAvailableValueCSVExport() {
    String componentUpgradeAvailableValueCSVExport =
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(true);
    assertThat(componentUpgradeAvailableValueCSVExport).isEqualTo("Available");
  }

  @Test
  public void testGetComponentUpgradeAvailableValueCSVExport_upgradePathNotAvailable() {
    String componentUpgradeAvailableValueCSVExport =
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(null);
    assertThat(componentUpgradeAvailableValueCSVExport).isEmpty();
  }

  private DashboardPolicyWaiverDTO getTestDto() {
    DashboardPolicyWaiverDTO dto = new DashboardPolicyWaiverDTO();
    dto.id = "waiverId";
    dto.threatLevel = 7;
    dto.createTime = Date.from(testNormalizedTime);
    dto.expiryTime = null;
    dto.policyId = "policyId";
    dto.policyName = "test policy";
    dto.ownerId = "ownerId";
    dto.ownerName = "ownerName";
    dto.ownerType = OwnerType.ORGANIZATION.toString();
    dto.componentMatchStrategy = ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
    dto.hash = "hash";
    dto.constraintFacts = null;
    dto.comment = null;
    dto.creatorId = "admin";
    dto.creatorName = "Admin User";
    dto.componentIdentifier = null;
    dto.componentUpgradeAvailable = true;
    return dto;
  }

  private void addJoiner(final StringBuilder builder) {
    builder.append(",");
  }
}
