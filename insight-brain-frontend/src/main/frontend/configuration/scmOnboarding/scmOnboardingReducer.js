/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from 'MainRoot/util/reduxUtil';
import {
  SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED,
  SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED,
  SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED,
  SCM_ONBOARDING_IMPORT_REPOS_FAILED,
  SCM_ONBOARDING_IMPORT_REPOS_FULFILLED,
  SCM_ONBOARDING_IMPORT_REPOS_REQUESTED,
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
  SCM_ONBOARDING_SHOW_HOST_DIALOG,
  SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE,
  SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE,
} from './scmOnboardingActions';
import { caseInsensitiveComparator, sortItemsByFieldsWithComparator } from 'MainRoot/util/sortUtils';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { validateHostUrl } from './utils/validators';
import { hasValidationErrors } from 'MainRoot/util/validationUtil';
import { over, lensPath } from 'ramda';
import { propSet } from 'MainRoot/util/jsUtil';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import ownerConstant from 'MainRoot/utility/services/owner.constant';
import { valueFromHierarchy, tokenForOrg } from './utils/providers';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const initialState = {
  permissionsState: {
    loadingPermissions: false,
    loadingPermissionsError: null,
  },
  configState: {
    isScmTokenConfigured: null,
    isScmTokenOverridden: null,
    isRootScmConfigured: null,
    scmProvider: '',
    rootProvider: null,
    rootOrgHasToken: null,
  },
  viewState: {
    loadingPage: false,
    loadingRepositories: false,
    isSelectingOrganization: false,
    validatingCompositeSourceControl: false,
    isGitHostNeeded: false,
    isGitHostDialogVisible: false,
    isNewOrganizationModalVisible: false,
    isImportStatusDialogVisible: false,
    isImporting: false,

    generalError: null,
    loadRepositoriesErrorCode: null,
    addOrganizationError: null,
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
    failedImportCount: 0,
    failedRepos: [],
  },
  sortConfiguration: {
    key: 'namespace',
    sortingOrder: ['namespace', 'project', 'description', 'defaultBranch'],
    dir: 'asc',
  },
};

/*
 * Router information has changed
 */
function onRouterFinish(payload, state) {
  // retain state if navigating using router within the same page
  if (payload.toState.name === 'scmOnboardingOrg' && payload.fromState.name === 'scmOnboardingOrg') {
    return state;
  }

  // resets the page to a clean state, ready for subsequent calls
  return {
    ...initialState,
    // retain only the config
    configState: state.configState,
  };
}

function checkScmOnboardingPermissionsRequested() {
  return {
    ...initialState,
    permissionsState: {
      ...initialState.permissionsState,
      loadingPermissions: true,
    },
  };
}

function checkScmOnboardingPermissionsFailed(payload) {
  return {
    ...initialState,
    permissionsState: {
      loadingPermissions: false,
      loadingPermissionsError: Messages.getHttpErrorMessage(payload),
    },
  };
}

function checkScmOnboardingPermissionsFulfilled(_, state) {
  return {
    ...state,
    permissionsState: {
      loadingPermissions: false,
      loadingPermissionsError: null,
    },
  };
}

function loadPageRequested(payload) {
  return {
    ...initialState,
    viewState: {
      ...initialState.viewState,
      loadingPage: true,
    },
    formState: {
      ...initialState.formState,
      preselectedOrganizationId: payload,
    },
  };
}

function loadPageFailed(payload) {
  return {
    ...initialState,
    viewState: {
      ...initialState.viewState,
      loadingPage: false,
      generalError: payload,
    },
  };
}

function loadPageFulfilled(payload, state) {
  let rootOrg = payload.organizationsResults.find((org) => org.organization.id === ownerConstant.ROOT_ORGANIZATION_ID);
  const selectedOrganization = payload.organizationsResults.find(
    (org) => org.organization.id === state.formState.preselectedOrganizationId
  );
  if (!rootOrg) {
    rootOrg = selectedOrganization;
  }
  const selectedOrgProvider = !selectedOrganization
    ? null
    : valueFromHierarchy(selectedOrganization.sourceControl.provider);
  const rootOrgProvider = rootOrg === null ? null : rootOrg.sourceControl.provider.value;
  const hasToken = !!tokenForOrg(selectedOrganization);
  let newState = {
    ...state,
    viewState: {
      ...state.viewState,
      loadingPage: false,
    },
    configState: {
      ...state.configState,
      isScmTokenConfigured: hasToken,
      scmProvider: selectedOrgProvider !== null ? selectedOrgProvider : rootOrgProvider,
      rootOrgHasToken: rootOrg !== null && !!rootOrg.sourceControl.token.value,
      rootProvider: rootOrgProvider,
      isRootScmConfigured: rootOrg.sourceControl && rootOrg.sourceControl.id !== null,
    },
    formState: {
      ...state.formState,
      organizations: payload.organizationsResults.filter(
        (org) => org.organization.id !== ownerConstant.ROOT_ORGANIZATION_ID
      ),
    },
  };
  if (!selectedOrganization) {
    return newState;
  }
  return setTargetOrgFulfilled(
    {
      defaultHostUrl: payload.hostUrlResult ? payload.hostUrlResult.defaultHostUrl : null,
      selectedOrganization: selectedOrganization,
    },
    newState
  );
}

