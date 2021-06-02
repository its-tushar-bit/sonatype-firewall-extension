/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton, NxTag } from '@sonatype/react-shared-components';
import { availableScopesPropType, componentTransitivePolicyViolationsPropType } from './transitiveViolationsPropTypes';
import LoadWrapper from '../react/LoadWrapper';
import { getLatestReportUrl } from '../util/CLMLocation';
import TransitiveViolationsPageSubtitle from './TransitiveViolationsPageSubtitle';
import TransitiveViolationsPageTable from './TransitiveViolationsPageTable';

export default function TransitiveViolationsPage(props) {
  const {
    ownerType,
    ownerId,
    stageTypeId,
    hash,
    scanId,
    $state,
    availableScopes,
    componentTransitivePolicyViolations,
    loadAvailableScopes,
    loadTransitiveViolations,
    setSortingParameters,
    setFilteringParameters,
  } = props;

  function load() {
    if (ownerType && ownerId && stageTypeId && hash) {
      loadAvailableScopes(ownerType, ownerId);
      loadTransitiveViolations(ownerType, ownerId, stageTypeId, hash);
    }
  }

  useEffect(load, [ownerType, ownerId, stageTypeId, hash]);

  const getBackHref = () => {
    if (ownerType === 'application') {
      if (scanId) {
        return $state.href($state.get('applicationReport.policy'), {
          publicId: ownerId,
          scanId: scanId,
        });
      }
      return getLatestReportUrl(ownerId, stageTypeId);
    }
    if (hash) {
      return $state.href($state.get('dashboard.component'), {
        hash: hash,
      });
    }
    return $state.href($state.get('dashboard.component'));
  };

  return (
    <main id="transitive-violations-page" className="nx-page-main">
      <LoadWrapper
        loading={availableScopes.loading || componentTransitivePolicyViolations.loading}
        error={availableScopes.error || componentTransitivePolicyViolations.error}
        retryHandler={load}
      >
        <NxBackButton href={getBackHref()} />
        <div className="nx-page-title">
          <h1 className="nx-h1">Transitive Violations</h1>
          <TransitiveViolationsPageSubtitle
            availableScopes={availableScopes.values}
            componentName={componentTransitivePolicyViolations.displayName}
            stageTypeId={stageTypeId}
          />
          {componentTransitivePolicyViolations.isInnerSource && (
            <div className="nx-page-title__tags--vertical">
              <NxTag id="iq-transitive-violations-page-is-inner-source" color="light-blue">
                InnerSource
              </NxTag>
            </div>
          )}
        </div>
        <section className="nx-tile">
          <div className="nx-tile-content">
            <TransitiveViolationsPageTable
              stageTypeId={stageTypeId}
              componentTransitivePolicyViolations={componentTransitivePolicyViolations}
              setFilteringParameters={setFilteringParameters}
              setSortingParameters={setSortingParameters}
            />
          </div>
        </section>
      </LoadWrapper>
    </main>
  );
}

TransitiveViolationsPage.propTypes = {
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  stageTypeId: PropTypes.string,
  hash: PropTypes.string,
  scanId: PropTypes.string,
  $state: PropTypes.object.isRequired,
  availableScopes: availableScopesPropType.isRequired,
  componentTransitivePolicyViolations: componentTransitivePolicyViolationsPropType.isRequired,
  loadAvailableScopes: PropTypes.func.isRequired,
  loadTransitiveViolations: PropTypes.func.isRequired,
  setSortingParameters: PropTypes.func.isRequired,
  setFilteringParameters: PropTypes.func.isRequired,
};
