/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiDependencyTreeSearcherTest
{
  private ApiDependencyTreeSearcher searcher;

  @Before
  public void setup() {
    searcher = new ApiDependencyTreeSearcher();
  }

  // example test tree structure
  // root
  // ├── rootChild1 (direct)
  // │ ├── child4 (transitive
  // │ └── child5 (transitive)
  // ├── rootChild2 (direct)
  // │ ├── child6 (transitive)
  // │ └── child7 (direct)
  // | └── child8 (transitive)
  // └── rootChild3 (direct)
  // └── child8 (transitive)
  // └── child9 (transitive)

  @Test
  public void testFindAllDirectParents_singleTransitive() {
    ApiDependencyTreeNodeDTO root = generateTestTree();

    Set<ApiDependencyTreeNodeDTO> parents = searcher.findAllDirectParents(root,
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child6", "child6"))));

    // Expecting rootChild2
    assertThat(parents).hasSize(1);
    assertThat(parents.iterator().next().isDirect()).isTrue();
  }

  @Test
  public void testFindAllDirectParents_singleDirect_shouldReturn0() {
    ApiDependencyTreeNodeDTO root = generateTestTree();

    Set<ApiDependencyTreeNodeDTO> parents = searcher.findAllDirectParents(root,
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child7", "child7"))));

    // Don't expect any parents if it is direct
    assertThat(parents).hasSize(0);
  }

  @Test
  public void testFindAllDirectParents_singleTransitiveWithMultipleDirectParents() {
    ApiDependencyTreeNodeDTO root = generateTestTree();

    Set<ApiDependencyTreeNodeDTO> parents = searcher.findAllDirectParents(root,
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child8", "child8"))));

    // Expecting child7(the closest) and rootChild3
    assertThat(parents).hasSize(2);
    assertThat(parents.stream().allMatch(ApiDependencyTreeNodeDTO::isDirect)).isTrue();
    assertThat(parents.stream().map(ApiDependencyTreeNodeDTO::getComponentIdentifier))
        .containsExactlyInAnyOrder(
            ApiComponentIdentifierDTOV2.fromComponentIdentifier(
                new ComponentIdentifier("maven", generateCoordinate("child7", "child7"))),
            ApiComponentIdentifierDTOV2.fromComponentIdentifier(
                new ComponentIdentifier("maven", generateCoordinate("rootChild3", "rootChild3"))));
  }

  public ApiDependencyTreeNodeDTO generateTestTree() {

    ApiDependencyTreeNodeDTO root = new ApiDependencyTreeNodeDTO();
    root.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("root", "root"))));

    root.setChildren(new ArrayList<>());
    ApiDependencyTreeNodeDTO rootChild1 = new ApiDependencyTreeNodeDTO();
    rootChild1.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("rootChild1", "rootChild1"))));
    rootChild1.setChildren(new ArrayList<>());
    rootChild1.setDirect(true);

    ApiDependencyTreeNodeDTO rootChild2 = new ApiDependencyTreeNodeDTO();
    rootChild2.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("rootChild2", "rootChild2"))));
    rootChild2.setChildren(new ArrayList<>());
    rootChild2.setDirect(true);

    ApiDependencyTreeNodeDTO rootChild3 = new ApiDependencyTreeNodeDTO();
    rootChild3.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("rootChild3", "rootChild3"))));
    rootChild3.setChildren(new ArrayList<>());
    rootChild3.setDirect(true);

    root.getChildren().add(rootChild1);
    root.getChildren().add(rootChild2);
    root.getChildren().add(rootChild3);

    ApiDependencyTreeNodeDTO child4 = new ApiDependencyTreeNodeDTO();
    child4.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child4", "child4"))));
    child4.setChildren(new ArrayList<>());
    child4.setDirect(false);

    ApiDependencyTreeNodeDTO child5 = new ApiDependencyTreeNodeDTO();
    child5.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child5", "child5"))));
    child5.setChildren(new ArrayList<>());
    child5.setDirect(false);

    rootChild1.getChildren().add(child4);
    rootChild1.getChildren().add(child5);

    ApiDependencyTreeNodeDTO child6 = new ApiDependencyTreeNodeDTO();
    child6.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child6", "child6"))));
    child6.setChildren(new ArrayList<>());
    child6.setDirect(false);
    ApiDependencyTreeNodeDTO child7 = new ApiDependencyTreeNodeDTO();
    child7.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child7", "child7"))));
    child7.setChildren(new ArrayList<>());
    child7.setDirect(true);

    ApiDependencyTreeNodeDTO child8 = new ApiDependencyTreeNodeDTO();
    child8.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child8", "child8"))));
    child8.setChildren(new ArrayList<>());
    child8.setDirect(false);
    child7.getChildren().add(child8);

    rootChild2.getChildren().add(child6);
    rootChild2.getChildren().add(child7);

    ApiDependencyTreeNodeDTO child9 = new ApiDependencyTreeNodeDTO();
    child9.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(
            new ComponentIdentifier("maven", generateCoordinate("child9", "child9"))));
    child9.setChildren(new ArrayList<>());
    child9.setDirect(false);

    rootChild3.getChildren().add(child8);
    rootChild3.getChildren().add(child9);

    return root;
  }

  public TreeMap<String, String> generateCoordinate(String artifactId, String groupdId) {
    return new TreeMap<>()
    {
      {
        this.put("artifactId", artifactId);
        this.put("groupId", groupdId);
        this.put("version", "1.1.1");
      }
    };
  }
}
