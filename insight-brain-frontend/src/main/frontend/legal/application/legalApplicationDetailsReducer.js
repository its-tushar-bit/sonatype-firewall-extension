/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED,
} from './legalApplicationDetailsActions';

const initState = {
  application: {
    name: null,
    error: null,
    loading: false,
  },
  stageType: {
    name: null,
    error: null,
    loading: false,
  },
  components: {
    results: [],
    error: null,
    loading: false,
  },
};

export default function (state = initState, { type, payload }) {
  switch (type) {
    case LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED: {
      const application = { ...initState.application, loading: true };
      return { ...initState, application: application };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED: {
      const application = { ...initState.application, name: payload.name };
      return { ...initState, application: application };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED: {
      const application = { ...initState.application, error: payload };
      return { ...initState, application: application };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED: {
      const stageType = { ...state.stageType, loading: true };
      return { ...state, stageType: stageType };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED: {
      const stageType = { ...state.stageType, loading: false, name: payload };
      return { ...state, stageType: stageType };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED: {
      const stageType = { ...state.stageType, loading: false, error: payload };
      return { ...state, stageType: stageType };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED: {
      const components = { ...state.components, loading: true };
      return { ...state, components: components };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED: {
      const components = {
        ...state.components,
        loading: false,
        results: payload,
      };
      return { ...state, components: components };
    }
    case LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED: {
      const components = {
        ...state.components,
        loading: false,
        error: payload,
      };
      return { ...state, components: components };
    }
    default:
      return state;
  }
}
