/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';

import { gotoNewVulnerability, loadSidebarNav } from './sidebarNavListActions';
import SidebarNavList from './SidebarNavList';

function mapStateToProps({ sidebarNavList, router, violation }) {
  let props = pick(['data', 'error', 'loading', 'contentType', 'backButtonStateName'], sidebarNavList);

  if (!props.contentType) {
    const currentStateName = router.currentState.name;
    switch (currentStateName) {
      case 'sidebarView.violation':
        if (violation.violationDetails) {
          props = {
            data: [violation.violationDetails],
            loading: false,
            contentType: 'violations',
            backButtonStateName: 'dashboard.overview.violations',
            error: null,
          };
        }
        break;
    }
  }
  return {
    ...props,
    stateParams: router.currentParams,
    // dont scroll to selection if we're coming from an entry in the sidebar (same parent state)
    scrollToSelection: router.currentState.name !== router.prevState.name,
  };
}

const mapDispatchToProps = {
  loadSidebarNav,
  gotoNewVulnerability,
};

export default connect(mapStateToProps, mapDispatchToProps)(SidebarNavList);
