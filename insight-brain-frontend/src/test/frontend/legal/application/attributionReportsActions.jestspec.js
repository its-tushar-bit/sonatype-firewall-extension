/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED,
  LOAD_ATTRIBUTION_REPORT_TEMPLATES_FULLFILED,
  LOAD_ATTRIBUTION_REPORT_TEMPLATES_FAILED,
  SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED,
  SAVE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED,
  SAVE_ATTRIBUTION_REPORT_TEMPLATE_FAILED,
  DELETE_ATTRIBUTION_REPORT_TEMPLATE_FAILED,
  DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED,
  DELETE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED,
  SELECT_ATTRIBUTION_REPORT_TEMPLATE,
  ATTRIBUTION_REPORT_TEMPLATE_SUBMIT_MASK_DONE,
  APPLY_ATTRIBUTION_REPORT_TEMPLATE,
  ATTRIBUTION_REPORT_SET_DIRTY_FLAG,
  ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG,
  getAttributionReportTemplates,
  saveAttributionReportTemplate,
  selectAttributionReportTemplate,
  deleteAttributionReportTemplateById,
  startAttributionReportTemplateSubmitMaskDoneTimer,
  applyAttributionReportTemplateByIndex,
  setDirtyFlagToAttributionReport,
  setDirtyFlagToAttributionReportTemplate,
} from '../../../../main/frontend/legal/application/attributionReportsActions';
import {
  getAttributionReportTemplatesUrl,
  getAttributionReportTemplateUrl,
} from '../../../../main/frontend/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('attributionReportsActions', function () {
  let store,
    mockAxiosCalls,
    initialState = {
      attributionReports: {
        results: [],
        error: null,
        loading: false,
        selectedTemplateIndex: 0,
        submitMaskState: null,
        isFormDirty: false,
      },
      attributionReportTemplates: {
        results: [],
        error: null,
        loading: false,
        selectedTemplateIndex: 0,
        submitMaskState: null,
        isFormDirty: false,
      },
    };

  const newReportTemplate = {
    templateName: 'Template 2',
    documentTitle: 'Application Name 2',
    header: 'custom header',
    footer: 'custom footer',
    includeTableOfContents: false,
    includeAppendix: false,
    includeStandardLicenseTexts: false,
  };

  const existingEditedReportTemplate = {
    ...newReportTemplate,
    id: '32d5ed0f663741e68ac7979a889a0324',
    lastUpdatedAt: 1630351372428,
  };

  const allReportTemplates = [
    {
      id: '9373a43a8fe84422a6d8a235512c363f',
      templateName: 'Template 1',
      documentTitle: 'Application Name 1',
      header: 'custom header',
      footer: 'custom footer',
      includeTableOfContents: false,
      includeAppendix: true,
      includeStandardLicenseTexts: true,
      lastUpdatedAt: 1630351357414,
    },
    existingEditedReportTemplate,
  ];

  beforeEach(function () {
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    store = SpecUtil.mockReduxStore(initialState);
  });

  const getDefaultRequestSpyFunctions = () => {
    mockAxiosCalls({
      get: {
        [getAttributionReportTemplatesUrl()]: Promise.resolve({
          data: allReportTemplates,
        }),
        [getAttributionReportTemplateUrl(existingEditedReportTemplate.id)]: Promise.resolve({
          data: existingEditedReportTemplate,
        }),
      },
      post: {
        [getAttributionReportTemplatesUrl()]: Promise.resolve({
          data: existingEditedReportTemplate,
        }),
        [getAttributionReportTemplateUrl(existingEditedReportTemplate)]: Promise.resolve({
          data: existingEditedReportTemplate,
        }),
      },
      del: {
        [getAttributionReportTemplateUrl(existingEditedReportTemplate.id)]: Promise.resolve(),
      },
    });
  };

  it('loads attribution report templates', function (done) {
    getDefaultRequestSpyFunctions();
    store.dispatch(getAttributionReportTemplates()).then(() => {
      expect(store.getActions()[0]).toEqual({
        type: LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED,
      });
      expect(store.getActions()[1]).toEqual({
        type: LOAD_ATTRIBUTION_REPORT_TEMPLATES_FULLFILED,
        payload: allReportTemplates,
      });
      expect(store.getActions().length).toBe(2);
      done();
    });
  });

  it('error loading attribution report templates', function (done) {
    mockAxiosCalls({
      get: {
        [getAttributionReportTemplatesUrl()]: () => Promise.reject('error'),
      },
    });
    store.dispatch(getAttributionReportTemplates()).then(() => {
      expect(store.getActions()[0]).toEqual({
        type: LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED,
      });
      expect(store.getActions()[1]).toEqual({
        type: LOAD_ATTRIBUTION_REPORT_TEMPLATES_FAILED,
        payload: 'error',
      });
      expect(store.getActions().length).toBe(2);
      done();
    });
  });

  it('save a new attribution report template', function (done) {
    getDefaultRequestSpyFunctions();
    store.dispatch(saveAttributionReportTemplate(newReportTemplate)).then(() => {
      expect(store.getActions()[0]).toEqual({
        type: SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED,
      });
      expect(store.getActions()[1]).toEqual({
        type: SAVE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED,
        payload: existingEditedReportTemplate,
      });
      expect(store.getActions().length).toBe(2);
      done();
    });
  });

  it('error saving a new attribution report template', function (done) {
    mockAxiosCalls({
      post: {
        [getAttributionReportTemplatesUrl()]: () => Promise.reject('error'),
      },
    });
    store.dispatch(saveAttributionReportTemplate(newReportTemplate)).then(() => {
      expect(store.getActions()[0]).toEqual({
        type: SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED,
      });
      expect(store.getActions()[1]).toEqual({
        type: SAVE_ATTRIBUTION_REPORT_TEMPLATE_FAILED,
        payload: 'error',
      });
      expect(store.getActions().length).toBe(2);
      done();
    });
  });

  it('delete an attribution report template', function (done) {
    getDefaultRequestSpyFunctions();
    store.dispatch(deleteAttributionReportTemplateById(existingEditedReportTemplate.id)).then(() => {
      expect(store.getActions()[0]).toEqual({
        type: DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED,
      });
      expect(store.getActions()[1]).toEqual({
        type: DELETE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED,
        payload: '32d5ed0f663741e68ac7979a889a0324',
      });
      expect(store.getActions().length).toBe(2);
      done();
    });
  });

  it('error deleting an attribution report template', function (done) {
    mockAxiosCalls({
      del: {
        [getAttributionReportTemplateUrl(existingEditedReportTemplate.id)]: () => Promise.reject('error'),
      },
    });
    store.dispatch(deleteAttributionReportTemplateById(existingEditedReportTemplate.id)).then(() => {
      expect(store.getActions()[0]).toEqual({
        type: DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED,
      });
      expect(store.getActions()[1]).toEqual({
        type: DELETE_ATTRIBUTION_REPORT_TEMPLATE_FAILED,
        payload: 'error',
      });
      expect(store.getActions().length).toBe(2);
      done();
    });
  });

  it('change selected attribution report template', function () {
    store = SpecUtil.mockReduxStore({
      ...initialState,
      attributionReportTemplates: {
        ...initialState.attributionReportTemplates,
        results: allReportTemplates,
      },
    });
    store.dispatch(selectAttributionReportTemplate(existingEditedReportTemplate.id));
    expect(store.getActions().length).toBe(1);
    expect(store.getActions()[0]).toEqual({
      type: SELECT_ATTRIBUTION_REPORT_TEMPLATE,
      payload: existingEditedReportTemplate.id,
    });
  });

  it('change submitMaskState to null to hide success message', function (done) {
    store = SpecUtil.mockReduxStore({
      ...initialState,
      attributionReportTemplates: {
        ...initialState.attributionReportTemplates,
        submitMaskState: true,
      },
    });
    startAttributionReportTemplateSubmitMaskDoneTimer(store.dispatch).then(() => {
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: ATTRIBUTION_REPORT_TEMPLATE_SUBMIT_MASK_DONE,
      });
      done();
    });
  });

  it('change applyied attribution report template', function () {
    store = SpecUtil.mockReduxStore({
      ...initialState,
      attributionReports: {
        selectedTemplateIndex: -1,
      },
    });
    store.dispatch(applyAttributionReportTemplateByIndex(0));
    expect(store.getActions().length).toBe(1);
    expect(store.getActions()[0]).toEqual({
      type: APPLY_ATTRIBUTION_REPORT_TEMPLATE,
      payload: 0,
    });
  });

  it('changes isFormDirty flag from attributionReports', function () {
    store = SpecUtil.mockReduxStore({
      ...initialState,
      attributionReports: {
        ...initialState.attributionReports,
        isFormDirty: false,
      },
    });
    store.dispatch(setDirtyFlagToAttributionReport(true));
    expect(store.getActions().length).toBe(1);
    expect(store.getActions()[0]).toEqual({
      type: ATTRIBUTION_REPORT_SET_DIRTY_FLAG,
      payload: true,
    });
  });

  it('changes isFormDirty flag from attributionReportsTemplates', function () {
    store = SpecUtil.mockReduxStore({
      ...initialState,
      attributionReportTemplates: {
        ...initialState.attributionReportTemplates,
        isFormDirty: false,
      },
    });
    store.dispatch(setDirtyFlagToAttributionReportTemplate(true));
    expect(store.getActions().length).toBe(1);
    expect(store.getActions()[0]).toEqual({
      type: ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG,
      payload: true,
    });
  });
});
