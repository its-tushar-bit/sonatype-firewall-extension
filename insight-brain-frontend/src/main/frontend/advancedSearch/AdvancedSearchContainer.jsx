/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import * as advancedSearchActions from './advancedSearchActions';
import AdvancedSearch from './AdvancedSearch';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectNoSbomManagerEnabledError } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

function mapStateToProps(state) {
  const { advancedSearch } = state;
  const isSbomManager = selectIsSbomManager(state);
  const noSbomManagerEnabledError = selectNoSbomManagerEnabledError(state);
  const routerCurrentParams = selectRouterCurrentParams(state);

  return {
    ...advancedSearch.viewState,
    ...advancedSearch.configurationState,
    ...advancedSearch.formState,
    isSbomManager,
    noSbomManagerEnabledError,
    routerCurrentParams,
  };
}

export default connect(mapStateToProps, advancedSearchActions)(AdvancedSearch);
