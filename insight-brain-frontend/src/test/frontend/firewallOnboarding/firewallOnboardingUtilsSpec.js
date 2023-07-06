/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { steps, next, prev, groupAndSortByFormat } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

describe('FirewallOnboardingUtils', () => {
  const defaultRepoProps = {
    publicId: 'publicId',
    repositoryManagerId: 'repositoryManagerId',
    repositoryType: 'hosted',
    enabled: true,
    quarantineEnabled: false,
    namespaceConfusionProtectionEnabled: false,
    policyCompliantComponentSelectionEnabled: false,
  };

  describe('prev', () => {
    it('return previous step', () => {
      expect(prev(steps[0])).toBe(undefined);
      expect(prev(steps[1])).toBe(steps[0]);
      expect(prev(steps[2])).toBe(steps[1]);
      expect(prev(steps[3])).toBe(steps[2]);
    });
  });

  describe('next', () => {
    it('return next step', () => {
      expect(next(steps[0])).toBe(steps[1]);
      expect(next(steps[1])).toBe(steps[2]);
      expect(next(steps[2])).toBe(steps[3]);
      expect(next(steps[3])).toBe(undefined);
    });
  });

  describe('groupAndSortByFormat', () => {
    it('should group and sort repositories by format', () => {
      const repositories = [
        { id: '1', format: 'maven', ...defaultRepoProps },
        { id: '2', format: 'npm', ...defaultRepoProps },
        { id: '3', format: 'maven', ...defaultRepoProps },
        { id: '4', format: 'npm', ...defaultRepoProps },
        { id: '5', format: 'bower', ...defaultRepoProps },
        { id: '6', format: 'bower', ...defaultRepoProps },
        { id: '7', format: 'pypi', ...defaultRepoProps },
        { id: '8', format: 'pypi', ...defaultRepoProps },
        { id: '9', format: 'gem', ...defaultRepoProps },
        { id: '10', format: 'gem', ...defaultRepoProps },
        { id: '11', format: 'gem', ...defaultRepoProps },
        { id: '12', format: 'gem', ...defaultRepoProps },
        { id: '13', format: 'apt', ...defaultRepoProps },
        { id: '14', format: 'apt', ...defaultRepoProps },
        { id: '15', format: 'apt', ...defaultRepoProps },
      ];

      const allowedFormats = ['maven', 'npm', 'bower', 'pypi'];

      const result = groupAndSortByFormat(repositories, allowedFormats);

      expect(result.length).toEqual(4);
      expect(result[0].format).toEqual('bower');
      expect(result[1].format).toEqual('maven');
      expect(result[2].format).toEqual('npm');
      expect(result[3].format).toEqual('other');

      expect(result[3].repositories.length).toEqual(9);
      expect(result[3].repositories[0].format).toEqual('pypi');
      expect(result[3].repositories[1].format).toEqual('pypi');
      expect(result[3].repositories[2].format).toEqual('gem');
      expect(result[3].repositories[3].format).toEqual('gem');
      expect(result[3].repositories[4].format).toEqual('gem');
      expect(result[3].repositories[5].format).toEqual('gem');
      expect(result[3].repositories[6].format).toEqual('apt');
      expect(result[3].repositories[7].format).toEqual('apt');
      expect(result[3].repositories[8].format).toEqual('apt');
    });

    it('should handle repositories with a single format', () => {
      const repositories = [
        { id: '1', format: 'maven', ...defaultRepoProps },
        { id: '2', format: 'maven', ...defaultRepoProps },
        { id: '3', format: 'maven', ...defaultRepoProps },
      ];

      const allowedFormats = ['maven'];

      const result = groupAndSortByFormat(repositories, allowedFormats);

      expect(result.length).toEqual(1);
      expect(result[0].format).toEqual('maven');
      expect(result[0].repositories.length).toEqual(3);
    });

    it('should handle no repositories matching the allowed formats', () => {
      // given a list of repositories
      const repositories = [
        { id: '1', format: 'npm', ...defaultRepoProps },
        { id: '2', format: 'npm', ...defaultRepoProps },
        { id: '3', format: 'gem', ...defaultRepoProps },
        { id: '4', format: 'pypi', ...defaultRepoProps },
        { id: '5', format: 'apt', ...defaultRepoProps },
        { id: '6', format: 'conda', ...defaultRepoProps },
      ];
      const allowedFormats = ['maven', 'bower'];

      // when the repositories are grouped and sorted
      const result = groupAndSortByFormat(repositories, allowedFormats);

      // then NPM is in the first column, and the rest in no particular order
      expect(result.length).toEqual(4);
      expect(result[0].format).toEqual('npm');
      expect(result[3].repositories.length).toEqual(2);
    });

    it('should always sort supported formats before disabled formats even when there are more disabled repository formats', () => {
      // given a repository list where 'notSupportedFormat' has more repositories than 'supportedFormat1' or 'supportedFormat2'
      const repositories = [
        { id: '1', format: 'supportedFormat2', ...defaultRepoProps },
        { id: '2', format: 'supportedFormat1', ...defaultRepoProps },
        { id: '3', format: 'supportedFormat1', ...defaultRepoProps },
        { id: '4', format: 'notSupportedFormat', ...defaultRepoProps },
        { id: '5', format: 'notSupportedFormat', ...defaultRepoProps },
        { id: '6', format: 'notSupportedFormat', ...defaultRepoProps },
      ];
      const supportedFormats = ['supportedFormat1', 'supportedFormat2'];

      // when the repositories are grouped and sorted
      const result = groupAndSortByFormat(repositories, supportedFormats);

      // then the supported formats are sorted before the disabled formats
      expect(result.length).toEqual(3);
      expect(result[0].repositories).toEqual(repositories.filter((repo) => repo.format === 'supportedFormat1'));
      expect(result[1].repositories).toEqual(repositories.filter((repo) => repo.format === 'supportedFormat2'));
      expect(result[2].repositories).toEqual(repositories.filter((repo) => repo.format === 'notSupportedFormat'));
    });
  });
});
