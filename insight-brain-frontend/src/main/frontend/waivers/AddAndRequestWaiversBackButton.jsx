/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { isNil } from 'ramda';

import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { originNamesForAddRequestPages } from 'MainRoot/util/waiverUtils';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const setValueForMultipleKeys = (keys, value) => keys.reduce((p, c) => ({ ...p, [c]: value }), {});
const someParamValuesAreNil = (params) => Object.values(params).some(isNil);

const appReportParamsExtractor = (props) => ({
  hash: props?.prevParams?.hash,
  scanId: props?.prevParams?.scanId,
  publicId: props?.prevParams?.publicId,
});

const dashboardParamsExtractor = (props) => ({
  id: props?.violationId,
  type: props?.prevParams?.type,
  sidebarReference: props?.prevParams?.sidebarReference,
});

const firewallParamsExtractor = (props) => ({
  violationId: props?.violationId,
  componentDisplayName: props?.prevParams?.componentDisplayName,
  componentHash: props?.prevParams?.componentHash,
  componentIdentifier: props?.prevParams?.componentIdentifier,
  matchState: props?.prevParams?.matchState,
  pathname: props?.prevParams?.pathname,
  proprietary: props?.prevParams?.proprietary,
  repositoryId: props?.prevParams?.repositoryId,
  tabId: props?.prevParams?.tabId,
});

const getDefaultNavigationId = (props) => {
  if (props?.isFirewallOrRepositoryComponent) {
    return originNamesForAddRequestPages.FIREWALL_VIOLATION_WAIVERS;
  }
  if (props?.isStandaloneDeveloper) {
    return originNamesForAddRequestPages.DASHBOARD_PRIORITIES_PAGE;
  }
  return originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW;
};

/**
 * This rules define the navigation id (key), the back button title and a params
 * extractor function (to extract the necesary information from the props)
 *
 * Some navigation ids share the same logic but the location where each navigate
 * back to is controlled by the navigation id inside props.prevStateName
 */
const NAVIGATION_RULES = {
  [originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW]: {
    backButtonTitle: 'Back to Violation Details',
    paramsExtractor: dashboardParamsExtractor,
  },
  [originNamesForAddRequestPages.DASHBOARD_PRIORITIES_PAGE]: {
    backButtonTitle: 'Back to Developer Dashboard',
    paramsExtractor: dashboardParamsExtractor,
  },
  ...setValueForMultipleKeys(
    [
      originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS,
      originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_SECURITY,
      originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_LEGAL,
    ],
    {
      backButtonTitle: 'Back to Component Details',
      paramsExtractor: appReportParamsExtractor,
      requiredParams: true,
    }
  ),
  ...setValueForMultipleKeys(
    [
      originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS,
      originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_SECURITY,
      originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_LEGAL,
      originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS,
      originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS_SECURITY,
      originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS_LEGAL,
    ],
    {
      backButtonTitle: 'Back to Component Details',
      paramsExtractor: firewallParamsExtractor,
    }
  ),
  ...setValueForMultipleKeys(
    [
      originNamesForAddRequestPages.FIREWALL_VIOLATION_WAIVERS,
      originNamesForAddRequestPages.REPOSITORY_VIOLATION_WAIVERS,
    ],
    {
      backButtonTitle: 'Back to Component Details',
      paramsExtractor: (props) => props,
    }
  ),
  ...setValueForMultipleKeys(
    [
      originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS,
      originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD,
      originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_SECURITY,
      originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_SECURITY,
      originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_LEGAL,
      originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_LEGAL,
    ],
    {
      backButtonTitle: 'Back to Component Details',
      paramsExtractor: appReportParamsExtractor,
    }
  ),
};

/**
 * This function uses the rules and the props to set the necessary href with the props
 * and the back button title.
 *
 * The default back route is defined by the getDefaultNavigationId function
 */
const getButtonHrefAndTitle = (uiRouterState, props, rules) => {
  let navigationId = props?.prevStateName;
  let rule = rules?.[navigationId];

  if (!rule || (rule.requiredParams && someParamValuesAreNil(rule.paramsExtractor(props)))) {
    navigationId = getDefaultNavigationId(props);
    rule = rules[navigationId];
  }

  let params = rule.paramsExtractor(props);

  return {
    backButtonHref: uiRouterState.href(navigationId, params),
    backButtonTitle: rule.backButtonTitle,
  };
};

export default function AddAndRequestWaiversBackButton(props) {
  const uiRouterState = useRouterState();

  const { backButtonHref, backButtonTitle } = getButtonHrefAndTitle(uiRouterState, props, NAVIGATION_RULES);

  return <MenuBarBackButton text={backButtonTitle} href={backButtonHref} />;
}

AddAndRequestWaiversBackButton.propTypes = {
  violationId: PropTypes.string,
  prevStateName: PropTypes.string,
  prevParams: PropTypes.shape({
    publicId: PropTypes.string,
    scanId: PropTypes.string,
    hash: PropTypes.string,
    sidebarReference: PropTypes.string,
    type: PropTypes.string,
    repositoryPolicyId: PropTypes.string,
  }),
  isFirewall: PropTypes.bool,
  isFirewallOrRepositoryComponent: PropTypes.bool,
  isStandaloneDeveloper: PropTypes.bool,
};
