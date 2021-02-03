/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import LegalDashboardFilter from './LegalDashboardFilter';
import * as legalDashboardFilterActions from './legalDashboardFilterActions';

function mapStateToProps({ legalDashboardFilter }) {
  return legalDashboardFilter;
}

const LegalDashboardFilterContainer = connect(mapStateToProps, legalDashboardFilterActions)(LegalDashboardFilter);
export default LegalDashboardFilterContainer;
