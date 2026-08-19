/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.AbstractComponentConditionType;
import com.sonatype.insight.brain.model.policy.conditions.AbstractLicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.AbstractVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DroolsGenerator
{
  private static final Logger log = LoggerFactory.getLogger(DroolsGenerator.class);

  public static final String LICENSE_THREAT_GROUP_VARIABLE = "$licenseThreatGroup";

  private static final String INDENT = "    ";

  private DroolsGenerator() {
  }

  public static void generate(Policy policy, LabelDAO labelDAO) {
    try (TransactionContext tx = labelDAO.createTransactionContext()) {
      generate(tx, policy);
    }
  }

  public static void generate(TransactionContext tx, Policy policy) {
    long start = System.currentTimeMillis();

    final StringBuilder droolsCode = new StringBuilder();

    droolsCode.append("import com.sonatype.insight.brain.model.component.*\n");
    droolsCode.append("import com.sonatype.insight.brain.model.license.*\n");
    droolsCode.append("import com.sonatype.insight.brain.model.policy.facts.*\n");
    droolsCode.append("import com.sonatype.insight.brain.model.policy.conditions.*\n");
    droolsCode.append("import java.util.List\n");
    droolsCode.append("import java.util.ArrayList\n");
    droolsCode.append('\n');
    droolsCode.append("// Begin policy: ")
        .append(policy.getName())
        .append(" (Id=")
        .append(policy.getId())
        .append(")\n");

    for (final Constraint constraint : policy.getConstraints()) {
      droolsCode.append("// Begin constraint: ")
          .append(constraint.getName())
          .append(" (Id=")
          .append(constraint.getId())
          .append(")\n");

      if (constraint.getOperator() == LogicalOperator.AND) {
        droolsCode.append("rule \"").append(constraint.getId()).append("\"\n");
        droolsCode.append("when\n");
        ConditionGenerator conditionGenerator = new ConditionGenerator();
        int conditionIndex = 0;
        for (final Condition condition : constraint.getConditions()) {
          conditionGenerator.add(tx, condition, conditionIndex);
          conditionIndex++;
        }
        droolsCode.append(conditionGenerator.generateConditionCode());

        droolsCode.append("then\n");
        droolsCode.append(INDENT).append("List $conditionTriggers = new ArrayList();\n");
        droolsCode.append(conditionGenerator.generateConditionTriggerCode());
        droolsCode.append(INDENT)
            .append("insert( new MatchFact( $component, \"")
            .append(policy.getId())
            .append("\", \"");
        droolsCode.append(constraint.getId()).append("\", $conditionTriggers ) );\n");
        droolsCode.append("end\n");
      }
      else {
        int conditionIndex = 0;
        for (final Condition condition : constraint.getConditions()) {
          droolsCode.append("rule \"").append(constraint.getId()).append("#").append(conditionIndex).append("\"\n");
          droolsCode.append("when\n");
          ConditionGenerator conditionGenerator = new ConditionGenerator();
          conditionGenerator.add(tx, condition, conditionIndex);
          droolsCode.append(conditionGenerator.generateConditionCode());

          droolsCode.append("then\n");
          droolsCode.append(INDENT).append("List $conditionTriggers = new ArrayList();\n");
          droolsCode.append(conditionGenerator.generateConditionTriggerCode());
          droolsCode.append(INDENT)
              .append("insert( new MatchFact( $component, \"")
              .append(policy.getId())
              .append("\", \"");
          droolsCode.append(constraint.getId())
              .append("\", ")
              .append(conditionIndex)
              .append(", $conditionTriggers ) );\n");
          droolsCode.append("end\n");

          conditionIndex++;
        }
      }

      droolsCode.append("// End constraint: ").append(constraint.getName()).append('\n');
    }

    droolsCode.append("// End policy: ").append(policy.getName()).append('\n');

    policy.setDroolsCode(droolsCode.toString());
    log.debug("Generated drools code:\n{}", policy.getDroolsCode());
    log.debug("Generated drools code in {} millisecs", System.currentTimeMillis() - start);
  }

  public static String get(List<Policy> policies) {
    StringBuilder droolsCodeBuilder = new StringBuilder();
    for (Policy policy : policies) {
      droolsCodeBuilder.append(policy.getDroolsCode()).append('\n');
    }
    return droolsCodeBuilder.toString();
  }

  private static class ConditionGenerator
  {
    private final StringBuilder componentConditionCode = new StringBuilder();

    private final StringBuilder vulnerabilityConditionCode = new StringBuilder();

    private final StringBuilder licenseThreatGroupConditionCode = new StringBuilder();

    private final StringBuilder otherConditionCode = new StringBuilder();

    private final StringBuilder conditionTriggerCode = new StringBuilder();

    public void add(TransactionContext tx, Condition condition, int conditionIndex) {
      ConditionType conditionType = ConditionTypes.getById(condition.getConditionTypeId());
      String conditionCode = conditionType.generateDroolsConditionCode(tx, condition);
      if (conditionType instanceof AbstractComponentConditionType) {
        appendConditionCode(componentConditionCode, conditionCode);
      }
      else if (conditionType instanceof AbstractVulnerabilityConditionType) {
        appendConditionCode(vulnerabilityConditionCode, conditionCode);
      }
      else if (conditionType instanceof AbstractLicenseThreatGroupConditionType) {
        appendConditionCode(licenseThreatGroupConditionCode, conditionCode);
      }
      else {
        otherConditionCode.append(INDENT).append(conditionCode).append("\n");
      }

      String triggerCode = conditionType.generateDroolsTriggerCode(condition, conditionIndex);
      if (triggerCode != null) {
        conditionTriggerCode.append(INDENT).append(triggerCode).append("\n");
      }
    }

    private static void appendConditionCode(StringBuilder code, String conditionCode) {
      if (code.length() > 0) {
        code.append(INDENT).append(INDENT).append("&&\n");
      }
      code.append(INDENT).append(INDENT).append("( ");
      code.append(conditionCode);
      code.append(" )\n");
    }

    public CharSequence generateConditionCode() {
      StringBuilder code = new StringBuilder();

      code.append(INDENT).append("$component : Component\n");
      code.append(INDENT).append("(\n");
      code.append(componentConditionCode);
      code.append(INDENT).append(")\n");

      if (vulnerabilityConditionCode.length() > 0) {
        code.append(INDENT).append("$securityVulnerability : (SecurityVulnerability\n");
        code.append(INDENT).append("(\n");
        code.append(vulnerabilityConditionCode);
        code.append(INDENT).append(") from $component.securityVulnerabilities)\n");
      }

      if (licenseThreatGroupConditionCode.length() > 0) {
        code.append(INDENT).append(LICENSE_THREAT_GROUP_VARIABLE).append(" : (LicenseThreatGroup\n");
        code.append(INDENT).append("(\n");
        code.append(licenseThreatGroupConditionCode);
        code.append(INDENT).append(") from $component.licenseThreatGroups)\n");
      }

      if (otherConditionCode.length() > 0) {
        code.append(otherConditionCode);
      }

      return code;
    }

    public CharSequence generateConditionTriggerCode() {
      return conditionTriggerCode.toString();
    }
  }
}
