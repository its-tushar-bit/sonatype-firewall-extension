/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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
} from './attributionReportsActions';

const initialState = {
  attributionReportTemplates: {
    isFormDirty: false,
    results: Object.freeze([]),
    error: null,
    loading: false,
    selectedTemplateIndex: -1,
    submitMaskState: null,
  },
  attributionReports: {
    isFormDirty: false,
    selectedTemplateIndex: -1,
  },
};

const sortByTemplateNameFn = (a, b) => a.templateName.localeCompare(b.templateName);

export default function (state = initialState, { type, payload }) {
  switch (type) {
    case LOAD_ATTRIBUTION_REPORT_TEMPLATES_REQUESTED:
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          loading: true,
        },
      };
    case LOAD_ATTRIBUTION_REPORT_TEMPLATES_FULLFILED: {
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          loading: false,
          results: [...payload.sort(sortByTemplateNameFn)],
          error: null,
          selectedTemplateIndex: payload.length ? 0 : -1,
        },
      };
    }
    case LOAD_ATTRIBUTION_REPORT_TEMPLATES_FAILED:
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          loading: false,
          error: { type: 'loadError', message: payload.toString() },
        },
      };

    case DELETE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED:
    case SAVE_ATTRIBUTION_REPORT_TEMPLATE_REQUESTED:
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          submitMaskState: false,
        },
      };
    case SAVE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED: {
      let newResults;

      if (state.attributionReportTemplates.selectedTemplateIndex >= 0) {
        newResults = state.attributionReportTemplates.results.map((reportTemplate) => {
          if (reportTemplate.id === payload.id) {
            return payload;
          } else {
            return reportTemplate;
          }
        });
      } else {
        newResults = [...state.attributionReportTemplates.results, payload];
      }
      newResults = newResults.sort(sortByTemplateNameFn);
      const newIndex = newResults.findIndex((template) => template.id === payload.id);
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          error: null,
          loading: false,
          results: newResults,
          selectedTemplateIndex: payload.id ? newIndex : newResults.length - 1,
          submitMaskState: true,
          isFormDirty: false,
        },
      };
    }
    case SAVE_ATTRIBUTION_REPORT_TEMPLATE_FAILED:
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          error: { type: 'saveError', message: payload.toString() },
          loading: false,
          submitMaskState: null,
          isFormDirty: false,
        },
      };

    case DELETE_ATTRIBUTION_REPORT_TEMPLATE_FULLFILED: {
      const newResults = state.attributionReportTemplates.results
        .filter((template) => template.id !== payload)
        .sort(sortByTemplateNameFn);

      return {
        ...state,
        attributionReportTemplates: {
          results: newResults,
          error: null,
          loading: false,
          selectedTemplateIndex: newResults.length ? 0 : -1,
          submitMaskState: true,
        },
      };
    }
    case DELETE_ATTRIBUTION_REPORT_TEMPLATE_FAILED:
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          error: { type: 'deleteError', message: payload.toString() },
          loading: false,
          submitMaskState: null,
        },
      };

    case SELECT_ATTRIBUTION_REPORT_TEMPLATE:
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          error: null,
          loading: false,
          selectedTemplateIndex: payload,
        },
      };

    case APPLY_ATTRIBUTION_REPORT_TEMPLATE:
      return {
        ...state,
        attributionReports: {
          ...state.attributionReports,
          selectedTemplateIndex: payload,
        },
      };

    case ATTRIBUTION_REPORT_TEMPLATE_SUBMIT_MASK_DONE:
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          error: null,
          submitMaskState: null,
          loading: false,
        },
      };

    case ATTRIBUTION_REPORT_SET_DIRTY_FLAG:
      return {
        ...state,
        attributionReports: {
          ...state.attributionReports,
          isFormDirty: payload,
        },
      };

    case ATTRIBUTION_REPORT_TEMPLATE_SET_DIRTY_FLAG:
      return {
        ...state,
        attributionReportTemplates: {
          ...state.attributionReportTemplates,
          isFormDirty: payload,
        },
      };

    default:
      return state;
  }
}
