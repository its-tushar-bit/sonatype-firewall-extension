/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  steps,
  next,
  prev,
  groupAndSortByFormat,
  groupRepositoriesByTypes,
} from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

describe('FirewallOnboardingUtils', () => {
  describe('prev', () => {
    it('return previous step', () => {
      expect(prev(steps[0])).toBe(undefined);
      expect(prev(steps[1])).toBe(steps[0]);
    });
  });

  describe('next', () => {
    it('return next step', () => {
      expect(next(steps[0])).toBe(steps[1]);
      expect(next(steps[1])).toBe(undefined);
    });
  });

  describe('groupAndSortByFormat', () => {
    it('should group and sort repositories by format', () => {
      const repositories = [
        { id: '1', format: 'maven' },
        { id: '2', format: 'npm' },
        { id: '3', format: 'maven' },
        { id: '4', format: 'npm' },
        { id: '5', format: 'bower' },
        { id: '6', format: 'bower' },
        { id: '7', format: 'pypi' },
        { id: '8', format: 'pypi' },
        { id: '9', format: 'gem' },
        { id: '10', format: 'gem' },
        { id: '11', format: 'gem' },
        { id: '12', format: 'gem' },
        { id: '13', format: 'apt' },
        { id: '14', format: 'apt' },
        { id: '15', format: 'apt' },
      ];

      const allowedFormats = ['maven', 'npm', 'bower', 'pypi'];

      const result = groupAndSortByFormat(repositories, allowedFormats);

      expect(result.length).toEqual(4);
      expect(result[0].format).toEqual('bower');
      expect(result[1].format).toEqual('maven');
      expect(result[2].format).toEqual('npm');
      expect(result[3].format).toEqual('other');
      expect(result[3].repositories.length).toEqual(2);
    });

    it('should handle repositories with a single format', () => {
      const repositories = [
        { id: '1', format: 'maven' },
        { id: '2', format: 'maven' },
        { id: '3', format: 'maven' },
      ];

      const allowedFormats = ['maven'];

      const result = groupAndSortByFormat(repositories, allowedFormats);

      expect(result.length).toEqual(1);
      expect(result[0].format).toEqual('maven');
      expect(result[0].repositories.length).toEqual(3);
    });

    it('should handle no repositories matching the allowed formats', () => {
      const repositories = [
        { id: '1', format: 'npm' },
        { id: '2', format: 'gem' },
        { id: '3', format: 'pypi' },
      ];

      const allowedFormats = ['maven', 'bower'];

      const result = groupAndSortByFormat(repositories, allowedFormats);

      expect(result.length).toEqual(0);
    });
  });

  describe('groupRepositoriesByTypes', () => {
    it('should filter repositories by type', () => {
      const repositories = [
        { id: '1', repositoryType: 'proxy' },
        { id: '2', repositoryType: 'hosted' },
        { id: '3', repositoryType: 'proxy' },
        { id: '4', repositoryType: 'group' },
        { id: '5', repositoryType: 'proxy' },
      ];

      const result = groupRepositoriesByTypes(repositories);

      expect(result).toEqual({
        proxy: [
          { id: '1', repositoryType: 'proxy' },
          { id: '3', repositoryType: 'proxy' },
          { id: '5', repositoryType: 'proxy' },
        ],
      });
    });

    it('should handle empty repositories', () => {
      const result = groupRepositoriesByTypes([]);

      expect(result).toEqual({ proxy: [] });
    });

    it('should handle repositories with different types', () => {
      const repositories = [
        { id: '1', repositoryType: 'proxy' },
        { id: '2', repositoryType: 'hosted' },
        { id: '3', repositoryType: 'group' },
      ];

      const result = groupRepositoriesByTypes(repositories);

      expect(result).toEqual({
        proxy: [{ id: '1', repositoryType: 'proxy' }],
      });
    });

    it('should handle repositories with no matching types', () => {
      const repositories = [
        { id: '1', repositoryType: 'hosted' },
        { id: '2', repositoryType: 'hosted' },
      ];

      const result = groupRepositoriesByTypes(repositories);

      expect(result).toEqual({ proxy: [] });
    });
  });
});
