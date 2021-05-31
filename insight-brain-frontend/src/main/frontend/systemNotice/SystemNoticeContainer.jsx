/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { loadSystemNotice } from '../configuration/systemNoticeConfiguration/systemNoticeConfigurationActions';
import SystemNotice from './SystemNotice';

function mapStateToProps({ systemNoticeConfiguration: { serverData } }) {
  return { message: serverData.enabled ? serverData.message : null };
}

export default connect(mapStateToProps, { loadSystemNotice })(SystemNotice);