function setIsImportStatusDialogVisibleChanged(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isImportStatusDialogVisible: payload,
    },
  };
}

function setShowHostDialogChanged(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isGitHostDialogVisible: payload,
    },
  };
}

function setIsGitHostNeeded(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isGitHostNeeded: payload,
    },
  };
}

function setTargetOrgRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isSelectingOrganization: true,
      loadRepositoriesErrorCode: null,
    },
  };
}
function setTargetOrgFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isSelectingOrganization: false,
      generalError: payload,
    },
  };
}
function setTargetOrgFulfilled({ selectedOrganization, defaultHostUrl }, state) {
  const prevOrg = state.formState.selectedOrganization;
  const prevTokenOverridden = state.configState.isScmTokenOverridden;

  const provider = selectedOrganization ? valueFromHierarchy(selectedOrganization.sourceControl.provider) : null;
  const providerChanged = provider !== state.configState.scmProvider;

  const currOrg = selectedOrganization;
  const currTokenOverridden =
    !!selectedOrganization && !!selectedOrganization.sourceControl && !!selectedOrganization.sourceControl.token.value;
  const hasToken = !!tokenForOrg(selectedOrganization);

  const isAuthFailure = !!state.viewState.loadRepositoriesErrorCode;

  const prevGitHostNeeded = state.viewState.isGitHostNeeded;

  // we need to prompt the user to enter a host URL when they have a token AND:
  // A. we get an authentication failure OR
  // B. the default host URL is empty AND an org is selected AND
  //    1. the token or provider is overridden at the org level
  //    2. OR the previous token was overridden at the org level
  //    3. OR the previous org was empty (ie: this is the first selected org)
  //    4. OR the user needed to enter the git URL in the previous org
  const showHostDialog =
    hasToken &&
    (isAuthFailure ||
      (!defaultHostUrl &&
        !!currOrg &&
        (currTokenOverridden || prevTokenOverridden || providerChanged || !prevOrg || prevGitHostNeeded)));

  // we will set the current host URL to a default cloud value if the current host URL is empty
  const overrideCurrentHostUrl = !defaultHostUrl;

  let newState = {
    ...state,
    viewState: {
      ...state.viewState,
      isSelectingOrganization: false,
      isGitHostNeeded: showHostDialog,
      isGitHostDialogVisible: showHostDialog,
    },
    configState: {
      ...state.configState,
      isScmTokenOverridden: currTokenOverridden,
      isScmTokenConfigured: hasToken,
      scmProvider: provider,
    },
    formState: {
      ...state.formState,
      defaultHostUrl: defaultHostUrl,
      selectedOrganization: selectedOrganization,
      currentHostUrlState: overrideCurrentHostUrl
        ? initialHostUrlState(defaultHostUrl, provider)
        : textInputStateHelpers.initialState(defaultHostUrl),
    },
  };

  if (providerChanged || currTokenOverridden || !hasToken) {
    newState = {
      ...newState,
      formState: {
        ...newState.formState,
        repositories: [],
      },
    };
  }
  return newState;
}

const providerCloudDefaults = {
  github: 'https://github.com/',
  gitlab: 'https://gitlab.com/',
  bitbucket: 'https://bitbucket.org/',
  azure: 'https://dev.azure.com/',
};

function initialHostUrlState(defaultHostUrl, scmProvider) {
  if (defaultHostUrl) {
    return textInputStateHelpers.initialState(defaultHostUrl);
  }
  let initialHostUrl = providerCloudDefaults[scmProvider];
  initialHostUrl = !initialHostUrl ? '' : initialHostUrl;
  return textInputStateHelpers.initialState(initialHostUrl);
}

function setIsNewOrganizationModalVisible(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isNewOrganizationModalVisible: payload,
      addOrganizationError: null,
    },
  };
}

function loadRepositoriesRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingRepositories: true,
      generalError: null,
      loadRepositoriesErrorCode: null,
    },
    formState: {
      ...state.formState,
      repositories: [],
      totalRepositories: 0,
      importedRepositoryCount: 0,
      selectedRepositoryCount: 0,
    },
  };
}

