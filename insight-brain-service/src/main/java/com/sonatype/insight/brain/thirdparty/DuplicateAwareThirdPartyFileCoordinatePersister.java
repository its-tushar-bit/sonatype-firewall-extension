/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.sbom.SbomDependencyType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DuplicateAwareThirdPartyFileCoordinatePersister
{
  private static final Logger log = LoggerFactory.getLogger(DuplicateAwareThirdPartyFileCoordinatePersister.class);

  private final ThirdPartyFileCoordinateDAO dao;

  @Inject
  public DuplicateAwareThirdPartyFileCoordinatePersister(final ThirdPartyFileCoordinateDAO dao) {
    this.dao = dao;
  }

  public ThirdPartyFileCoordinate persist(ThirdPartyFileCoordinate componentToSave) {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      ThirdPartyFileCoordinate persist = persist(tx, componentToSave);
      tx.commit();
      return persist;
    }
  }

  /**
   * Persists the given component. If a component with the same hash and componentRef already exists, it merges or
   * overrides depending on the dependency type of the component to save (picks the higher dependency type).
   * @param tx
   * @param componentToSave
   * @return the persisted component that was saved (either the existing one or the new one after the merge)
   */
  public ThirdPartyFileCoordinate persist(TransactionContext tx, ThirdPartyFileCoordinate componentToSave) {
    if (componentToSave == null || componentToSave.getHash() == null ||
        componentToSave.getComponentRef() == null) {
      throw new IllegalArgumentException("Cannot persist null or incomplete ThirdPartyFileCoordinate");
    }

    List<ThirdPartyFileCoordinate> existing =
        dao.getByHashOrComponentRefForThirdPartyFileId(tx, componentToSave.getThirdPartyFileId(),
            componentToSave.getHash(), componentToSave.getComponentRef());
    if (CollectionUtils.isNotEmpty(existing)) {
      ThirdPartyFileCoordinate existingComponent = existing.get(0);
      if (existing.size() > 1) {
        log.debug("Multiple ThirdPartyFileCoordinates found for thirdPartyFileId {}, hash {}, and componentRef {}. " +
                "Merging with the first one having id {}",
            componentToSave.getThirdPartyFileId(), componentToSave.getHash(),
            componentToSave.getComponentRef(), existingComponent.getId());
      }
      //merge with existing component
      merge(componentToSave, existingComponent);
      dao.update(tx, existingComponent);
      return existingComponent;
    }
    else {
      dao.insert(tx, componentToSave);
      return componentToSave;
    }
  }

  /**
   * Merges or overrides the toSave component with the existing component depending on the higher level of dependency
   * type. If the dependency type of toSave is higher than the existing component, then it overrides (replaces) the
   * existing component. In all other cases the existing component remains, and it merges the toSave component's
   * occurrences, filename and identification sources.
   *
   * @param toSave
   * @param existing
   */
  private void merge(
      final ThirdPartyFileCoordinate toSave,
      final ThirdPartyFileCoordinate existing)
  {
    if (shouldOverrideBasedOnDependencyType(toSave, existing)) {
      //replace existing with toSave
      existing.override(toSave);
    }
    else {
      //keep existing and merge
      existing.merge(toSave);
    }
  }

  private boolean shouldOverrideBasedOnDependencyType(
      final ThirdPartyFileCoordinate toSave,
      final ThirdPartyFileCoordinate existing)
  {
    if (toSave.getDependencyType() != null) {
      if (existing.getDependencyType() == null) {
        return true;
      }
      return SbomDependencyType.fromCode(toSave.getDependencyType()).equals(SbomDependencyType.DIRECT) &&
          SbomDependencyType.fromCode(existing.getDependencyType()).equals(SbomDependencyType.TRANSITIVE);
    }
    return false;
  }
}
