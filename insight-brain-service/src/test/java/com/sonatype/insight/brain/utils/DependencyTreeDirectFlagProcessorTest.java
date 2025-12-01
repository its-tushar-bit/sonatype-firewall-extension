/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.DependencyTreeDirectFlagProcessor.populateDirectFlags;
import static org.assertj.core.api.Assertions.assertThat;

public class DependencyTreeDirectFlagProcessorTest
{
  @Test
  public void testPopulateDirectFlags_nullRoot() {
    // Should not throw exception
    populateDirectFlags(null);
  }

  @Test
  public void testPopulateDirectFlags_RootWithNoChildren() {
    ApiDependencyTreeNodeDTO root = createNode("root");

    populateDirectFlags(root);

    assertThat(root.isDirect()).isFalse();
  }

  @Test
  public void testPopulateDirectFlags_SingleDirectDependency() {
    ApiDependencyTreeNodeDTO directDep = createNode("direct-dep");
    ApiDependencyTreeNodeDTO root = createNode("root", directDep);

    populateDirectFlags(root);

    assertThat(directDep.isDirect()).isTrue();
  }

  @Test
  public void testPopulateDirectFlags_MultipleDirectDependencies() {
    ApiDependencyTreeNodeDTO direct1 = createNode("direct-1");
    ApiDependencyTreeNodeDTO direct2 = createNode("direct-2");
    ApiDependencyTreeNodeDTO direct3 = createNode("direct-3");
    ApiDependencyTreeNodeDTO root = createNode("root", direct1, direct2, direct3);

    populateDirectFlags(root);

    assertThat(direct1.isDirect()).isTrue();
    assertThat(direct2.isDirect()).isTrue();
    assertThat(direct3.isDirect()).isTrue();
  }

  @Test
  public void testPopulateDirectFlags_TransitiveDependencies() {
    ApiDependencyTreeNodeDTO transitive = createNode("transitive");
    ApiDependencyTreeNodeDTO directDep = createNode("direct-dep", transitive);
    ApiDependencyTreeNodeDTO root = createNode("root", directDep);

    populateDirectFlags(root);

    assertThat(directDep.isDirect()).isTrue();
    assertThat(transitive.isDirect()).isFalse();
  }

  @Test
  public void testPopulateDirectFlags_DeeplyNestedTransitiveDependencies() {
    // Create a deep tree: root -> direct -> transitive1 -> transitive2 -> transitive3
    ApiDependencyTreeNodeDTO transitive3 = createNode("transitive-3");
    ApiDependencyTreeNodeDTO transitive2 = createNode("transitive-2", transitive3);
    ApiDependencyTreeNodeDTO transitive1 = createNode("transitive-1", transitive2);
    ApiDependencyTreeNodeDTO directDep = createNode("direct-dep", transitive1);
    ApiDependencyTreeNodeDTO root = createNode("root", directDep);

    populateDirectFlags(root);

    assertThat(directDep.isDirect()).isTrue();
    assertThat(transitive1.isDirect()).isFalse();
    assertThat(transitive2.isDirect()).isFalse();
    assertThat(transitive3.isDirect()).isFalse();
  }

  @Test
  public void testPopulateDirectFlags_ComplexTreeStructure() {
    // Build a complex tree structure:
    //   root
    //   ├── direct-1
    //   │   ├── transitive-1a
    //   │   └── transitive-1b
    //   │       └── transitive-1b-child
    //   └── direct-2
    //       └── transitive-2a
    ApiDependencyTreeNodeDTO transitive1bChild = createNode("transitive-1b-child");
    ApiDependencyTreeNodeDTO transitive1b = createNode("transitive-1b", transitive1bChild);
    ApiDependencyTreeNodeDTO transitive1a = createNode("transitive-1a");
    ApiDependencyTreeNodeDTO transitive2a = createNode("transitive-2a");

    ApiDependencyTreeNodeDTO direct1 = createNode("direct-1", transitive1a, transitive1b);
    ApiDependencyTreeNodeDTO direct2 = createNode("direct-2", transitive2a);
    ApiDependencyTreeNodeDTO root = createNode("root", direct1, direct2);

    populateDirectFlags(root);

    // Verify direct dependencies
    assertThat(direct1.isDirect()).isTrue();
    assertThat(direct2.isDirect()).isTrue();

    // Verify first-level transitive dependencies
    assertThat(transitive1a.isDirect()).isFalse();
    assertThat(transitive1b.isDirect()).isFalse();
    assertThat(transitive2a.isDirect()).isFalse();

    // Verify deeply nested transitive dependencies
    assertThat(transitive1bChild.isDirect()).isFalse();
  }

  @Test
  public void testPopulateDirectFlags_DirectDependencyWithNoChildren() {
    ApiDependencyTreeNodeDTO direct1 = createNode("direct-1");
    ApiDependencyTreeNodeDTO direct2 = createNode("direct-2");
    ApiDependencyTreeNodeDTO root = createNode("root", direct1, direct2);

    populateDirectFlags(root);

    assertThat(direct1.isDirect()).isTrue();
    assertThat(direct2.isDirect()).isTrue();
  }

  private ApiDependencyTreeNodeDTO createNode(String packageUrl, ApiDependencyTreeNodeDTO... children) {
    ApiDependencyTreeNodeDTO node = new ApiDependencyTreeNodeDTO();
    node.setPackageUrl(packageUrl);
    if (children.length > 0) {
      List<ApiDependencyTreeNodeDTO> childList = new ArrayList<>(Arrays.asList(children));
      node.setChildren(childList);
    }
    return node;
  }
}
