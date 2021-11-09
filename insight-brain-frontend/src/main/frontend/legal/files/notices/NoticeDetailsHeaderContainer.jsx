/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import NoticeDetailsHeader from './NoticeDetailsHeader';
import { loadComponentAndNoticeDetails } from './componentNoticeDetailsActions';
import { setShowNoticesModal } from '../advancedLegalFileActions';
import { loadComponentByComponentIdentifier } from '../../advancedLegalActions';

function mapStateToProps({ advancedLegal, router, componentNoticeDetails }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};
  const noticeDetailsStateName = 'noticeDetails';

  let routerParams = router.currentParams;
  if (
    !router.currentState.name.includes(noticeDetailsStateName) &&
    router.prevState.name.includes(noticeDetailsStateName)
  ) {
    routerParams = router.prevParams;
  }
  return {
    loading: component.loading || availableScopes.loading || componentNoticeDetails.loadingNoticeDetails,
    error: component.error || availableScopes.error,
    availableScopes,
    ...pick(['component'], component),
    ...pick(['hash', 'ownerType', 'ownerId', 'noticeIndex', 'stageTypeId', 'componentIdentifier'], routerParams),
    showNoticesModal: component.component ? component.component.licenseLegalData.showNoticesModal : false,
  };
}

const mapDispatchToProps = {
  setShowNoticesModal,
  loadComponentAndNoticeDetails,
};

const NoticeDetailsHeaderContainer = connect(mapStateToProps, mapDispatchToProps)(NoticeDetailsHeader);
export default NoticeDetailsHeaderContainer;
