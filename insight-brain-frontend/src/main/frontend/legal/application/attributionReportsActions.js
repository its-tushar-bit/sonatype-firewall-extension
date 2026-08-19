/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { getAttributionReportTemplatesUrl, getAttributionReportTemplateUrl } from '../../util/CLMLocation';
import { payloadParamActionCreator, noPayloadActionCreator } from '../../util/reduxUtil';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

// load templates
export const LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED = 'LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED';
export const LOAD_ATTRIBUTION_REPORT_TEMPLATES_FULLFILED = 'LOAD_ATTRIBUTION_REPORT_TEMPLATES_FULLFILED';
export const LOAD_ATTRIBUTION_REPORT_TEMPLATES_FAILED = 'LOAD_ATTRIBUTION_REPORT_TEMPLATES_FAILED';

// save templates
export const SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED = 'SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED';
export const SAVE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED = 'SAVE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED';
export const SAVE_ATTRIBUTION_REPORT_TEMPLATE_FAILED = 'SAVE_ATTRIBUTION_REPORT_TEMPLATE_FAILED';

// delete template
export const DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED = 'DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED';
export const DELETE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED = 'DELETE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED';
export const DELETE_ATTRIBUTION_REPORT_TEMPLATE_FAILED = 'DELETE_ATTRIBUTION_REPORT_TEMPLATE_FAILED';

export const SELECT_ATTRIBUTION_REPORT_TEMPLATE = 'SELECT_ATTRIBUTION_REPORT_TEMPLATE';
export const APPLY_ATTRIBUTION_REPORT_TEMPLATE = 'APPLY_ATTRIBUTION_REPORT_TEMPLATE';

export const ATTRIBUTION_REPORT_TEMPLATE_SUBMIT_MASK_DONE = 'ATTRIBUTION_REPORT_TEMPLATE_SUBMIT_MASK_DONE';

export const ATTRIBUTION_REPORT_SET_DIRTY_FLAG = 'ATTRIBUTION_REPORT_SET_DIRTY_FLAG';
export const ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG = 'ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG';

// load templates
const attributionReportTemplatesRequested = noPayloadActionCreator(LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED);
const loadAttributionReportTemplates = payloadParamActionCreator(LOAD_ATTRIBUTION_REPORT_TEMPLATES_FULLFILED);
const loadAttributionReportTemplatesFailed = payloadParamActionCreator(LOAD_ATTRIBUTION_REPORT_TEMPLATES_FAILED);

// save templates
const saveAttributionReportTemplatesRequested = noPayloadActionCreator(SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED);
const saveAttributionReportTemplates = payloadParamActionCreator(SAVE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED);
const saveAttributionReportTemplatesFailed = payloadParamActionCreator(SAVE_ATTRIBUTION_REPORT_TEMPLATE_FAILED);

// delete templates
const deleteAttributionReportTemplatesRequested = noPayloadActionCreator(DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED);
const deleteAttributionReportTemplate = payloadParamActionCreator(DELETE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED);
const deleteAttributionReportTemplateFailed = payloadParamActionCreator(DELETE_ATTRIBUTION_REPORT_TEMPLATE_FAILED);

const selectAttributionReportTemplates = payloadParamActionCreator(SELECT_ATTRIBUTION_REPORT_TEMPLATE);
const applyAttributionReportTemplate = payloadParamActionCreator(APPLY_ATTRIBUTION_REPORT_TEMPLATE);

const attributionReportTemplatesSubmitMaskDone = noPayloadActionCreator(ATTRIBUTION_REPORT_TEMPLATE_SUBMIT_MASK_DONE);

export const setDirtyFlagToAttributionReport = payloadParamActionCreator(ATTRIBUTION_REPORT_SET_DIRTY_FLAG);
export const setDirtyFlagToAttributionReportTemplate = payloadParamActionCreator(
  ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG
);

export function getAttributionReportTemplates() {
  return (dispatch) => {
    dispatch(attributionReportTemplatesRequested());
    return axios
      .get(getAttributionReportTemplatesUrl())
      .then((response) => {
        dispatch(loadAttributionReportTemplates(response.data));
      })
      .catch((error) => {
        dispatch(loadAttributionReportTemplatesFailed(error));
      });
  };
}

export function saveAttributionReportTemplate(template) {
  return (dispatch) => {
    const attributionReportTemplateUrl = getAttributionReportTemplatesUrl();
    dispatch(saveAttributionReportTemplatesRequested());
    return axios
      .post(attributionReportTemplateUrl, template)
      .then((response) => {
        dispatch(saveAttributionReportTemplates(response.data));
        startAttributionReportTemplateSubmitMaskDoneTimer(dispatch);
      })
      .catch((error) => {
        dispatch(saveAttributionReportTemplatesFailed(error));
      });
  };
}

export function selectAttributionReportTemplate(templateIndex) {
  return (dispatch) => {
    return dispatch(selectAttributionReportTemplates(templateIndex));
  };
}

export function applyAttributionReportTemplateByIndex(templateIndex) {
  return (dispatch) => {
    return dispatch(applyAttributionReportTemplate(templateIndex));
  };
}

export function startAttributionReportTemplateSubmitMaskDoneTimer(dispatch) {
  return new Promise((resolve) => {
    setTimeout(() => {
      dispatch(attributionReportTemplatesSubmitMaskDone());
      resolve();
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
}

export function deleteAttributionReportTemplateById(templateId) {
  return (dispatch) => {
    dispatch(deleteAttributionReportTemplatesRequested());
    return axios
      .delete(getAttributionReportTemplateUrl(templateId))
      .then(() => {
        dispatch(deleteAttributionReportTemplate(templateId));
        startAttributionReportTemplateSubmitMaskDoneTimer(dispatch);
      })
      .catch((error) => {
        dispatch(deleteAttributionReportTemplateFailed(error));
      });
  };
}