function loadRepositoriesFulfilled(payload, state) {
  const repos = payload.availableRepositories
    ? sortItemsByFieldsWithComparator(
        caseInsensitiveComparator,
        state.sortConfiguration.sortingOrder,
        payload.availableRepositories
      )
    : [];
  return payload.status === 'SUCCESS'
    ? {
        ...state,
        viewState: {
          ...state.viewState,
          loadingRepositories: false,
        },
        formState: {
          ...state.formState,
          repositories: repos,
          totalRepositories: payload.totalRepositories,
        },
      }
    : handleLoadRepositoriesFailed({ loadRepositoriesErrorCode: payload.status }, state);
}

function loadRepositoriesFailed(payload, state) {
  return handleLoadRepositoriesFailed({ generalError: payload }, state);
}

function handleLoadRepositoriesFailed({ generalError, loadRepositoriesErrorCode }, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingRepositories: false,
      generalError: generalError ? generalError : null,
      loadRepositoriesErrorCode: loadRepositoriesErrorCode ? loadRepositoriesErrorCode : null,
      isGitHostDialogVisible: state.viewState.isGitHostNeeded || !!loadRepositoriesErrorCode,
    },
    formState: {
      ...state.formState,
      repositories: [],
    },
  };
}

function importRepositoriesRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isImporting: true,
    },
    formState: {
      ...state.formState,
      selectedRepositoryCount: 0,
      failedImportCount: 0,
      failedRepos: [],
    },
  };
}

function importRepositoriesFulfilled(payload, state) {
  let importedRepos = payload.importedRepositories;
  let newRepositoryList = state.formState.repositories.filter(function (repo) {
    return !importedRepos.some((imported) => imported.httpCloneUrl === repo.httpCloneUrl);
  });
  return {
    ...state,
    formState: {
      ...state.formState,
      repositories: newRepositoryList,
      importedRepositoryCount: state.formState.importedRepositoryCount + importedRepos.length,
      selectedRepositoryCount: 0,
      newlyImportedRepos: importedRepos,
      failedImportCount: payload.failedImportCount,
      failedRepos: payload.failedRepositories,
    },
    viewState: {
      ...state.viewState,
      isImportStatusDialogVisible: true,
      isImporting: false,
    },
  };
}

function importRepositoriesFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      generalError: payload,
      isImporting: false,
    },
  };
}

function setSortingParameters(payload, state) {
  const { sortFields } = payload;

  // set sortConfiguration and sort repositories
  return over(
    lensPath(['formState', 'repositories']),
    sortItemsByFieldsWithComparator(caseInsensitiveComparator, sortFields),
    propSet('sortConfiguration', payload, state)
  );
}

function validateScmHostUrlRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      validatingCompositeSourceControl: true,
    },
  };
}

function validateScmHostUrlFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      validatingCompositeSourceControl: false,
    },
    formState: {
      ...state.formState,
      currentHostUrlState: {
        ...state.formState.currentHostUrlState,
        validationErrors: payload.isValid ? null : payload.errorMessages,
      },
    },
  };
}

function validateScmHostUrlFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      validatingCompositeSourceControl: false,
      generalError: payload,
    },
  };
}

function setCurrentHostUrl(payload, state) {
  // stop the visual input validation feedback in the form flip between red and green on every keystroke
  // aka: given existing data is invalid (red) when data is changed to pass local validation but fail server validation
  //      don't change the UI to valid (green) then back to invalid (red) when the server side check completes
  //      instead keep the invalid state and wait for the server side check to complete
  let currentErrors = validateHostUrl(payload);
  let previousErrors = state.formState.currentHostUrlState.validationErrors;
  let validator =
    payload && hasValidationErrors(previousErrors) && !hasValidationErrors(currentErrors)
      ? () => previousErrors
      : validateHostUrl;

  return {
    ...state,
    formState: {
      ...state.formState,
      currentHostUrlState: textInputStateHelpers.userInput(validator, payload),
    },
  };
}

const reducerActionMap = {
  [SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED]: checkScmOnboardingPermissionsRequested,
  [SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED]: checkScmOnboardingPermissionsFulfilled,
  [SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED]: checkScmOnboardingPermissionsFailed,

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

  [SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE]: setIsNewOrganizationModalVisible,

  [SCM_ONBOARDING_SET_CURRENT_HOST_URL]: setCurrentHostUrl,

  [SCM_ONBOARDING_SET_SORTING_PARAMETERS]: setSortingParameters,

  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED]: validateScmHostUrlRequested,
  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED]: validateScmHostUrlFulfilled,
  [SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED]: validateScmHostUrlFailed,

  [SCM_ONBOARDING_IS_GIT_HOST_NEEDED]: setIsGitHostNeeded,
  [SCM_ONBOARDING_SHOW_HOST_DIALOG]: setShowHostDialogChanged,

  [SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE]: setIsImportStatusDialogVisibleChanged,

  [UI_ROUTER_ON_FINISH]: onRouterFinish,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
