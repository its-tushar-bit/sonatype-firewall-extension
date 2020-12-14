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
  SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED,
  SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED,
  SCM_ONBOARDING_LOAD_COMPOSITE_SCM_REQUESTED,
  SCM_ONBOARDING_LOAD_CONFIG_FAILED,
  SCM_ONBOARDING_LOAD_CONFIG_FULFILLED,
  SCM_ONBOARDING_LOAD_CONFIG_REQUESTED,
  SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED,
  SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED,
  SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED,
  SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED,
  SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED,
  SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED,
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
import {Messages} from '../../util/CommonServices';
import {sortItemsByFields} from '../../util/sortUtils';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import {validateHostUrl} from './utils/validators';
import {hasValidationErrors} from '../../util/validationUtil';

const initialState = {
  configState: {
    isScmOnboardingFeatureEnabled: null,
    isScmTokenConfigured: null,
    scmProvider: ''
  },
  viewState: {
    loadingConfig: false,
    loadingOrganizations: false,
    loadingRepositories: false,
    loadingCompositeSourceControl: false,
    validatingCompositeSourceControl: false,

    lastErrorMessage: null
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

function loadConfigRequested() {
  return {
    ...initialState,
    viewState: {
      ...initialState.viewState,
      loadingConfig: true
    }
  };
}

function loadConfigFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingConfig: false
    },
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
      loadingConfig: false,
      lastErrorMessage: Messages.getHttpErrorMessage(payload)
    },
    configState: {
      ...state.configState,
      isScmOnboardingFeatureEnabled: null
    }
  };
}

function loadOrganizationsRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingOrganizations: true
    },
    formState: {
      ...state.formState,
      preselectedOrganizationId: payload
    }
  };
}

function loadOrganizationsFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingOrganizations: false
    },
    formState: {
      ...state.formState,
      selectedOrganization: payload.find(org => org.id === state.formState.preselectedOrganizationId)
          || state.selectedOrganization,
      organizations: payload
    }
  };
}

function loadOrganizationsFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingOrganizations: false,
      lastErrorMessage: Messages.getHttpErrorMessage(payload)
    },
    formState: {
      organizations: null,
      selectedOrganization: null
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
  return {
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
  };
}

function loadRepositoriesFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingRepositories: false,
      lastErrorMessage: Messages.getHttpErrorMessage(payload)
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
      lastErrorMessage: Messages.getHttpErrorMessage(payload)
    }
  };
}

function loadOrgDefaultHostUrlRequested(payload, state) {
  return {
    ...state,
    formState: {
      ...state.formState,
      defaultHostUrl: '',
      currentHostUrlState: textInputStateHelpers.initialState('')
    }
  };
}

function loadOrgDefaultHostUrlFulfilled(payload, state) {
  if (state.formState.currentHostUrlState.value === state.formState.defaultHostUrl) {
    // user has not changed the current value from the default, safe to update both
    return {
      ...state,
      formState: {
        ...state.formState,
        defaultHostUrl: payload.defaultHostUrl,
        currentHostUrlState: textInputStateHelpers.initialState(payload.defaultHostUrl)
      }
    };
  }
  else {
    return {
      ...state,
      formState: {
        ...state.formState,
        defaultHostUrl: payload.defaultHostUrl
      }
    };
  }
}

function loadOrgDefaultHostUrlFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      lastErrorMessage: Messages.getHttpErrorMessage(payload)
    },
    formState: {
      ...state.formState,
      defaultHostUrl: '',
      currentHostUrlState: textInputStateHelpers.initialState('')
    }
  };
}

function loadCompositeSourceControlRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingCompositeSourceControl: true
    },
    configState: {
      ...state.configState,
      isScmTokenConfigured: null,
      scmProvider: null
    }
  };
}

function loadCompositeSourceControlFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingCompositeSourceControl: false
    },
    configState: {
      ...state.configState,
      isScmTokenConfigured: !!payload.token.value || !!payload.token.parentValue,
      scmProvider: payload.provider
    }
  };
}

function loadCompositeSourceControlFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingCompositeSourceControl: false,
      lastErrorMessage: Messages.getHttpErrorMessage(payload)
    },
    configState: {
      ...state.configState,
      isScmTokenConfigured: null,
      scmProvider: null
    }
  };
}

function setSortingParameters(payload, state) {
  console.log('>> setSortingParameters', payload);
  return {
    ...state,
    sortConfiguration: payload
  };
}

function setSorting(payload, state) {
  console.log('>> setSorting, sortConfiguration', state.sortConfiguration);
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
      lastErrorMessage: Messages.getHttpErrorMessage(payload)
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
  [SCM_ONBOARDING_LOAD_CONFIG_REQUESTED]: loadConfigRequested,
  [SCM_ONBOARDING_LOAD_CONFIG_FULFILLED]: loadConfigFulfilled,
  [SCM_ONBOARDING_LOAD_CONFIG_FAILED]: loadConfigFailed,

  [SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED]: loadOrganizationsRequested,
  [SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED]: loadOrganizationsFulfilled,
  [SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED]: loadOrganizationsFailed,

  [SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED]: loadRepositoriesRequested,
  [SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED]: loadRepositoriesFulfilled,
  [SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED]: loadRepositoriesFailed,

  [SCM_ONBOARDING_IMPORT_REPOS_REQUESTED]: importRepositoriesRequested,
  [SCM_ONBOARDING_IMPORT_REPOS_FULFILLED]: importRepositoriesFulfilled,
  [SCM_ONBOARDING_IMPORT_REPOS_FAILED]: importRepositoriesFailed,

  [SCM_ONBOARDING_SET_TARGET_ORGANIZATION]: setSelectedOrganization,

  [SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED]: loadOrgDefaultHostUrlRequested,
  [SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED]: loadOrgDefaultHostUrlFulfilled,
  [SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED]: loadOrgDefaultHostUrlFailed,

  [SCM_ONBOARDING_SET_CURRENT_HOST_URL]: setCurrentHostUrl,

  [SCM_ONBOARDING_LOAD_COMPOSITE_SCM_REQUESTED]: loadCompositeSourceControlRequested,
  [SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED]: loadCompositeSourceControlFulfilled,
  [SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED]: loadCompositeSourceControlFailed,

  [SCM_ONBOARDING_SET_SORTING_PARAMETERS]: setSortingParameters,
  [SCM_ONBOARDING_SET_SORTING]: setSorting,

  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED]: validateScmHostUrlRequested,
  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED]: validateScmHostUrlFulfilled,
  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED]: validateScmHostUrlFailed
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
