/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import NoticesModal from './NoticesModal';
import {
  addNotice,
  cancelNoticesModal,
  saveNotices,
  setNoticeContent,
  setNoticesScope,
  setNoticeStatus
} from '../advancedLegalFileActions';

function mapStateToProps({ advancedLegal }) {
  return {
    scope: advancedLegal.component.component.licenseLegalData.componentLegalFileScopeOwnerId,
    originalScope: advancedLegal.component.component.licenseLegalData.originalComponentLegalFileScopeOwnerId,
    availableScopes: advancedLegal.availableScopes,
    notices: advancedLegal.component.component.licenseLegalData.noticeFiles,
    error: advancedLegal.component.component.licenseLegalData.noticesError,
    submitMaskState: advancedLegal.component.component.licenseLegalData.saveNoticesSubmitMask
  };
}

const mapDispatchToProps = {
  cancelNoticesModal,
  setNoticeContent,
  setNoticeStatus,
  addNotice,
  setNoticesScope,
  saveNotices
};

export default connect(mapStateToProps, mapDispatchToProps)(NoticesModal);
