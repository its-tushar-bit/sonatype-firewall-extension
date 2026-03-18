/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.io.IOException;
import java.io.InputStream;

import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencyTreeParserTest
{
  @Test
  public void testParse_EmptyDependenciesJson() throws IOException {
    DependencyTreeParser dependencyTreeParser = new DependencyTreeParser();
    ContainerNode<?> dependenciesJson = getDependenciesJson("empty-dependencies.json");
    dependencyTreeParser.parse(dependenciesJson);
    assertThat(dependencyTreeParser.getDependencyTreeMap().isEmpty()).isTrue();
    assertThat(dependencyTreeParser.getDependencyTypeMap().isEmpty()).isTrue();
  }

  @Test
  public void testParse_MissingDependencyTreeDependenciesJson() throws IOException {
    DependencyTreeParser dependencyTreeParser = new DependencyTreeParser();
    ContainerNode<?> dependenciesJson = getDependenciesJson("missing-dependency-tree-dependencies.json");
    dependencyTreeParser.parse(dependenciesJson);
    assertThat(dependencyTreeParser.getDependencyTreeMap().isEmpty()).isTrue();
    assertThat(dependencyTreeParser.getDependencyTypeMap().isEmpty()).isTrue();
  }

  @Test
  public void testParse_DependencyFlagNotSetDependenciesJson() throws IOException {
    DependencyTreeParser dependencyTreeParser = new DependencyTreeParser();
    ContainerNode<?> dependenciesJson = getDependenciesJson("dependency-flag-not-set-dependencies.json");
    dependencyTreeParser.parse(dependenciesJson);
    System.out.println(dependencyTreeParser.getDependencyTypeMap());
    assertThat(dependencyTreeParser.getDependencyTreeMap().isEmpty()).isFalse();
    assertThat(dependencyTreeParser.getDependencyTypeMap().isEmpty()).isFalse();
    assertThat(dependencyTreeParser
        .getDependencyType("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0")
        .get()).isEqualTo("T");
    assertThat(dependencyTreeParser
        .getDependencyType("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1")
        .get()).isEqualTo("T");
    assertThat(dependencyTreeParser
        .getComponentDependencies("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0")
        .get())
            .containsExactly("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    assertThat(dependencyTreeParser
        .getComponentDependencies("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1")
        .get()).isEmpty();

  }

  @Test
  public void testParse_DependencyFlagSetDependenciesJson() throws IOException {
    DependencyTreeParser dependencyTreeParser = new DependencyTreeParser();
    ContainerNode<?> dependenciesJson = getDependenciesJson("dependency-flag-set-dependencies.json");
    dependencyTreeParser.parse(dependenciesJson);
    System.out.println(dependencyTreeParser.getDependencyTypeMap());
    assertThat(dependencyTreeParser.getDependencyTreeMap().isEmpty()).isFalse();
    assertThat(dependencyTreeParser.getDependencyTypeMap().isEmpty()).isFalse();
    assertThat(dependencyTreeParser
        .getDependencyType("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0")
        .get()).isEqualTo("D");
    assertThat(dependencyTreeParser
        .getDependencyType("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1")
        .get()).isEqualTo("D");
    assertThat(dependencyTreeParser
        .getComponentDependencies("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0")
        .get())
            .containsExactly("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    assertThat(dependencyTreeParser
        .getComponentDependencies("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1")
        .get()).isEmpty();

  }

  private ContainerNode<?> getDependenciesJson(String fileName) throws IOException {
    try (InputStream inputStream =
        getClass().getResourceAsStream("/HdsComponentDependencyInformationTest/" + fileName))
    {
      byte[] buffer = inputStream.readAllBytes();
      return JsonUtils.parse(buffer);
    }
  }
}
