/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import LegalDashboardFilter from './LegalDashboardFilter';
import { pick } from 'ramda';
import * as manageLegalFiltersActions from './manageLegalFiltersActions';
import * as legalDashboardFilterActions from './legalDashboardFilterActions';

function mapStateToProps({ manageLegalFilters, legalDashboardFilter, orgsAndPolicies: { ownerSideNav } }) {
  return {
    ...legalDashboardFilter,
    ...pick(['appliedFilterName', 'showDirtyAsterisk', 'savedFilters'], manageLegalFilters),
    ...pick(['ownersMap', 'topParentOrganizationId'], ownerSideNav),
  };
}

const mapDispatchToProps = {
  ...manageLegalFiltersActions,
  ...legalDashboardFilterActions,
};

const LegalDashboardFilterContainer = connect(mapStateToProps, mapDispatchToProps)(LegalDashboardFilter);
export default LegalDashboardFilterContainer;
