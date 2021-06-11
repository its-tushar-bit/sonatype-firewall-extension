/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton } from '@sonatype/react-shared-components';
import {
  availableScopesPropType,
  componentTransitivePolicyViolationsPropType,
  reportMetadataPropType,
} from './transitiveViolationsPropTypes';
import LoadWrapper from '../react/LoadWrapper';
import TransitiveViolationsPageTable from './TransitiveViolationsPageTable';
import { ComponentDetailsReportInfo } from '../componentDetails/ComponentDetailsHeader/ComponentDetailsReportInfo';
import { ComponentDetailsHeader, ComponentDetailsTags, Title } from '../componentDetails/ComponentDetailsHeader';

export default function TransitiveViolationsPage(props) {
  const {
    ownerType,
    ownerId,
    hash,
    scanId,
    $state,
    availableScopes,
    reportMetadata,
    componentTransitivePolicyViolations,
    loadAvailableScopes,
    loadTransitiveViolations,
    setSortingParameters,
    setFilteringParameters,
    loadReportMetadata,
  } = props;

  function load() {
    if (ownerType && ownerId && scanId && hash) {
      loadAvailableScopes(ownerType, ownerId);
      loadTransitiveViolations(ownerType, ownerId, scanId, hash);
      loadReportMetadata(ownerId, scanId);
    }
  }

  useEffect(load, [ownerType, ownerId, scanId, hash]);

  const getBackHref = () => {
    return $state.href($state.get('applicationReport.policy'), {
      publicId: ownerId,
      scanId: scanId,
    });
  };

  return (
    <main id="transitive-violations-page" className="nx-page-main">
      <LoadWrapper
        loading={availableScopes.loading || reportMetadata.loading || componentTransitivePolicyViolations.loading}
        error={availableScopes.error || reportMetadata.error || componentTransitivePolicyViolations.error}
        retryHandler={load}
      >
        {availableScopes.data && reportMetadata.data && componentTransitivePolicyViolations.data && (
          <Fragment>
            {ownerId && scanId && <NxBackButton href={getBackHref()} />}
            <ComponentDetailsHeader>
              <Title id="transitive-violations-page-title">
                {componentTransitivePolicyViolations.data.displayName}
              </Title>
              <ComponentDetailsReportInfo
                applicationName={availableScopes.data[0].name}
                organizationName={availableScopes.data[1].name}
                reportTime={reportMetadata.data.reportTime}
                reportTitle={reportMetadata.data.reportTitle}
              />
              <ComponentDetailsTags isInnerSource={componentTransitivePolicyViolations.data.isInnerSource} />
            </ComponentDetailsHeader>
            <section className="nx-tile">
              <header className="nx-tile-header">
                <div className="nx-tile-header__title">
                  <h2 className="nx-h2">Transitive Violations</h2>
                </div>
              </header>
              <div className="nx-tile-content">
                <TransitiveViolationsPageTable
                  stageTypeId={reportMetadata.data.stageId}
                  componentTransitivePolicyViolations={componentTransitivePolicyViolations}
                  setFilteringParameters={setFilteringParameters}
                  setSortingParameters={setSortingParameters}
                />
              </div>
            </section>
          </Fragment>
        )}
      </LoadWrapper>
    </main>
  );
}

TransitiveViolationsPage.propTypes = {
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  scanId: PropTypes.string,
  $state: PropTypes.object.isRequired,
  availableScopes: availableScopesPropType.isRequired,
  reportMetadata: reportMetadataPropType.isRequired,
  componentTransitivePolicyViolations: componentTransitivePolicyViolationsPropType.isRequired,
  loadAvailableScopes: PropTypes.func.isRequired,
  loadTransitiveViolations: PropTypes.func.isRequired,
  setSortingParameters: PropTypes.func.isRequired,
  setFilteringParameters: PropTypes.func.isRequired,
  loadReportMetadata: PropTypes.func.isRequired,
};
