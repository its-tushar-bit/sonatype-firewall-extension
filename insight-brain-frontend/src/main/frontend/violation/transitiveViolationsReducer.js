/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED,
  TRANSITIVE_VIOLATIONS_LOAD_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED,
  TRANSITIVE_VIOLATIONS_LOAD_REQUESTED,
  TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS,
  TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
} from './transitiveViolationsActions';
import { Messages } from '../util/CommonServices';
import { caseInsensitiveComparator, defaultComparator, sortItemsByFieldsWithComparator } from '../util/sortUtils';

const initialState = {
  availableScopes: {
    loading: false,
    error: null,
    data: null,
  },
  reportMetadata: {
    loading: false,
    error: null,
    data: null,
  },
  componentTransitivePolicyViolations: {
    loading: false,
    error: null,
    sortConfiguration: {
      key: 'threatLevel',
      dir: 'desc',
    },
    filterConfiguration: {
      policyName: '',
      displayName: '',
    },
    data: null,
  },
};

function loadAvailableScopesRequested(_, state) {
  return {
    ...state,
    availableScopes: {
      ...initialState.availableScopes,
      loading: true,
      error: null,
    },
  };
}

function loadAvailableScopesFulfilled(payload, state) {
  return {
    ...state,
    availableScopes: {
      ...state.availableScopes,
      loading: false,
      error: null,
      data: payload,
    },
  };
}

function loadAvailableScopesFailed(payload, state) {
  return {
    ...state,
    availableScopes: {
      ...state.availableScopes,
      loading: false,
      error: Messages.getHttpErrorMessage(payload),
    },
  };
}

function loadReportMetadataRequested(_, state) {
  return {
    ...state,
    reportMetadata: {
      ...initialState.reportMetadata,
      loading: true,
      error: null,
    },
  };
}

function loadReportMetadataFulfilled(payload, state) {
  return {
    ...state,
    reportMetadata: {
      ...state.reportMetadata,
      loading: false,
      error: null,
      data: payload,
    },
  };
}

function loadReportMetadataFailed(payload, state) {
  return {
    ...state,
    reportMetadata: {
      ...state.reportMetadata,
      loading: false,
      error: Messages.getHttpErrorMessage(payload),
    },
  };
}

function loadTransitiveViolationsRequested(_, state) {
  return {
    ...state,
    componentTransitivePolicyViolations: {
      ...initialState.componentTransitivePolicyViolations,
      loading: true,
      error: null,
      sortConfiguration: state.componentTransitivePolicyViolations.sortConfiguration,
      filterConfiguration: state.componentTransitivePolicyViolations.filterConfiguration,
    },
  };
}

function loadTransitiveViolationsFulfilled(payload, state) {
  const payloadWithoutViolations = { ...payload };
  delete payloadWithoutViolations.transitivePolicyViolations;
  return {
    ...state,
    componentTransitivePolicyViolations: {
      ...state.componentTransitivePolicyViolations,
      loading: false,
      error: null,
      data: {
        violations: payload.transitivePolicyViolations,
        displayedViolations: payload.transitivePolicyViolations,
        ...payloadWithoutViolations,
      },
    },
  };
}

function loadTransitiveViolationsFailed(payload, state) {
  return {
    ...state,
    componentTransitivePolicyViolations: {
      ...state.componentTransitivePolicyViolations,
      loading: false,
      error: Messages.getHttpErrorMessage(payload),
    },
  };
}

const getDisplayedViolations = (state) => {
  const violations = state.componentTransitivePolicyViolations.data.violations;
  const filterConfiguration = state.componentTransitivePolicyViolations.filterConfiguration;
  const sortConfiguration = state.componentTransitivePolicyViolations.sortConfiguration;
  const result = violations.filter(
    (transitivePolicyViolation) =>
      transitivePolicyViolation.policyName.toLowerCase().includes(filterConfiguration.policyName.toLowerCase()) &&
      transitivePolicyViolation.displayName.toLowerCase().includes(filterConfiguration.displayName.toLowerCase())
  );
  return sortItemsByFieldsWithComparator(
    (a, b) => {
      if (typeof a === 'string' && typeof b === 'string') {
        return caseInsensitiveComparator(a, b);
      }
      return defaultComparator(a, b);
    },
    [(sortConfiguration.dir === 'desc' ? '-' : '') + sortConfiguration.key],
    result
  );
};

const getSortConfiguration = (payload, sortConfiguration) => {
  let newDir = null;
  if (sortConfiguration.key === payload) {
    if (sortConfiguration.dir === 'asc') {
      newDir = 'desc';
    }
    if (sortConfiguration.dir === 'desc') {
      newDir = 'asc';
    }
  } else {
    newDir = 'desc';
  }
  return {
    key: payload,
    dir: newDir,
  };
};

function updateDisplayedViolations(state) {
  return {
    ...state,
    componentTransitivePolicyViolations: {
      ...state.componentTransitivePolicyViolations,
      data: {
        ...state.componentTransitivePolicyViolations.data,
        displayedViolations: getDisplayedViolations(state),
      },
    },
  };
}

function setSortingParameters(payload, state) {
  return updateDisplayedViolations({
    ...state,
    componentTransitivePolicyViolations: {
      ...state.componentTransitivePolicyViolations,
      sortConfiguration: getSortConfiguration(payload, state.componentTransitivePolicyViolations.sortConfiguration),
    },
  });
}

function setFilteringParameters(payload, state) {
  return updateDisplayedViolations({
    ...state,
    componentTransitivePolicyViolations: {
      ...state.componentTransitivePolicyViolations,
      filterConfiguration: {
        ...state.componentTransitivePolicyViolations.filterConfiguration,
        ...payload,
      },
    },
  });
}

const reducerActionMap = {
  [TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED]: loadAvailableScopesRequested,
  [TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED]: loadAvailableScopesFulfilled,
  [TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED]: loadAvailableScopesFailed,
  [TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED]: loadReportMetadataRequested,
  [TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED]: loadReportMetadataFulfilled,
  [TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED]: loadReportMetadataFailed,
  [TRANSITIVE_VIOLATIONS_LOAD_REQUESTED]: loadTransitiveViolationsRequested,
  [TRANSITIVE_VIOLATIONS_LOAD_FULFILLED]: loadTransitiveViolationsFulfilled,
  [TRANSITIVE_VIOLATIONS_LOAD_FAILED]: loadTransitiveViolationsFailed,
  [TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS]: setSortingParameters,
  [TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS]: setFilteringParameters,
};

const transitiveViolationsReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default transitiveViolationsReducer;
