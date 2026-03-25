/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectOrgsAndPoliciesSlice, selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectSourceControlSlice,
  selectLoadError,
  selectLoading,
  selectSourceControl,
  selectEffectiveProvider,
  selectItemText,
  selectItemSubText,
  selectRepositoryUrl,
  selectScmProviderIcon,
} from 'MainRoot/OrgsAndPolicies/sourceControlSelectors';
import {
  selectIsApplication,
  selectIsOrganization,
  selectIsRootOrganization,
} from 'MainRoot/reduxUiRouter/routerSelectors';

describe('sourceControlSelectors', () => {
  describe('selectSourceControlSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectSourceControlSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects selectSourceControl slice', () => {
      const orgsAndPoliciesSlice = {
        sourceControl: 'sourceControl',
      };

      const selected = selectSourceControlSlice.resultFunc(orgsAndPoliciesSlice);
      expect(selected).toBe('sourceControl');
    });
  });

  describe('immediate slice keys', () => {
    const orgsAndPoliciesSlice = {
      loading: 'loading',
      loadError: 'loadError',
      data: 'data',
    };
    describe('selectLoadError', () => {
      it('is composed from the following selector', () => {
        expect(selectLoadError.dependencies).toEqual([selectSourceControlSlice]);
      });

      it('selects loadError from the selectSourceControlSlice', () => {
        const selected = selectLoadError.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('loadError');
      });
    });

    describe('selectLoading', () => {
      it('is composed from the following selector', () => {
        expect(selectLoading.dependencies).toEqual([selectSourceControlSlice]);
      });

      it('selects loading from the selectSourceControlSlice', () => {
        const selected = selectLoading.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('loading');
      });
    });

    describe('selectSourceControl', () => {
      it('is composed from the following selector', () => {
        expect(selectSourceControl.dependencies).toEqual([selectSourceControlSlice]);
      });

      it('selects data from the selectSourceControlSlice', () => {
        const selected = selectSourceControl.resultFunc(orgsAndPoliciesSlice);
        expect(selected).toBe('data');
      });
    });
  });

  describe('selectEffectiveProvider', () => {
    it('is composed from the following selector', () => {
      expect(selectEffectiveProvider.dependencies).toEqual([selectSourceControl]);
    });

    it('selects EffectiveProvider when it applies', () => {
      const sourceControl = {
        provider: {
          value: 'some val',
        },
      };

      const selected = selectEffectiveProvider.resultFunc(sourceControl);
      expect(selected).toBe('some val');
    });

    it('selects null when it applies', () => {
      const sourceControl = { otherVal: 'other key' };

      const selected = selectEffectiveProvider.resultFunc(sourceControl);
      expect(selected).toBe(null);
    });
  });

  describe('selectItemText', () => {
    const testsToRun = [
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
        },
        effectiveProvider: 'azure',
        isOrg: true,
        expectedMessage: 'Azure DevOps',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
        },
        effectiveProvider: 'azure',
        isOrg: false,
        expectedMessage: 'Some Url',
      },
      {
        sourceControl: {},
        effectiveProvider: 'bitbucket',
        isOrg: false,
        expectedMessage: 'Repository URL needed',
      },
    ];

    it('is composed from the following selector', () => {
      expect(selectItemText.dependencies).toEqual([selectSourceControl, selectEffectiveProvider, selectIsOrganization]);
    });

    testsToRun.forEach(({ sourceControl, effectiveProvider, isOrg, expectedMessage }) => {
      it(`selects selectItemText with isOrg = ${isOrg}, effectiveProvider = ${effectiveProvider} and repositoryUrl = ${sourceControl.repositoryUrl}`, () => {
        const selected = selectItemText.resultFunc(sourceControl, effectiveProvider, isOrg);
        expect(selected).toEqual(expectedMessage);
      });
    });
  });

  describe('selectItemSubText', () => {
    const testsToRun = [
      {
        effectiveProvider: 'azure',
        isRootOrg: true,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Source Control not configured',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            value: 'token value',
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          provider: { value: 'provider value' },
        },
        isRootOrg: true,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Source Control not configured',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            value: 'token value',
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          provider: { value: 'provider value' },
        },
        effectiveProvider: 'azure',
        isRootOrg: true,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Provides the default source control configuration settings',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          provider: { value: 'provider value' },
        },
        effectiveProvider: 'azure',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token (Azure DevOps)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
        },
        effectiveProvider: 'bitbucket',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token from token parentName (Bitbucket)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
        },
        effectiveProvider: 'azure',
        isRootOrg: false,
        isApp: false,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token from token parentName',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {},
        },
        effectiveProvider: 'bitbucket',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token (Bitbucket)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {},
        },
        effectiveProvider: 'azure',
        isRootOrg: false,
        isApp: false,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            value: 'token value',
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
        },
        effectiveProvider: 'azure',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Provides default access token for ownerName (Azure DevOps)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            value: 'token value',
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
        },
        effectiveProvider: 'azure',
        isRootOrg: false,
        isApp: false,
        ownerName: 'ownerName',
        expectedMessage: 'Provides default access token for ownerName',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            value: 'token value',
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          authenticationType: {
            value: 'GITHUB_APP',
          },
          githubApp: {
            value: {
              installationId: '12345',
            },
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Provides default authentication method: GitHub App for ownerName (GitHub)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            value: 'token value',
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          authenticationType: {
            value: 'GITHUB_APP',
          },
          githubApp: {
            value: {
              installationId: '12345',
            },
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: false,
        ownerName: 'ownerName',
        expectedMessage: 'Provides default authentication method: GitHub App for ownerName',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            value: 'token value',
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          authenticationType: {
            value: 'PAT',
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: false,
        ownerName: 'ownerName',
        expectedMessage: 'Provides default access token for ownerName',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          provider: { value: 'github' },
          authenticationType: {
            parentValue: 'GITHUB_APP',
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit authentication method: GitHub App',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          provider: { value: 'github' },
          authenticationType: {
            parentValue: 'PAT',
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token (GitHub)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          authenticationType: {
            parentValue: 'GITHUB_APP',
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit authentication method: GitHub App',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          authenticationType: {
            parentValue: 'GITHUB_APP',
          },
        },
        effectiveProvider: 'azure',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token from token parentName (Azure DevOps)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'parent token',
            parentName: 'parent org',
          },
          authenticationType: {
            value: 'GITHUB_APP',
          },
          githubApp: {
            value: {
              installationId: '12345',
            },
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Provides default authentication method: GitHub App for ownerName (GitHub)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {},
          authenticationType: {
            value: 'GITHUB_APP',
          },
          githubApp: {
            value: {
              installationId: '',
            },
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token (GitHub)',
      },
      {
        sourceControl: {
          repositoryUrl: 'Some Url',
          token: {},
          authenticationType: {
            value: 'GITHUB_APP',
          },
          githubApp: {
            value: null,
          },
        },
        effectiveProvider: 'github',
        isRootOrg: false,
        isApp: true,
        ownerName: 'ownerName',
        expectedMessage: 'Inherit access token (GitHub)',
      },
    ];

    it('is composed from the following selector', () => {
      expect(selectItemSubText.dependencies).toEqual([
        selectSourceControl,
        selectEffectiveProvider,
        selectIsRootOrganization,
        selectIsApplication,
        selectSelectedOwnerName,
        expect.any(Function),
      ]);
    });

    testsToRun.forEach(({ sourceControl, effectiveProvider, isRootOrg, isApp, ownerName, expectedMessage }) => {
      it(`selects selectItemSubText with isRootOrg = ${isRootOrg}, isApp = ${isApp}, effectiveProvider = ${effectiveProvider} and sourceControlExists = ${!!sourceControl}`, () => {
        const isGithubAppAuthenticationEnabled = true;
        const selected = selectItemSubText.resultFunc(
          sourceControl,
          effectiveProvider,
          isRootOrg,
          isApp,
          ownerName,
          isGithubAppAuthenticationEnabled
        );
        expect(selected).toEqual(expectedMessage);
      });
    });

    describe('when GitHub App feature flag is disabled', () => {
      it('should show "Provides default access token" instead of GitHub App message when local GitHub App is configured', () => {
        const sourceControl = {
          repositoryUrl: 'Some Url',
          token: {
            value: 'token value',
          },
          authenticationType: {
            value: 'GITHUB_APP',
          },
          githubApp: {
            value: {
              installationId: '12345',
            },
          },
        };
        const isGithubAppAuthenticationEnabled = false;
        const selected = selectItemSubText.resultFunc(
          sourceControl,
          'github',
          false,
          true,
          'ownerName',
          isGithubAppAuthenticationEnabled
        );
        expect(selected).toEqual('Provides default access token for ownerName (GitHub)');
      });

      it('should show "Inherit access token" instead of GitHub App inheritance message', () => {
        const sourceControl = {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          provider: { value: 'github' },
          authenticationType: {
            parentValue: 'GITHUB_APP',
          },
        };
        const isGithubAppAuthenticationEnabled = false;
        const selected = selectItemSubText.resultFunc(
          sourceControl,
          'github',
          false,
          true,
          'ownerName',
          isGithubAppAuthenticationEnabled
        );
        expect(selected).toEqual('Inherit access token (GitHub)');
      });

      it('should show "Inherit access token from parent" when feature is disabled and parent has GitHub App', () => {
        const sourceControl = {
          repositoryUrl: 'Some Url',
          token: {
            parentValue: 'token parentValue',
            parentName: 'token parentName',
          },
          authenticationType: {
            parentValue: 'GITHUB_APP',
          },
        };
        const isGithubAppAuthenticationEnabled = false;
        const selected = selectItemSubText.resultFunc(
          sourceControl,
          'github',
          false,
          true,
          'ownerName',
          isGithubAppAuthenticationEnabled
        );
        expect(selected).toEqual('Inherit access token from token parentName (GitHub)');
      });
    });
  });

  describe('selectRepositoryUrl', () => {
    it('is composed from the following selector', () => {
      expect(selectRepositoryUrl.dependencies).toEqual([selectSourceControl]);
    });

    it('selects repositoryUrl from sourceControl', () => {
      const sourceControl = {
        repositoryUrl: 'repositoryUrl',
      };

      const selected = selectRepositoryUrl.resultFunc(sourceControl);
      expect(selected).toBe('repositoryUrl');
    });
  });

  describe('selectScmProviderIcon', () => {
    const testsToRun = [
      {
        sourceControl: {
          provider: {
            value: 'bitbucket',
          },
        },
        expected: 'bitbucket',
      },
      {
        sourceControl: {
          provider: {
            parentValue: 'bitbucket',
          },
        },
        expected: 'bitbucket',
      },
      {
        sourceControl: {
          provider: {
            value: 'azure',
          },
        },
        expected: 'git',
      },
      {
        sourceControl: {
          provider: {
            parentValue: 'azure',
          },
        },
        expected: 'git',
      },
    ];

    it('is composed from the following selector', () => {
      expect(selectScmProviderIcon.dependencies).toEqual([selectSourceControl]);
    });

    testsToRun.forEach(({ sourceControl, expected }) => {
      it(`selects ScmProviderIcon from sourceControl with ${sourceControl.provider.value ? 'value' : 'parentValue'} ${
        sourceControl.provider.value || sourceControl.provider.parentValue
      }`, () => {
        const selected = selectScmProviderIcon.resultFunc(sourceControl);
        expect(selected).toBe(expected);
      });
    });
  });
});
