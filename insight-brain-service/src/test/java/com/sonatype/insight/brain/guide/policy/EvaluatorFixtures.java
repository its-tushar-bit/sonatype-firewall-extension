/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.github.packageurl.PackageURL;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import org.mockito.invocation.InvocationOnMock;

final class EvaluatorFixtures
{
  private EvaluatorFixtures() {
  }

  static Organization rootOrg() {
    return org(Organization.ROOT_ORGANIZATION_ID);
  }

  static Organization org(String id) {
    Organization o = new Organization();
    o.setId(id);
    return o;
  }

  static List<ComponentEvaluationData> evaluationDataFor(String... purls) {
    List<ComponentEvaluationData> list = new ArrayList<>();
    int i = 0;
    for (String purl : purls) {
      ComponentEvaluationData data = new ComponentEvaluationData();
      data.requestIndex = i++;
      data.componentIdentifier = identifierFor(purl);
      list.add(data);
    }
    return list;
  }

  static ComponentIdentifier identifierFor(String purl) {
    try {
      PackageURL p = new PackageURL(purl);
      String type = p.getType();
      TreeMap<String, String> coords = new TreeMap<>();
      if ("maven".equals(type)) {
        if (p.getNamespace() != null) {
          coords.put(ComponentIdentifier.MAVEN_GROUP_ID, p.getNamespace());
        }
        coords.put(ComponentIdentifier.MAVEN_ARTIFACT_ID, p.getName());
        coords.put(ComponentIdentifier.VERSION, p.getVersion());
      }
      else if ("npm".equals(type)) {
        // npm uses packageId which combines namespace + name
        String packageId = (p.getNamespace() != null)
            ? p.getNamespace() + "/" + p.getName()
            : p.getName();
        coords.put(ComponentIdentifier.NPM_PACKAGE_ID, packageId);
        coords.put(ComponentIdentifier.VERSION, p.getVersion());
      }
      else {
        // generic fallback
        if (p.getNamespace() != null) {
          coords.put(ComponentIdentifier.GENERIC_NAMESPACE, p.getNamespace());
        }
        coords.put(ComponentIdentifier.GENERIC_NAME, p.getName());
        coords.put(ComponentIdentifier.VERSION, p.getVersion());
      }
      return new ComponentIdentifier(type, coords);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static Component componentForFirstArg(InvocationOnMock invocation) {
    NamedComponentDetails details = invocation.getArgument(0);
    Component c = new Component();
    c.setComponentIdentifier(details.getComponentIdentifier());
    return c;
  }
}
