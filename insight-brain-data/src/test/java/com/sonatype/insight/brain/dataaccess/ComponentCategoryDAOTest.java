/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.model.component.ComponentCategory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ComponentCategoryDAOTest
    extends AbstractDbDAOTest
{
  private AbstractComponentCategoryUpdater savedComponentCategoryUpdater;

  @Before
  public void before() {
    savedComponentCategoryUpdater = AbstractComponentCategoryUpdater.getUpdater();
  }

  @After
  public void after() {
    AbstractComponentCategoryUpdater.setUpdater(savedComponentCategoryUpdater);
  }

  @Test
  public void test_getAll() {
    ComponentCategoryDAO componentCategoryDAO = new ComponentCategoryDAO();
    List<ComponentCategory> componentCategories = componentCategoryDAO.getAll();
    assertThat(componentCategories).isNotEmpty().isSortedAccordingTo(
        Comparator.comparing(ComponentCategory::getPath, String.CASE_INSENSITIVE_ORDER));
  }

  @Test
  public void test_getById_RefreshesComponentCategories() {
    String newId = "newId";
    ComponentCategoryDAO dao = new ComponentCategoryDAO();
    assertThat(dao.getById(newId)).isNull();
    int count = dao.getAll().size();

    ComponentCategory newCategory = new ComponentCategory(newId, "Test Category");
    dao.insert(newCategory);
    assertThat(dao.getById(newId)).isNull();

    AbstractComponentCategoryUpdater.setUpdater(mock(AbstractComponentCategoryUpdater.class));

    assertThat(dao.getById(newId)).isNotNull();
    assertThat(dao.getAll()).hasSize(count + 1);

    dao.delete(newCategory);
    dao.load();
  }

  @Test
  public void test_getChildren() {
    ComponentCategoryDAO componentCategoryDAO = new ComponentCategoryDAO();
    ComponentCategory category1 = new ComponentCategory("category1", "Test Category1");
    ComponentCategory level1 = new ComponentCategory("level1", "Test Category1/level1");
    ComponentCategory level2 = new ComponentCategory("level2", "Test Category1/level1/level2");
    ComponentCategory category2 = new ComponentCategory("category2", "Test Category2");

    componentCategoryDAO.insert(category1);
    componentCategoryDAO.insert(level1);
    componentCategoryDAO.insert(level2);
    componentCategoryDAO.insert(category2);

    try {
      AbstractComponentCategoryUpdater.setUpdater(mock(AbstractComponentCategoryUpdater.class));

      List<ComponentCategory> results = componentCategoryDAO.getChildren("category1");
      assertThat(results).containsExactlyInAnyOrder(level1, level2);

      results = componentCategoryDAO.getChildren("level1");
      assertThat(results).containsExactlyInAnyOrder(level2);

      results = componentCategoryDAO.getChildren("level2");
      assertThat(results).isEmpty();

      results = componentCategoryDAO.getChildren("category2");
      assertThat(results).isEmpty();

      results = componentCategoryDAO.getChildren("unknownId");
      assertThat(results).isEmpty();
    }
    finally {
      componentCategoryDAO.delete(category1);
      componentCategoryDAO.delete(level1);
      componentCategoryDAO.delete(level2);
      componentCategoryDAO.delete(category2);
    }
  }
}
