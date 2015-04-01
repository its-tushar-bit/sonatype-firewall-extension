/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DroolsGenerator
{
  private static final Logger log = LoggerFactory.getLogger(DroolsGenerator.class);

  private static final String INDENT = "    ";

  private DroolsGenerator() {
  }

  public static void generate(Policy policy) {
    long start = System.currentTimeMillis();

    final StringBuilder droolsCode = new StringBuilder();

    droolsCode.append("import com.sonatype.insight.brain.model.component.Component\n");
    droolsCode.append("import com.sonatype.insight.brain.model.policy.facts.MatchFact\n");
    droolsCode.append("import com.sonatype.insight.brain.model.policy.conditions.*\n");
    droolsCode.append('\n');
    droolsCode.append("// Begin policy: ").append(policy.getName()).append(" (Id=").append(policy.getId())
        .append(")\n");

    for (final Constraint constraint : policy.getConstraints()) {
      if (!constraint.isEnabled()) {
        continue;
      }

      droolsCode.append("// Begin constraint: ").append(constraint.getName()).append(" (Id=")
          .append(constraint.getId()).append(")\n");

      if (constraint.getOperator() == LogicalOperator.AND) {
        droolsCode.append("rule \"").append(constraint.getId()).append("\"\n");
        droolsCode.append("when\n");
        droolsCode.append(INDENT).append("$component : Component\n");
        droolsCode.append(INDENT).append("(\n");

        int conditionIndex = 0;
        for (final Condition condition : constraint.getConditions()) {
          if (conditionIndex > 0) {
            droolsCode.append(INDENT).append(INDENT).append("&&\n");
          }

          droolsCode.append(INDENT).append(INDENT).append("( ");
          final ConditionType<?> conditionType = ConditionTypes.getById(condition.getConditionTypeId());
          droolsCode.append(conditionType.generateDroolsCode(condition));
          droolsCode.append(" )\n");

          conditionIndex++;
        }

        droolsCode.append(INDENT).append(")\n");
        droolsCode.append("then\n");
        droolsCode.append(INDENT).append("insert( new MatchFact( $component, \"").append(policy.getId())
            .append("\", \"");
        droolsCode.append(constraint.getId()).append("\" ) );\n");
        droolsCode.append("end\n");
      }
      else {
        int conditionIndex = 0;
        for (final Condition condition : constraint.getConditions()) {
          droolsCode.append("rule \"").append(constraint.getId()).append("#").append(conditionIndex).append("\"\n");
          droolsCode.append("when\n");
          droolsCode.append(INDENT).append("$component : Component\n");
          droolsCode.append(INDENT).append("(\n");

          droolsCode.append(INDENT).append(INDENT).append("( ");
          final ConditionType<?> conditionType = ConditionTypes.getById(condition.getConditionTypeId());
          droolsCode.append(conditionType.generateDroolsCode(condition));
          droolsCode.append(" )\n");

          droolsCode.append(INDENT).append(")\n");
          droolsCode.append("then\n");
          droolsCode.append(INDENT).append("insert( new MatchFact( $component, \"").append(policy.getId())
              .append("\", \"");
          droolsCode.append(constraint.getId()).append("\", ").append(conditionIndex).append(" ) );\n");
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
}
