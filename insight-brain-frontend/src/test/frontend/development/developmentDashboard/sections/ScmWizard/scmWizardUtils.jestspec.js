/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { getSCMProviderTokenUrl } from 'MainRoot/development/developmentDashboard/sections/scmWizard/scmWizardUtil';
import {
  formatSCMProvider,
  getSCMProviderTokenDocUrl,
} from 'MainRoot/development/developmentDashboard/sections/scmWizard/scmWizardUtil';

describe('scmWizardUtils', () => {
  describe('getSCMProviderTokenUrl', () => {
    it('should return the correct url for github', () => {
      expect(getSCMProviderTokenUrl('github')).toBe('https://github.com/settings/tokens');
    });

    it('should return the correct url for gitlab', () => {
      expect(getSCMProviderTokenUrl('gitlab')).toBe('https://gitlab.com/-/user_settings/personal_access_tokens');
    });

    it('should return the correct url for azure devops', () => {
      expect(getSCMProviderTokenUrl('azure devops')).toBe('https://dev.azure.com/{organization}/_usersSettings/tokens');
    });

    it('should return the correct url for bitbucket', () => {
      expect(getSCMProviderTokenUrl('bitbucket')).toBe(
        'https://bitbucket.org/{workspace_name}/{repository_name}/admin/access-tokens'
      );
    });

    it('should return an empty string for an unknown provider', () => {
      expect(getSCMProviderTokenUrl('unknown')).toBe('');
    });
  });

  describe('getSCMProviderTokenDocUrl', () => {
    it('should return the correct url for github', () => {
      expect(getSCMProviderTokenDocUrl('github')).toBe(
        'https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens'
      );
    });

    it('should return the correct url for gitlab', () => {
      expect(getSCMProviderTokenDocUrl('gitlab')).toBe(
        'https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html'
      );
    });

    it('should return the correct url for azure devops', () => {
      expect(getSCMProviderTokenDocUrl('azure devops')).toBe(
        'https://docs.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=azure-devops'
      );
    });

    it('should return the correct url for bitbucket', () => {
      expect(getSCMProviderTokenDocUrl('bitbucket')).toBe(
        'https://support.atlassian.com/bitbucket-cloud/docs/app-passwords/'
      );
    });
  });

  describe('formatSCMProvider', () => {
    it('should return the correct format for github', () => {
      expect(formatSCMProvider('GITHUB')).toBe('GitHub');
      expect(formatSCMProvider('github')).toBe('GitHub');
    });

    it('should return the correct format for gitlab', () => {
      expect(formatSCMProvider('GITLAB')).toBe('GitLab');
      expect(formatSCMProvider('gitlab')).toBe('GitLab');
    });

    it('should return the correct format for azure', () => {
      expect(formatSCMProvider('AZURE DEVOPS')).toBe('Azure DevOps');
      expect(formatSCMProvider('azure devops')).toBe('Azure DevOps');
    });

    it('should return the correct format for bitbucket', () => {
      expect(formatSCMProvider('BITBUCKET')).toBe('Bitbucket');
      expect(formatSCMProvider('bitbucket')).toBe('Bitbucket');
    });

    it('should return the correct format for unknown', () => {
      expect(formatSCMProvider('unknown')).toBe('unknown');
    });
  });
});
