/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import ComponentLegalOverviewPage from './ComponentLegalOverviewPage';
import { loadComponent } from '../advancedLegal/advancedLegalActions';

function mapStateToProps({ advancedLegal, router }) {
  return {
    ...pick(['component', 'licenseLegalMetadata', 'loading', 'error'], advancedLegal.component || {}),
    ...pick(['hash'], router.currentParams)
  };
}

const mapDispatchToProps = {
  loadComponent
};

const ComponentLegalOverviewContainer = connect(mapStateToProps, mapDispatchToProps)(ComponentLegalOverviewPage);
export default ComponentLegalOverviewContainer;
