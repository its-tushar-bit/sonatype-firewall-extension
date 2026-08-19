/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.jira.ADFNode.Type;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertCounts;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ADFBuilderTest
    extends AbstractComponentH2Test
{
  // Subject
  private ADFBuilder adfBuilder;

  @Inject
  private BaseUrl baseUrl;

  private Application app;

  private final Stage stage = new Stage(Stage.ID_BUILD, "Build");

  private final PolicyAlertCounts counts = new PolicyAlertCounts(5, 4, 3, 2, 1);

  private final ContactDTO contactDTO = new ContactDTO();

  @BeforeEach
  public void before() {
    setBaseUrl("http://localhost");
    app = tempEntity.newApplicationWithParent();
    adfBuilder = new ADFBuilder(baseUrl);
  }

  @Test
  public void testCreateDescription_ChicletsSection() {
    // when:
    ADFNode node = adfBuilder.createDescription(app, contactDTO, "scanId", stage, counts, null);

    // then:
    assertThat(node).isNotNull();
    ADFNode tableNode = node.getContent().get(1);
    assertThat(tableNode.getType()).isEqualTo(Type.table);
    ADFNode trNode = tableNode.getContent().get(0);
    assertThat(trNode.getType()).isEqualTo(Type.tableRow);
    assertThat(trNode.getContent().get(0).toString()).contains(String.valueOf(counts.getRed()));
    assertThat(trNode.getContent().get(1).toString()).contains(String.valueOf(counts.getOrange()));
    assertThat(trNode.getContent().get(2).toString()).contains(String.valueOf(counts.getYellow()));
    assertThat(trNode.getContent().get(3).toString()).contains(String.valueOf(counts.getDarkBlue()));
    assertThat(trNode.getContent().get(4).toString()).contains(String.valueOf(counts.getBlue()));
  }

  @Test
  public void testCreateDescription_EvaluationDetailSection() {
    // when:
    ADFNode node = adfBuilder.createDescription(app, contactDTO, "scanId", stage, counts, null);

    // then:
    assertThat(node).isNotNull();
    ADFNode tableNode = node.getContent().get(2);
    assertThat(tableNode.getType()).isEqualTo(Type.table);
    assertThat(tableNode.getContent().get(0).toString()).contains(app.getName());
    assertThat(tableNode.getContent().get(1).toString()).contains("scanId");
    assertThat(tableNode.getContent().get(2).toString()).contains(stage.getStageName());
  }

  @Test
  public void testCreateDescription_PolicyAlertsSection() {
    // given:
    List<PolicyFact> facts = new ArrayList<>();
    facts.add(new PolicyFact("123", "Security Critical", 10));

    // when:
    ADFNode node = adfBuilder.createDescription(app, contactDTO, "scanId", stage, counts, facts);

    // then:
    assertThat(node).isNotNull();
    assertThat(node.getContent().get(3).toString()).contains("Policy Alerts");
    ADFNode tableNode = node.getContent().get(4);
    assertThat(tableNode.getType()).isEqualTo(Type.table);
    assertThat(tableNode.getContent().get(0).toString()).contains("10 - Security Critical");
  }
}
