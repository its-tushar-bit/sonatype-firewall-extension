/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentCategoryList;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.hds.ComponentCategoryUpdater;
import com.sonatype.insight.brain.model.component.ComponentCategory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@IqH2Test
class IqH2ComponentCategoryUpdaterTest
{
  private IqTestContext ctx;

  private ComponentCategoryDAO componentCategoryDAO;

  @BeforeEach
  void setUp() {
    componentCategoryDAO = ctx.lookup(ComponentCategoryDAO.class);
  }

  @Test
  void testComponentCategory() {
    assertThat(componentCategoryDAO.getById("0")).isNull();

    ComponentCategoryList componentCategoryList = new ComponentCategoryList();
    componentCategoryList.setComponentCategories(Collections
        .singletonList(new com.sonatype.clm.dto.model.component.ComponentCategory(0, "CategoryUpdaterTest")));
    ctx.hdsRespondWith(componentCategoryList).atUri(ComponentCategoryUpdater.HDS_COMPONENT_CATEGORY_PATH);

    // triggers the category updater
    ComponentCategory componentCategory = componentCategoryDAO.getById("0");

    assertThat(componentCategory).isNotNull();
    componentCategoryDAO.delete(componentCategory);
  }

  @Test
  void testNoHdsServer() throws Exception {
    ctx.getHdsServer().stop();

    try {
      String newId = "New category id";
      assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> componentCategoryDAO.getById(newId))
          .withMessageStartingWith("Could not retrieve component category data from Sonatype HDS:");
    }
    finally {
      ctx.getHdsServer().start();
    }
  }
}
