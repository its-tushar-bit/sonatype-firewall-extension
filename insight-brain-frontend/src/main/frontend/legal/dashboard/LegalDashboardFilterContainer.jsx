/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import LegalDashboardFilter from './LegalDashboardFilter';

function mapStateToProps() {
  return {};
}

const mapDispatchToProps = {};

const LegalDashboardFilterContainer = connect(mapStateToProps, mapDispatchToProps)(LegalDashboardFilter);
export default LegalDashboardFilterContainer;
