/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import ComponentDetailsTabs from '../../componentDetails/ComponentDetailsTabs';
import {
  ComponentDetailsReportInfo,
  ComponentDetailsHeader,
  ComponentDetailsTags,
  Title,
} from '../../componentDetails/ComponentDetailsHeader';
import { NxButton, NxFontAwesomeIcon, NxLoadWrapper, NxTooltip } from '@sonatype/react-shared-components';
import { createTabConfiguration, isUnknownComponent } from 'MainRoot/componentDetails/componentDetailsUtils';
import FirewallOverview from 'MainRoot/firewall/firewallComponentDetailsPage/overview/FirewallOverview';
import FirewallPolicyViolations from './policyViolations/FirewallPolicyViolations';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import FirewallSecurityTab from 'MainRoot/firewall/firewallComponentDetailsPage/security/FirewallSecurityTab';
import FirewallLegalTab from 'MainRoot/firewall/firewallComponentDetailsPage/legal/FirewallLegalTab';
import FirewallLabelsTab from 'MainRoot/firewall/firewallComponentDetailsPage/labels/FirewallLabelsTab';
import FirewallPolicyViolationDetailsPopover from './policyViolations/policyViolationsTile/FirewallPolicyViolationDetailsPopover';

import { selectIsStandaloneFirewall, selectRouterPrevState } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectFirewallComponentDetailsPageRouteParams,
  selectFirewallComponentDetailsPage,
} from '../firewallSelectors';
import { selectLabels } from 'MainRoot/componentDetails/componentDetailsSelectors';

import * as firewallActions from '../firewallActions';
import { actions as componentDetailsActions } from 'MainRoot/componentDetails/componentDetailsSlice';

import { faSync } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch, useSelector } from 'react-redux';

export const getEnabledTabs = (componentDetails) => {
  const isUnknown = isUnknownComponent(componentDetails);
  let tabsConfiguration = [
    createTabConfiguration('overview', 'Overview', <FirewallOverview />),
    createTabConfiguration('violations', 'Policy Violations', <FirewallPolicyViolations />),
  ];
  if (!isUnknown) {
    tabsConfiguration = [
      ...tabsConfiguration,
      createTabConfiguration('security', 'Security', <FirewallSecurityTab />),
      createTabConfiguration('legal', 'Legal', <FirewallLegalTab />),
      createTabConfiguration('labels', 'Labels', <FirewallLabelsTab />),
    ];
  }
  return tabsConfiguration;
};

export default function FirewallComponentDetailsPage() {
  const dispatch = useDispatch();
  const componentDetailsPageResponseState = useSelector(selectFirewallComponentDetailsPage);
  const routeParams = useSelector(selectFirewallComponentDetailsPageRouteParams);
  const labels = useSelector(selectLabels);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const prevState = useSelector(selectRouterPrevState);

  const loadComponentDetails = (routeParams) => dispatch(firewallActions.loadComponentDetails(routeParams));
  const loadComponentPolicyViolations = (pathname, repositoryId) =>
    dispatch(firewallActions.loadComponentPolicyViolations(pathname, repositoryId));
  const loadExistingWaiversData = (type, repositoryId, componentHash) =>
    dispatch(firewallActions.loadExistingWaiversData(type, repositoryId, componentHash));
  const onComponentDetailsPageTabChange = (tabIdToMoveTo) =>
    dispatch(firewallActions.onComponentDetailsPageTabChange(tabIdToMoveTo));
  const reevaluateComponent = () => dispatch(firewallActions.reevaluateComponent());
  const firewallLoadApplicableLabels = () => dispatch(componentDetailsActions.firewallLoadApplicableLabels());

  const { tabId, componentDisplayName } = routeParams;
  const { componentDetails, isLoadingComponentDetails, componentDetailsError } = componentDetailsPageResponseState;
  const componentCoordinates =
    componentDetails?.displayName?.parts?.reduce((prev, part) => prev + part.value, '') || componentDisplayName;

  const uiRouterState = useRouterState();

  const prevStateIsRepositoryReport = prevState?.name?.includes('firewall.repository-report');
  const href = uiRouterState.href('firewall.repository-report', {
    repositoryId: routeParams.repositoryId,
  });

  const backButtonParams =
    !prevStateIsRepositoryReport && isStandaloneFirewall
      ? { text: 'Back to Firewall Dashboard', stateName: 'firewall.firewallPage' }
      : { text: 'Back to Repository Results', href };

  useEffect(() => {
    loadComponentDetails(routeParams);
    loadComponentPolicyViolations(routeParams.pathname, routeParams.repositoryId);
    loadExistingWaiversData('repository', routeParams.repositoryId, routeParams.componentHash);
    firewallLoadApplicableLabels();
  }, []);

  const handleTabChange = (tabIdToMoveTo) => {
    if (tabIdToMoveTo === tabId) {
      return;
    }
    onComponentDetailsPageTabChange(tabIdToMoveTo);
  };

  return (
    <>
      <FirewallPolicyViolationDetailsPopover />
      <main id="firewall-component-details-page" className="nx-viewport-sized nx-page-main">
        <MenuBarBackButton {...backButtonParams} />
        <div className="nx-viewport-sized__scrollable nx-scrollable firewall-component-details-page__container">
          <NxLoadWrapper
            loading={isLoadingComponentDetails}
            error={componentDetailsError}
            retryHandler={() => loadComponentDetails(routeParams)}
          >
            {() => (
              <>
                <ComponentDetailsHeader>
                  <Title id="component-details-title">{componentCoordinates}</Title>
                  <div className="nx-btn-bar">
                    <NxTooltip
                      id="firewall-component-details-page--reevalaute-tooltip"
                      title="Re-evaluating will check for policy violations. Quarantined components will be released from quarantine if no policy violations causing quarantine are found."
                      placement="bottom"
                    >
                      <NxButton
                        id="firewall-component-details-page__reevaluate-button"
                        name="re-evaluate"
                        variant="tertiary"
                        onClick={reevaluateComponent}
                      >
                        <NxFontAwesomeIcon icon={faSync} />
                        <span>Re-evaluate Component</span>
                      </NxButton>
                    </NxTooltip>
                  </div>
                  <ComponentDetailsReportInfo {...componentDetails?.metadata} />
                  <ComponentDetailsTags format={componentDetails?.componentIdentifier?.format} labels={labels} />
                </ComponentDetailsHeader>
              </>
            )}
          </NxLoadWrapper>
          {componentDetails && (
            <ComponentDetailsTabs
              tabsConfiguration={getEnabledTabs(componentDetails)}
              onTabChange={handleTabChange}
              activeTabId={tabId}
            />
          )}
        </div>
      </main>
    </>
  );
}
