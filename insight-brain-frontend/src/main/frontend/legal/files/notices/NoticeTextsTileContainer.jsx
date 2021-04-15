/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { setShowNoticesModal } from '../advancedLegalFileActions';

import NoticeTextsTile from './NoticeTextsTile';

function mapStateToProps({ advancedLegal }) {
  return {
    noticeFiles: advancedLegal.component.component.licenseLegalData.noticeFiles,
    showNoticesModal: advancedLegal.component.component.licenseLegalData.showNoticesModal,
  };
}

const mapDispatchToProps = {
  setShowNoticesModal,
};

export default connect(mapStateToProps, mapDispatchToProps)(NoticeTextsTile);
