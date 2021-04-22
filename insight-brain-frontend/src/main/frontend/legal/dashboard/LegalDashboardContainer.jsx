/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import { stateGo } from '../../reduxUiRouter/routerActions';
import LegalDashboardPage from './LegalDashboardPage';
import * as legalDashboardActions from './legalDashboardActions';
import * as legalDashboardFilterActions from './filter/legalDashboardFilterActions';

function mapStateToProps({ legalDashboard, legalDashboardFilter, manageLegalFilters }) {
  return {
    ...pick(['applications', 'components', 'loading', 'loadError'], legalDashboard),
    ...pick(['filtersAreDirty', 'filterSidebarOpen'], legalDashboardFilter),
    ...pick(['appliedFilterName', 'showDirtyAsterisk'], manageLegalFilters),
  };
}

const mapDispatchToProps = {
  ...legalDashboardActions,
  ...legalDashboardFilterActions,
  stateGo,
};

const LegalDashboardContainer = connect(mapStateToProps, mapDispatchToProps)(LegalDashboardPage);
export default LegalDashboardContainer;
