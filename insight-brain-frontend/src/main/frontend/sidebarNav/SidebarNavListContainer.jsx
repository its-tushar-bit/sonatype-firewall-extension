/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';

import { gotoNewVulnerability, loadSidebarNav } from './sidebarNavListActions';
import SidebarNavList from './SidebarNavList';

function mapStateToProps({ sidebarNavList, router, violationPage }) {
  let props = pick(['data', 'error', 'loading', 'contentType', 'backButtonStateName'], sidebarNavList);

  if (!props.contentType) {
    const currentStateName = router.currentState.name;
    switch (currentStateName) {
      case 'violation':
        if (violationPage.violationDetails) {
          return {
            data: [violationPage.violationDetails],
            contentType: 'violations',
            backButtonStateName: 'dashboard.overview.violations',
            loading: false,
            error: null
          };
        }
        break;
    }
  }
  return props;
}

const mapDispatchToProps = {
  loadSidebarNav,
  gotoNewVulnerability
};

export default connect(mapStateToProps, mapDispatchToProps)(SidebarNavList);
