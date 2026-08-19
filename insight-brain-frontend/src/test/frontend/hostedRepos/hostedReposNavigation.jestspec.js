/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as urlUtil from 'MainRoot/util/urlUtil';
import { hostedReposState } from 'MainRoot/hostedRepos/hostedReposNavigation';

describe('hostedReposNavigation', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('hostedReposState', () => {
    it('returns the Classic state names when running in the Classic bundle', () => {
      jest.spyOn(urlUtil, 'isNexusOneBundle').mockReturnValue(false);

      expect(hostedReposState('hostedRepos')).toBe('hostedRepos');
      expect(hostedReposState('hostedRepositories')).toBe('hostedRepositories');
      expect(hostedReposState('hostedRepoComponents')).toBe('hostedRepoComponents');
    });

    it('maps to the Nexus One state names when running in the Nexus One bundle', () => {
      jest.spyOn(urlUtil, 'isNexusOneBundle').mockReturnValue(true);

      expect(hostedReposState('hostedRepos')).toBe('nexusOneRepositories');
      expect(hostedReposState('hostedRepositories')).toBe('nexusOneRepositoriesDetail');
      expect(hostedReposState('hostedRepoComponents')).toBe('nexusOneRepositoriesComponents');
    });
  });
});
