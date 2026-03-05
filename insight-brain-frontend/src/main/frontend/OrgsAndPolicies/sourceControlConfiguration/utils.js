/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  combineValidationErrors,
  nxFormSelectStateHelpers,
  nxTextInputStateHelpers,
} from '@sonatype/react-shared-components';
import {
  validateDoubleWhitespace,
  validateMaxLength,
  validateNonEmpty,
  validatePatternMatch,
} from 'MainRoot/util/validationUtil';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
const { initialState: rscInitialState } = nxTextInputStateHelpers;
const { initialState: rscSelectInitialState } = nxFormSelectStateHelpers;

const validateUrlPatternMatch = validatePatternMatch(
  /(http[s]?:\/\/[^?#\s]+)/,
  'A valid HTTP(S) repository clone URL is required'
);

export const SOURCE_CONTROL_UNSUPPORTED_MESSAGE = 'Source Control is not supported by your license';
export const SCM_FEATURE_UNSUPPORTED_MESSAGE = 'This feature is not supported by your license';
export const GITHUB_APP_NOT_CONFIGURED_MESSAGE =
  'Please configure and install a GitHub App or switch to Personal Access Token authentication.';
export const DEFAULT_BRANCH_SUBLABEL =
  'The branch used for automated remediation pull requests and as the basis for finding introduced violations in feature branches';
export const ROOT_ORG_NAME = 'Root Organization';
export const PROVIDER_TYPES = [
  { name: 'Azure DevOps', value: 'azure' },
  { name: 'Bitbucket', value: 'bitbucket' },
  { name: 'GitHub', value: 'github' },
  { name: 'GitLab', value: 'gitlab' },
];
export const PROVIDERS_WITH_USERNAME = ['azure', 'bitbucket'];
export const PROVIDERS_SUPPORTING_PULL_REQUESTS = ['azure', 'bitbucket', 'github', 'gitlab'];
export const PROVIDERS_SUPPORTING_FAILED_CHECKS_CLOSE = ['github', 'gitlab'];
export const AUTHENTICATION_TYPES = {
  GITHUB_APP: 'GITHUB_APP',
  PAT: 'PAT',
};
export const BRANCH_INPUT_MAX_CHARACTERS = 243,
  USERNAME_INPUT_MAX_CHARACTERS = 255,
  TOKEN_INPUT_MAX_CHARACTERS = 512;
export const SCM_FORM_STATE_KEY_PREFIX = 'scmFormState_';

/**
 * Generates sessionStorage key for SCM form state scoped to specific owner.
 * Keys are unique per owner to prevent cross-tenant data leakage in multi-tenant environments.
 *
 * @param {string} ownerType - Owner type: 'application' or 'organization'
 * @param {string|number} ownerId - Unique identifier for the owner
 * @returns {string} Storage key in format 'scmFormState_{ownerType}_{ownerId}'
 * @example
 * getScmFormStateStorageKey('organization', '12345')
 * // Returns: 'scmFormState_organization_12345'
 */
export const getScmFormStateStorageKey = (ownerType, ownerId) => `${SCM_FORM_STATE_KEY_PREFIX}${ownerType}_${ownerId}`;

/**
 * Saves form state to sessionStorage.
 * SessionStorage persists across cross-origin navigation (GitHub OAuth redirect)
 * in all major browsers (Chrome, Edge, Firefox, Safari) and auto-clears on tab close.
 *
 * @param {string} key - Storage key (from getScmFormStateStorageKey)
 * @param {Object} data - Form state data to save
 */
export const saveFormStateWithFallback = (key, data) => {
  try {
    sessionStorage.setItem(key, JSON.stringify(data));
  } catch (error) {
    console.warn('Failed to save form state to sessionStorage:', error);
  }
};

/**
 * Loads form state from sessionStorage.
 *
 * @param {string} key - Storage key (from getScmFormStateStorageKey)
 * @returns {string|null} JSON string of saved form state, or null if not found
 */
export const loadFormStateWithFallback = (key) => {
  try {
    return sessionStorage.getItem(key);
  } catch (error) {
    console.warn('Failed to read from sessionStorage:', error);
    return null;
  }
};

/**
 * Removes form state from sessionStorage.
 * Best-effort cleanup - doesn't throw if removal fails.
 *
 * @param {string} key - Storage key (from getScmFormStateStorageKey)
 */
export const removeFormStateWithFallback = (key) => {
  try {
    sessionStorage.removeItem(key);
  } catch (e) {
    // Silent fail - cleanup is best-effort
  }
};
export const SOURCE_CONTROL_OPTIONS = [
  {
    id: 'source-control-ssh',
    title: 'Use SSH for Git Operations',
    description: 'Clone and push changes to repositories via Secure Shell Protocol (SSH)',
    optionName: 'sshEnabled',
  },
  {
    id: 'source-control-remediation-pull-requests',
    title: 'Automated Remediation with GoldenPRs™',
    description:
      'Create pull requests with remediation suggestions for policy violations discovered on the default branch.\n' +
      '##### Golden Versions for Maven\n' +
      'Pull requests for Maven dependencies are generated when the recommended version, including transitive dependencies, is non-breaking and safe to use.',
    optionName: 'remediationPullRequestsEnabled',
  },
  {
    id: 'inner-source-automated-updates',
    title: 'Automated InnerSource Updates',
    description:
      'Create pull requests for consuming applications when new versions of InnerSource components are released.',
    optionName: 'innerSourceAutomatedUpdatesEnabled',
  },
  {
    id: 'source-control-pull-request-commenting',
    title: 'Pull Request Commenting',
    description:
      'Comment on pull requests with remediation suggestions for policy violations discovered on the default branch.',
    optionName: 'pullRequestCommentingEnabled',
  },
  {
    id: 'source-control-evaluations',
    title: 'Source Control Evaluations',
    description: 'Allow IQ server to scan and evaluate the contents of the configured repository when needed.',
    optionName: 'sourceControlEvaluationsEnabled',
  },
  {
    id: 'automated-commit-feedback',
    title: 'Automated Commit Feedback',
    description: 'Allow IQ server to create commit statuses.',
    optionName: 'commitStatusEnabled',
  },
  {
    id: 'manual-pull-requests',
    title: 'Manual Pull Requests',
    description:
      'Display an option to manually create a pull request against the default branch when suggested version changes are available.',
    optionName: 'manualPullRequestsEnabled',
  },
];

export const compositeSourceControlToModel = (
  {
    ownerId,
    id,
    repositoryUrl,
    provider,
    token,
    username,
    baseBranch,
    authenticationType,
    pullRequestCommentingEnabled,
    remediationPullRequestsEnabled,
    sourceControlEvaluationsEnabled,
    commitStatusEnabled,
    statusChecksEnabled,
    sshEnabled,
    manualPullRequestsEnabled,
    innerSourceAutomatedUpdatesEnabled,
    closePrOnFailedChecksEnabled,
    closePrAfterDaysOpenEnabled,
    closePrAfterDays,
    githubApp,
  },
  isRootOrg
) => {
  const sourceControlData = {
    ownerId,
    id,
    repositoryUrl: rscInitialState(repositoryUrl ?? '', urlFieldValidator),
    provider: {
      rscValue: rscSelectInitialState(provider.value ?? '', validateNonEmpty),
      isInherited: provider.value === null && !isRootOrg,
      parentValue: rscSelectInitialState(provider.parentValue ?? '', validateNonEmpty),
      parentName: provider.parentName,
    },
    baseBranch: {
      rscValue:
        !baseBranch.value && isRootOrg
          ? rscInitialState('main')
          : rscInitialState(baseBranch?.value ?? '', () =>
              textFieldValidator(baseBranch?.value, BRANCH_INPUT_MAX_CHARACTERS)
            ),
      isInherited: baseBranch.value === null && !isRootOrg,
      parentValue: rscInitialState(baseBranch?.parentValue ?? '', () =>
        textFieldValidator(baseBranch?.parentValue, BRANCH_INPUT_MAX_CHARACTERS)
      ),
      parentName: baseBranch.parentName,
    },
    authenticationType: {
      value: authenticationType?.value ?? null,
      isInherited: authenticationType?.value === null && !isRootOrg,
      parentValue: authenticationType?.parentValue ?? null,
      parentName: authenticationType?.parentName,
      rscValue: rscInitialState(authenticationType?.value ?? ''),
    },
    pullRequestCommentingEnabled: {
      ...pullRequestCommentingEnabled,
      isInherited: pullRequestCommentingEnabled.value === null && !isRootOrg,
      value: setDefaultIfNull(pullRequestCommentingEnabled.value, pullRequestCommentingEnabled.parentValue, true),
    },
    remediationPullRequestsEnabled: {
      ...remediationPullRequestsEnabled,
      isInherited: remediationPullRequestsEnabled.value === null && !isRootOrg,
      value: setDefaultIfNull(remediationPullRequestsEnabled.value, remediationPullRequestsEnabled.parentValue, false),
    },
    sourceControlEvaluationsEnabled: {
      ...sourceControlEvaluationsEnabled,
      isInherited: sourceControlEvaluationsEnabled.value === null && !isRootOrg,
      value: setDefaultIfNull(sourceControlEvaluationsEnabled.value, sourceControlEvaluationsEnabled.parentValue, true),
    },
    sshEnabled: {
      ...sshEnabled,
      isInherited: sshEnabled.value === null && !isRootOrg,
      value: setDefaultIfNull(sshEnabled.value, sshEnabled.parentValue, null),
    },
    commitStatusEnabled: {
      ...commitStatusEnabled,
      isInherited: commitStatusEnabled.value === null && !isRootOrg,
      value: setDefaultIfNull(commitStatusEnabled.value, commitStatusEnabled.parentValue, true),
    },
    statusChecksEnabled,
    manualPullRequestsEnabled: {
      ...manualPullRequestsEnabled,
      isInherited: manualPullRequestsEnabled?.value == null && !isRootOrg,
      value: setDefaultIfNull(
        manualPullRequestsEnabled?.value ?? null,
        manualPullRequestsEnabled?.parentValue ?? null,
        true
      ),
    },
    innerSourceAutomatedUpdatesEnabled: {
      ...innerSourceAutomatedUpdatesEnabled,
      isInherited: innerSourceAutomatedUpdatesEnabled?.value == null && !isRootOrg,
      value: setDefaultIfNull(
        innerSourceAutomatedUpdatesEnabled?.value ?? null,
        innerSourceAutomatedUpdatesEnabled?.parentValue ?? null,
        true
      ),
    },
    closePrOnFailedChecksEnabled: {
      ...closePrOnFailedChecksEnabled,
      isInherited: closePrOnFailedChecksEnabled?.value === null && !isRootOrg,
      value: setDefaultIfNull(closePrOnFailedChecksEnabled?.value, closePrOnFailedChecksEnabled?.parentValue, true),
    },
    closePrAfterDaysOpenEnabled: {
      ...closePrAfterDaysOpenEnabled,
      isInherited: closePrAfterDaysOpenEnabled?.value === null && !isRootOrg,
      value: setDefaultIfNull(closePrAfterDaysOpenEnabled?.value, closePrAfterDaysOpenEnabled?.parentValue, false),
    },
    closePrAfterDays: {
      parentName: closePrAfterDays?.parentName,
      isInherited: closePrAfterDays?.value === null && !isRootOrg,
      parentValue: rscInitialState(closePrAfterDays?.value?.toString() ?? '', (val) => {
        const numVal = parseInt(val, 10);
        return !val || (numVal > 0 && numVal <= 365) ? null : 'Must be a number between 1 and 365';
      }),
      rscValue: rscInitialState(closePrAfterDays?.value?.toString() ?? '', (val) => {
        const numVal = parseInt(val, 10);
        return !val || (numVal > 0 && numVal <= 365) ? null : 'Must be a number between 1 and 365';
      }),
    },
    githubApp: {
      // Shallow copy prevents shared references (githubApp contains only primitives)
      value: githubApp?.value ? { ...githubApp.value } : null,
      isInherited: githubApp?.value === null && !isRootOrg,
      parentValue: githubApp?.parentValue ? { ...githubApp.parentValue } : null,
      parentName: githubApp?.parentName,
    },
  };
  // Handle edge case: provider inherited from sub-org but token at root level
  if (provider.parentName !== ROOT_ORG_NAME && token.parentName === ROOT_ORG_NAME) {
    sourceControlData.token = {
      rscValue: rscInitialState(token.value ?? '', () => textFieldValidator(token.value, TOKEN_INPUT_MAX_CHARACTERS)),
      isInherited: false,
      parentName: null,
    };
    sourceControlData.username = {
      rscValue: rscInitialState('', () => textFieldValidator('', USERNAME_INPUT_MAX_CHARACTERS)),
      isInherited: false,
      parentName: null,
      parentValue: null,
    };
  } else {
    sourceControlData.token = {
      rscValue: rscInitialState(
        token.value ?? '',
        token.value ? () => textFieldValidator(token.value, TOKEN_INPUT_MAX_CHARACTERS) : null
      ),
      isInherited: token.value === null && !isRootOrg,
      parentName: token.parentName,
      parentValue: rscInitialState(
        token.parentValue ?? '',
        token.parentValue ? () => textFieldValidator(token.parentValue, TOKEN_INPUT_MAX_CHARACTERS) : null
      ),
    };
    sourceControlData.username = {
      rscValue: rscInitialState(
        username.value ?? '',
        PROVIDERS_WITH_USERNAME.includes(provider.value) && username.value
          ? () => textFieldValidator(username.value ?? '', USERNAME_INPUT_MAX_CHARACTERS)
          : null
      ),
      isInherited: username.value === null && !isRootOrg,
      parentName: username.parentName,
      parentValue: rscInitialState(
        username.parentValue ?? '',
        PROVIDERS_WITH_USERNAME.includes(provider.parentValue) && username.parentValue
          ? () => textFieldValidator(username.parentValue ?? '', USERNAME_INPUT_MAX_CHARACTERS)
          : null
      ),
    };
  }
  return sourceControlData;
};

export const prepareSubmitData = (sourceControl, serverSourceControl, isApp, isRootOrg, isAutomationSupported) => {
  if (!sourceControl) return {};
  const { ownerId, id, sshEnabled } = sourceControl;
  const submitData = {
    ownerId,
    id,
    pullRequestCommentingEnabled: getPullRequestCommentingEnabledFlagFromModel(
      sourceControl,
      serverSourceControl,
      isRootOrg,
      isAutomationSupported
    ),
    commitStatusEnabled: getCommitStatusEnabledFlagFromModel(
      sourceControl,
      serverSourceControl,
      isRootOrg,
      isAutomationSupported
    ),
    remediationPullRequestsEnabled: getRemediationPullRequestsEnabledFlagFromModel(
      sourceControl,
      serverSourceControl,
      isRootOrg,
      isAutomationSupported
    ),
    sourceControlEvaluationsEnabled: getSourceControlEvaluationsEnabledFlagFromModel(
      sourceControl,
      serverSourceControl,
      isRootOrg,
      isAutomationSupported
    ),
    manualPullRequestsEnabled: getManualPullRequestsEnabledFlagFromModel(
      sourceControl,
      serverSourceControl,
      isRootOrg,
      isAutomationSupported
    ),
    innerSourceAutomatedUpdatesEnabled: getInnerSourceAutomatedUpdatesEnabledFlagFromModel(
      sourceControl,
      serverSourceControl,
      isRootOrg,
      isAutomationSupported
    ),
    statusChecksEnabled: true,
    repositoryUrl: isApp ? sourceControl.repositoryUrl.trimmedValue : null,
    sshEnabled: sshEnabled.value,
    closePrOnFailedChecksEnabled: getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl),
    closePrAfterDaysOpenEnabled: sourceControl.closePrAfterDaysOpenEnabled.value ?? false,
    closePrAfterDays: isNaN(parseInt(sourceControl.closePrAfterDays?.rscValue?.trimmedValue, 10))
      ? null
      : parseInt(sourceControl.closePrAfterDays?.rscValue?.trimmedValue, 10),
    username: null,
    token: null,
    provider: null,
    authenticationType: null,
  };
  if (
    PROVIDERS_WITH_USERNAME.includes(sourceControl.provider.rscValue.value) ||
    (PROVIDERS_WITH_USERNAME.includes(sourceControl.provider.parentValue.value) && sourceControl.provider.isInherited)
  ) {
    // bitbucket uses 'credentials' to gather username & password. They both move as a single block
    if (
      (!sourceControl.token.isInherited || isRootOrg || !sourceControl.provider.isInherited) &&
      sourceControl.token.rscValue.trimmedValue &&
      sourceControl.username.rscValue.trimmedValue
    ) {
      submitData.username = sourceControl.username.rscValue.trimmedValue;
      submitData.token = sourceControl.token.rscValue.trimmedValue;
    }
  } else {
    // username only supported in Bitbucket & Azure DevOps
    if (
      (!sourceControl.token.isInherited || isRootOrg || !sourceControl.provider.isInherited) &&
      sourceControl.token.rscValue.trimmedValue
    ) {
      submitData.token =
        sourceControl.token.rscValue.trimmedValue === '' ? null : sourceControl.token.rscValue.trimmedValue;
    }
  }

  if (!sourceControl.provider.isInherited || (isRootOrg && sourceControl.provider.rscValue.value)) {
    submitData.provider = sourceControl.provider.rscValue.value === '' ? null : sourceControl.provider.rscValue.value;
  }

  if (!sourceControl.baseBranch.isInherited || (isRootOrg && sourceControl.baseBranch.trimmedValue)) {
    submitData.baseBranch =
      sourceControl.baseBranch.rscValue.trimmedValue === ''
        ? null
        : getBaseBranchValueFromModel(sourceControl, serverSourceControl, isRootOrg, isAutomationSupported);
  } else {
    submitData.baseBranch = null;
  }

  // Save or clear authenticationType based on provider
  const effectiveProviderValue = sourceControl.provider.isInherited
    ? serverSourceControl.provider.parentValue.value
    : sourceControl.provider.rscValue.value;

  if (effectiveProviderValue === 'github') {
    if (!sourceControl.authenticationType.isInherited || isRootOrg) {
      submitData.authenticationType = sourceControl.authenticationType.value;
    } else {
      submitData.authenticationType = null;
    }
  } else {
    submitData.authenticationType = null;
  }
  // Clear githubApp when: inherited, non-GitHub provider, or PAT auth selected
  const isInheritingGithubApp = !isRootOrg && sourceControl.githubApp.isInherited;
  const isNotGitHubProvider = effectiveProviderValue !== 'github';
  const isUserSelectingPAT =
    effectiveProviderValue === 'github' && sourceControl.authenticationType.value === AUTHENTICATION_TYPES.PAT;

  const shouldClearGithubApp = isInheritingGithubApp || isNotGitHubProvider || isUserSelectingPAT;

  if (shouldClearGithubApp) {
    submitData.githubApp = null;
  }

  return submitData;
};

export const effectiveProvider = (sourceControl, serverSourceControl) => {
  if (!sourceControl) return;
  return sourceControl.provider?.isInherited
    ? serverSourceControl?.provider?.parentValue?.value
    : sourceControl.provider?.rscValue?.value;
};

/**
 * Determines if GitHub App authentication should be shown instead of standard credentials.
 *
 * Returns true when ALL of the following are true:
 * 1. The provider is GitHub (either current or inherited)
 * 2. The GitHub App authentication feature is enabled
 *
 * @param {Object} sourceControl - Current source control configuration
 * @param {Object} serverSourceControl - Server-level source control configuration
 * @param {boolean} isGithubAppAuthenticationEnabled - Feature flag state
 * @returns {boolean} True if GitHub App auth should be displayed
 */
export const shouldShowGitHubAppAuth = (sourceControl, serverSourceControl, isGithubAppAuthenticationEnabled) => {
  const isGitHub = effectiveProvider(sourceControl, serverSourceControl) === 'github';
  return isGitHub && isGithubAppAuthenticationEnabled;
};

export const effectiveFieldInheritFrom = (sourceControl, serverSourceControl, field) => {
  if (!sourceControl) return;
  return sourceControl.provider.isInherited ? serverSourceControl[field].parentName : null;
};

export const providerNeedsUsername = (sourceControl, serverSourceControl) =>
  PROVIDERS_WITH_USERNAME.includes(effectiveProvider(sourceControl, serverSourceControl));

export const isUsernameRequiredOnNode = (sourceControl, serverSourceControl, isApp) => {
  return (
    isApp &&
    !effectiveFieldInheritFrom(sourceControl, serverSourceControl, 'username') &&
    providerNeedsUsername(sourceControl, serverSourceControl)
  );
};

export const isAccessTokenRequiredOnNode = (sourceControl, serverSourceControl, isApp) => {
  // Only check at application level
  if (!isApp) {
    return false;
  }

  // Get effective provider (could be inherited or overridden)
  const provider = effectiveProvider(sourceControl, serverSourceControl);
  const isGitHub = provider === 'github';

  // Check if provider is inherited (applies to all providers)
  const isProviderInherited = sourceControl?.provider?.isInherited;
  const hasParentProvider = sourceControl?.provider?.parentValue?.value;

  // Cross-provider token validation: If provider changed and token is inherited from different provider
  // then token is incompatible and required at app level
  const isTokenInherited = sourceControl?.token?.isInherited;
  const parentProvider = serverSourceControl?.provider?.parentValue?.value;
  const currentProvider = sourceControl?.provider?.rscValue?.value;

  if (
    isTokenInherited &&
    !isProviderInherited &&
    currentProvider &&
    parentProvider &&
    currentProvider !== parentProvider
  ) {
    // Provider was overridden to a different provider but token is still inherited from old provider
    // This means the inherited token is incompatible with the new provider
    return true; // Token required - inherited token cannot be used with different provider
  }

  if (isGitHub) {
    // For GitHub, auth inheritance is INDEPENDENT of provider inheritance
    // User can inherit provider but override auth method (or vice versa)
    const isAuthInherited = sourceControl?.authenticationType?.isInherited;
    const hasParentAuth = sourceControl?.authenticationType?.parentValue;
    if (isAuthInherited && hasParentAuth) {
      // Auth method inherited from parent = no token needed at App level
      return false;
    }

    // Auth method is overridden - check if it's GitHub App (no token needed)
    const authType = sourceControl?.authenticationType?.value;
    if (authType === AUTHENTICATION_TYPES.GITHUB_APP) {
      // GitHub App authentication doesn't use token
      return false;
    }

    // If provider is inherited AND auth is inherited AND parent has a token
    // Check: provider inherited + no local auth override + auth inherited + token inherited + parent has token
    const isTokenInherited = sourceControl?.token?.isInherited;
    const hasParentToken = sourceControl?.token?.parentValue?.value;
    if (
      isProviderInherited &&
      hasParentProvider &&
      !authType &&
      isAuthInherited &&
      isTokenInherited &&
      hasParentToken
    ) {
      // Fully inherited with token = no token needed at app level
      return false;
    }

    // Check if token is inherited and parent has token (even if provider inherited)
    if (isTokenInherited && hasParentToken) {
      return false; // Token inherited from parent
    }

    // Check if app has a token value (either in current input or saved on server)
    const hasCurrentToken = sourceControl?.token?.rscValue?.value || sourceControl?.token?.rscValue?.trimmedValue;
    const hasSavedToken = serverSourceControl?.token?.value;

    // Special case: If token is NOT currently inherited but parent has a token,
    // this might be a case where app is switching from inherited to override
    // and the field value hasn't been populated yet (happens during radio click).
    // In this case, check if there's a parent token available that can be used.
    const isOverridingWithParentToken = !isTokenInherited && hasParentToken && !hasCurrentToken && !hasSavedToken;

    if (hasCurrentToken || hasSavedToken || isOverridingWithParentToken) {
      return false; // App has a token (current, saved, or available from parent during override)
    }

    // Auth method overridden to PAT or provider overridden or no parent token = token required at app level
    return true;
  }

  // For non-GitHub providers (Bitbucket, GitLab, Azure, etc.)
  // Check if provider is inherited WITH a parent value AND parent has credentials
  if (isProviderInherited && hasParentProvider) {
    // Check if parent actually has credentials to inherit
    const hasParentToken = sourceControl?.token?.parentValue?.value;
    const hasParentUsername = sourceControl?.username?.parentValue?.value;
    const providerNeedsUsernameValue = PROVIDERS_WITH_USERNAME.includes(hasParentProvider);

    const hasParentCredentials = providerNeedsUsernameValue ? hasParentToken && hasParentUsername : hasParentToken;

    if (hasParentCredentials) {
      // Provider inherited AND parent has credentials = no token needed at app level
      return false;
    }

    // Provider inherited but parent has NO credentials = token required at app level
    return true;
  }

  // Provider overridden or no parent provider = token required
  return true;
};

export const setDefaultIfNull = (value, parentValue, defaultValue) => {
  return value === null && parentValue === null ? defaultValue : value;
};

export const arePullRequestsSupported = (sourceControl, serverSourceControl, isAutomationSupported) =>
  (!effectiveProvider(sourceControl, serverSourceControl) ||
    PROVIDERS_SUPPORTING_PULL_REQUESTS.includes(effectiveProvider(sourceControl, serverSourceControl))) &&
  isAutomationSupported;

export const getPullRequestCommentingEnabledFlagFromModel = (
  sourceControl,
  serverSourceControl,
  isRootOrg,
  isAutomationSupported
) => {
  if (!isRootOrg || isAutomationSupported) {
    return sourceControl.pullRequestCommentingEnabled.value;
  }

  return serverSourceControl.pullRequestCommentingEnabled.value === null
    ? true
    : serverSourceControl.pullRequestCommentingEnabled.value;
};

export const getCommitStatusEnabledFlagFromModel = (
  sourceControl,
  serverSourceControl,
  isRootOrg,
  isAutomationSupported
) => {
  if (!isRootOrg || isAutomationSupported) {
    return sourceControl.commitStatusEnabled.value;
  }

  return serverSourceControl.commitStatusEnabled.value === null ? true : serverSourceControl.commitStatusEnabled.value;
};

export const getRemediationPullRequestsEnabledFlagFromModel = (
  sourceControl,
  serverSourceControl,
  isRootOrg,
  isAutomationSupported
) => {
  if (!isRootOrg || arePullRequestsSupported(sourceControl, serverSourceControl, isAutomationSupported)) {
    return sourceControl.remediationPullRequestsEnabled.value;
  }

  return serverSourceControl.remediationPullRequestsEnabled.value === null
    ? true
    : serverSourceControl.remediationPullRequestsEnabled.value;
};

export const getSourceControlEvaluationsEnabledFlagFromModel = (
  sourceControl,
  serverSourceControl,
  isRootOrg,
  isAutomationSupported
) => {
  if (!isRootOrg || isAutomationSupported) {
    return sourceControl.sourceControlEvaluationsEnabled.value;
  }

  return serverSourceControl.sourceControlEvaluationsEnabled.value === null
    ? true
    : serverSourceControl.sourceControlEvaluationsEnabled.value;
};

export const getManualPullRequestsEnabledFlagFromModel = (
  sourceControl,
  serverSourceControl,
  isRootOrg,
  isAutomationSupported
) => {
  if (!isRootOrg || arePullRequestsSupported(sourceControl, serverSourceControl, isAutomationSupported)) {
    return sourceControl.manualPullRequestsEnabled?.value ?? null;
  }

  return serverSourceControl.manualPullRequestsEnabled?.value ?? true;
};

export const getInnerSourceAutomatedUpdatesEnabledFlagFromModel = (
  sourceControl,
  serverSourceControl,
  isRootOrg,
  isAutomationSupported
) => {
  if (!isRootOrg || isAutomationSupported) {
    return sourceControl.innerSourceAutomatedUpdatesEnabled?.value ?? null;
  }

  return serverSourceControl.innerSourceAutomatedUpdatesEnabled?.value ?? false;
};

export const getBaseBranchValueFromModel = (sourceControl, serverSourceControl, isRootOrg, isAutomationSupported) => {
  if (!isRootOrg || arePullRequestsSupported(sourceControl, serverSourceControl, isAutomationSupported)) {
    return sourceControl.baseBranch.rscValue.trimmedValue;
  }

  return serverSourceControl.baseBranch.rscValue.trimmedValue === ''
    ? 'main'
    : serverSourceControl.baseBranch.rscValue.trimmedValue;
};

export const getClosePrOnFailedChecksEnabledFlagFromModel = (sourceControl, serverSourceControl) => {
  const provider = effectiveProvider(sourceControl, serverSourceControl);

  if (!PROVIDERS_SUPPORTING_FAILED_CHECKS_CLOSE.includes(provider)) {
    return null;
  }

  return sourceControl.closePrOnFailedChecksEnabled.value ?? false;
};

export const getDataFromSourceControl = (
  ownerType,
  {
    provider,
    username,
    token,
    baseBranch,
    authenticationType,
    pullRequestCommentingEnabled,
    commitStatusEnabled,
    remediationPullRequestsEnabled,
    sourceControlEvaluationsEnabled,
    statusChecksEnabled,
    sshEnabled,
    manualPullRequestsEnabled,
    repositoryUrl,
    innerSourceAutomatedUpdatesEnabled,
    closePrOnFailedChecksEnabled,
    closePrAfterDaysOpenEnabled,
    closePrAfterDays,
  }
) => {
  const data = {
    provider,
    username,
    token,
    baseBranch,
    authenticationType,
    remediationPullRequestsEnabled,
    statusChecksEnabled,
    pullRequestCommentingEnabled,
    commitStatusEnabled,
    sourceControlEvaluationsEnabled,
    sshEnabled,
    manualPullRequestsEnabled,
    innerSourceAutomatedUpdatesEnabled,
    closePrOnFailedChecksEnabled,
    closePrAfterDaysOpenEnabled,
    closePrAfterDays,
  };
  if (ownerType === 'application') {
    data.repositoryUrl = repositoryUrl;
  }
  return data;
};

export const textFieldValidator = (val, maxLength) =>
  combineValidationErrors(
    validateNonEmpty(val?.trim()),
    validateMaxLength(maxLength, val),
    validateDoubleWhitespace(val)
  );

export const urlFieldValidator = (val) =>
  combineValidationErrors(validateNonEmpty(val?.trim()), validateUrlPatternMatch(val));

export const setIsDirty = (state) => {
  const { sourceControl, serverSourceControl } = state;
  const formFields = [
    'provider',
    'token',
    'username',
    'baseBranch',
    'authenticationType',
    'githubApp',
    'pullRequestCommentingEnabled',
    'commitStatusEnabled',
    'remediationPullRequestsEnabled',
    'sourceControlEvaluationsEnabled',
    'manualPullRequestsEnabled',
    'sshEnabled',
    'repositoryUrl',
    'innerSourceAutomatedUpdatesEnabled',
    'closePrOnFailedChecksEnabled',
    'closePrAfterDaysOpenEnabled',
    'closePrAfterDays',
  ];

  const isDirty = formFields.some((property) => {
    let fieldIsDirty = false;

    if (property === 'provider') {
      fieldIsDirty =
        sourceControl[property]?.rscValue?.value !== serverSourceControl[property]?.rscValue?.value ||
        sourceControl[property]?.isInherited !== serverSourceControl[property]?.isInherited;
    } else if (property === 'repositoryUrl') {
      fieldIsDirty = sourceControl[property]?.value !== serverSourceControl[property]?.value;
    } else if (property === 'authenticationType') {
      fieldIsDirty =
        sourceControl[property]?.value !== serverSourceControl[property]?.value ||
        sourceControl[property]?.isInherited !== serverSourceControl[property]?.isInherited;
    } else if (property === 'githubApp') {
      // Compare GitHub App installation IDs to detect when a new installation is completed
      const currentInstallationId = sourceControl[property]?.value?.installationId;
      const serverInstallationId = serverSourceControl[property]?.value?.installationId;
      const isInheritedChanged = sourceControl[property]?.isInherited !== serverSourceControl[property]?.isInherited;
      fieldIsDirty = currentInstallationId !== serverInstallationId || isInheritedChanged;
    } else {
      fieldIsDirty =
        sourceControl[property]?.rscValue?.trimmedValue !== serverSourceControl[property]?.rscValue?.trimmedValue ||
        sourceControl[property]?.value !== serverSourceControl[property]?.value ||
        sourceControl[property]?.isInherited !== serverSourceControl[property]?.isInherited;
    }

    return fieldIsDirty;
  });

  return isDirty;
};

export const setIsRepoUrlDirty = (state) => {
  const { sourceControl, serverSourceControl } = state;
  return sourceControl['repositoryUrl']?.value !== serverSourceControl['repositoryUrl']?.value;
};

export const getValidationMessage = (
  isDirty,
  validationError,
  sourceControl,
  isGithubAppAuthenticationEnabled = true
) => {
  if (!isDirty) {
    const hasGitHubApp = sourceControl?.githubApp?.value?.installationId;
    const isGitHubAppAuth = sourceControl?.authenticationType?.value === AUTHENTICATION_TYPES.GITHUB_APP;

    if (hasGitHubApp && isGitHubAppAuth && isGithubAppAuthenticationEnabled) {
      return 'GitHub App is already configured. No additional changes to save.';
    }
    return MSG_NO_CHANGES_TO_SAVE;
  }
  // For dirty forms, check validation errors (catches both user input errors and invalid backend states)
  if (validationError) {
    return validationError;
  }

  return null;
};
