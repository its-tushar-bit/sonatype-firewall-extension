/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import { NxButton } from '@sonatype/react-shared-components';
import * as actions from './transitiveViolationsActions';
import { actions as policyViolationsActions } from '../componentDetails/ViolationsTableTile/policyViolationsSlice';
import { setWaiverToDelete } from './../waivers/waiverActions';
import LoadWrapper from '../react/LoadWrapper';
import TransitiveViolationsPageTable from './TransitiveViolationsPageTable';
import { ComponentDetailsReportInfo } from '../componentDetails/ComponentDetailsHeader/ComponentDetailsReportInfo';
import { ComponentDetailsHeader, ComponentDetailsTags, Title } from '../componentDetails/ComponentDetailsHeader';
import WaiveTransitiveViolationsPopoverContainer from './WaiveTransitiveViolationsPopoverContainer';
import RequestWaiveTransitiveViolationsPopoverContainer from './RequestWaiveTransitiveViolationsPopoverContainer';
import PolicyViolationDetailsPopover from '../componentDetails/ViolationsTableTile/PolicyViolationDetailsPopover';
import { useRouterState } from '../react/RouterStateContext';
import ComponentWaiversPopover from '../componentDetails/ViolationsTableTile/componentWaivers/ComponentWaiversPopover';
import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectShouldGoBackToComponentDetails,
  selectTransitiveViolations,
} from 'MainRoot/violation/transitiveViolationsSelectors';
import { selectWaiverToDelete } from 'MainRoot/waivers/deleteWaiverModal/deleteWaiverSelector';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function TransitiveViolationsPage() {
  const dispatch = useDispatch();
  const { ownerType, ownerId, hash, scanId } = useSelector(selectRouterCurrentParams);

  const {
    availableScopes,
    componentTransitivePolicyViolations,
    isRequestWaiveTransitiveViolationsOpen,
    isViewTransitiveViolationWaiversOpen,
    isWaiveTransitiveViolationsOpen,
    reportMetadata,
    transitiveViolationWaivers,
  } = useSelector(selectTransitiveViolations);
  const shouldGoBackToComponentDetails = useSelector(selectShouldGoBackToComponentDetails);
  const waiverToDelete = useSelector(selectWaiverToDelete);
  const loadAvailableScopes = (ownerType, ownerId) => dispatch(actions.loadAvailableScopes(ownerType, ownerId));
  const loadReportMetadata = (ownerId, ScanId) => dispatch(actions.loadReportMetadata(ownerId, ScanId));
  const loadTransitiveViolations = (ownerType, ownerId, scanId, hash) =>
    dispatch(actions.loadTransitiveViolations(ownerType, ownerId, scanId, hash));
  const loadTransitiveViolationWaivers = (ownerId, scanId, hash) =>
    dispatch(actions.loadTransitiveViolationWaivers(ownerId, scanId, hash));
  const setSortingParameters = (payload) => dispatch(actions.setSortingParameters(payload));
  const setFilteringParameters = (payload) => dispatch(actions.setFilteringParameters(payload));
  const toggleRequestWaiveTransitiveViolations = () => dispatch(actions.toggleRequestWaiveTransitiveViolations());
  const toggleWaiveTransitiveViolations = () => dispatch(actions.toggleWaiveTransitiveViolations());
  const toggleViewTransitiveViolationWaivers = () => dispatch(actions.toggleViewTransitiveViolationWaivers());
  const setSelectedPolicyViolationId = (id) => dispatch(policyViolationsActions.setSelectedPolicyViolationId(id));
  const toggleShowViolationsDetailPopover = () => dispatch(policyViolationsActions.toggleShowViolationsDetailPopover());
  const setViolationsDetailRowClicked = () => dispatch(policyViolationsActions.setViolationsDetailRowClicked());
  const setWaiverToDeleteBtn = (waiver) => dispatch(setWaiverToDelete(waiver));

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
    if (shouldGoBackToComponentDetails) {
      return routerState.href(routerState.get('applicationReport.componentDetails.violations'), {
        publicId: ownerId,
        scanId: scanId,
        hash: hash,
      });
    }
    return routerState.href(routerState.get('applicationReport.policy'), {
      publicId: ownerId,
      scanId: scanId,
      componentHash: hash,
      tabId: 'policy',
    });
  };

  return (
    <>
      <PolicyViolationDetailsPopover />
      <main id="transitive-violations-page" className="nx-page-main nx-viewport-sized__container">
        <LoadWrapper
          loading={availableScopes.loading || reportMetadata.loading || componentTransitivePolicyViolations.loading}
          error={availableScopes.error || reportMetadata.error || componentTransitivePolicyViolations.error}
          retryHandler={load}
        >
          {availableScopes.data && reportMetadata.data && componentTransitivePolicyViolations.data && (
            <Fragment>
              {ownerId && scanId && <MenuBarBackButton href={getBackHref()} />}
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
              {isViewTransitiveViolationWaiversOpen && (
                <ComponentWaiversPopover
                  title="Transitive Component Waivers"
                  toggleComponentWaiversPopover={toggleViewTransitiveViolationWaivers}
                  waivers={transitiveViolationWaivers.data.componentPolicyWaivers}
                  setWaiverToDelete={setWaiverToDeleteBtn}
                  waiverToDelete={waiverToDelete}
                />
              )}
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
                    <NxButton
                      id="transitive-violations-page-view-waivers"
                      variant="tertiary"
                      onClick={() => loadTransitiveViolationWaivers(ownerId, scanId, hash)}
                    >
                      View Existing Waivers
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
                      setViolationsDetailRowClicked={setViolationsDetailRowClicked}
                    />
                  </div>
                </div>
              </section>
            </Fragment>
          )}
        </LoadWrapper>
      </main>
    </>
  );
}
