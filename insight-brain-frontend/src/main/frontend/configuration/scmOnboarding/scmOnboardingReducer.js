/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {createReducerFromActionMap} from '../../util/reduxUtil';
import {
  SCM_ONBOARDING_IMPORT_REPOS_FAILED,
  SCM_ONBOARDING_IMPORT_REPOS_FULFILLED,
  SCM_ONBOARDING_IMPORT_REPOS_REQUESTED,
  SCM_ONBOARDING_LOAD_CONFIG_FAILED,
  SCM_ONBOARDING_LOAD_CONFIG_FULFILLED,
  SCM_ONBOARDING_LOAD_PAGE_REQUESTED,
  SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
  SCM_ONBOARDING_LOAD_PAGE_FAILED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED,
  SCM_ONBOARDING_SET_CURRENT_HOST_URL,
  SCM_ONBOARDING_SET_SORTING,
  SCM_ONBOARDING_SET_SORTING_PARAMETERS,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION,
  SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED,
  SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED,
  SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED
} from './scmOnboardingActions';
import {sortItemsByFields} from '../../util/sortUtils';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import {validateHostUrl} from './utils/validators';
import {hasValidationErrors} from '../../util/validationUtil';

const initialState = {
  configState: {
    isScmOnboardingFeatureEnabled: null,
    isScmTokenConfigured: null,
    isScmTokenOverridden: null,
    scmProvider: ''
  },
  viewState: {
    loadingPage: false,
    loadingRepositories: false,
    validatingCompositeSourceControl: false,

    generalError: null,
    loadRepositoriesAuthError: null
  },
  formState: {
    organizations: [],
    selectedOrganization: null,
    preselectedOrganizationId: null,
    repositories: [],
    selectedRepositoryCount: 0,
    importedRepositoryCount: 0,
    totalRepositories: 0,
    newlyImportedRepos: [],
    defaultHostUrl: '',
    currentHostUrlState: textInputStateHelpers.initialState(''),
    failedImportCount: 0
  },
  sortConfiguration: {
    key: 'namespace',
    sortingOrder: ['namespace', 'project', 'description'],
    dir: 'asc'
  }
};

function loadConfigFulfilled(payload, state) {
  return {
    ...state,
    configState: {
      ...state.configState,
      isScmOnboardingFeatureEnabled: payload.scmOnboardingFeatureEnabled
    }
  };
}

function loadConfigFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      generalError: payload
    },
    configState: {
      ...state.configState,
      isScmOnboardingFeatureEnabled: null
    }
  };
}

function loadPageRequested(payload) {
  return {
    ...initialState,
    viewState: {
      ...initialState.viewState,
      loadingPage: true
    },
    formState: {
      ...initialState.formState,
      preselectedOrganizationId: payload
    }
  };
}

function loadPageFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingPage: false
    },
    configState: {
      ...state.configState,
      isScmOnboardingFeatureEnabled: payload.configResults.scmOnboardingFeatureEnabled,
      isScmTokenConfigured: payload.compositeSourceControlResults === null ? false :
        !!payload.compositeSourceControlResults.token.value ||
        !!payload.compositeSourceControlResults.token.parentValue,
      isScmTokenOverridden: payload.compositeSourceControlResults !== null &&
        !!payload.compositeSourceControlResults.token.value,
      scmProvider: payload.compositeSourceControlResults !== null
        ? payload.compositeSourceControlResults.provider : null
    },
    formState: {
      ...state.formState,
      selectedOrganization: payload.organizationsResults.find(org =>
        org.id === state.formState.preselectedOrganizationId),
      organizations: payload.organizationsResults,
      defaultHostUrl: payload.hostUrlResult !== null
        ? payload.hostUrlResult.defaultHostUrl : null,
      currentHostUrlState: payload.hostUrlResult !== null
        ? textInputStateHelpers.initialState(payload.hostUrlResult.defaultHostUrl)
        : textInputStateHelpers.initialState('')
    }
  };
}

function loadPageFailed(payload) {
  return {
    ...initialState,
    viewState: {
      ...initialState.viewState,
      loadingPage: false,
      generalError: payload
    }
  };
}

function setSelectedOrganization(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      selectedOrganization: payload
    }
  };
}

function loadRepositoriesRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingRepositories: true
    },
    formState: {
      ...state.formState,
      repositories: []
    }
  };
}

function loadRepositoriesFulfilled(payload, state) {
  return payload.status === 'SUCCESS' ? {
    ...state,
    viewState: {
      ...state.viewState,
      loadingRepositories: false
    },
    formState: {
      ...state.formState,
      repositories: sortItemsByFields(state.sortConfiguration.sortingOrder, payload.availableRepositories),
      totalRepositories: payload.totalRepositories,
      importedRepositoryCount: 0,
      selectedRepositoryCount: 0
    }
  } : handleLoadRepositoriesFailed({
    loadRepositoriesAuthError: (() => {
      switch (payload.status) {
        case 'SCM_AUTHN_FAILURE':
          return new Error(`Authentication with ${state.configState.scmProvider} failed`);
        case 'SCM_AUTHZ_FAILURE':
          return new Error(`Permission denied by ${state.configState.scmProvider}`);
        default:
          return new Error('Unknown Error');
      }
    })()
  }, state);
}

