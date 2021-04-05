/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {connect} from 'react-redux';

import ComponentCopyrightDetailsPage from './ComponentCopyrightDetailsPage';
import {pick} from 'ramda';
import {loadFilePathsOnPageUpdate, loadCopyrightContexts, loadComponentAndCopyrightDetails, unloadCopyrightContexts}
  from './componentCopyrightDetailsActions';

function mapStateToProps({advancedLegal, componentCopyrightDetails, router}) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};
  return {
    loading: component.loading || availableScopes.loading || componentCopyrightDetails.loadingCopyrightFileCounts,
    error: component.error || availableScopes.error || componentCopyrightDetails.errorCopyrightFileCounts,
    availableScopes,
    componentCopyrightDetails,
    ...pick(['component'], component),
    ...pick(['hash', 'ownerType', 'ownerId', 'copyrightIndex'], router.currentParams)
  };
}

const mapDispatchToProps = {
  loadComponentAndCopyrightDetails,
  loadCopyrightContexts,
  unloadCopyrightContexts,
  loadFilePathsOnPageUpdate
};

const ComponentCopyrightDetailsContainer = connect(mapStateToProps, mapDispatchToProps)(ComponentCopyrightDetailsPage);
export default ComponentCopyrightDetailsContainer;
