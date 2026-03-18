/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.List;
import java.util.Map.Entry;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.jira.JiraPolicyAlertNotifier.PolicyAlertSections;
import com.sonatype.insight.brain.jira.JiraPolicyAlertNotifier.PolicyAlertSections.Section;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertCounts;
import com.sonatype.insight.brain.service.BaseUrl;

import com.google.common.annotations.VisibleForTesting;

import static com.sonatype.insight.brain.jira.ADFNode.Type.*;

/**
 * Utility methods for building common Atlassian Document Format structures
 *
 * @since 1.95.0
 */
public class ADFBuilder
{
  private final BaseUrl baseUrl;

  public ADFBuilder(final BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  @VisibleForTesting
  ADFNode createDescription(
      Application app,
      ContactDTO appContact,
      String scanId,
      Stage stage,
      PolicyAlertCounts counts,
      List<PolicyFact> policyFacts)
  {
    ADFNode root = new ADFNode().setType(doc).setVersion(1);

    root.addContent(createHeading(1, "Nexus IQ Notification"));
    root.addContent(createChiclets(counts));
    root.addContent(createEvaluationDetailSection(app.getName(), scanId, stage.getStageName(),
        baseUrl.getConfigured() + UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), scanId),
        appContact));
    root.addContent(createHeading(2, "Policy Alerts"));

    PolicyAlertSections policyAlertSections = new PolicyAlertSections(policyFacts);
    for (Section section : policyAlertSections.getSections()) {
      root.addContent(createPolicyAlertSection(section));
    }
    return root;
  }

  private ADFNode createPolicyAlertSection(final Section section) {
    ADFNode tableNode = createTable();

    tableNode.addContent(new ADFNode().setType(tableRow)
        .addContent(new ADFNode().setType(tableCell)
            .addAttribute("background", getColorByThreatLevel(section.getThreatLevel()))
            .addAttribute("colspan", 2)
            .addAttribute("colwidth", new Integer[]{0, 80})
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text)
                    .setText(section.getThreatLevel() + " - " + section.getPolicyName())
                    .addMarks(new ADFNode().setType(textColor).addAttribute("color", "#ffffff"))
                    .addMarks(new ADFNode().setType(strong))))));
    tableNode.addContent(createTableRow("Component", "Violations", true));
    for (Entry<String, Integer> entry : section.getComponentViolationCountMap().entrySet()) {
      tableNode.addContent(createTableRow(entry.getKey(), String.valueOf(entry.getValue()), false));
    }
    return tableNode;
  }

  private ADFNode createTableRow(final String cell1, final String cell2, boolean isBold) {
    return new ADFNode().setType(tableRow)
        .addContent(new ADFNode().setType(tableCell)
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text)
                    .setText(cell1)
                    .addMarks(isBold ? new ADFNode().setType(strong) : null))))
        .addContent(new ADFNode().setType(tableCell)
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text)
                    .setText(cell2)
                    .addMarks(isBold ? new ADFNode().setType(strong) : null))));
  }

  private String getColorByThreatLevel(int threatLevel) {
    String color = "#6d98cf";
    if (threatLevel > 7) {
      color = "#ed1c24";
    }
    else if (threatLevel > 3) {
      color = "#f7941d";
    }
    else if (threatLevel > 1) {
      color = "#fedf15";
    }
    else if (threatLevel > 0) {
      color = "#006bbf";
    }
    return color;
  }

  private ADFNode createEvaluationDetailSection(
      final String appName,
      final String scanId,
      final String stageName,
      final String reportUrl,
      final ContactDTO contact)
  {
    ADFNode tableNode = createTable();

    ADFNode tr = new ADFNode().setType(tableRow)
        .addContent(new ADFNode().setType(tableCell)
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text)
                    .setText("Application")
                    .addMarks(new ADFNode().setType(strong))))
            .addAttribute("colwidth", new Integer[]{90}))
        .addContent(new ADFNode().setType(tableCell)
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text).setText(appName))));
    tableNode.addContent(tr);

    tr = new ADFNode().setType(tableRow)
        .addContent(new ADFNode().setType(tableCell)
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text)
                    .setText("Scan")
                    .addMarks(new ADFNode().setType(strong)))))
        .addContent(new ADFNode().setType(tableCell)
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text).setText(scanId + "; "))
                .addContent(new ADFNode().setType(text)
                    .setText("View detailed report")
                    .addMarks(new ADFNode().setType(link).addAttribute("href", reportUrl)))));
    tableNode.addContent(tr);

    tr = new ADFNode().setType(tableRow)
        .addContent(new ADFNode().setType(tableCell)
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text)
                    .setText("Stage")
                    .addMarks(new ADFNode().setType(strong)))))
        .addContent(new ADFNode().setType(tableCell)
            .addContent(new ADFNode().setType(paragraph)
                .addContent(new ADFNode().setType(text).setText(stageName))));
    tableNode.addContent(tr);

    if (contact != null) {
      tr = new ADFNode().setType(tableRow)
          .addContent(new ADFNode().setType(tableCell)
              .addContent(new ADFNode().setType(paragraph)
                  .addContent(new ADFNode().setType(text)
                      .setText("Contact")
                      .addMarks(new ADFNode().setType(strong)))))
          .addContent(new ADFNode().setType(tableCell)
              .addContent(new ADFNode().setType(paragraph)
                  .addContent(new ADFNode().setType(text)
                      .setText(contact.getDisplayName())
                      .addMarks(new ADFNode().setType(link).addAttribute("href", "mailto:" + contact.getEmail())))));
      tableNode.addContent(tr);
    }
    return tableNode;
  }

  private ADFNode createChiclets(final PolicyAlertCounts counts) {
    ADFNode tr = new ADFNode().setType(tableRow)
        .addContent(createChiclet(counts.getRed(), "#ed1c24"))
        .addContent(createChiclet(counts.getOrange(), "#f7931d"))
        .addContent(createChiclet(counts.getYellow(), "#ffdd17"))
        .addContent(createChiclet(counts.getDarkBlue(), "#006bbf"))
        .addContent(createChiclet(counts.getBlue(), "#6d98cf"));

    return createTable().addContent(tr);
  }

  private ADFNode createChiclet(final int count, final String color) {
    return new ADFNode().setType(tableCell)
        .addAttribute("background", color)
        .addContent(new ADFNode().setType(heading)
            .addAttribute("level", 2)
            .addContent(new ADFNode().setType(text)
                .setText(String.valueOf(count))
                .addMarks(new ADFNode().setType(textColor).addAttribute("color", "#ffffff"))
                .addMarks(new ADFNode().setType(strong))));
  }

  private ADFNode createHeading(final int level, final String content) {
    return new ADFNode().setType(heading)
        .addAttribute("level", level)
        .addContent(new ADFNode().setType(text).setText(content));
  }

  private ADFNode createTable() {
    return new ADFNode().setType(table)
        .addAttribute("isNumberColumnEnabled", false)
        .addAttribute("layout", "default");
  }
}
