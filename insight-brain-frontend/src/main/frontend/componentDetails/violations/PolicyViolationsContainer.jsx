/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import PolicyViolations from './PolicyViolations';
import { actions } from './PolicyViolationsRedux';
import { selectComponentDetailsViolationsSlice, selectComponentViolations } from './PolicyViolationsSelectors';

function mapStateToProps(state) {
  const { loading, loadError, showViolationsDetail } = selectComponentDetailsViolationsSlice(state);

  return {
    violations: selectComponentViolations(state),
    loading,
    loadError,
    showViolationsDetail,
  };
}

const mapDispatchToProps = {
  loadPolicyViolationsInformation: actions.load,
  setShowViolationsDetail: actions.setShowViolationsDetail,
  goToWaivers: actions.goToWaivers,
};

export const PolicyViolationsContainer = connect(mapStateToProps, mapDispatchToProps)(PolicyViolations);
