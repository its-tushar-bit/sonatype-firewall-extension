/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.definition.KnowledgePackage;
import org.drools.io.ResourceFactory;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;

public class DroolsScratchPad
{
  public static void main(String[] args) throws Exception {
    new DroolsScratchPad().run();
  }

  private String getDroolsCode() {
    return getDroolsCode3();
  }

  private String getDroolsCode1() {
    String code = "";
    code += "import com.sonatype.insight.brain.model.component.Component\n";
    code += "import com.sonatype.insight.brain.model.policy.facts.MatchFact\n";
    code += "import com.sonatype.insight.brain.model.policy.conditions.*\n\n";
    code += "rule \"2203f0b50c9a47a4bf09a363eb3e27c1\"\n";
    code += "when\n";
    code += "  $component : Component\n";
    code += "  (\n";
    code += "    ( ConditionTypes.SecurityVulnerabilitySeverityConditionType.evaluateCondition(this, \">=\", Float.valueOf( (float) 7 )) )\n";
    code += "    &&\n";
    code += "    ( ConditionTypes.SecurityVulnerabilitySeverityConditionType.evaluateCondition(this, \"<=\", Float.valueOf( (float) 9 )) )\n";
    code += "    &&\n";
    code += "    ( ConditionTypes.SecurityVulnerabilityStatusConditionType.evaluateCondition(this, \"is not\", \"NOT_APPLICABLE\") )\n";
    code += "  )\n";
    code += "then\n";
    code += "  insert( new MatchFact( $component, \"policyId\", \"constraintId\" ) );\n";
    code += "end\n";
    return code;
  }

  private String getDroolsCode2() {
    String code = "";
    code += "import com.sonatype.insight.brain.model.component.*\n";
    code += "import com.sonatype.insight.brain.model.policy.facts.MatchFact\n";
    code += "import com.sonatype.insight.brain.model.policy.conditions.*\n\n";
    code += "rule \"2203f0b50c9a47a4bf09a363eb3e27c1\"\n";
    code += "when\n";
    code += "  $component : Component()\n";
    code += "  $vuln : SecurityVulnerability(severity >= 7, severity <= 9, status != SecurityVulnerabilityStatus.NOT_APPLICABLE) from $component.securityVulnerabilities\n";
    code += "then\n";
    code += "  insert( new MatchFact( $component, \"policyId\", \"constraintId\" ) );\n";
    code += "end\n";
    return code;
  }

  private String getDroolsCode3() {
    String code = "";
    code += "import com.sonatype.insight.brain.model.component.*\n";
    code += "import com.sonatype.insight.brain.model.policy.facts.MatchFact\n";
    code += "import com.sonatype.insight.brain.model.policy.conditions.*\n\n";
    code += "rule \"2203f0b50c9a47a4bf09a363eb3e27c1\"\n";
    code += "when\n";
    code += "  $component : Component()\n";
    code += "  exists (SecurityVulnerability(severity >= 7, severity <= 9, status != SecurityVulnerabilityStatus.NOT_APPLICABLE) from $component.securityVulnerabilities)\n";
    code += "then\n";
    code += "  insert( new MatchFact( $component, \"policyId\", \"constraintId\" ) );\n";
    code += "end\n";
    return code;
  }

  private SecurityVulnerability newVulnerability(String refId, Float severity, SecurityVulnerabilityStatus status) {
    SecurityVulnerability vulnerability = new SecurityVulnerability("cve", refId, severity);
    vulnerability.setStatus(status);
    return vulnerability;
  }

  private List<Component> getComponents() {
    Component comp1 = new Component();

    comp1.setHash("12345678901234567890");
    comp1.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));

    comp1.setCatalogDate(System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000);
    comp1.setRelativePopularity(80);

    comp1.setMatchState(MatchState.EXACT);
    comp1.setProprietary(false);

    comp1.addSecurityVulnerability(newVulnerability("CVE-1234-1111", 4.0f, SecurityVulnerabilityStatus.OPEN));
    comp1.addSecurityVulnerability(newVulnerability("CVE-1234-1112", 7.0f, SecurityVulnerabilityStatus.NOT_APPLICABLE));
    comp1.addSecurityVulnerability(newVulnerability("CVE-1234-1113", 8.0f, SecurityVulnerabilityStatus.NOT_APPLICABLE));

    return Arrays.asList(comp1);
  }

  private void run() throws Exception {
    final String droolsCode = getDroolsCode();

    final KnowledgeBuilder droolsKnowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    droolsKnowledgeBuilder.add(ResourceFactory.newReaderResource(new StringReader(droolsCode)), ResourceType.DRL);
    if (droolsKnowledgeBuilder.hasErrors()) {
      throw new RuntimeException(droolsKnowledgeBuilder.getErrors().toString());
    }
    final Collection<KnowledgePackage> droolsKnowledgePackages = droolsKnowledgeBuilder.getKnowledgePackages();
    final KnowledgeBase droolsKnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
    droolsKnowledgeBase.addKnowledgePackages(droolsKnowledgePackages);
    final StatefulKnowledgeSession droolsSession = droolsKnowledgeBase.newStatefulKnowledgeSession();

    for (final Component component : getComponents()) {
      droolsSession.insert(component);
    }

    droolsSession.fireAllRules();

    List<MatchFact> matchFacts = getMatchFacts(droolsSession);

    droolsSession.dispose();

    System.out.println(matchFacts);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static List<MatchFact> getMatchFacts(StatefulKnowledgeSession droolsSession) {
    return new ArrayList<>((Collection) droolsSession.getObjects(new ObjectFilter()
    {
      @Override
      public boolean accept(final Object object) {
        return object instanceof MatchFact;
      }
    }));
  }
}