function loadRepositoriesFailed(payload, state) {
  return handleLoadRepositoriesFailed({generalError: payload}, state);
}

function handleLoadRepositoriesFailed({generalError, loadRepositoriesAuthError}, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingRepositories: false,
      generalError: generalError ? generalError : null,
      loadRepositoriesAuthError: loadRepositoriesAuthError ? loadRepositoriesAuthError : null
    },
    formState: {
      ...state.formState,
      repositories: null,
      totalRepositories: 0,
      importedRepositoryCount: 0,
      selectedRepositoryCount: 0
    }
  };
}

function importRepositoriesRequested(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      selectedRepositoryCount: 0,
      failedImportCount: 0
    }
  };
}

function importRepositoriesFulfilled(payload, state) {
  let importedRepos = payload.importedRepositories;
  let newRepositoryList = state.formState.repositories.filter(function(repo) {
    return !importedRepos.some(imported => imported.httpCloneUrl === repo.httpCloneUrl);
  });
  return {
    ...state,
    formState: {
      ...state.formState,
      repositories: newRepositoryList,
      importedRepositoryCount: state.formState.importedRepositoryCount + importedRepos.length,
      selectedRepositoryCount: 0,
      newlyImportedRepos: importedRepos,
      failedImportCount: payload.failedImportCount
    }
  };
}

function importRepositoriesFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      generalError: payload
    }
  };
}

function setSortingParameters(payload, state) {
  return {
    ...state,
    sortConfiguration: payload
  };
}

function setSorting(payload, state) {
  let newEntries = sortItemsByFields(state.sortConfiguration.sortFields, state.formState.repositories);
  return {
    ...state,
    formState: {
      ...state.formState,
      repositories: newEntries
    }
  };
}

function validateScmHostUrlRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      validatingCompositeSourceControl: true
    }
  };
}

function validateScmHostUrlFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      validatingCompositeSourceControl: false
    },
    formState: {
      ...state.formState,
      currentHostUrlState: {
        ...state.formState.currentHostUrlState,
        validationErrors: payload.isValid ? null : payload.errorMessages
      }
    }
  };
}

function validateScmHostUrlFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      validatingCompositeSourceControl: false,
      generalError: payload
    }
  };
}

function setCurrentHostUrl(payload, state) {
  // stop the visual input validation feedback in the form flip between red and green on every keystroke
  // aka: given existing data is invalid (red) when data is changed to pass local validation but fail server validation
  //      don't change the UI to valid (green) then back to invalid (red) when the server side check completes
  //      instead keep the invalid state and wait for the server side check to complete
  let currentErrors = validateHostUrl(payload);
  let previousErrors = state.formState.currentHostUrlState.validationErrors;
  let validator = payload && hasValidationErrors(previousErrors) && !hasValidationErrors(currentErrors)
    ? () => previousErrors : validateHostUrl;

  return {
    ...state,
    formState: {
      ...state.formState,
      currentHostUrlState: textInputStateHelpers.userInput(validator, payload)
    }
  };
}

const reducerActionMap = {
  [SCM_ONBOARDING_LOAD_CONFIG_FULFILLED]: loadConfigFulfilled,
  [SCM_ONBOARDING_LOAD_CONFIG_FAILED]: loadConfigFailed,

  [SCM_ONBOARDING_LOAD_PAGE_REQUESTED]: loadPageRequested,
  [SCM_ONBOARDING_LOAD_PAGE_FULFILLED]: loadPageFulfilled,
  [SCM_ONBOARDING_LOAD_PAGE_FAILED]: loadPageFailed,

  [SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED]: loadRepositoriesRequested,
  [SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED]: loadRepositoriesFulfilled,
  [SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED]: loadRepositoriesFailed,

  [SCM_ONBOARDING_IMPORT_REPOS_REQUESTED]: importRepositoriesRequested,
  [SCM_ONBOARDING_IMPORT_REPOS_FULFILLED]: importRepositoriesFulfilled,
  [SCM_ONBOARDING_IMPORT_REPOS_FAILED]: importRepositoriesFailed,

  [SCM_ONBOARDING_SET_TARGET_ORGANIZATION]: setSelectedOrganization,

  [SCM_ONBOARDING_SET_CURRENT_HOST_URL]: setCurrentHostUrl,

  [SCM_ONBOARDING_SET_SORTING_PARAMETERS]: setSortingParameters,
  [SCM_ONBOARDING_SET_SORTING]: setSorting,

  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED]: validateScmHostUrlRequested,
  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED]: validateScmHostUrlFulfilled,
  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED]: validateScmHostUrlFailed
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
