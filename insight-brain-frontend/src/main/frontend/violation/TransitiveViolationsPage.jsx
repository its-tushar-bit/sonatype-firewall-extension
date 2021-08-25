/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton, NxButton } from '@sonatype/react-shared-components';
import {
  availableScopesPropType,
  componentTransitivePolicyViolationsPropType,
  reportMetadataPropType,
} from './transitiveViolationsPropTypes';
import LoadWrapper from '../react/LoadWrapper';
import TransitiveViolationsPageTable from './TransitiveViolationsPageTable';
import { ComponentDetailsReportInfo } from '../componentDetails/ComponentDetailsHeader/ComponentDetailsReportInfo';
import { ComponentDetailsHeader, ComponentDetailsTags, Title } from '../componentDetails/ComponentDetailsHeader';
import WaiveTransitiveViolationsPopoverContainer from './WaiveTransitiveViolationsPopoverContainer';
import RequestWaiveTransitiveViolationsPopoverContainer from './RequestWaiveTransitiveViolationsPopoverContainer';
import PolicyViolationDetailsPopover from '../componentDetails/violations/PolicyViolationDetailsPopover';
import { useRouterState } from '../react/RouterStateContext';

export default function TransitiveViolationsPage(props) {
  const {
    ownerType,
    ownerId,
    hash,
    scanId,
    availableScopes,
    reportMetadata,
    componentTransitivePolicyViolations,
    isRequestWaiveTransitiveViolationsOpen,
    isWaiveTransitiveViolationsOpen,
    showViolationsDetailPopover,
    loadAvailableScopes,
    loadTransitiveViolations,
    setSortingParameters,
    setFilteringParameters,
    loadReportMetadata,
    toggleRequestWaiveTransitiveViolations,
    toggleWaiveTransitiveViolations,
    setSelectedPolicyViolationId,
    toggleShowViolationsDetailPopover,
  } = props;

  function load() {
    if (ownerType && ownerId && scanId && hash) {
      loadAvailableScopes(ownerType, ownerId);
      loadTransitiveViolations(ownerType, ownerId, scanId, hash);
      loadReportMetadata(ownerId, scanId);
    }
  }

  useEffect(load, [ownerType, ownerId, scanId, hash]);

  const routerState = useRouterState();

  const getBackHref = () => {
    return routerState.href(routerState.get('applicationReport.policy'), {
      publicId: ownerId,
      scanId: scanId,
    });
  };

  return (
    <main id="transitive-violations-page" className="nx-page-main nx-viewport-sized__container">
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
            {isRequestWaiveTransitiveViolationsOpen && <RequestWaiveTransitiveViolationsPopoverContainer />}
            {isWaiveTransitiveViolationsOpen && <WaiveTransitiveViolationsPopoverContainer />}
            <section className="nx-tile nx-viewport-sized__container">
              <header className="nx-tile-header">
                <div className="nx-tile-header__title">
                  <h2 className="nx-h2">Transitive Violations</h2>
                </div>
                <div className="nx-tile__actions">
                  <NxButton
                    id="transitive-violations-page-request-waive"
                    variant="tertiary"
                    onClick={toggleRequestWaiveTransitiveViolations}
                    disabled={componentTransitivePolicyViolations.data.violations.length === 0}
                  >
                    Request Waiver
                  </NxButton>
                  <NxButton
                    id="transitive-violations-page-waive"
                    variant="tertiary"
                    onClick={toggleWaiveTransitiveViolations}
                    disabled={componentTransitivePolicyViolations.data.violations.length === 0}
                  >
                    Waive Transitive Violations
                  </NxButton>
                </div>
              </header>
              <div className="nx-tile-content nx-viewport-sized__container">
                <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
                  <TransitiveViolationsPageTable
                    stageTypeId={reportMetadata.data.stageId}
                    componentTransitivePolicyViolations={componentTransitivePolicyViolations}
                    setFilteringParameters={setFilteringParameters}
                    setSortingParameters={setSortingParameters}
                    setSelectedPolicyViolationId={setSelectedPolicyViolationId}
                    toggleShowViolationsDetailPopover={toggleShowViolationsDetailPopover}
                  />
                </div>
                {showViolationsDetailPopover && (
                  <PolicyViolationDetailsPopover onClose={toggleShowViolationsDetailPopover} />
                )}
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
  availableScopes: availableScopesPropType.isRequired,
  reportMetadata: reportMetadataPropType.isRequired,
  componentTransitivePolicyViolations: componentTransitivePolicyViolationsPropType.isRequired,
  isRequestWaiveTransitiveViolationsOpen: PropTypes.bool.isRequired,
  isWaiveTransitiveViolationsOpen: PropTypes.bool.isRequired,
  showViolationsDetailPopover: PropTypes.bool.isRequired,
  loadAvailableScopes: PropTypes.func.isRequired,
  loadTransitiveViolations: PropTypes.func.isRequired,
  setSortingParameters: PropTypes.func.isRequired,
  setFilteringParameters: PropTypes.func.isRequired,
  loadReportMetadata: PropTypes.func.isRequired,
  toggleRequestWaiveTransitiveViolations: PropTypes.func.isRequired,
  toggleWaiveTransitiveViolations: PropTypes.func.isRequired,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
  toggleShowViolationsDetailPopover: PropTypes.func.isRequired,
};
