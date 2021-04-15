/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { selectExportTitle, selectExportRequestData, selectExportUrl } from '../../dashboardSelectors';
import ExportButton from './ExportButton/ExportButton';

function mapStateToProps(state) {
  return {
    exportTitle: selectExportTitle(state),
    exportRequestData: selectExportRequestData(state),
    exportUrl: selectExportUrl(state),
  };
}

const ExportButtonContainer = connect(mapStateToProps)(ExportButton);
export default ExportButtonContainer;
