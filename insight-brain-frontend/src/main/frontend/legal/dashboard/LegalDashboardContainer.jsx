/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import LegalDashboardPage from './LegalDashboardPage';
import { loadResults } from './legalDashboardActions';

function mapStateToProps({ legalDashboard }) {
  return {
    ...pick(['applications', 'components', 'loading', 'loadError', 'isAuthorized'], legalDashboard)
  };
}

const mapDispatchToProps = {
  loadResults
};

const LegalDashboardContainer = connect(mapStateToProps, mapDispatchToProps)(LegalDashboardPage);
export default LegalDashboardContainer;
