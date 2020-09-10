/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';

import AddWaiverPage from './AddWaiverPage';
import {
  loadAddWaiverData,
  saveWaiver,
  setWaiverComment,
  setWaiverScope,
  setApplyToAllComponents,
  returnToAddWaiverOriginPage
} from './addWaiverActions';
import { openVulnerabilityDetailsModal } from '../vulnerabilityDetails/vulnerabilityDetailsModalActions';

function mapStateToProps({ addWaiver, violationPage, router }) {
  return {
    ...addWaiver,
    ...pick(['violationDetails'], violationPage),
    ...pick(['violationId'], router.currentParams)
  };
}

const mapDispatchToProps = {
  loadAddWaiverData,
  openVulnerabilityDetailsModal,
  saveWaiver,
  setWaiverComment,
  setWaiverScope,
  setApplyToAllComponents,
  cancelAction: returnToAddWaiverOriginPage
};

const AddWaiverPageContainer = connect(mapStateToProps, mapDispatchToProps)(AddWaiverPage);
export default AddWaiverPageContainer;
