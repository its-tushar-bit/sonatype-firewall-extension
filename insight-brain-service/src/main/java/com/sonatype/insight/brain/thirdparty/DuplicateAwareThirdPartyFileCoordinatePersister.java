/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
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

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDao;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDao;

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDao;

  private final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDao;

  @Inject
  public DuplicateAwareThirdPartyFileCoordinatePersister(
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDao,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDao,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDao,
      ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDao)
  {
    this.thirdPartyFileCoordinateDao = thirdPartyFileCoordinateDao;
    this.thirdPartyCoordinateSecurityDao = thirdPartyCoordinateSecurityDao;
    this.thirdPartyCoordinateLicenseDao = thirdPartyCoordinateLicenseDao;
    this.thirdPartyVulnerabilityExploitabilityExchangeDao = thirdPartyVulnerabilityExploitabilityExchangeDao;
  }

  public ThirdPartyFileCoordinate persist(ThirdPartyFileCoordinate componentToSave) {
    try (TransactionContext tx = thirdPartyFileCoordinateDao.createTransactionContext()) {
      tx.begin();
      ThirdPartyFileCoordinate persist = persist(tx, componentToSave);
      tx.commit();
      return persist;
    }
  }

  /**
   * Persists the given component. If a component with the same hash and componentRef already exists, it merges or
   * overrides depending on the dependency type of the component to save (picks the higher dependency type).
   *
   * @param tx
   * @param componentToSave
   * @return the persisted component that was saved (either the existing one or the new one after the merge)
   */
  public ThirdPartyFileCoordinate persist(TransactionContext tx, ThirdPartyFileCoordinate componentToSave) {
    if (componentToSave == null || componentToSave.getHash() == null ||
        componentToSave.getComponentRef() == null)
    {
      throw new IllegalArgumentException("Cannot persist null or incomplete ThirdPartyFileCoordinate");
    }

    List<ThirdPartyFileCoordinate> existing =
        thirdPartyFileCoordinateDao.getByHashOrComponentRefForThirdPartyFileId(tx,
            componentToSave.getThirdPartyFileId(), componentToSave.getHash(), componentToSave.getComponentRef());
    if (CollectionUtils.isNotEmpty(existing)) {
      ThirdPartyFileCoordinate existingComponent = existing.get(0);
      if (existing.size() > 1) {
        log.debug("Multiple ThirdPartyFileCoordinates found for thirdPartyFileId {}, hash {}, and componentRef {}. " +
            "Merging with the first one having id {}",
            componentToSave.getThirdPartyFileId(), componentToSave.getHash(),
            componentToSave.getComponentRef(), existingComponent.getId());
      }
      // merge with existing component
      merge(componentToSave, existingComponent);
      thirdPartyFileCoordinateDao.update(tx, existingComponent);
      return existingComponent;
    }
    else {
      thirdPartyFileCoordinateDao.insert(tx, componentToSave);
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
      // replace existing with toSave
      existing.override(toSave);
    }
    else {
      // keep existing and merge
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

  /*
   * Given a list of "componentRefs", this method will consolidate all component, license and vulnerability data
   * under a single third party file coordinate and then delete any duplicate components and associated data
   */
  public Optional<String> consolidate(final List<String> componentRefs, final String thirdPartyFileId) {
    Optional<String> result = Optional.empty();
    try (TransactionContext tx = thirdPartyFileCoordinateDao.createTransactionContext()) {
      tx.begin();
      List<ThirdPartyFileCoordinate> byComponentRefs =
          thirdPartyFileCoordinateDao.getByComponentRefsAndThirdPartyFileId(tx, thirdPartyFileId, componentRefs);
      if (CollectionUtils.isEmpty(byComponentRefs)) {
        log.debug("No file coordinate records were found which had component-refs {}", componentRefs);
      }
      else if (CollectionUtils.size(byComponentRefs) == 1) {
        // nothing to do. there's only 1 record in database
        result = Optional.of(byComponentRefs.get(0).getComponentRef());
      }
      else {
        ThirdPartyFileCoordinate toKeep = byComponentRefs.get(0);
        for (int i = 1; i < byComponentRefs.size(); i++) {
          ThirdPartyFileCoordinate toMerge = byComponentRefs.get(i);
          merge(toMerge, toKeep);
          mergeVulnerabilities(tx, toMerge, toKeep);
          mergeLicenses(tx, toMerge, toKeep);
          thirdPartyFileCoordinateDao.delete(tx, toMerge);
        }
        thirdPartyFileCoordinateDao.update(tx, toKeep);
        result = Optional.of(toKeep.getComponentRef());
      }
      tx.commit();
    }
    return result;
  }

  private void mergeVulnerabilities(
      TransactionContext tx,
      ThirdPartyFileCoordinate toMerge,
      ThirdPartyFileCoordinate toKeep)
  {
    Map<String, ThirdPartyCoordinateSecurity> keepVulns =
        thirdPartyCoordinateSecurityDao.getByFileCoordinateId(tx, toKeep.getId())
            .stream()
            .collect(Collectors.toMap(ThirdPartyCoordinateSecurity::getRefId, v -> v));
    thirdPartyCoordinateSecurityDao.getByFileCoordinateId(tx, toMerge.getId()).forEach(vMerge -> {
      if (!keepVulns.containsKey(vMerge.getRefId())) {
        // this should merge the vulnerability along with any associated vex records
        vMerge.setFileCoordinateId(toKeep.getId());
        thirdPartyCoordinateSecurityDao.update(tx, vMerge);
      }
      else {
        ThirdPartyCoordinateSecurity vKeep = keepVulns.get(vMerge.getRefId());
        ThirdPartyVulnerabilityExploitabilityExchange vexKeep =
            thirdPartyVulnerabilityExploitabilityExchangeDao.getByCoordinateSecurityIdAndRefId(tx, vKeep.getId(),
                vKeep.getRefId());
        if (vexKeep == null) {
          ThirdPartyVulnerabilityExploitabilityExchange vexMerge =
              thirdPartyVulnerabilityExploitabilityExchangeDao.getByCoordinateSecurityIdAndRefId(tx, vMerge.getId(),
                  vMerge.getRefId());
          if (vexMerge != null) {
            // we only merge the vex record if the keep record doesn't have one and the merge record does
            vexMerge.setCoordinateSecurityId(vKeep.getId());
            thirdPartyVulnerabilityExploitabilityExchangeDao.update(tx, vexMerge);
          }
        }
      }
    });
  }

  private void mergeLicenses(TransactionContext tx, ThirdPartyFileCoordinate toMerge, ThirdPartyFileCoordinate toKeep) {
    Map<String, ThirdPartyCoordinateLicense> keepLicenses =
        thirdPartyCoordinateLicenseDao.getByFileCoordinateId(tx, toKeep.getId())
            .stream()
            .collect(Collectors.toMap(
                ThirdPartyCoordinateLicense::getLicenseId, l -> l));
    thirdPartyCoordinateLicenseDao.getByFileCoordinateId(tx, toMerge.getId()).forEach(mergeLicense -> {
      if (!keepLicenses.containsKey(mergeLicense.getLicenseId())) {
        mergeLicense.setFileCoordinateId(toKeep.getId());
        thirdPartyCoordinateLicenseDao.update(tx, mergeLicense);
      }
    });
  }
}
