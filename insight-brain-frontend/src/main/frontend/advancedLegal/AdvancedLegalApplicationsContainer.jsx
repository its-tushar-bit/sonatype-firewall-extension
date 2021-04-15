/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';
import * as advancedLegalActions from './advancedLegalActions';
import AdvancedLegalApplicationsPage from './AdvancedLegalApplicationsPage';

function mapStateToProps({ advancedLegal }) {
  return {
    ...pick(['viewStateApplications', 'applications'], advancedLegal),
  };
}

const mapDispatchToProps = { ...advancedLegalActions };

const AdvancedLegalApplicationsContainer = connect(mapStateToProps, mapDispatchToProps)(AdvancedLegalApplicationsPage);
export default AdvancedLegalApplicationsContainer;
