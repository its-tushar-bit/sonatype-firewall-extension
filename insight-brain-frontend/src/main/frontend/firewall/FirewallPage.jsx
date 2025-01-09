/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useRef } from 'react';

import { compose } from 'ramda';
import * as PropTypes from 'prop-types';
import { NxPageTitle, NxH1 } from '@sonatype/react-shared-components';

import FirewallStatus from './FirewallStatus';
import LoadWrapper from '../react/LoadWrapper';
import FirewallMetrics from './FirewallMetrics';
import FirewallWelcomeModal from './FirewallWelcomeModal';
import FirewallTabs from 'MainRoot/firewall/FirewallTabs';
import { QUARANTINE, WAIVERS } from 'MainRoot/firewall/firewallConstants';
import FirewallConfigurationModalContainer from './config/FirewallConfigurationModalContainer';

export default function FirewallPage(props) {
  // Actions
  const {
    loadFirewallData,
    setQuarantineGridPolicyFilter,
    setQuarantineGridPolicyFilterWithProprietaryNameConflict,
    setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode,
  } = props;

  // Welcome Modal
  const { initializeWelcomeModal, showWelcomeModal, closeWelcomeModal } = props;

  // viewState
  const { isShowConfigurationModal, loadError } = props;

  // autoUnquarantineState.viewState
  const { loadedReleaseQuarantineSummary, loadedConfiguration } = props;

  // tileMetricsState
  const {
    componentsAutoReleased,
    componentsQuarantined,
    namespaceAttacksBlocked,
    safeVersionsSelected,
    supplyChainAttacksBlocked,
    waivedComponents,
  } = props;

  // quarantineSummaryState
  const { loadedQuarantineSummary } = props;

  const dataLoaded = isDataLoaded(loadedReleaseQuarantineSummary, loadedConfiguration, loadedQuarantineSummary);

  const { filterPolicies } = props;

  const firewallTabsFuncRefs = useRef();

  useEffect(() => {
    loadFirewallData();
    initializeWelcomeModal();
  }, []);

  const setQuarantineGridPolicyFilterEmpty = () => {
    if (filterPolicies?.length !== 0) {
      setQuarantineGridPolicyFilter([]);
    }
  };

  const scrollToQuarantineTable = () => {
    firewallTabsFuncRefs?.current?.clickTab(QUARANTINE);
    setTimeout(() => firewallTabsFuncRefs?.current?.scrollToPanel(QUARANTINE), 100);
  };

  const onViewQuarantinedComponentsClick = (filterFn) => compose(scrollToQuarantineTable, filterFn);

  const onViewWaivedComponentsClick = () => {
    firewallTabsFuncRefs?.current?.clickTab(WAIVERS);
    setTimeout(() => firewallTabsFuncRefs?.current?.scrollToPanel(WAIVERS), 100);
  };

  return (
    <main id="firewall-page" className="nx-page-main">
      {showWelcomeModal && <FirewallWelcomeModal close={closeWelcomeModal} />}
      {isShowConfigurationModal && <FirewallConfigurationModalContainer />}
      <LoadWrapper loading={!dataLoaded} error={loadError} retryHandler={loadFirewallData}>
        <NxPageTitle className="iq-firewall-page__title">
          <NxPageTitle.Headings>
            <NxH1>Firewall</NxH1>
          </NxPageTitle.Headings>
        </NxPageTitle>
        <FirewallStatus {...props} />
        <header className="iq-firewall-metrics-header">
          <h2 className="nx-h2 iq-firewall-metrics-label">Component Data Insights</h2>
          <span>
            These totals include quarantined, waived, and auto-released components that differ from those actively in
            quarantine.
          </span>
        </header>
        <FirewallMetrics
          supplyChainAttacksBlocked={supplyChainAttacksBlocked}
          namespaceAttacksBlocked={namespaceAttacksBlocked}
          componentsQuarantined={componentsQuarantined}
          componentsAutoReleased={componentsAutoReleased}
          saferVersionsSelectedAutomatically={safeVersionsSelected}
          waivedComponents={waivedComponents}
          onNamespaceAttacksBlockedLinkClick={onViewQuarantinedComponentsClick(
            setQuarantineGridPolicyFilterWithProprietaryNameConflict
          )}
          onSupplyChainAttacksBlockedLinkClick={onViewQuarantinedComponentsClick(
            setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode
          )}
          onComponentsQuarantinedLinkClick={onViewQuarantinedComponentsClick(setQuarantineGridPolicyFilterEmpty)}
          onViewWaivedComponentsClick={onViewWaivedComponentsClick}
        />
        <FirewallTabs ref={firewallTabsFuncRefs} {...props} />
      </LoadWrapper>
    </main>
  );
}

function isDataLoaded(loadedReleaseQuarantineSummary, loadedConfiguration, loadedQuarantineSummary) {
  return loadedReleaseQuarantineSummary && loadedConfiguration && loadedQuarantineSummary;
}

FirewallPage.propTypes = {
  showWelcomeModal: PropTypes.bool.isRequired,
  initializeWelcomeModal: PropTypes.func.isRequired,
  closeWelcomeModal: PropTypes.func.isRequired,
  loadFirewallData: PropTypes.func.isRequired,
  setQuarantineGridPolicyFilter: PropTypes.func.isRequired,
  setQuarantineGridPolicyFilterWithProprietaryNameConflict: PropTypes.func.isRequired,
  setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode: PropTypes.func.isRequired,
  autoReleaseQuarantineCountMTD: PropTypes.string.isRequired,
  loadedReleaseQuarantineSummary: PropTypes.bool.isRequired,
  isShowConfigurationModal: PropTypes.bool.isRequired,
  loadedConfiguration: PropTypes.bool.isRequired,
  loadedQuarantineSummary: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  componentsAutoReleased: PropTypes.number.isRequired,
  componentsQuarantined: PropTypes.number.isRequired,
  namespaceAttacksBlocked: PropTypes.number.isRequired,
  safeVersionsSelected: PropTypes.number.isRequired,
  supplyChainAttacksBlocked: PropTypes.number.isRequired,
  waivedComponents: PropTypes.number.isRequired,
  filterPolicies: PropTypes.array.isRequired,
  uiRouterState: PropTypes.shape({
    href: PropTypes.func.isRequired,
  }),
  isStandaloneFirewall: PropTypes.bool,
  stateGo: PropTypes.func.isRequired,
};
