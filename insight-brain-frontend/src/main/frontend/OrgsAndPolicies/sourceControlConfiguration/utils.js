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
export const BRANCH_INPUT_MAX_CHARACTERS = 243,
  USERNAME_INPUT_MAX_CHARACTERS = 255,
  TOKEN_INPUT_MAX_CHARACTERS = 512;
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
    pullRequestCommentingEnabled,
    remediationPullRequestsEnabled,
    sourceControlEvaluationsEnabled,
    commitStatusEnabled,
    statusChecksEnabled,
    sshEnabled,
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
  };

  //set username and token
  if (provider.parentName !== ROOT_ORG_NAME && token.parentName === ROOT_ORG_NAME) {
    // provider is inherited from a suborg but the token is at the root
    // so act as if token is not set
    sourceControlData.token = {
      rscValue: rscInitialState(token.value ?? '', () =>
        textFieldValidator(token.value ?? '', TOKEN_INPUT_MAX_CHARACTERS)
      ),
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
        token.value ? () => textFieldValidator(token.value ?? '', TOKEN_INPUT_MAX_CHARACTERS) : null
      ),
      isInherited: token.value === null && !isRootOrg,
      parentName: token.parentName,
      parentValue: rscInitialState(
        token.parentValue ?? '',
        token.parentValue ? () => textFieldValidator(token.parentValue ?? '', TOKEN_INPUT_MAX_CHARACTERS) : null
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
    statusChecksEnabled: true,
    repositoryUrl: isApp ? sourceControl.repositoryUrl.trimmedValue : null,
    sshEnabled: sshEnabled.value,
    username: null,
    token: null,
    provider: null,
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
  return submitData;
};

export const effectiveProvider = (sourceControl, serverSourceControl) => {
  if (!sourceControl) return;
  return sourceControl.provider.isInherited
    ? serverSourceControl.provider.parentValue.value
    : sourceControl.provider.rscValue.value;
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
  return isApp && !effectiveFieldInheritFrom(sourceControl, serverSourceControl, 'token');
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

export const getBaseBranchValueFromModel = (sourceControl, serverSourceControl, isRootOrg, isAutomationSupported) => {
  if (!isRootOrg || arePullRequestsSupported(sourceControl, serverSourceControl, isAutomationSupported)) {
    return sourceControl.baseBranch.rscValue.trimmedValue;
  }

  return serverSourceControl.baseBranch.rscValue.trimmedValue === ''
    ? 'main'
    : serverSourceControl.baseBranch.rscValue.trimmedValue;
};

export const getDataFromSourceControl = (
  ownerType,
  {
    provider,
    username,
    token,
    baseBranch,
    pullRequestCommentingEnabled,
    commitStatusEnabled,
    remediationPullRequestsEnabled,
    sourceControlEvaluationsEnabled,
    statusChecksEnabled,
    sshEnabled,
    repositoryUrl,
  }
) => {
  const data = {
    provider,
    username,
    token,
    baseBranch,
    remediationPullRequestsEnabled,
    statusChecksEnabled,
    pullRequestCommentingEnabled,
    commitStatusEnabled,
    sourceControlEvaluationsEnabled,
    sshEnabled,
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
    'pullRequestCommentingEnabled',
    'commitStatusEnabled',
    'remediationPullRequestsEnabled',
    'sourceControlEvaluationsEnabled',
    'sshEnabled',
    'repositoryUrl',
  ];
  return formFields.some((property) => {
    if (property === 'provider')
      return (
        sourceControl[property]?.rscValue?.value !== serverSourceControl[property]?.rscValue?.value ||
        sourceControl[property]?.isInherited !== serverSourceControl[property]?.isInherited
      );
    if (property === 'repositoryUrl') return sourceControl[property]?.value !== serverSourceControl[property]?.value;
    return (
      sourceControl[property]?.rscValue?.trimmedValue !== serverSourceControl[property]?.rscValue?.trimmedValue ||
      sourceControl[property]?.value !== serverSourceControl[property]?.value ||
      sourceControl[property]?.isInherited !== serverSourceControl[property]?.isInherited
    );
  });
};

export const setIsRepoUrlDirty = (state) => {
  const { sourceControl, serverSourceControl } = state;
  return sourceControl['repositoryUrl']?.value !== serverSourceControl['repositoryUrl']?.value;
};

export const getValidationMessage = (isDirty, validationError) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }
  return validationError;
};
