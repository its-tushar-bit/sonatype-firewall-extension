/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { pick } from 'ramda';
import { connect } from 'react-redux';
import NoticeDetailsOverview from './NoticeDetailsOverview';

function mapStateToProps({ advancedLegal, componentNoticeDetails, router }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};

  let routerParams = router.currentParams;
  return {
    componentNoticeDetails,
    loading: component.loading || availableScopes.loading || componentNoticeDetails.loadingNoticeDetails,
    error: component.error || availableScopes.error,
    ...pick(['component'], component),
    ...pick(['hash', 'ownerType', 'ownerId', 'noticeIndex'], routerParams),
  };
}

const NoticeDetailsOverviewContainer = connect(mapStateToProps)(NoticeDetailsOverview);
export default NoticeDetailsOverviewContainer;
