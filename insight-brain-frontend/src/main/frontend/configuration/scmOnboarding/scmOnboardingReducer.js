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
  SCM_ONBOARDING_IS_GIT_HOST_NEEDED,
  SCM_ONBOARDING_SET_SORTING_PARAMETERS,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
  SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED,
  SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED,
  SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED,
  SCM_ONBOARDING_SHOW_HOST_DIALOG
} from './scmOnboardingActions';
import {sortItemsByFields} from '../../util/sortUtils';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import {validateHostUrl} from './utils/validators';
import {hasValidationErrors} from '../../util/validationUtil';
import {over, lensPath} from 'ramda';
import { propSet } from '../../util/jsUtil';
import {UI_ROUTER_ON_FINISH} from '../../reduxUiRouter/routerActions';
import ownerConstant from '../../utility/services/owner.constant';

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
    isSelectingOrganization: false,
    validatingCompositeSourceControl: false,
    isGitHostNeeded: false,
    isGitHostDialogVisible: false,

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

/*
 resets the page to a clean state, ready for subsequent calls
 */
function resetPage(payload, state) {
  return {
    ...initialState,
    // retain only the config
    configState: state.configState
  };
}

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

function loadPageFulfilled(payload, state) {
  const rootOrg = payload.organizationsResults.find(org => org.organization.id === ownerConstant.ROOT_ORGANIZATION_ID);
  const selectedOrganization = payload.organizationsResults.find(org =>
    org.organization.id === state.formState.preselectedOrganizationId);
  let newState = {
    ...state,
    viewState: {
      ...state.viewState,
      loadingPage: false
    },
    configState: {
      ...state.configState,
      isScmOnboardingFeatureEnabled: payload.configResults.scmOnboardingFeatureEnabled,
      isScmTokenConfigured: !!rootOrg.sourceControl.token.value,
      scmProvider: rootOrg !== null ? rootOrg.sourceControl.provider : null
    },
    formState: {
      ...state.formState,
      organizations: payload.organizationsResults.filter(
          org => org.organization.id !== ownerConstant.ROOT_ORGANIZATION_ID)
    }
  };
  return setTargetOrgFulfilled({
    defaultHostUrl: payload.hostUrlResult ? payload.hostUrlResult.defaultHostUrl : null,
    selectedOrganization: selectedOrganization
  }, newState);
}

function setShowHostDialogChanged(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isGitHostDialogVisible: payload
    }
  };
}

function setIsGitHostNeeded(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isGitHostNeeded: payload
    }
  };
}

function setTargetOrgRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isSelectingOrganization: true
    }
  };
}
function setTargetOrgFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isSelectingOrganization: false,
      generalError: payload
    }
  };
}
function setTargetOrgFulfilled({selectedOrganization, defaultHostUrl}, state) {
  const prevOrg = state.formState.selectedOrganization;
  const prevTokenOverridden = state.configState.isScmTokenOverridden;

  const currOrg = selectedOrganization;
  const currTokenOverridden = !!selectedOrganization && !!selectedOrganization.sourceControl &&
      !!selectedOrganization.sourceControl.token.value;

  const isAuthFailure = !!state.viewState.loadRepositoriesAuthError;

  const prevGitHostNeeded = state.viewState.isGitHostNeeded;

  // we need to prompt the user to enter a host URL when:
  // A. we get an authentication failure OR
  // B. the default host URL is empty AND an org is selected AND
  //    1. the token is overridden at the org level
  //    2. OR the previous token was overridden at the org level
  //    3. OR the previous org was empty (ie: this is the first selected org)
  //    4. OR the user needed to enter the git URL in the previous org
  const showHostDialog = isAuthFailure ||
      (!defaultHostUrl && !!currOrg && (currTokenOverridden || prevTokenOverridden || !prevOrg || prevGitHostNeeded));

  // we will set the current host URL to a default cloud value if the current host URL is empty
  const overrideCurrentHostUrl = !defaultHostUrl;

  return {
    ...state,
    viewState: {
      ...state.viewState,
      isSelectingOrganization: false,
      isGitHostNeeded: showHostDialog,
      isGitHostDialogVisible: showHostDialog
    },
    configState: {
      ...state.configState,
      isScmTokenOverridden: currTokenOverridden
    },
    formState: {
      ...state.formState,
      defaultHostUrl: defaultHostUrl,
      selectedOrganization: selectedOrganization,
      currentHostUrlState: overrideCurrentHostUrl ?
        initialHostUrlState(defaultHostUrl, state.configState.scmProvider) :
        textInputStateHelpers.initialState(defaultHostUrl)
    }
  };
}

const providerCloudDefaults = {
  'github': 'https://github.com/',
  'gitlab': 'https://gitlab.com/',
  'bitbucket': 'https://bitbucket.org/'
};

function initialHostUrlState(defaultHostUrl, scmProvider) {
  if (defaultHostUrl) {
    return textInputStateHelpers.initialState(defaultHostUrl);
  }
  let initialHostUrl = providerCloudDefaults[scmProvider];
  initialHostUrl = !initialHostUrl ? '' : initialHostUrl;
  return textInputStateHelpers.initialState(initialHostUrl);
}

function loadRepositoriesRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingRepositories: true,
      generalError: null,
      loadRepositoriesAuthError: null
    },
    formState: {
      ...state.formState,
      repositories: [],
      totalRepositories: 0,
      importedRepositoryCount: 0,
      selectedRepositoryCount: 0
    }
  };
}

function loadRepositoriesFulfilled(payload, state) {
  const repos = payload.availableRepositories ? sortItemsByFields(state.sortConfiguration.sortingOrder,
      payload.availableRepositories) : null;
  return payload.status === 'SUCCESS' ? {
    ...state,
    viewState: {
      ...state.viewState,
      loadingRepositories: false
    },
    formState: {
      ...state.formState,
      repositories: repos,
      totalRepositories: payload.totalRepositories
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
      loadRepositoriesAuthError: loadRepositoriesAuthError ? loadRepositoriesAuthError : null,
      isGitHostDialogVisible: state.viewState.isGitHostNeeded || !!loadRepositoriesAuthError
    },
    formState: {
      ...state.formState,
      repositories: null
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
  const { sortFields } = payload;

  // set sortConfiguration and sort repositories
  return over(lensPath(['formState', 'repositories']), sortItemsByFields(sortFields),
      propSet('sortConfiguration', payload, state));
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

  [SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED]: setTargetOrgFulfilled,
  [SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED]: setTargetOrgRequested,
  [SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED]: setTargetOrgFailed,

  [SCM_ONBOARDING_SET_CURRENT_HOST_URL]: setCurrentHostUrl,

  [SCM_ONBOARDING_SET_SORTING_PARAMETERS]: setSortingParameters,

  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED]: validateScmHostUrlRequested,
  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED]: validateScmHostUrlFulfilled,
  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED]: validateScmHostUrlFailed,

  [SCM_ONBOARDING_IS_GIT_HOST_NEEDED]: setIsGitHostNeeded,
  [SCM_ONBOARDING_SHOW_HOST_DIALOG]: setShowHostDialogChanged,

  [UI_ROUTER_ON_FINISH]: resetPage
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
