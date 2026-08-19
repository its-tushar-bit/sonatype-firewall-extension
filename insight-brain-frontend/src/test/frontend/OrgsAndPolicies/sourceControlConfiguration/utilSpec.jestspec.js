/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  arePullRequestsSupported,
  AUTHENTICATION_TYPES,
  compositeSourceControlToModel,
  effectiveAuthenticationType,
  effectiveProvider,
  getBaseBranchValueFromModel,
  getCleanAccountName,
  getClosePrOnFailedChecksEnabledFlagFromModel,
  getCompositeGitHubAppState,
  getDataFromSourceControl,
  getGitHubAppInstallationUrl,
  getGitHubAppIdentifier,
  getPullRequestCommentingEnabledFlagFromModel,
  getGitHubAppReturnParam,
  getRemediationPullRequestsEnabledFlagFromModel,
  getScmFormStateStorageKey,
  getSourceControlEvaluationsEnabledFlagFromModel,
  getValidationMessage,
  isAccessTokenRequiredOnNode,
  isPersonalAccount,
  isUsernameRequiredOnNode,
  loadFormStateWithFallback,
  providerNeedsUsername,
  prepareSubmitData,
  removeFormStateWithFallback,
  saveFormStateWithFallback,
  selectCommittedGitHubAppInfo,
  setDefaultIfNull,
  setIsDirty,
  setIsRepoUrlDirty,
  shouldShowGitHubAppAuth,
  textFieldValidator,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';
