/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  displayName,
  valueFromHierarchy,
  tokenForOrg,
  getAuthMethodForOrg,
  hasAuth,
} from '../../../../main/frontend/configuration/scmOnboarding/utils/providers';

describe('scmOnboardingProviders', function () {
  describe('displayName', () => {
    const testData = [
      { scmProvider: 'github', expected: 'GitHub' },
      { scmProvider: 'gitlab', expected: 'GitLab' },
      { scmProvider: 'bitbucket', expected: 'Bitbucket' },
      { scmProvider: 'azure', expected: 'Azure DevOps' },
      { scmProvider: 'unknown', expected: 'unknown' },
      { scmProvider: '', expected: '' },
      { scmProvider: null, expected: null },
    ];

    for (let currTest of testData) {
      it('Describes provider ' + currTest.scmProvider + ' as ' + currTest.expected, () => {
        expect(displayName(currTest.scmProvider)).toEqual(currTest.expected);
      });
    }
  });

  describe('valueFromHierarchy', () => {
    it('should return null when compositeDto is null', () => {
      expect(valueFromHierarchy(null)).toBeNull();
    });

    it('should return value when value is not null', () => {
      const dto = { value: 'myValue', parentValue: 'parentValue' };
      expect(valueFromHierarchy(dto)).toBe('myValue');
    });

    it('should return parentValue when value is null', () => {
      const dto = { value: null, parentValue: 'parentValue' };
      expect(valueFromHierarchy(dto)).toBe('parentValue');
    });

    it('should return null when both value and parentValue are null', () => {
      const dto = { value: null, parentValue: null };
      expect(valueFromHierarchy(dto)).toBeNull();
    });
  });

  describe('getAuthMethodForOrg and hasAuth', () => {
    it('should return null/false when org is null', () => {
      expect(getAuthMethodForOrg(null)).toBeNull();
      expect(hasAuth(null)).toBe(false);
    });

    it('should return null/false when org.sourceControl is null', () => {
      const org = { sourceControl: null };
      expect(getAuthMethodForOrg(org)).toBeNull();
      expect(hasAuth(org)).toBe(false);
    });

    it('should return null/false when org.sourceControl is undefined', () => {
      const org = {};
      expect(getAuthMethodForOrg(org)).toBeNull();
      expect(hasAuth(org)).toBe(false);
    });

    describe('GitHub App authentication', () => {
      it('should return GITHUB_APP/true when GitHub App is configured with installationId at org level', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: 'GITHUB_APP' },
            githubApp: { value: { installationId: 12345 } },
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('GITHUB_APP');
        expect(hasAuth(org)).toBe(true);
      });

      it('should return GITHUB_APP/true when the live plural githubApps payload is configured at org level', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github', parentValue: null },
            authenticationType: { value: 'GITHUB_APP', parentValue: null },
            githubApps: [
              {
                value: { id: 'github-app-1', installationId: 12345, isActive: true },
                parentValue: null,
                parentName: null,
              },
            ],
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('GITHUB_APP');
        expect(hasAuth(org)).toBe(true);
      });

      it('should return GITHUB_APP when GitHub App is inherited with installationId', () => {
        const org = {
          sourceControl: {
            provider: { value: null, parentValue: 'github' },
            authenticationType: { value: null, parentValue: 'GITHUB_APP' },
            githubApp: { value: null, parentValue: { installationId: 12345 } },
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('GITHUB_APP');
      });

      it('should return GITHUB_APP when the live plural githubApps payload is inherited', () => {
        const org = {
          sourceControl: {
            provider: { value: null, parentValue: 'github' },
            authenticationType: { value: null, parentValue: 'GITHUB_APP' },
            githubApps: [
              {
                value: null,
                parentValue: {
                  id: 'github-app-1',
                  installationId: 12345,
                  isActive: true,
                },
                parentName: 'Root Organization',
              },
            ],
            token: { value: null, parentValue: null },
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('GITHUB_APP');
        expect(hasAuth(org)).toBe(true);
      });

      it('should return null/false when GitHub App is configured but missing installationId', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: 'GITHUB_APP' },
            githubApp: { value: { installationId: null } },
          },
        };
        expect(getAuthMethodForOrg(org)).toBeNull();
        expect(hasAuth(org)).toBe(false);
      });

      it('should return null when GitHub App is configured but githubApp is null', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: 'GITHUB_APP' },
            githubApp: { value: null, parentValue: null },
          },
        };
        expect(getAuthMethodForOrg(org)).toBeNull();
      });

      it('should return null when GitHub App is configured but githubApp is undefined', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: 'GITHUB_APP' },
            // githubApp is undefined
          },
        };
        expect(getAuthMethodForOrg(org)).toBeNull();
      });
    });

    describe('PAT authentication', () => {
      it('should return PAT/true when token is configured at org level', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: 'PAT' },
            token: { value: 'my-token' },
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('PAT');
        expect(hasAuth(org)).toBe(true);
      });

      it('should return PAT when token is inherited', () => {
        const org = {
          sourceControl: {
            provider: { value: null, parentValue: 'github' },
            authenticationType: { value: null, parentValue: 'PAT' },
            token: { value: null, parentValue: 'parent-token' },
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('PAT');
      });

      it('should return null/false when PAT is configured but token is null', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: 'PAT' },
            token: { value: null, parentValue: null },
          },
        };
        expect(getAuthMethodForOrg(org)).toBeNull();
        expect(hasAuth(org)).toBe(false);
      });

      it('should return PAT when no authenticationType but token exists (legacy)', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: null, parentValue: null },
            token: { value: 'my-token' },
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('PAT');
      });
    });

    describe('Custom provider override scenarios', () => {
      it('should use org-level config when provider is overridden at org level', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github', parentValue: 'gitlab' },
            authenticationType: { value: 'GITHUB_APP', parentValue: 'PAT' },
            githubApp: { value: { installationId: 12345 }, parentValue: null },
            token: { value: null, parentValue: 'parent-token' },
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('GITHUB_APP');
      });

      it('should ignore parent token when provider is overridden', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github', parentValue: 'gitlab' },
            authenticationType: { value: 'PAT', parentValue: 'PAT' },
            token: { value: null, parentValue: 'parent-token' },
          },
        };
        // When provider is overridden, should only look at org-level token
        expect(getAuthMethodForOrg(org)).toBeNull();
      });
    });

    describe('Edge cases', () => {
      it('should handle empty string authenticationType', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: '', parentValue: null },
            token: { value: 'my-token' },
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('PAT');
      });

      it('should handle undefined authenticationType gracefully', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            token: { value: 'my-token' },
            // authenticationType is undefined
          },
        };
        expect(getAuthMethodForOrg(org)).toBe('PAT');
      });

      it('should return null when neither GitHub App nor PAT is configured', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: null, parentValue: null },
            token: { value: null, parentValue: null },
          },
        };
        expect(getAuthMethodForOrg(org)).toBeNull();
      });

      it('should handle installationId as 0 (falsy but valid)', () => {
        const org = {
          sourceControl: {
            provider: { value: 'github' },
            authenticationType: { value: 'GITHUB_APP' },
            githubApp: { value: { installationId: 0 } },
          },
        };
        // installationId of 0 should be considered invalid (installations IDs start from 1)
        expect(getAuthMethodForOrg(org)).toBeNull();
      });
    });
  });

  describe('Integration scenarios', () => {
    it('should handle complete GitHub App configuration hierarchy', () => {
      const rootOrg = {
        sourceControl: {
          provider: { value: 'github', parentValue: null },
          authenticationType: { value: 'GITHUB_APP', parentValue: null },
          githubApp: { value: { installationId: 99999 }, parentValue: null },
          token: { value: null, parentValue: null },
        },
      };

      const childOrg = {
        sourceControl: {
          provider: { value: null, parentValue: 'github' },
          authenticationType: { value: null, parentValue: 'GITHUB_APP' },
          githubApp: { value: null, parentValue: { installationId: 99999 } },
          token: { value: null, parentValue: null },
        },
      };

      expect(getAuthMethodForOrg(rootOrg)).toBe('GITHUB_APP');
      expect(getAuthMethodForOrg(childOrg)).toBe('GITHUB_APP');
    });

    it('should handle migration scenario from PAT to GitHub App', () => {
      // Org initially configured with PAT
      const orgWithPAT = {
        sourceControl: {
          provider: { value: 'github' },
          authenticationType: { value: 'PAT' },
          token: { value: 'old-token' },
          githubApp: { value: null },
        },
      };
      expect(getAuthMethodForOrg(orgWithPAT)).toBe('PAT');

      // Org migrated to GitHub App (PAT still present but not used)
      const orgWithGitHubApp = {
        sourceControl: {
          provider: { value: 'github' },
          authenticationType: { value: 'GITHUB_APP' },
          token: { value: 'old-token' },
          githubApp: { value: { installationId: 12345 } },
        },
      };
      expect(getAuthMethodForOrg(orgWithGitHubApp)).toBe('GITHUB_APP');
    });
  });
});
