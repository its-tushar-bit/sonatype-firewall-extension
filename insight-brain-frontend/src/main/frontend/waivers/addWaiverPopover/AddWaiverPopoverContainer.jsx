/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';

import AddWaiverPopover from './AddWaiverPopover';
import {
  loadAddWaiverData,
  saveWaiverAndLoadPolicyViolationData,
  setWaiverComment,
  setWaiverScope,
  setApplyToAllComponents,
  setExpiryTime,
  setCustomExpiryTime,
  setShowUnsavedChangesModal,
  resetAddWaiverData,
} from '../waiverActions';
import { openVulnerabilityDetailsModal } from '../../vulnerabilityDetails/vulnerabilityDetailsModalActions';

function mapStateToProps({ addWaiver, violation }) {
  return {
    ...addWaiver,
    ...pick(['violationDetails'], violation),
  };
}

const mapDispatchToProps = {
  loadAddWaiverData,
  openVulnerabilityDetailsModal,
  saveWaiver: saveWaiverAndLoadPolicyViolationData,
  setWaiverComment,
  setWaiverScope,
  setApplyToAllComponents,
  setExpiryTime,
  setCustomExpiryTime,
  setShowUnsavedChangesModal,
  resetAddWaiverData,
};

export default connect(mapStateToProps, mapDispatchToProps)(AddWaiverPopover);
