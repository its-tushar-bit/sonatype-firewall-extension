/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';
import AttributionReportTemplateForm from './AttributionReportTemplateForm';
import {
  getAttributionReportTemplates,
  saveAttributionReportTemplate,
  selectAttributionReportTemplate,
  deleteAttributionReportTemplateById,
} from './attributionReportsActions';

function mapStateToProps(state) {
  const {
    router,
    attributionReports: { attributionReportTemplates },
  } = state;
  return {
    ...pick(['applicationPublicId', 'stageTypeId', '$state'], router.currentParams),
    attributionReportTemplates,
  };
}

const mapDispatchToProps = {
  getAttributionReportTemplates,
  saveAttributionReportTemplate,
  selectAttributionReportTemplate,
  deleteAttributionReportTemplateById,
};

const AttributionReportTemplateFormContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(AttributionReportTemplateForm);
export default AttributionReportTemplateFormContainer;
