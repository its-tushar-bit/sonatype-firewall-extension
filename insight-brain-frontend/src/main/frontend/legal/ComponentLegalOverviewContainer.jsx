/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import ComponentLegalOverviewPage from './ComponentLegalOverviewPage';
import { loadComponent, loadAvailableScopes } from '../advancedLegal/advancedLegalActions';

function mapStateToProps({ advancedLegal, router }) {
  let component = advancedLegal.component || {};
  let availableScopes = advancedLegal.availableScopes || {};
  return {
    loading: component.loading || availableScopes.loading,
    error: component.error || availableScopes.error,
    availableScopes: availableScopes,
    ...pick(['component', 'licenseLegalMetadata', 'obligations'], component),
    ...pick(['hash', 'organizationId', 'applicationPublicId'], router.currentParams)
  };
}

const mapDispatchToProps = {
  loadComponent,
  loadAvailableScopes
};

const ComponentLegalOverviewContainer = connect(mapStateToProps, mapDispatchToProps)(ComponentLegalOverviewPage);
export default ComponentLegalOverviewContainer;
