/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { pick } from 'ramda';
import { connect } from 'react-redux';
import NoticeDetailsList from './NoticeDetailsList';

function mapStateToProps({ advancedLegal, componentNoticeDetails, router }) {
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
    componentNoticeDetails,
    loading: component.loading || availableScopes.loading || componentNoticeDetails.loadingNoticeDetails,
    error: component.error || availableScopes.error,
    ...pick(['component'], component),
    ...pick(['hash', 'ownerType', 'ownerId', 'stageTypeId', 'noticeIndex', 'componentIdentifier'], routerParams),
  };
}

const NoticeDetailsListContainer = connect(mapStateToProps)(NoticeDetailsList);
export default NoticeDetailsListContainer;
NoticeDetailsListContainer.propTypes = pick(['$state'], NoticeDetailsList.propTypes);
