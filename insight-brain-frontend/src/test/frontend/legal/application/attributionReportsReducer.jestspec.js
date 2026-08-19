/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import attributionReportsReducer from '../../../../main/frontend/legal/application/attributionReportsReducer';
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
  ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG,
  ATTRIBUTION_REPORT_SET_DIRTY_FLAG,
} from '../../../../main/frontend/legal/application/attributionReportsActions';
const initialState = {
  results: [],
  error: null,
  loading: false,
  selectedTemplateIndex: -1,
  submitMaskState: null,
  isFormDirty: false,
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
  {
    id: '32d5ed0f663741e68ac7979a889a0324',
    templateName: 'Template 2',
    documentTitle: 'Application Name 2',
    header: 'custom header',
    footer: 'custom footer',
    includeTableOfContents: false,
    includeAppendix: false,
    includeStandardLicenseTexts: false,
    lastUpdatedAt: 1630351372428,
  },
];

describe('attributionReportsReducer', function () {
  it('has default fields', function () {
    const action = { type: 'UNKNOWN' };
    const newState = attributionReportsReducer(undefined, action);

    expect(newState.attributionReportTemplates).toEqual(initialState);
  });

  it('returns original state on unknown action', function () {
    const state = { foo: 'bar' };
    const action = {
      type: 'UNKNOWN',
    };
    const newState = attributionReportsReducer(state, action);
    expect(newState).toBe(state);
  });

  describe(LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED, function () {
    it('sets load property to true while the templates are loaded', function () {
      const action = {
        type: LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED,
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReportTemplates.loading).toBe(true);
    });
  });

  describe(LOAD_ATTRIBUTION_REPORT_TEMPLATES_FULLFILED, function () {
    it('sets the templates in results property', function () {
      const action = {
        type: LOAD_ATTRIBUTION_REPORT_TEMPLATES_FULLFILED,
        payload: allReportTemplates,
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReportTemplates.results.length).toBe(2);
    });
  });

  describe(LOAD_ATTRIBUTION_REPORT_TEMPLATES_FAILED, function () {
    it('sets the error type loadError', function () {
      const action = {
        type: LOAD_ATTRIBUTION_REPORT_TEMPLATES_FAILED,
        payload: 'error',
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReportTemplates.error.type).toBe('loadError');
    });
  });

  describe(SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED, function () {
    it('sets submitMaskState to false while the server responds', function () {
      const action = {
        type: SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED,
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReportTemplates.submitMaskState).toBe(false);
    });
  });

  describe(DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED, function () {
    it('sets submitMaskState to false while the server responds', function () {
      const action = {
        type: DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED,
        payload: allReportTemplates[1].id,
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReportTemplates.submitMaskState).toBe(false);
    });
  });

  describe(SAVE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED, function () {
    it('updates the templates in results property', function () {
      const customInitialState = {
        ...initialState,
        attributionReportTemplates: {
          ...initialState.attributionReportTemplates,
          results: [allReportTemplates[0]],
        },
      };
      const action = {
        type: SAVE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED,
        payload: allReportTemplates[1],
      };
      const newState = attributionReportsReducer(customInitialState, action);
      expect(newState.attributionReportTemplates.results.length).toBe(2);
    });
  });

  describe(SAVE_ATTRIBUTION_REPORT_TEMPLATE_FAILED, function () {
    it('sets error type saveError', function () {
      const action = {
        type: SAVE_ATTRIBUTION_REPORT_TEMPLATE_FAILED,
        payload: 'error',
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReportTemplates.error.type).toBe('saveError');
    });
  });

  describe(DELETE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED, function () {
    it('sets removes the template from results property', function () {
      const customInitialState = {
        ...initialState,
        attributionReportTemplates: {
          ...initialState.attributionReportTemplates,
          results: allReportTemplates,
        },
      };
      const action = {
        type: DELETE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED,
        payload: allReportTemplates[1].id,
      };
      const newState = attributionReportsReducer(customInitialState, action);
      expect(newState.attributionReportTemplates.results.length).toBe(1);
    });
  });

  describe(DELETE_ATTRIBUTION_REPORT_TEMPLATE_FAILED, function () {
    it('sets error type deleteError', function () {
      const action = {
        type: DELETE_ATTRIBUTION_REPORT_TEMPLATE_FAILED,
        payload: 'error',
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReportTemplates.error.type).toBe('deleteError');
    });
  });

  describe(SELECT_ATTRIBUTION_REPORT_TEMPLATE, function () {
    it('sets selectedTemplateIndex on template change', function () {
      const action = {
        type: SELECT_ATTRIBUTION_REPORT_TEMPLATE,
        payload: 1,
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReportTemplates.selectedTemplateIndex).toBe(1);
    });
  });

  describe(ATTRIBUTION_REPORT_TEMPLATE_SUBMIT_MASK_DONE, function () {
    it('change submitMaskState to null to hide success message', function () {
      const customInitialState = {
        ...initialState,
        attributionReportTemplates: {
          ...initialState.attributionReportTemplates,
          submitMaskState: true,
        },
      };
      const action = {
        type: ATTRIBUTION_REPORT_TEMPLATE_SUBMIT_MASK_DONE,
      };
      const newState = attributionReportsReducer(customInitialState, action);
      expect(newState.attributionReportTemplates.submitMaskState).toBe(null);
    });
  });

  describe(APPLY_ATTRIBUTION_REPORT_TEMPLATE, function () {
    it('sets selectedTemplateIndex on template change', function () {
      const action = {
        type: APPLY_ATTRIBUTION_REPORT_TEMPLATE,
        payload: 0,
      };
      const newState = attributionReportsReducer(initialState, action);
      expect(newState.attributionReports.selectedTemplateIndex).toBe(0);
    });
  });

  describe(ATTRIBUTION_REPORT_SET_DIRTY_FLAG, function () {
    const customInitialState = {
      ...initialState,
      attributionReports: {
        ...initialState.attributionReports,
        isFormDirty: false,
      },
    };
    it('sets isFormDirty flag state on attributionReport', function () {
      const action = {
        type: ATTRIBUTION_REPORT_SET_DIRTY_FLAG,
        payload: true,
      };
      const newState = attributionReportsReducer(customInitialState, action);
      expect(newState.attributionReports.isFormDirty).toBe(true);
    });
  });

  describe(ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG, function () {
    const customInitialState = {
      ...initialState,
      attributionReportTemplates: {
        ...initialState.attributionReportTemplates,
        isFormDirty: false,
      },
    };
    it('sets isFormDirty flag state on attributionReportTemplates', function () {
      const action = {
        type: ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG,
        payload: true,
      };
      const newState = attributionReportsReducer(customInitialState, action);
      expect(newState.attributionReportTemplates.isFormDirty).toBe(true);
    });
  });
});
