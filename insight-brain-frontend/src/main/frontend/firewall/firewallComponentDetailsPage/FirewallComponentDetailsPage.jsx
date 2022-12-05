/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import ComponentDetailsTabs from '../../componentDetails/ComponentDetailsTabs';
import {
  ComponentDetailsReportInfo,
  ComponentDetailsHeader,
  ComponentDetailsTags,
  Title,
} from '../../componentDetails/ComponentDetailsHeader';
import { NxButton, NxFontAwesomeIcon, NxLoadWrapper, NxTooltip } from '@sonatype/react-shared-components';
import { createTabConfiguration } from '../../componentDetails/componentDetailsUtils';
import FirewallOverview from './overview/FirewallOverview';
import FirewallPolicyViolations from './policyViolations/FirewallPolicyViolations';
import FirewallSecurityTab from 'MainRoot/firewall/firewallComponentDetailsPage/security/FirewallSecurityTab';
import FirewallLegalTab from 'MainRoot/firewall/firewallComponentDetailsPage/legal/FirewallLegalTab';
import FirewallLabelsTab from 'MainRoot/firewall/firewallComponentDetailsPage/labels/FirewallLabelsTab';

import { faSync } from '@fortawesome/pro-solid-svg-icons';

export const tabsConfiguration = [
  createTabConfiguration('overview', 'Overview', <FirewallOverview />),
  createTabConfiguration('violations', 'Policy Violations', <FirewallPolicyViolations />),
  createTabConfiguration('security', 'Security', <FirewallSecurityTab />),
  createTabConfiguration('legal', 'Legal', <FirewallLegalTab />),
  createTabConfiguration('labels', 'Labels', <FirewallLabelsTab />),
];

export default function FirewallComponentDetailsPage(props) {
  const {
    loadComponentDetails,
    componentDetailsPageResponseState,
    onComponentDetailsPageTabChange,
    routeParams,
    loadComponentPolicyViolations,
    loadExistingWaiversData,
    reevaluateComponent,
    firewallLoadApplicableLabels,
    labels,
  } = props;
  const { tabId } = routeParams;
  const { componentDetails, isLoadingComponentDetails, componentDetailsError } = componentDetailsPageResponseState;
  const componentCoordinates =
    componentDetails?.displayName?.parts?.reduce((prev, part) => prev + part.value, '') || '';

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
    <main id="firewall-component-details-page" className="nx-viewport-sized nx-page-main">
      <div className="nx-viewport-sized__scrollable nx-scrollable firewall-component-details-page__container">
        <NxLoadWrapper
          loading={isLoadingComponentDetails}
          error={componentDetailsError}
          retryHandler={() => loadComponentDetails(routeParams)}
        >
          {() => (
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
          )}
        </NxLoadWrapper>
        {componentDetails && (
          <ComponentDetailsTabs
            tabsConfiguration={tabsConfiguration}
            onTabChange={handleTabChange}
            activeTabId={tabId}
          />
        )}
      </div>
    </main>
  );
}

FirewallComponentDetailsPage.propTypes = {
  loadComponentDetails: PropTypes.func,
  onComponentDetailsPageTabChange: PropTypes.func.isRequired,
  loadComponentPolicyViolations: PropTypes.func.isRequired,
  loadExistingWaiversData: PropTypes.func.isRequired,
  routeParams: PropTypes.shape({
    repositoryId: PropTypes.string,
    componentHash: PropTypes.string,
    matchState: PropTypes.string,
    proprietary: PropTypes.string,
    identificationSource: PropTypes.string,
    scanId: PropTypes.string,
    tabId: PropTypes.string,
    componentIdentifier: PropTypes.string,
    pathname: PropTypes.string,
  }).isRequired,
  componentDetailsPageResponseState: PropTypes.shape({
    componentDetails: PropTypes.object,
    isLoadingComponentDetails: PropTypes.bool.isRequired,
    componentDetailsError: PropTypes.string,
  }).isRequired,
  reevaluateComponent: PropTypes.func,
  firewallLoadApplicableLabels: PropTypes.func,
  labels: PropTypes.array,
};
