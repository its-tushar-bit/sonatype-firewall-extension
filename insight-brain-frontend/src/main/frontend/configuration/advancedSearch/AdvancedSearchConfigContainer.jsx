/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import * as advancedSearchConfigActions from './advancedSearchConfigActions';
import AdvancedSearchConfig from './AdvancedSearchConfig';

function mapStateToProps({ advancedSearchConfig }) {
  return {
    ...advancedSearchConfig.formState,
    ...advancedSearchConfig.viewState,
  };
}

export default connect(mapStateToProps, advancedSearchConfigActions)(AdvancedSearchConfig);
