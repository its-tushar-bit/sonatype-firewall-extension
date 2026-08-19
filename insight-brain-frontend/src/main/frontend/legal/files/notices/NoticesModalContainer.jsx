/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { setObligationScope, setObligationStatus } from '../../obligation/advancedLegalObligationActions';

import NoticesModal from './NoticesModal';
import {
  addNotice,
  cancelNoticesModal,
  saveNotices,
  setNoticeContent,
  setNoticesScope,
  setNoticeStatus,
} from '../advancedLegalFileActions';

function mapStateToProps({ advancedLegal }) {
  return {
    scope: advancedLegal.component.component.licenseLegalData.componentNoticesScopeOwnerId,
    originalScope: advancedLegal.component.component.licenseLegalData.originalComponentNoticesScopeOwnerId,
    availableScopes: advancedLegal.availableScopes,
    notices: advancedLegal.component.component.licenseLegalData.noticeFiles,
    error: advancedLegal.component.component.licenseLegalData.noticesError,
    submitMaskState: advancedLegal.component.component.licenseLegalData.saveNoticesSubmitMask,
    existingObligation: advancedLegal.component.component.licenseLegalData.obligations.find(
      (o) => o.name === 'Inclusion of Notice'
    ),
  };
}

const mapDispatchToProps = {
  cancelNoticesModal,
  setNoticeContent,
  setNoticeStatus,
  addNotice,
  setNoticesScope,
  saveNotices,
  setObligationScope,
  setObligationStatus,
};

export default connect(mapStateToProps, mapDispatchToProps)(NoticesModal);
