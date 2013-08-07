/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

public class DroolsGenerator
{
  private static final Logger log = LoggerFactory.getLogger(DroolsGenerator.class);

  private static final String INDENT = "    ";

  public String generate(final String applicationId, final List<Policy> policies) {
    long start = System.currentTimeMillis();

    final StringBuilder droolsCode = new StringBuilder();

    droolsCode.append("import com.sonatype.insight.brain.model.component.Component\n");
    droolsCode.append("import com.sonatype.insight.brain.model.policy.facts.MatchFact\n");
    droolsCode.append("import com.sonatype.insight.brain.model.policy.conditions.*\n");

    for (final Policy policy : policies) {
      if (!policy.isEnabled()) {
        continue;
      }

      ValidationResult validationResult = policy.validate(applicationId, true);
      if (validationResult != null && !validationResult.isValid()) {
        throw new InvalidPolicyException(validationResult);
      }

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
    }

    String result = droolsCode.toString();
    log.debug("Generated drools code in {} millisecs", System.currentTimeMillis() - start);

    return result;
  }
}
