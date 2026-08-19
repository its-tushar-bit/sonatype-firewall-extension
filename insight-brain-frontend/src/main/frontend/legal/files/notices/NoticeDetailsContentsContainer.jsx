/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { pick } from 'ramda';
import { connect } from 'react-redux';
import NoticeDetailsContents from './NoticeDetailsContents';

function mapStateToProps({ advancedLegal, componentNoticeDetails }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};
  return {
    loading: component.loading || availableScopes.loading || componentNoticeDetails.loadingNoticeDetails,
    error: component.error || availableScopes.error,
    availableScopes,
    componentNoticeDetails,
    ...pick(['component'], component),
  };
}

const NoticeDetailsContentsContainer = connect(mapStateToProps)(NoticeDetailsContents);
export default NoticeDetailsContentsContainer;
