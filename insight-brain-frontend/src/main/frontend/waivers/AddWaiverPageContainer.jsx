/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';

import AddWaiverPage from './AddWaiverPage';
import { addWaiver } from './addWaiverActions';

function mapStateToProps({ router }) {
  return {
    stateParams: router.currentParams
  };
}

const mapDispatchToProps = {
  // ToDo: replace with real actions
  addWaiver
};

const AddWaiverPageContainer = connect(mapStateToProps, mapDispatchToProps)(AddWaiverPage);
export default AddWaiverPageContainer;
