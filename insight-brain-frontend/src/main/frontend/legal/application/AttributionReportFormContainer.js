/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';
import AttributionReportForm from './AttributionReportForm';
import { stateGo } from '../../reduxUiRouter/routerActions';
import {
  getAttributionReportTemplates,
  applyAttributionReportTemplateByIndex,
  setDirtyFlagToAttributionReport,
} from './attributionReportsActions';

function mapStateToProps(state) {
  const {
    router,
    attributionReports: { attributionReportTemplates, attributionReports },
  } = state;
  return {
    ...pick(['applicationPublicId', 'stageTypeId', '$state'], router.currentParams),
    ...pick(['isMultiApp'], router?.currentState?.data),
    attributionReportTemplates,
    attributionReports,
  };
}

const mapDispatchToProps = {
  stateGo,
  getAttributionReportTemplates,
  applyAttributionReportTemplateByIndex,
  setDirtyFlagToAttributionReport,
};

const AttributionReportFormContainer = connect(mapStateToProps, mapDispatchToProps)(AttributionReportForm);
export default AttributionReportFormContainer;
