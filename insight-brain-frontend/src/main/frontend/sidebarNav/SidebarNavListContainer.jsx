/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, pick } from 'ramda';
import { connect } from 'react-redux';

import { gotoNewVulnerability, goToWaiverWithType, loadSidebarNav } from './sidebarNavListActions';
import SidebarNavList from './SidebarNavList';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { FIREWALL_FIREWALLPAGE_WAIVERS, FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';

function mapStateToProps(state) {
  const { sidebarNavList, router, violation, waiverDetails } = state;

  const isStandaloneFirewall = selectIsStandaloneFirewall(state);

  let props = pick(['data', 'error', 'loading', 'contentType', 'backButtonStateName'], sidebarNavList);

  const whenCond = (cond, fn) => (cond ? fn() : props);

  const whenNoContentType = (fn) => whenCond(!props.contentType, fn);
  const getProps = (data, contentType, backButtonStateName, loading = false, error = null) =>
    whenCond(data, always({ data: [data], loading, contentType, backButtonStateName, error }));
  const waiversBackButtonStateName = isStandaloneFirewall
    ? FIREWALL_FIREWALLPAGE_WAIVERS
    : 'dashboard.overview.waivers';
  const getWaiverProps = (data) => getProps(data, 'waivers', waiversBackButtonStateName);
  const getViolationProps = (data) => getProps(data, 'violations', 'dashboard.overview.violations');

  props = whenNoContentType(() => {
    const currentStateName = router.currentState.name;
    switch (currentStateName) {
      case FIREWALL_WAIVER_DETAILS:
      case 'waiver.details':
        return getWaiverProps(waiverDetails.waiverDetails);
      case 'sidebarView.violation':
        return getViolationProps(violation.violationDetails);
      default:
        return props;
    }
  });
  return {
    ...props,
    stateParams: router.currentParams,
    prevParams: router.prevParams,
    // dont scroll to selection if we're coming from an entry in the sidebar (same parent state)
    scrollToSelection: router.currentState.name !== router.prevState.name,
  };
}

const mapDispatchToProps = {
  loadSidebarNav,
  gotoNewVulnerability,
  goToWaiverWithType,
};

export default connect(mapStateToProps, mapDispatchToProps)(SidebarNavList);
