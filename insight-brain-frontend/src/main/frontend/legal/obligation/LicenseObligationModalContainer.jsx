/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import {
  cancelObligationModal,
  saveObligation,
  setObligationComment,
  setObligationScope,
  setObligationStatus,
} from './advancedLegalObligationActions';
import LicenseObligationModal from './LicenseObligationModal';

function mapStateToProps({ advancedLegal }) {
  return {
    availableScopes: advancedLegal.availableScopes,
  };
}

const mapDispatchToProps = {
  setObligationStatus,
  setObligationComment,
  setObligationScope,
  saveObligation,
  cancelObligationModal,
};

export default connect(mapStateToProps, mapDispatchToProps)(LicenseObligationModal);
