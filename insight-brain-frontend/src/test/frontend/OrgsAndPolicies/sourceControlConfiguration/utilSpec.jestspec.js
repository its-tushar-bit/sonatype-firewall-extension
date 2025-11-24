/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  arePullRequestsSupported,
  compositeSourceControlToModel,
  effectiveProvider,
  getBaseBranchValueFromModel,
  getClosePrOnFailedChecksEnabledFlagFromModel,
  getPullRequestCommentingEnabledFlagFromModel,
  getRemediationPullRequestsEnabledFlagFromModel,
  getSourceControlEvaluationsEnabledFlagFromModel,
  getValidationMessage,
  isAccessTokenRequiredOnNode,
  isUsernameRequiredOnNode,
  providerNeedsUsername,
  setDefaultIfNull,
  setIsDirty,
  setIsRepoUrlDirty,
  textFieldValidator,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';

describe('sourceControlConfiguration util', () => {
  describe('validationMessage', () => {
    const MSG_NO_CHANGES_TO_SAVE = 'There are no changes to save.';
    const GLOBAL_FORM_VALIDATION_ERROR = 'Unable to save: fields with invalid or missing data';

    it('shows MSG_NO_CHANGES_TO_SAVE message if isDirty value is false', () => {
      const isDirty = false;
      const validationError = null;
      expect(getValidationMessage(isDirty, validationError)).toBe(MSG_NO_CHANGES_TO_SAVE);
    });

    it('shows validationError message if isDirty value is true and we have validation error', () => {
      const isDirty = true;
      const validationError = GLOBAL_FORM_VALIDATION_ERROR;
      expect(getValidationMessage(isDirty, validationError)).toBe(GLOBAL_FORM_VALIDATION_ERROR);
    });

    it('returns null if form is dirty and we don"t have any validation errors', () => {
      const isDirty = true;
      const validationError = null;
      expect(getValidationMessage(isDirty, validationError)).toBe(null);
    });
  });

  describe('setIsDirty', () => {
    let state;
    beforeEach(() => {
      state = {
        sourceControl: {
          provider: {
            rscValue: {
              value: 'azure',
            },
          },
          username: {
            rscValue: {
              value: 'admin',
              trimmedValue: 'admin',
            },
          },
          token: {
            rscValue: {
              value: 'password1',
              trimmedValue: 'password1',
            },
          },
          baseBranch: {
            rscValue: {
              value: 'main',
              trimmedValue: 'main',
            },
          },
          sshEnabled: { value: false },
          remediationPullRequestsEnabled: { value: false },
          pullRequestCommentingEnabled: { value: true },
          sourceControlEvaluationsEnabled: { value: true },
        },
        serverSourceControl: {
          provider: {
            rscValue: {
              value: 'azure',
            },
          },
          username: {
            rscValue: {
              value: 'admin',
              trimmedValue: 'admin',
            },
          },
          token: {
            rscValue: {
              value: 'password1',
              trimmedValue: 'password1',
            },
          },
          baseBranch: {
            rscValue: {
              value: 'main',
              trimmedValue: 'main',
            },
          },
          sshEnabled: { value: false },
          remediationPullRequestsEnabled: { value: false },
          pullRequestCommentingEnabled: { value: true },
          sourceControlEvaluationsEnabled: { value: true },
        },
      };
    });
    it('returns false if no values were changed', () => {
      expect(setIsDirty(state)).toBe(false);
    });

    it('returns true if provider was changed', () => {
      state.sourceControl.provider.rscValue.value = 'github';
      expect(setIsDirty(state)).toBe(true);
    });

    it('returns true if at least one input value was changed', () => {
      state.sourceControl.username.rscValue.trimmedValue = 'username';
      expect(setIsDirty(state)).toBe(true);
    });

    it('returns true if at least one toggle value was changed', () => {
      state.sourceControl.sshEnabled.value = true;
      expect(setIsDirty(state)).toBe(true);
    });
  });

  describe('setIsRepoUrlDirty', () => {
    let state;
    beforeEach(() => {
      state = {
        sourceControl: {
          repositoryUrl: {
            value: 'test value',
          },
        },
        serverSourceControl: {
          repositoryUrl: {
            value: 'test value',
          },
        },
      };
    });
    it('returns true if repo url value was changed', () => {
      state.sourceControl.repositoryUrl.value = 'new value';
      expect(setIsRepoUrlDirty(state)).toBe(true);
    });

    it('returns false if repo url value was not changed', () => {
      expect(setIsRepoUrlDirty(state)).toBe(false);
    });
  });

  describe('textFieldValidator', () => {
    let state;
    const maxLength = 255;
    beforeEach(() => {
      state = {
        sourceControl: {
          username: {
            rscValue: {
              value: 'admin',
              trimmedValue: 'admin',
              isPristine: true,
              validationErrors: [],
            },
          },
        },
      };
    });

    it('returns no validation error if input has valid value', () => {
      expect(textFieldValidator(state.sourceControl.username.rscValue.trimmedValue, maxLength)).toEqual([]);
    });

    it('returns validationError if input has empty string', () => {
      state.sourceControl.username.rscValue.trimmedValue = '';
      expect(textFieldValidator(state.sourceControl.username.rscValue.trimmedValue, maxLength)).toEqual([
        'Must be non-empty',
      ]);
    });

    it('returns validationError if input has double spaces', () => {
      state.sourceControl.username.rscValue.trimmedValue = 'text  with  double space';
      expect(textFieldValidator(state.sourceControl.username.rscValue.trimmedValue, maxLength)).toEqual([
        'No leading, trailing or double spaces or tabs',
      ]);
    });

    it('returns validationError if input has double spaces', () => {
      state.sourceControl.username.rscValue.trimmedValue = 'a'.repeat(256);
      expect(textFieldValidator(state.sourceControl.username.rscValue.trimmedValue, maxLength)).toEqual([
        `Please enter less than ${maxLength} characters`,
      ]);
    });
  });

  describe('effectiveProvider', () => {
    let sourceControl, serverSourceControl;
    beforeEach(() => {
      sourceControl = {
        provider: { isInherited: true, rscValue: { value: 'gitlab' } },
      };
      serverSourceControl = {
        provider: { parentValue: { value: 'azure' } },
      };
    });
    it('returns undefined if there is no sourceControl', () => {
      expect(effectiveProvider(null, serverSourceControl)).toBe(undefined);
    });
    it('returns provider parentValue if isInherited value is true', () => {
      expect(effectiveProvider(sourceControl, serverSourceControl)).toBe('azure');
    });
    it('returns provider value if isInherited value is false', () => {
      sourceControl.provider.isInherited = false;
      expect(effectiveProvider(sourceControl, serverSourceControl)).toBe('gitlab');
    });
  });

  describe('setDefaultIfNull', () => {
    it('returns defaultValue if value and parentValue are null', () => {
      let value = null,
        parentValue = null,
        defaultValue = true;
      expect(setDefaultIfNull(value, parentValue, defaultValue)).toBe(defaultValue);
    });
    it('returns value if value is not null', () => {
      let value = true,
        parentValue = null,
        defaultValue = true;
      expect(setDefaultIfNull(value, parentValue, defaultValue)).toBe(defaultValue);
    });
    it('returns value if parentValue is not null', () => {
      let value = null,
        parentValue = true,
        defaultValue = false;
      expect(setDefaultIfNull(value, parentValue, defaultValue)).toBe(value);
    });
  });

  describe('providerNeedsUsername', () => {
    let sourceControl, serverSourceControl;
    beforeEach(() => {
      sourceControl = {
        provider: { isInherited: true, rscValue: { value: 'gitlab' } },
      };
      serverSourceControl = {
        provider: { parentValue: { value: 'azure' } },
      };
    });
    it('returns true if provider inherited, has parentValue which requires username', () => {
      expect(providerNeedsUsername(sourceControl, serverSourceControl)).toBe(true);
    });

    it("returns false if provider inherited, has parentValue which doesn't require username", () => {
      serverSourceControl.provider.parentValue = 'gitlab';
      expect(providerNeedsUsername(sourceControl, serverSourceControl)).toBe(false);
    });

    it("returns false if provider is not inherited, has value which doesn't require username", () => {
      sourceControl.provider.isInherited = false;
      expect(providerNeedsUsername(sourceControl, serverSourceControl)).toBe(false);
    });
    it('returns true if provider is not inherited, and has value which requires username', () => {
      sourceControl.provider.isInherited = false;
      sourceControl.provider.rscValue.value = 'azure';
      expect(providerNeedsUsername(sourceControl, serverSourceControl)).toBe(true);
    });
  });

  describe('isUsernameRequiredOnNode', () => {
    let sourceControl, serverSourceControl, isApp;
    beforeEach(() => {
      sourceControl = {
        provider: { isInherited: true, rscValue: { value: 'gitlab' } },
        username: { parentName: null },
      };
      serverSourceControl = {
        provider: { parentValue: { value: 'azure' } },
        username: { parentName: null },
      };
      isApp = true;
    });
    it('returns true for application with providerNeedsUserName returns true and without parent username', () => {
      expect(isUsernameRequiredOnNode(sourceControl, serverSourceControl, isApp)).toBe(true);
    });

    it('returns false for organizations', () => {
      isApp = false;
      expect(isUsernameRequiredOnNode(sourceControl, serverSourceControl, isApp)).toBe(false);
    });

    it('returns false if providerNeedsUserName returns false value', () => {
      serverSourceControl.username.parentName = 'some Organization';
      expect(isUsernameRequiredOnNode(sourceControl, serverSourceControl, isApp)).toBe(false);
    });

    it('returns false if has parentName', () => {
      sourceControl = {
        provider: { parentValue: { value: 'gitlab' }, rscValue: { value: 'gitlab' } },
      };
      expect(isUsernameRequiredOnNode(sourceControl, serverSourceControl, isApp)).toBe(false);
    });
  });

  describe('isAccessTokenRequiredOnNode', () => {
    let sourceControl, serverSourceControl, isApp;
    beforeEach(() => {
      sourceControl = {
        provider: { isInherited: false },
      };
      serverSourceControl = {
        token: { parentName: 'parentOrgName' },
      };
      isApp = true;
    });
    it('returns true for application  where effectiveTokenInheritFrom returns falsy value(null or undefined)', () => {
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp)).toBe(true);
    });
    it('returns false for non application entities(Root org, orgs)', () => {
      isApp = false;
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp)).toBe(false);
    });
    it('returns false if effectiveTokenInheritFrom returns token parentName', () => {
      sourceControl.provider.isInherited = true;
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp)).toBe(false);
    });
  });

  describe('arePullRequestsSupported', () => {
    let sourceControl, serverSourceControl, isAutomationSupported;
    beforeEach(() => {
      sourceControl = {
        provider: { isInherited: true, rscValue: { value: 'gitlab' } },
      };
      serverSourceControl = {
        provider: { parentValue: { value: 'azure' } },
      };
      isAutomationSupported = true;
    });
    it('returns true if isAutomationSupported is true and provider supports pull requests', () => {
      expect(arePullRequestsSupported(sourceControl, serverSourceControl, isAutomationSupported)).toBe(true);
    });
    it('returns true if isAutomationSupported is true and there is no effectiveProvider', () => {
      sourceControl = {
        provider: { isInherited: false, rscValue: { value: '' } },
      };
      expect(arePullRequestsSupported(sourceControl, serverSourceControl, isAutomationSupported)).toBe(true);
    });
    it('returns false if isAutomationSupported is false', () => {
      isAutomationSupported = false;
      expect(arePullRequestsSupported(sourceControl, serverSourceControl, isAutomationSupported)).toBe(false);
    });
  });

  describe('getBaseBranchValueFromModel', () => {
    let sourceControl, serverSourceControl, isRootOrg, isAutomationSupported;
    beforeEach(() => {
      sourceControl = {
        provider: { isInherited: true, rscValue: { value: 'gitlab' } },
        baseBranch: { rscValue: { trimmedValue: 'develop' } },
      };
      serverSourceControl = {
        provider: { parentValue: { value: 'azure' } },
        baseBranch: { rscValue: { trimmedValue: '' } },
      };
      isAutomationSupported = true;
      isRootOrg = true;
    });
    it('returns current baseBranch value if isAutomationSupported is true', () => {
      expect(getBaseBranchValueFromModel(sourceControl, serverSourceControl, isRootOrg, isAutomationSupported)).toBe(
        'develop'
      );
    });
    it('returns current baseBranch value if it"s not Root org level', () => {
      isRootOrg = false;
      expect(getBaseBranchValueFromModel(sourceControl, serverSourceControl, isRootOrg, isAutomationSupported)).toBe(
        'develop'
      );
    });
    it('returns "main" baseBranch value if it"s Root org level, pull requests are not supported and there were no baseBranch value from server', () => {
      isRootOrg = true;
      isAutomationSupported = false;
      expect(getBaseBranchValueFromModel(sourceControl, serverSourceControl, isRootOrg, isAutomationSupported)).toBe(
        'main'
      );
    });
    it('returns server baseBranch value if it"s Root org level, pull requests are not supported and there were some baseBranch value from server', () => {
      isRootOrg = true;
      isAutomationSupported = false;
      serverSourceControl = {
        provider: { parentValue: { value: 'azure' } },
        baseBranch: { rscValue: { trimmedValue: 'someBranchName' } },
      };
      expect(getBaseBranchValueFromModel(sourceControl, serverSourceControl, isRootOrg, isAutomationSupported)).toBe(
        'someBranchName'
      );
    });
  });

  describe('getPullRequestCommentingEnabledFlagFromModel', () => {
    let sourceControl, serverSourceControl, isRootOrg, isAutomationSupported;
    beforeEach(() => {
      sourceControl = {
        pullRequestCommentingEnabled: { value: false },
      };
      serverSourceControl = {
        pullRequestCommentingEnabled: { value: true },
      };
      isAutomationSupported = true;
      isRootOrg = true;
    });
    it('returns current pullRequestCommentingEnabled value if isPullRequestCommentingSupported is true', () => {
      expect(
        getPullRequestCommentingEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(false);
    });
    it('returns current pullRequestCommentingEnabled value if if it"s not Root org level', () => {
      isRootOrg = false;
      expect(
        getPullRequestCommentingEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(false);
    });
    it('returns "true" pullRequestCommentingEnabled value if it"s Root org level, pull requests are not supported and initial value from server equal to null', () => {
      isAutomationSupported = false;
      serverSourceControl = {
        pullRequestCommentingEnabled: { value: null },
      };

      expect(
        getPullRequestCommentingEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(true);
    });
    it('returns server pullRequestCommentingEnabled value if it"s Root org level, pull requests are not supported and initial value from server not equal to null', () => {
      isAutomationSupported = false;
      serverSourceControl = {
        pullRequestCommentingEnabled: { value: false },
      };

      expect(
        getPullRequestCommentingEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(false);
    });
  });

  describe('getSourceControlEvaluationsEnabledFlagFromModel', () => {
    let sourceControl, serverSourceControl, isRootOrg, isAutomationSupported;
    beforeEach(() => {
      sourceControl = {
        sourceControlEvaluationsEnabled: { value: false },
      };
      serverSourceControl = {
        sourceControlEvaluationsEnabled: { value: true },
      };
      isAutomationSupported = true;
      isRootOrg = true;
    });
    it('returns current sourceControlEvaluationsEnabled value if areSourceControlEvaluationsSupported is true', () => {
      expect(
        getSourceControlEvaluationsEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(sourceControl.sourceControlEvaluationsEnabled.value);
    });
    it('returns current sourceControlEvaluationsEnabled value if if it"s not Root org level', () => {
      isRootOrg = false;
      expect(
        getSourceControlEvaluationsEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(sourceControl.sourceControlEvaluationsEnabled.value);
    });
    it('returns "true" sourceControlEvaluationsEnabled value if it"s Root org level, source control evaluation is not supported and initial value from server equal to null', () => {
      isAutomationSupported = false;
      serverSourceControl = {
        sourceControlEvaluationsEnabled: { value: null },
      };

      expect(
        getSourceControlEvaluationsEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(true);
    });
    it('returns server sourceControlEvaluationsEnabled value if it"s Root org level, source control evaluation is not supported and initial value from server not equal to null', () => {
      isAutomationSupported = false;
      serverSourceControl = {
        sourceControlEvaluationsEnabled: { value: false },
      };

      expect(
        getSourceControlEvaluationsEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(serverSourceControl.sourceControlEvaluationsEnabled.value);
    });
  });

  describe('getRemediationPullRequestsEnabledFlagFromModel', () => {
    let sourceControl, serverSourceControl, isRootOrg, isAutomationSupported;
    beforeEach(() => {
      sourceControl = {
        provider: { isInherited: true, rscValue: { value: 'gitlab' } },
        remediationPullRequestsEnabled: { value: false },
      };
      serverSourceControl = {
        provider: { parentValue: { value: 'azure' } },
        remediationPullRequestsEnabled: { value: true },
      };
      isAutomationSupported = true;
      isRootOrg = true;
    });
    it('returns current sourceControlEvaluationsEnabled value if areSourceControlEvaluationsSupported is true', () => {
      expect(
        getRemediationPullRequestsEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(sourceControl.remediationPullRequestsEnabled.value);
    });
    it('returns current sourceControlEvaluationsEnabled value if it"s not Root org level', () => {
      isRootOrg = false;
      expect(
        getRemediationPullRequestsEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(sourceControl.remediationPullRequestsEnabled.value);
    });
    it('returns "true" remediationPullRequestsEnabled value if it"s Root org level, arePullRequestsSupported has falsy value and initial value from server equal to null', () => {
      isAutomationSupported = false;
      sourceControl = {
        provider: { isInherited: false, rscValue: { value: '' } },
      };
      serverSourceControl = {
        remediationPullRequestsEnabled: { value: null },
      };

      expect(
        getRemediationPullRequestsEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(true);
    });
    it('returns server remediationPullRequestsEnabled value if it"s Root org level, arePullRequestsSupported has falsy value and initial value from server not equal to null', () => {
      isAutomationSupported = false;
      sourceControl = {
        provider: { isInherited: false, rscValue: { value: '' } },
      };
      serverSourceControl = {
        remediationPullRequestsEnabled: { value: false },
      };

      expect(
        getRemediationPullRequestsEnabledFlagFromModel(
          sourceControl,
          serverSourceControl,
          isRootOrg,
          isAutomationSupported
        )
      ).toBe(serverSourceControl.remediationPullRequestsEnabled.value);
    });
  });

  describe('getClosePrOnFailedChecksEnabledFlagFromModel', () => {
    let sourceControl, serverSourceControl;
    beforeEach(() => {
      sourceControl = {
        provider: { isInherited: false, rscValue: { value: 'github' } },
        closePrOnFailedChecksEnabled: { value: true },
      };
      serverSourceControl = {
        provider: { parentValue: { value: 'gitlab' } },
      };
    });

    it('returns current closePrOnFailedChecksEnabled value for GitHub provider', () => {
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(true);
    });

    it('returns current closePrOnFailedChecksEnabled value for GitLab provider', () => {
      sourceControl.provider.rscValue.value = 'gitlab';
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(true);
    });

    it('returns false for closePrOnFailedChecksEnabled value when value is false for GitHub', () => {
      sourceControl.closePrOnFailedChecksEnabled.value = false;
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(false);
    });

    it('returns null for Bitbucket provider regardless of value', () => {
      sourceControl.provider.rscValue.value = 'bitbucket';
      sourceControl.closePrOnFailedChecksEnabled.value = true;
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(null);
    });

    it('returns null for Azure DevOps provider regardless of value', () => {
      sourceControl.provider.rscValue.value = 'azure';
      sourceControl.closePrOnFailedChecksEnabled.value = true;
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(null);
    });

    it('returns null when provider is not set', () => {
      sourceControl.provider.rscValue.value = '';
      sourceControl.closePrOnFailedChecksEnabled.value = true;
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(null);
    });

    it('returns false when value is null for GitHub provider', () => {
      sourceControl.closePrOnFailedChecksEnabled.value = null;
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(false);
    });

    it('uses inherited provider value when provider is inherited', () => {
      sourceControl.provider.isInherited = true;
      sourceControl.closePrOnFailedChecksEnabled.value = true;
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(true);
    });

    it('returns null when inherited provider is Azure DevOps', () => {
      sourceControl.provider.isInherited = true;
      serverSourceControl.provider.parentValue.value = 'azure';
      sourceControl.closePrOnFailedChecksEnabled.value = true;
      expect(getClosePrOnFailedChecksEnabledFlagFromModel(sourceControl, serverSourceControl)).toBe(null);
    });
  });

  describe('compositeSourceControlToModel', () => {
    let configResponse, isRootOrg;
    describe('server response without previous configuration', () => {
      beforeEach(() => {
        configResponse = {
          id: null,
          ownerId: 'ROOT_ORGANIZATION_ID',
          repositoryUrl: null,
          provider: { value: null, parentValue: null, parentName: null },
          username: { value: null, parentValue: null, parentName: null },
          token: { value: null, parentValue: null, parentName: null },
          baseBranch: { value: null, parentValue: null, parentName: null },
          remediationPullRequestsEnabled: { value: null, parentValue: null, parentName: null },
          statusChecksEnabled: { value: null, parentValue: null, parentName: null },
          commitStatusEnabled: { value: null, parentValue: null, parentName: null },
          pullRequestCommentingEnabled: { value: null, parentValue: null, parentName: null },
          sourceControlEvaluationsEnabled: { value: null, parentValue: null, parentName: null },
          sourceControlScanTarget: { value: null, parentValue: null, parentName: null },
          sshEnabled: { value: null, parentValue: null, parentName: null },
          manualPullRequestsEnabled: { value: null, parentValue: null, parentName: null },
          innerSourceAutomatedUpdatesEnabled: { value: null, parentValue: null, parentName: null },
          closePrOnFailedChecksEnabled: { value: null, parentValue: null, parentName: null },
          closePrAfterDaysOpenEnabled: { value: null, parentValue: null, parentName: null },
          closePrAfterDays: { value: null, parentValue: null, parentName: null },
        };
        isRootOrg = true;
      });
      it('returns object with default values if there were no previous configuration', () => {
        const resultDataForForm = {
          ownerId: 'ROOT_ORGANIZATION_ID',
          id: null,
          repositoryUrl: {
            value: '',
            isPristine: true,
            trimmedValue: '',
            validationErrors: ['Must be non-empty', 'A valid HTTP(S) repository clone URL is required'],
          },
          provider: {
            rscValue: { value: '', isPristine: true, validationErrors: 'Must be non-empty' },
            isInherited: false,
            parentValue: { value: '', isPristine: true, validationErrors: 'Must be non-empty' },
            parentName: null,
          },
          baseBranch: {
            rscValue: { isPristine: true, value: 'main', trimmedValue: 'main', validationErrors: null },
            isInherited: false,
            parentValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: ['Must be non-empty'] },
            parentName: null,
          },
          pullRequestCommentingEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          remediationPullRequestsEnabled: { value: false, parentValue: null, parentName: null, isInherited: false },
          sourceControlEvaluationsEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          sshEnabled: { value: null, parentValue: null, parentName: null, isInherited: false },
          commitStatusEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          manualPullRequestsEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          innerSourceAutomatedUpdatesEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          statusChecksEnabled: { value: null, parentValue: null, parentName: null },
          token: {
            rscValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
            isInherited: false,
            parentName: null,
            parentValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
          },
          username: {
            rscValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
            isInherited: false,
            parentName: null,
            parentValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
          },
          closePrOnFailedChecksEnabled: { parentValue: null, parentName: null, isInherited: false, value: true },
          closePrAfterDaysOpenEnabled: { parentValue: null, parentName: null, isInherited: false, value: false },
          closePrAfterDays: {
            isInherited: false,
            parentName: null,
            parentValue: {
              isPristine: true,
              trimmedValue: '',
              validationErrors: null,
              value: '',
            },
            rscValue: {
              isPristine: true,
              trimmedValue: '',
              validationErrors: null,
              value: '',
            },
          },
        };
        expect(compositeSourceControlToModel(configResponse, isRootOrg)).toEqual(resultDataForForm);
      });

      it('returns "main" baseBranch value for Root organization if no baseBranch value in response provided', () => {
        const sourceControl = compositeSourceControlToModel(configResponse, isRootOrg);
        expect(sourceControl.baseBranch.rscValue.value).toBe('main');
      });

      it('returns empty string value for provider "" on Root organization if no value in response provided', () => {
        const sourceControl = compositeSourceControlToModel(configResponse, isRootOrg);
        expect(sourceControl.provider.rscValue.value).toBe('');
        expect(sourceControl.provider.rscValue.validationErrors).toBe('Must be non-empty');
      });

      it('returns empty strings for username and token fields if there"re no values in response ', () => {
        const sourceControl = compositeSourceControlToModel(configResponse, isRootOrg);
        expect(sourceControl.username.rscValue.value).toBe('');
        expect(sourceControl.token.rscValue.value).toBe('');
      });

      it('returns default toggles values for Root organization if no values for toggles in response provided', () => {
        const sourceControl = compositeSourceControlToModel(configResponse, isRootOrg);
        expect(sourceControl.pullRequestCommentingEnabled.value).toBe(true);
        expect(sourceControl.remediationPullRequestsEnabled.value).toBe(false);
        expect(sourceControl.sourceControlEvaluationsEnabled.value).toBe(true);
        expect(sourceControl.sshEnabled.value).toBe(null);
      });
    });

    describe('server response with previous configuration', () => {
      let existConfigResponse;
      beforeEach(() => {
        existConfigResponse = {
          id: 'b47108b2c35e4781b91d8c3973961b65',
          ownerId: 'ROOT_ORGANIZATION_ID',
          repositoryUrl: null,
          provider: { value: 'azure', parentValue: null, parentName: null },
          username: { value: 'admin', parentValue: null, parentName: null },
          token: { value: '#~FAKE~SECRET~KEY~#', parentValue: null, parentName: null },
          baseBranch: { value: 'develop', parentValue: null, parentName: null },
          remediationPullRequestsEnabled: { value: false, parentValue: null, parentName: null },
          commitStatusEnabled: { value: null, parentValue: null, parentName: null },
          statusChecksEnabled: { value: true, parentValue: null, parentName: null },
          pullRequestCommentingEnabled: { value: true, parentValue: null, parentName: null },
          manualPullRequestsEnabled: { value: false, parentValue: null, parentName: null },
          sourceControlEvaluationsEnabled: { value: true, parentValue: null, parentName: null },
          sourceControlScanTarget: { value: null, parentValue: null, parentName: null },
          sshEnabled: { value: null, parentValue: null, parentName: null },
          innerSourceAutomatedUpdatesEnabled: { value: true, parentValue: null, parentName: null },
          closePrOnFailedChecksEnabled: { value: false, parentValue: null, parentName: null },
          closePrAfterDaysOpenEnabled: { value: false, parentValue: null, parentName: null },
          closePrAfterDays: { value: null, parentValue: null, parentName: null },
        };
        isRootOrg = true;
      });
      it('returns object with previous configuration values if present', () => {
        const resultDataForRorm = {
          ownerId: 'ROOT_ORGANIZATION_ID',
          id: 'b47108b2c35e4781b91d8c3973961b65',
          repositoryUrl: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: ['Must be non-empty', 'A valid HTTP(S) repository clone URL is required'],
          },
          provider: {
            rscValue: { value: 'azure', isPristine: true, validationErrors: null },
            isInherited: false,
            parentValue: { isPristine: true, value: '', validationErrors: 'Must be non-empty' },
            parentName: null,
          },
          baseBranch: {
            rscValue: { isPristine: true, value: 'develop', trimmedValue: 'develop', validationErrors: [] },
            isInherited: false,
            parentValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: ['Must be non-empty'] },
            parentName: null,
          },
          pullRequestCommentingEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          remediationPullRequestsEnabled: { value: false, parentValue: null, parentName: null, isInherited: false },
          commitStatusEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          manualPullRequestsEnabled: { value: false, parentValue: null, parentName: null, isInherited: false },
          sourceControlEvaluationsEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          sshEnabled: { value: null, parentValue: null, parentName: null, isInherited: false },
          statusChecksEnabled: { value: true, parentValue: null, parentName: null },
          innerSourceAutomatedUpdatesEnabled: { value: true, parentValue: null, parentName: null, isInherited: false },
          token: {
            rscValue: {
              isPristine: true,
              value: '#~FAKE~SECRET~KEY~#',
              trimmedValue: '#~FAKE~SECRET~KEY~#',
              validationErrors: [],
            },
            isInherited: false,
            parentName: null,
            parentValue: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
          },
          username: {
            rscValue: { isPristine: true, value: 'admin', trimmedValue: 'admin', validationErrors: [] },
            isInherited: false,
            parentName: null,
            parentValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
          },
          closePrOnFailedChecksEnabled: {
            parentValue: null,
            parentName: null,
            isInherited: false,
            value: false,
          },
          closePrAfterDaysOpenEnabled: {
            parentValue: null,
            parentName: null,
            isInherited: false,
            value: false,
          },
          closePrAfterDays: {
            isInherited: false,
            parentName: null,
            parentValue: {
              isPristine: true,
              trimmedValue: '',
              validationErrors: null,
              value: '',
            },
            rscValue: {
              isPristine: true,
              trimmedValue: '',
              validationErrors: null,
              value: '',
            },
          },
        };
        expect(compositeSourceControlToModel(existConfigResponse, isRootOrg)).toEqual(resultDataForRorm);
      });

      it('returns baseBranch value if baseBranch value in response provided', () => {
        const sourceControl = compositeSourceControlToModel(existConfigResponse, isRootOrg);
        expect(sourceControl.baseBranch.rscValue.value).toBe('develop');
      });

      it('returns provider value Root organization if provider value in response provided', () => {
        const sourceControl = compositeSourceControlToModel(existConfigResponse, isRootOrg);
        expect(sourceControl.provider.rscValue.value).toBe('azure');
        expect(sourceControl.provider.rscValue.validationErrors).toBe(null);
      });

      it('returns username and token values if there"re provided in server response', () => {
        const sourceControl = compositeSourceControlToModel(existConfigResponse, isRootOrg);
        expect(sourceControl.username.rscValue.value).toBe('admin');
        expect(sourceControl.token.rscValue.value).toBe('#~FAKE~SECRET~KEY~#');
      });

      it('returns toggle values which provided in response', () => {
        const sourceControl = compositeSourceControlToModel(existConfigResponse, isRootOrg);
        expect(sourceControl.pullRequestCommentingEnabled.value).toBe(true);
        expect(sourceControl.remediationPullRequestsEnabled.value).toBe(false);
        expect(sourceControl.sourceControlEvaluationsEnabled.value).toBe(true);
        expect(sourceControl.sshEnabled.value).toBe(null);
      });
    });
  });
});