import { PERSONAL_ACCOUNT_MARKER } from 'MainRoot/OrgsAndPolicies/utility/constants';

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

    it('shows GitHub App specific message when GitHub App is configured and form is not dirty', () => {
      const isDirty = false;
      const validationError = null;
      const sourceControl = {
        githubApps: {
          value: {
            installationId: 12345,
          },
        },
        authenticationType: {
          value: 'GITHUB_APP',
        },
      };

      const message = getValidationMessage(isDirty, validationError, sourceControl);

      expect(message).toBe('GitHub App is already configured. No additional changes to save.');
    });

    it('shows MSG_NO_CHANGES_TO_SAVE when GitHub App exists but auth type is PAT', () => {
      const isDirty = false;
      const validationError = null;
      const sourceControl = {
        githubApps: {
          value: {
            installationId: 12345,
          },
        },
        authenticationType: {
          value: 'PAT',
        },
      };

      const message = getValidationMessage(isDirty, validationError, sourceControl);

      expect(message).toBe(MSG_NO_CHANGES_TO_SAVE);
    });

    it('shows MSG_NO_CHANGES_TO_SAVE when auth type is GitHub App but no installation exists', () => {
      const isDirty = false;
      const validationError = null;
      const sourceControl = {
        githubApps: {
          value: null,
        },
        authenticationType: {
          value: 'GITHUB_APP',
        },
      };

      const message = getValidationMessage(isDirty, validationError, sourceControl);

      expect(message).toBe(MSG_NO_CHANGES_TO_SAVE);
    });

    it('shows MSG_NO_CHANGES_TO_SAVE when sourceControl parameter is not provided', () => {
      const isDirty = false;
      const validationError = null;

      const message = getValidationMessage(isDirty, validationError);

      expect(message).toBe(MSG_NO_CHANGES_TO_SAVE);
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
          authenticationType: {
            value: 'PAT',
            isInherited: false,
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
          authenticationType: {
            value: 'PAT',
            isInherited: false,
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

    it('returns true if authenticationType value was changed', () => {
      state.sourceControl.authenticationType.value = 'GITHUB_APP';
      expect(setIsDirty(state)).toBe(true);
    });

    it('returns true if authenticationType isInherited was changed', () => {
      state.sourceControl.authenticationType.isInherited = true;
      expect(setIsDirty(state)).toBe(true);
    });

    it('returns true when the selected GitHub App id changes even if installation id stays the same', () => {
      state.sourceControl.githubApps = {
        value: {
          id: 'github-app-2',
          installationId: '12345',
        },
        isInherited: false,
      };
      state.serverSourceControl.githubApps = {
        value: {
          id: 'github-app-1',
          installationId: '12345',
        },
        isInherited: false,
      };

      expect(setIsDirty(state)).toBe(true);
    });
  });

  describe('GitHub App selection helpers', () => {
    it('uses only the active GitHub App for committed selection from list-based payloads', () => {
      expect(
        selectCommittedGitHubAppInfo([
          { id: 'github-app-1', installationId: '111', isActive: false, name: 'Inactive App' },
          { id: 'github-app-2', installationId: '222', isActive: true, name: 'Active App' },
        ])
      ).toEqual({
        id: 'github-app-2',
        installationId: '222',
        isActive: true,
        name: 'Active App',
      });
    });

    it('does not pick an inactive GitHub App from list-based payloads when there is no active install', () => {
      expect(
        selectCommittedGitHubAppInfo([
          { id: 'github-app-1', installationId: '111', isActive: false, name: 'Inactive App' },
        ])
      ).toBeNull();
    });

    it('does not treat a GitHub App payload without isActive metadata as committed', () => {
      expect(
        selectCommittedGitHubAppInfo({
          id: 'legacy-github-app',
          installationId: '111',
          name: 'Legacy App',
        })
      ).toBeNull();
    });

    it('treats blank GitHub App ids as unconfigured', () => {
      expect(getGitHubAppIdentifier({ id: '', installationId: '' })).toBeNull();
    });

    it('reads githubAppId from router params first and falls back to the URL search or hash string', () => {
      expect(getGitHubAppReturnParam({ githubAppId: 'router-github-app' }, '?githubAppId=url-github-app', '')).toBe(
        'router-github-app'
      );
      expect(getGitHubAppReturnParam({}, '?githubAppId=url-github-app', '')).toBe('url-github-app');
      expect(
        getGitHubAppReturnParam(
          {},
          '',
          '#/management/edit/organization/test/source-control?githubAppId=hash-github-app'
        )
      ).toBe('hash-github-app');
      expect(getGitHubAppReturnParam({}, '', '#/management/edit/organization/test/source-control')).toBeNull();
    });

    it('normalizes the plural githubApps backend payload and preserves isActive for selection', () => {
      expect(
        getCompositeGitHubAppState({
          githubApps: [
            {
              value: {
                id: 'github-app-1',
                installationId: 111,
                isActive: true,
                name: 'Active App',
              },
              parentValue: {
                id: 'parent-github-app',
                installationId: 999,
                isActive: true,
                name: 'Parent App',
              },
              parentName: 'Root Organization',
            },
            {
              value: {
                id: 'github-app-2',
                installationId: 222,
                isActive: false,
                name: 'Returned App',
              },
              parentValue: {
                id: 'parent-github-app',
                installationId: 999,
                isActive: true,
                name: 'Parent App',
              },
              parentName: 'Root Organization',
            },
          ],
        })
      ).toEqual({
        value: [
          {
            id: 'github-app-1',
            installationId: 111,
            isActive: true,
            name: 'Active App',
          },
          {
            id: 'github-app-2',
            installationId: 222,
            isActive: false,
            name: 'Returned App',
          },
        ],
        localCount: 2,
        parentValue: {
          id: 'parent-github-app',
          installationId: 999,
          isActive: true,
          name: 'Parent App',
        },
        parentCount: 2,
        parentName: 'Root Organization',
      });
    });

    it('counts parent apps separately from local apps when entries are split', () => {
      expect(
        getCompositeGitHubAppState({
          githubApps: [
            {
              value: {
                id: 'local-app-1',
                installationId: 111,
                isActive: true,
                name: 'Local App',
              },
              parentValue: null,
              parentName: null,
            },
            {
              value: null,
              parentValue: {
                id: 'parent-app-1',
                installationId: 901,
                isActive: true,
                name: 'Parent App 1',
              },
              parentName: 'Root Organization',
            },
            {
              value: null,
              parentValue: {
                id: 'parent-app-2',
                installationId: 902,
                isActive: true,
                name: 'Parent App 2',
              },
              parentName: 'Root Organization',
            },
            {
              value: null,
              parentValue: {
                id: 'parent-app-3',
                installationId: 903,
                isActive: true,
                name: 'Parent App 3',
              },
              parentName: 'Root Organization',
            },
          ],
        })
      ).toEqual({
        value: {
          id: 'local-app-1',
          installationId: 111,
          isActive: true,
          name: 'Local App',
        },
        localCount: 1,
        parentValue: {
          id: 'parent-app-1',
          installationId: 901,
          isActive: true,
          name: 'Parent App 1',
        },
        parentCount: 3,
        parentName: 'Root Organization',
      });
    });
  });

  describe('GitHub App submit payload', () => {
    const baseSourceControl = {
      ownerId: 'owner-1',
      id: null,
      provider: {
        rscValue: { value: 'github' },
        parentValue: { value: '' },
        isInherited: false,
      },
      token: {
        isInherited: false,
        rscValue: { trimmedValue: '', value: '' },
      },
      username: {
        isInherited: false,
        rscValue: { trimmedValue: '', value: '' },
      },
      baseBranch: {
        isInherited: false,
        rscValue: { trimmedValue: 'main', value: 'main' },
      },
      authenticationType: {
        value: AUTHENTICATION_TYPES.GITHUB_APP,
        isInherited: false,
      },
      githubApps: {
        value: {
          id: 'github-app-2',
          installationId: '222',
          name: 'Returned App',
        },
        isInherited: false,
      },
      pullRequestCommentingEnabled: { value: true },
      commitStatusEnabled: { value: true },
      remediationPullRequestsEnabled: { value: false },
      sourceControlEvaluationsEnabled: { value: true },
      manualPullRequestsEnabled: { value: true },
      innerSourceAutomatedUpdatesEnabled: { value: true },
      sshEnabled: { value: null },
      closePrOnFailedChecksEnabled: { value: true },
      closePrAfterDaysOpenEnabled: { value: false },
      closePrAfterDays: {
        rscValue: { trimmedValue: '', value: '' },
      },
      repositoryUrl: { trimmedValue: '', value: '' },
    };

    const baseServerSourceControl = {
      provider: { parentValue: { value: '' } },
      baseBranch: { rscValue: { trimmedValue: '' } },
      pullRequestCommentingEnabled: { value: true },
      commitStatusEnabled: { value: true },
      remediationPullRequestsEnabled: { value: false },
      sourceControlEvaluationsEnabled: { value: true },
      manualPullRequestsEnabled: { value: true },
      innerSourceAutomatedUpdatesEnabled: { value: true },
      closePrOnFailedChecksEnabled: { value: true },
    };

    it('does not include githubAppId in submit data', () => {
      const submitData = prepareSubmitData(baseSourceControl, baseServerSourceControl, false, true, true, true);

      expect(submitData.githubAppId).toBeUndefined();
      expect(getDataFromSourceControl('organization', submitData)).not.toHaveProperty('githubAppId');
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

  describe('effectiveAuthenticationType', () => {
    it('returns null when authenticationType is missing', () => {
      expect(effectiveAuthenticationType({})).toBeNull();
    });

    it('returns local authentication type when not inherited', () => {
      const sourceControl = {
        authenticationType: {
          value: AUTHENTICATION_TYPES.PAT,
          isInherited: false,
          parentValue: AUTHENTICATION_TYPES.GITHUB_APP,
        },
      };

      expect(effectiveAuthenticationType(sourceControl)).toBe(AUTHENTICATION_TYPES.PAT);
    });

    it('returns parent authentication type when inherited', () => {
      const sourceControl = {
        authenticationType: {
          value: null,
          isInherited: true,
          parentValue: AUTHENTICATION_TYPES.GITHUB_APP,
        },
      };

      expect(effectiveAuthenticationType(sourceControl)).toBe(AUTHENTICATION_TYPES.GITHUB_APP);
    });

    it('returns null when inherited auth is not configured', () => {
      const sourceControl = {
        authenticationType: {
          value: null,
          isInherited: true,
          parentValue: null,
        },
      };

      expect(effectiveAuthenticationType(sourceControl)).toBeNull();
    });
  });

  describe('setDefaultIfNull', () => {
    describe('at root org (isRootOrg = true)', () => {
      it('returns defaultValue when both value and parentValue are null', () => {
        expect(setDefaultIfNull(null, null, true, true)).toBe(true);
      });
      it('returns value when value is not null', () => {
        expect(setDefaultIfNull(true, null, false, true)).toBe(true);
      });
      it('returns null when value is null but parentValue is not null', () => {
        expect(setDefaultIfNull(null, true, false, true)).toBe(null);
      });
    });

    describe('at sub-org / application (isRootOrg = false) — CLM-32426', () => {
      it('returns null when value is null and parentValue is null (preserves "Inherit")', () => {
        expect(setDefaultIfNull(null, null, true, false)).toBe(null);
      });
      it('returns null when value is null and parentValue has a value (preserves "Inherit")', () => {
        expect(setDefaultIfNull(null, true, false, false)).toBe(null);
      });
      it('returns value when value is not null (user override)', () => {
        expect(setDefaultIfNull(false, null, true, false)).toBe(false);
      });
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
        token: {},
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
      sourceControl.provider.parentValue = { value: 'github' };
      sourceControl.token.isInherited = true;
      sourceControl.token.parentValue = { value: '#~FAKE~SECRET~KEY~#' };
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp)).toBe(false);
    });

    it('returns true when overriding auth method and parent uses GitHub App (feature enabled)', () => {
      sourceControl.provider.rscValue = { value: 'github' };
      sourceControl.token.isInherited = false;
      sourceControl.token.parentValue = { value: '#~FAKE~SECRET~KEY~#' };
      sourceControl.token.rscValue = { value: null, trimmedValue: null };
      sourceControl.authenticationType = { parentValue: AUTHENTICATION_TYPES.GITHUB_APP };
      serverSourceControl.token = { value: null };
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp, true)).toBe(true);
    });

    it('returns true when overriding provider and parent uses PAT (feature enabled)', () => {
      sourceControl.provider.rscValue = { value: 'github' };
      sourceControl.token.isInherited = false;
      sourceControl.token.parentValue = { value: '#~FAKE~SECRET~KEY~#' };
      sourceControl.token.rscValue = { value: null, trimmedValue: null };
      sourceControl.authenticationType = { parentValue: AUTHENTICATION_TYPES.PAT };
      serverSourceControl.token = { value: null };
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp, true)).toBe(true);
    });

    it('returns true when overriding provider and parent auth type is undefined (feature enabled)', () => {
      sourceControl.provider.rscValue = { value: 'github' };
      sourceControl.token.isInherited = false;
      sourceControl.token.parentValue = { value: '#~FAKE~SECRET~KEY~#' };
      sourceControl.token.rscValue = { value: null, trimmedValue: null };
      sourceControl.authenticationType = { parentValue: undefined };
      serverSourceControl.token = { value: null };
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp, true)).toBe(true);
    });

    it('returns true when overriding provider and parent auth type is null (feature enabled)', () => {
      sourceControl.provider.rscValue = { value: 'github' };
      sourceControl.token.isInherited = false;
      sourceControl.token.parentValue = { value: '#~FAKE~SECRET~KEY~#' };
      sourceControl.token.rscValue = { value: null, trimmedValue: null };
      sourceControl.authenticationType = { parentValue: null };
      serverSourceControl.token = { value: null };
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp, true)).toBe(true);
    });

    it('returns true when overriding provider and feature is disabled', () => {
      sourceControl.provider.rscValue = { value: 'github' };
      sourceControl.token.isInherited = false;
      sourceControl.token.parentValue = { value: '#~FAKE~SECRET~KEY~#' };
      sourceControl.token.rscValue = { value: null, trimmedValue: null };
      sourceControl.authenticationType = { parentValue: AUTHENTICATION_TYPES.GITHUB_APP };
      serverSourceControl.token = { value: null };
      expect(isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp, false)).toBe(true);
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
          authenticationType: { value: null, parentValue: null, parentName: null },
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
          authenticationType: {
            value: null,
            isInherited: false,
            parentValue: null,
            parentName: null,
            rscValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
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
          githubApps: {
            value: null,
            localCount: 0,
            parentCount: 0,
            parentValue: null,
            parentName: undefined,
            isInherited: false,
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

      it('applies toggle defaults at the Root organization when no values are configured (mirrors CLM-32426 sub-org test)', () => {
        const sourceControl = compositeSourceControlToModel(configResponse, isRootOrg);

        expect(sourceControl.pullRequestCommentingEnabled.value).toBe(true);
        expect(sourceControl.pullRequestCommentingEnabled.isInherited).toBe(false);
        expect(sourceControl.remediationPullRequestsEnabled.value).toBe(false);
        expect(sourceControl.remediationPullRequestsEnabled.isInherited).toBe(false);
        expect(sourceControl.sourceControlEvaluationsEnabled.value).toBe(true);
        expect(sourceControl.sourceControlEvaluationsEnabled.isInherited).toBe(false);
        expect(sourceControl.commitStatusEnabled.value).toBe(true);
        expect(sourceControl.commitStatusEnabled.isInherited).toBe(false);
        expect(sourceControl.manualPullRequestsEnabled.value).toBe(true);
        expect(sourceControl.manualPullRequestsEnabled.isInherited).toBe(false);
        expect(sourceControl.innerSourceAutomatedUpdatesEnabled.value).toBe(true);
        expect(sourceControl.innerSourceAutomatedUpdatesEnabled.isInherited).toBe(false);
        expect(sourceControl.sshEnabled.value).toBeNull();
        expect(sourceControl.sshEnabled.isInherited).toBe(false);
        expect(sourceControl.closePrOnFailedChecksEnabled.value).toBe(true);
        expect(sourceControl.closePrOnFailedChecksEnabled.isInherited).toBe(false);
        expect(sourceControl.closePrAfterDaysOpenEnabled.value).toBe(false);
        expect(sourceControl.closePrAfterDaysOpenEnabled.isInherited).toBe(false);
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
          authenticationType: { value: null, parentValue: null, parentName: null },
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
          authenticationType: {
            value: null,
            isInherited: false,
            parentValue: null,
            parentName: null,
            rscValue: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
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
          githubApps: {
            value: null,
            localCount: 0,
            parentCount: 0,
            parentValue: null,
            parentName: undefined,
            isInherited: false,
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

      it('maps the plural githubApps backend payload into the existing githubApps model', () => {
        const sourceControl = compositeSourceControlToModel(
          {
            ...existConfigResponse,
            githubApps: [
              {
                value: {
                  id: 'github-app-1',
                  installationId: 111,
                  name: 'Active App',
                  accountName: 'active-org',
                  isActive: true,
                },
                parentValue: {
                  id: 'parent-github-app',
                  installationId: 999,
                  name: 'Parent App',
                  accountName: 'parent-org',
                  isActive: true,
                },
                parentName: 'Root Organization',
              },
              {
                value: {
                  id: 'github-app-2',
                  installationId: 222,
                  name: 'Returned App',
                  accountName: 'returned-org',
                  isActive: false,
                },
                parentValue: {
                  id: 'parent-github-app',
                  installationId: 999,
                  name: 'Parent App',
                  accountName: 'parent-org',
                  isActive: true,
                },
                parentName: 'Root Organization',
              },
            ],
          },
          isRootOrg
        );

        expect(sourceControl.githubApps.value).toMatchObject({
          id: 'github-app-1',
          installationId: 111,
          isActive: true,
        });
        expect(sourceControl.githubApps.parentValue).toMatchObject({
          id: 'parent-github-app',
          installationId: 999,
          isActive: true,
        });
        expect(sourceControl.githubApps.parentName).toBe('Root Organization');
      });

      it('does not map an inactive local GitHub App from list-based payloads without an active selection', () => {
        const sourceControl = compositeSourceControlToModel(
          {
            ...existConfigResponse,
            githubApp: {
              value: [
                {
                  id: 'inactive-github-app',
                  installationId: 111,
                  name: 'Inactive App',
                  accountName: 'child-org',
                  isActive: false,
                },
              ],
              parentValue: {
                id: 'parent-github-app',
                installationId: 999,
                name: 'Parent App',
                accountName: 'parent-org',
                isActive: true,
              },
              parentName: 'Root Organization',
            },
          },
          isRootOrg
        );

        expect(sourceControl.githubApps.value).toBeNull();
        expect(sourceControl.githubApps.parentValue).toMatchObject({
          id: 'parent-github-app',
          installationId: 999,
          isActive: true,
        });
      });

      it('does not map a legacy local GitHub App without isActive when there is no persisted org/app SCM config', () => {
        const sourceControl = compositeSourceControlToModel(
          {
            ...existConfigResponse,
            id: null,
            githubApp: {
              value: {
                id: 'inactive-github-app',
                installationId: 111,
                name: 'Inactive App',
                accountName: 'child-org',
              },
              parentValue: {
                id: 'parent-github-app',
                installationId: 999,
                name: 'Parent App',
                accountName: 'parent-org',
                isActive: true,
              },
              parentName: 'Root Organization',
            },
          },
          false
        );

        expect(sourceControl.githubApps.value).toBeNull();
        expect(sourceControl.githubApps.parentValue).toMatchObject({
          id: 'parent-github-app',
          installationId: 999,
          isActive: true,
        });
      });
    });

    describe('sub-org with all toggles set to inherit (CLM-32426)', () => {
      it('preserves null toggle values so the UI shows "Inherit" instead of falling back to defaults', () => {
        const subOrgConfigResponse = {
          id: 'sub-org-id',
          ownerId: 'SUB_ORGANIZATION_ID',
          repositoryUrl: null,
          provider: { value: 'github', parentValue: null, parentName: null },
          username: { value: null, parentValue: null, parentName: null },
          token: { value: 'someToken', parentValue: null, parentName: null },
          baseBranch: { value: null, parentValue: null, parentName: null },
          authenticationType: { value: null, parentValue: null, parentName: null },
          pullRequestCommentingEnabled: { value: null, parentValue: null, parentName: null },
          remediationPullRequestsEnabled: { value: null, parentValue: null, parentName: null },
          sourceControlEvaluationsEnabled: { value: null, parentValue: null, parentName: null },
          commitStatusEnabled: { value: null, parentValue: null, parentName: null },
          statusChecksEnabled: { value: null, parentValue: null, parentName: null },
          sshEnabled: { value: null, parentValue: null, parentName: null },
          manualPullRequestsEnabled: { value: null, parentValue: null, parentName: null },
          innerSourceAutomatedUpdatesEnabled: { value: null, parentValue: null, parentName: null },
          closePrOnFailedChecksEnabled: { value: null, parentValue: null, parentName: null },
          closePrAfterDaysOpenEnabled: { value: null, parentValue: null, parentName: null },
          closePrAfterDays: { value: null, parentValue: null, parentName: null },
        };
        const sourceControl = compositeSourceControlToModel(subOrgConfigResponse, false);

        expect(sourceControl.pullRequestCommentingEnabled.value).toBeNull();
        expect(sourceControl.pullRequestCommentingEnabled.isInherited).toBe(true);
        expect(sourceControl.remediationPullRequestsEnabled.value).toBeNull();
        expect(sourceControl.remediationPullRequestsEnabled.isInherited).toBe(true);
        expect(sourceControl.sourceControlEvaluationsEnabled.value).toBeNull();
        expect(sourceControl.sourceControlEvaluationsEnabled.isInherited).toBe(true);
        expect(sourceControl.commitStatusEnabled.value).toBeNull();
        expect(sourceControl.commitStatusEnabled.isInherited).toBe(true);
        expect(sourceControl.manualPullRequestsEnabled.value).toBeNull();
        expect(sourceControl.manualPullRequestsEnabled.isInherited).toBe(true);
        expect(sourceControl.innerSourceAutomatedUpdatesEnabled.value).toBeNull();
        expect(sourceControl.innerSourceAutomatedUpdatesEnabled.isInherited).toBe(true);
        expect(sourceControl.sshEnabled.value).toBeNull();
        expect(sourceControl.sshEnabled.isInherited).toBe(true);
        expect(sourceControl.closePrOnFailedChecksEnabled.value).toBeNull();
        expect(sourceControl.closePrOnFailedChecksEnabled.isInherited).toBe(true);
        expect(sourceControl.closePrAfterDaysOpenEnabled.value).toBeNull();
        expect(sourceControl.closePrAfterDaysOpenEnabled.isInherited).toBe(true);
      });
    });

    describe('sub-org with all toggles inheriting a configured root parent (CLM-32426)', () => {
      it('preserves null value while exposing parentValue/parentName so the UI shows "Inherit from <parent> (<state>)"', () => {
        const parentName = 'Root Organization';
        const subOrgConfigResponse = {
          id: 'sub-org-id',
          ownerId: 'SUB_ORGANIZATION_ID',
          repositoryUrl: null,
          provider: { value: null, parentValue: 'github', parentName },
          username: { value: null, parentValue: null, parentName },
          token: { value: null, parentValue: '#~FAKE~SECRET~KEY~#', parentName },
          baseBranch: { value: null, parentValue: 'main', parentName },
          authenticationType: { value: null, parentValue: null, parentName },
          pullRequestCommentingEnabled: { value: null, parentValue: false, parentName },
          remediationPullRequestsEnabled: { value: null, parentValue: true, parentName },
          sourceControlEvaluationsEnabled: { value: null, parentValue: false, parentName },
          commitStatusEnabled: { value: null, parentValue: false, parentName },
          statusChecksEnabled: { value: null, parentValue: true, parentName },
          sshEnabled: { value: null, parentValue: true, parentName },
          manualPullRequestsEnabled: { value: null, parentValue: false, parentName },
          innerSourceAutomatedUpdatesEnabled: { value: null, parentValue: false, parentName },
          closePrOnFailedChecksEnabled: { value: null, parentValue: true, parentName },
          closePrAfterDaysOpenEnabled: { value: null, parentValue: false, parentName },
          closePrAfterDays: { value: null, parentValue: null, parentName },
        };
        const sourceControl = compositeSourceControlToModel(subOrgConfigResponse, false);

        const assertInheritsFromParent = (field, expectedParentValue) => {
          expect(sourceControl[field].value).toBeNull();
          expect(sourceControl[field].isInherited).toBe(true);
          expect(sourceControl[field].parentValue).toBe(expectedParentValue);
          expect(sourceControl[field].parentName).toBe(parentName);
        };

        assertInheritsFromParent('pullRequestCommentingEnabled', false);
        assertInheritsFromParent('remediationPullRequestsEnabled', true);
        assertInheritsFromParent('sourceControlEvaluationsEnabled', false);
        assertInheritsFromParent('commitStatusEnabled', false);
        assertInheritsFromParent('manualPullRequestsEnabled', false);
        assertInheritsFromParent('innerSourceAutomatedUpdatesEnabled', false);
        assertInheritsFromParent('sshEnabled', true);
        assertInheritsFromParent('closePrOnFailedChecksEnabled', true);
        assertInheritsFromParent('closePrAfterDaysOpenEnabled', false);
      });
    });
  });

  describe('shouldShowGitHubAppAuth', () => {
    let sourceControl, serverSourceControl;

    beforeEach(() => {
      // Default setup: GitHub provider, not inherited
      sourceControl = {
        provider: {
          isInherited: false,
          rscValue: { value: 'github' },
        },
        token: {
          isInherited: false,
        },
      };
      serverSourceControl = {
        provider: {
          parentValue: { value: 'github' },
        },
      };
    });

    describe('when all conditions are met', () => {
      it('returns true when provider is GitHub and provider is not inherited', () => {
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });

      it('returns true when provider is inherited and is GitHub (even if token is overridden)', () => {
        sourceControl.provider.isInherited = true;
        sourceControl.token.isInherited = false;
        serverSourceControl.provider.parentValue.value = 'github';
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });

      it('returns true when provider is GitHub and both provider and token are not inherited', () => {
        sourceControl.provider.isInherited = false;
        sourceControl.token.isInherited = false;
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });
    });

    describe('when provider is not GitHub', () => {
      it('returns false when provider is GitLab', () => {
        sourceControl.provider.rscValue.value = 'gitlab';
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          false
        );
      });

      it('returns false when provider is Azure DevOps', () => {
        sourceControl.provider.rscValue.value = 'azure';
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          false
        );
      });

      it('returns false when provider is Bitbucket', () => {
        sourceControl.provider.rscValue.value = 'bitbucket';
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          false
        );
      });

      it('returns false when provider is empty string', () => {
        sourceControl.provider.rscValue.value = '';
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          false
        );
      });
    });

    describe('when both provider and token are inherited', () => {
      it('returns true when both provider and token are inherited and provider is GitHub', () => {
        sourceControl.provider.isInherited = true;
        sourceControl.token.isInherited = true;
        serverSourceControl.provider.parentValue.value = 'github';
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });
    });

    describe('when provider is inherited from parent', () => {
      beforeEach(() => {
        sourceControl.provider.isInherited = true;
      });

      it('returns true when inherited provider is GitHub (even with token overridden)', () => {
        serverSourceControl.provider.parentValue.value = 'github';
        sourceControl.token.isInherited = false;
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });

      it('returns false when inherited provider is not GitHub', () => {
        serverSourceControl.provider.parentValue.value = 'gitlab';
        sourceControl.token.isInherited = false;
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          false
        );
      });
    });

    describe('edge cases', () => {
      it('returns false when sourceControl is null', () => {
        expect(shouldShowGitHubAppAuth(null, serverSourceControl)).toBe(false);
      });

      it('returns false when sourceControl is undefined', () => {
        expect(shouldShowGitHubAppAuth(undefined, serverSourceControl)).toBe(false);
      });

      it('returns false when sourceControl.provider is undefined (effectiveProvider returns undefined)', () => {
        sourceControl.provider = undefined;
        // When provider is undefined, effectiveProvider gracefully returns undefined (with optional chaining)
        // shouldShowGitHubAppAuth handles undefined provider by returning false
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          false
        );
      });

      it('returns true when sourceControl.token is undefined (only checks provider)', () => {
        sourceControl.token = undefined;
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });

      it('handles optional chaining correctly when provider.isInherited is undefined', () => {
        delete sourceControl.provider.isInherited;
        sourceControl.token.isInherited = false;
        // When isInherited is undefined, !sourceControl?.provider.isInherited evaluates to true
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });
    });

    describe('complex scenarios', () => {
      it('returns true when provider is GitHub and both are fully inherited', () => {
        sourceControl.provider.isInherited = true;
        sourceControl.token.isInherited = true;
        serverSourceControl.provider.parentValue.value = 'github';
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });

      it('returns true when switching from non-GitHub to GitHub with overrides', () => {
        // Simulating a user changing provider from GitLab to GitHub
        sourceControl.provider.rscValue.value = 'github';
        sourceControl.provider.isInherited = false;
        sourceControl.token.isInherited = false;
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          true
        );
      });

      it('returns false when inherited provider is not GitHub (fully inherited)', () => {
        sourceControl.provider.rscValue.value = 'gitlab';
        sourceControl.provider.isInherited = true;
        sourceControl.token.isInherited = true;
        serverSourceControl.provider.parentValue.value = 'gitlab';
        expect(shouldShowGitHubAppAuth(sourceControl, serverSourceControl)).toBe(
          false
        );
      });
    });
  });

  describe('storage utility functions', () => {
    const TEST_KEY = 'test-storage-key';
    const TEST_DATA = { field1: 'value1', field2: 'value2', timestamp: Date.now() };

    beforeEach(() => {
      sessionStorage.clear();
      localStorage.clear();
    });

    afterEach(() => {
      sessionStorage.clear();
      localStorage.clear();
    });

    describe('getScmFormStateStorageKey', () => {
      it('should generate correct storage key for organization', () => {
        const key = getScmFormStateStorageKey('organization', 'org-123');
        expect(key).toBe('scmFormState_organization_org-123');
      });

      it('should generate correct storage key for application', () => {
        const key = getScmFormStateStorageKey('application', 'app-456');
        expect(key).toBe('scmFormState_application_app-456');
      });

      it('should handle numeric IDs', () => {
        const key = getScmFormStateStorageKey('organization', 12345);
        expect(key).toBe('scmFormState_organization_12345');
      });
    });

    describe('saveFormStateWithFallback', () => {
      it('should save to sessionStorage successfully', () => {
        saveFormStateWithFallback(TEST_KEY, TEST_DATA);

        expect(sessionStorage.getItem(TEST_KEY)).toBe(JSON.stringify(TEST_DATA));
      });

      it('should handle save errors gracefully without throwing', () => {
        const sessionStorageSetSpy = jest.spyOn(Storage.prototype, 'setItem');
        sessionStorageSetSpy.mockImplementationOnce(() => {
          throw new Error('QuotaExceededError');
        });

        expect(() => saveFormStateWithFallback(TEST_KEY, TEST_DATA)).not.toThrow();

        sessionStorageSetSpy.mockRestore();
      });

      it('should handle complex nested objects', () => {
        const complexData = {
          provider: { value: 'github', rscValue: { isPristine: false } },
          booleanFields: [true, false, true],
          nested: { deep: { value: 'test' } },
        };

        saveFormStateWithFallback(TEST_KEY, complexData);

        expect(sessionStorage.getItem(TEST_KEY)).toBe(JSON.stringify(complexData));
      });

      it('should handle null and undefined values', () => {
        const dataWithNulls = { field1: null, field2: undefined, field3: 'value' };

        saveFormStateWithFallback(TEST_KEY, dataWithNulls);

        const stored = JSON.parse(sessionStorage.getItem(TEST_KEY));
        expect(stored.field1).toBeNull();
        expect(stored.field3).toBe('value');
      });
    });

    describe('loadFormStateWithFallback', () => {
      it('should load from sessionStorage when available', () => {
        sessionStorage.setItem(TEST_KEY, JSON.stringify(TEST_DATA));

        const result = loadFormStateWithFallback(TEST_KEY);

        expect(result).toBe(JSON.stringify(TEST_DATA));
      });

      it('should return null when no data exists in sessionStorage', () => {
        const result = loadFormStateWithFallback(TEST_KEY);

        expect(result).toBeNull();
      });

      it('should handle sessionStorage read errors gracefully', () => {
        const sessionStorageGetSpy = jest.spyOn(Storage.prototype, 'getItem');
        sessionStorageGetSpy.mockImplementationOnce(() => {
          throw new Error('SecurityError');
        });

        const result = loadFormStateWithFallback(TEST_KEY);

        expect(result).toBeNull();

        sessionStorageGetSpy.mockRestore();
      });
    });

    describe('removeFormStateWithFallback', () => {
      it('should remove from sessionStorage', () => {
        sessionStorage.setItem(TEST_KEY, JSON.stringify(TEST_DATA));

        removeFormStateWithFallback(TEST_KEY);

        expect(sessionStorage.getItem(TEST_KEY)).toBeNull();
      });

      it('should handle missing keys gracefully', () => {
        expect(() => removeFormStateWithFallback(TEST_KEY)).not.toThrow();

        expect(sessionStorage.getItem(TEST_KEY)).toBeNull();
      });

      it('should handle sessionStorage removal errors gracefully', () => {
        sessionStorage.setItem(TEST_KEY, JSON.stringify(TEST_DATA));

        const sessionStorageRemoveSpy = jest.spyOn(Storage.prototype, 'removeItem');
        sessionStorageRemoveSpy.mockImplementationOnce(() => {
          throw new Error('SecurityError');
        });

        expect(() => removeFormStateWithFallback(TEST_KEY)).not.toThrow();

        sessionStorageRemoveSpy.mockRestore();
      });

      it('should remove only specified key, not other keys', () => {
        const OTHER_KEY = 'other-key';
        sessionStorage.setItem(TEST_KEY, JSON.stringify(TEST_DATA));
        sessionStorage.setItem(OTHER_KEY, JSON.stringify({ other: 'data' }));

        removeFormStateWithFallback(TEST_KEY);

        expect(sessionStorage.getItem(TEST_KEY)).toBeNull();
        expect(sessionStorage.getItem(OTHER_KEY)).not.toBeNull();
      });
    });

    describe('integration tests - full save/load/remove cycle', () => {
      it('should handle complete save-load-remove cycle with sessionStorage', () => {
        saveFormStateWithFallback(TEST_KEY, TEST_DATA);

        const loadResult = loadFormStateWithFallback(TEST_KEY);
        expect(JSON.parse(loadResult)).toEqual(TEST_DATA);

        removeFormStateWithFallback(TEST_KEY);
        expect(loadFormStateWithFallback(TEST_KEY)).toBeNull();
      });

      it('should handle multiple keys independently', () => {
        const KEY1 = 'scmFormState_organization_1';
        const KEY2 = 'scmFormState_application_2';
        const DATA1 = { org: 'data1' };
        const DATA2 = { app: 'data2' };

        saveFormStateWithFallback(KEY1, DATA1);
        saveFormStateWithFallback(KEY2, DATA2);

        expect(JSON.parse(loadFormStateWithFallback(KEY1))).toEqual(DATA1);
        expect(JSON.parse(loadFormStateWithFallback(KEY2))).toEqual(DATA2);

        removeFormStateWithFallback(KEY1);
        expect(loadFormStateWithFallback(KEY1)).toBeNull();
        expect(JSON.parse(loadFormStateWithFallback(KEY2))).toEqual(DATA2);
      });
    });
  });

  describe('GitHub App Personal Account Utilities', () => {
    describe('isPersonalAccount', () => {
      it('returns true for account names with personal marker', () => {
        expect(isPersonalAccount('john-doe(personal)')).toBe(true);
        expect(isPersonalAccount('user_123(personal)')).toBe(true);
        expect(isPersonalAccount('test-user(personal)')).toBe(true);
      });

      it('returns false for organization account names without marker', () => {
        expect(isPersonalAccount('acme-corp')).toBe(false);
        expect(isPersonalAccount('my-org')).toBe(false);
        expect(isPersonalAccount('test-organization')).toBe(false);
      });

      it('handles null and undefined gracefully', () => {
        expect(isPersonalAccount(null)).toBeFalsy();
        expect(isPersonalAccount(undefined)).toBeFalsy();
      });

      it('handles empty string', () => {
        expect(isPersonalAccount('')).toBe(false);
      });

      it('is case-sensitive for marker detection', () => {
        expect(isPersonalAccount('user(Personal)')).toBe(false);
        expect(isPersonalAccount('user(PERSONAL)')).toBe(false);
      });

      it('handles marker-only string (edge case during registration)', () => {
        expect(isPersonalAccount('(personal)')).toBe(true);
      });

      it('does not match marker in the middle of string', () => {
        expect(isPersonalAccount('(personal)user')).toBe(false);
        expect(isPersonalAccount('user(personal)org')).toBe(false);
      });

      it('uses constant for marker detection', () => {
        const accountName = 'user' + PERSONAL_ACCOUNT_MARKER;
        expect(isPersonalAccount(accountName)).toBe(true);
      });
    });

    describe('getCleanAccountName', () => {
      it('strips personal marker from personal accounts', () => {
        expect(getCleanAccountName('john-doe(personal)')).toBe('john-doe');
        expect(getCleanAccountName('test_user(personal)')).toBe('test_user');
        expect(getCleanAccountName('my-account(personal)')).toBe('my-account');
      });

      it('returns organization names unchanged', () => {
        expect(getCleanAccountName('acme-corp')).toBe('acme-corp');
        expect(getCleanAccountName('my-org')).toBe('my-org');
        expect(getCleanAccountName('test-organization')).toBe('test-organization');
      });

      it('handles null and undefined by returning empty string', () => {
        expect(getCleanAccountName(null)).toBe('');
        expect(getCleanAccountName(undefined)).toBe('');
      });

      it('handles empty string', () => {
        expect(getCleanAccountName('')).toBe('');
      });

      it('handles marker-only string (edge case during registration)', () => {
        expect(getCleanAccountName('(personal)')).toBe('');
      });

      it('uses constant for marker length calculation', () => {
        const accountName = 'user' + PERSONAL_ACCOUNT_MARKER;
        const cleanName = getCleanAccountName(accountName);
        expect(cleanName).toBe('user');
        expect(cleanName).not.toContain(PERSONAL_ACCOUNT_MARKER);
      });

      it('handles names that contain parentheses but not the marker', () => {
        expect(getCleanAccountName('user(test)')).toBe('user(test)');
        expect(getCleanAccountName('org(2024)')).toBe('org(2024)');
      });
    });

    describe('getGitHubAppInstallationUrl', () => {
      describe('Personal Accounts', () => {
        it('generates personal account URL for personal accounts', () => {
          expect(getGitHubAppInstallationUrl('john-doe(personal)', 12345)).toBe(
            'https://github.com/settings/installations/12345'
          );
          expect(getGitHubAppInstallationUrl('user(personal)', '67890')).toBe(
            'https://github.com/settings/installations/67890'
          );
        });

        it('handles marker-only accountName (registration in progress)', () => {
          expect(getGitHubAppInstallationUrl('(personal)', 12345)).toBe(
            'https://github.com/settings/installations/12345'
          );
        });
      });

      describe('Organization Accounts', () => {
        it('generates organization URL for organization accounts', () => {
          expect(getGitHubAppInstallationUrl('acme-corp', 12345)).toBe(
            'https://github.com/organizations/acme-corp/settings/installations/12345'
          );
          expect(getGitHubAppInstallationUrl('my-org', '67890')).toBe(
            'https://github.com/organizations/my-org/settings/installations/67890'
          );
        });

        it('handles account names with hyphens and underscores', () => {
          expect(getGitHubAppInstallationUrl('test-org-123', 12345)).toBe(
            'https://github.com/organizations/test-org-123/settings/installations/12345'
          );
          expect(getGitHubAppInstallationUrl('my_organization', 12345)).toBe(
            'https://github.com/organizations/my_organization/settings/installations/12345'
          );
        });
      });

      describe('Installation ID Handling', () => {
        it('accepts installationId as both string and number', () => {
          expect(getGitHubAppInstallationUrl('org', 12345)).toBe(
            'https://github.com/organizations/org/settings/installations/12345'
          );
          expect(getGitHubAppInstallationUrl('org', '12345')).toBe(
            'https://github.com/organizations/org/settings/installations/12345'
          );
        });

        it('returns null when installationId is missing', () => {
          expect(getGitHubAppInstallationUrl('john-doe(personal)', null)).toBe(null);
          expect(getGitHubAppInstallationUrl('acme-corp', undefined)).toBe(null);
        });

        it('returns null when installationId is empty string', () => {
          expect(getGitHubAppInstallationUrl('acme-corp', '')).toBe(null);
        });

        it('returns null when installationId is zero', () => {
          expect(getGitHubAppInstallationUrl('acme-corp', 0)).toBe(null);
        });
      });

      describe('Backend Consistency', () => {
        it('generates URLs matching backend marker format', () => {
          // Backend stores as: accountName + "(personal)" with NO space
          const backendPersonalFormat = 'username(personal)';
          const url = getGitHubAppInstallationUrl(backendPersonalFormat, 12345);
          expect(url).toBe('https://github.com/settings/installations/12345');
        });

        it('uses constant for marker detection', () => {
          const accountName = 'user' + PERSONAL_ACCOUNT_MARKER;
          const url = getGitHubAppInstallationUrl(accountName, 12345);
          expect(url).toBe('https://github.com/settings/installations/12345');
        });
      });
    });

    describe('Integration Tests', () => {
      it('utilities work together for personal account flow', () => {
        const personalAccount = 'john-doe(personal)';

        // Check if personal
        expect(isPersonalAccount(personalAccount)).toBe(true);

        // Get clean name for display
        const displayName = getCleanAccountName(personalAccount);
        expect(displayName).toBe('john-doe');

        // Generate installation URL
        const url = getGitHubAppInstallationUrl(personalAccount, 12345);
        expect(url).toBe('https://github.com/settings/installations/12345');
        expect(url).not.toContain('organizations');
      });

      it('utilities work together for organization account flow', () => {
        const orgAccount = 'acme-corp';

        // Check if personal
        expect(isPersonalAccount(orgAccount)).toBe(false);

        // Get clean name (should be unchanged)
        const displayName = getCleanAccountName(orgAccount);
        expect(displayName).toBe('acme-corp');

        // Generate installation URL
        const url = getGitHubAppInstallationUrl(orgAccount, 12345);
        expect(url).toBe('https://github.com/organizations/acme-corp/settings/installations/12345');
        expect(url).toContain('organizations');
      });
    });
  });
});
