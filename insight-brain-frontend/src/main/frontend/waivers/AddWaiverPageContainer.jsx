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
  saveWaiverAndRedirect,
  setWaiverComment,
  setWaiverScope,
  setApplyToAllComponents,
  setExpiryTime,
  setCustomExpiryTime,
  returnToAddWaiverOriginPage,
} from './waiverActions';
import {
  openVulnerabilityDetailsModal,
  closeVulnerabilityDetailsModal,
} from '../vulnerabilityDetails/vulnerabilityDetailsModalActions';

function mapStateToProps({ addWaiver, violation, router, user }) {
  return {
    ...addWaiver,
    ...pick(['violationDetails'], violation),
    ...pick(['violationId'], router.currentParams),
    prevStateName: router.prevState.name,
    prevParams: router.prevParams,
    currentUser: user?.currentUser?.displayName,
  };
}

const mapDispatchToProps = {
  loadAddWaiverData,
  openVulnerabilityDetailsModal,
  closeVulnerabilityDetailsModal,
  saveWaiver: saveWaiverAndRedirect,
  setWaiverComment,
  setWaiverScope,
  setApplyToAllComponents,
  setExpiryTime,
  setCustomExpiryTime,
  cancelAction: returnToAddWaiverOriginPage,
};

const AddWaiverPageContainer = connect(mapStateToProps, mapDispatchToProps)(AddWaiverPage);
export default AddWaiverPageContainer;
