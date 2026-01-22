/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ComponentCategoryDAO
    extends AbstractDatamartSqlDAO<ComponentCategory>
{
  private static final Logger log = LoggerFactory.getLogger(ComponentCategoryDAO.class);

  private static volatile Map<String, ComponentCategory> componentCategoriesById = null;

  @Inject
  public ComponentCategoryDAO(final DataMartDataStore dataMartDataStore) {
    super(dataMartDataStore);
  }

  @Override
  public ComponentCategory getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ComponentCategory entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public ComponentCategory getById(String id) {
    if (componentCategoriesById == null) {
      load();
    }
    ComponentCategory componentCategory = componentCategoriesById.get(id);
    if (componentCategory == null) {
      log.info("Cannot find a componentCategory with ID '{}'.  Refreshing componentCategory data.", id);
      AbstractComponentCategoryUpdater.update(this);
      componentCategory = componentCategoriesById.get(id);
    }
    return componentCategory;
  }

  public List<ComponentCategory> getAll() {
    if (componentCategoriesById == null) {
      load();
    }

    return Collections.unmodifiableList(new ArrayList<>(componentCategoriesById.values()));
  }

  public void load() {
    synchronized (this.getClass()) {
      long start = System.currentTimeMillis();
      String sQuery = "SELECT componentCategory FROM ComponentCategory componentCategory";
      Map<String, ComponentCategory> componentCategoriesById = getList(sQuery).stream()
          .sorted((category1, category2) -> category1.getPath().compareToIgnoreCase(category2.getPath()))
          .collect(Collectors.toMap(ComponentCategory::getId, Function.identity(), (c1, c2) -> c1, LinkedHashMap::new));
      ComponentCategoryDAO.componentCategoriesById = Collections.unmodifiableMap(componentCategoriesById);
      log.debug("Loaded all component categories in {} ms.", System.currentTimeMillis() - start);
    }
  }

  public List<ComponentCategory> getChildren(String componentCategoryId) {
    ComponentCategory componentCategory = getById(componentCategoryId);
    if (componentCategory == null) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(
        componentCategoriesById.values().stream()
            .filter(category -> category.getPath().startsWith(componentCategory.getPath() + "/"))
            .collect(Collectors.toList()));
  }
}
