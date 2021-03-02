/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {displayName} from '../../../../main/frontend/configuration/scmOnboarding/utils/providers';

describe('scmOnboardingProviders', function() {
  const testData = [
    {scmProvider: 'github', expected: 'GitHub'},
    {scmProvider: 'gitlab', expected: 'GitLab'},
    {scmProvider: 'bitbucket', expected: 'Bitbucket'},
    {scmProvider: 'unknown', expected: 'unknown'},
    {scmProvider: '', expected: ''},
    {scmProvider: null, expected: null}
  ];

  for (let currTest of testData) {
    it('Describes provider ' + currTest.scmProvider + ' as ' + currTest.expected, () => {
      expect(displayName(currTest.scmProvider)).toEqual(currTest.expected);
    });
  }
});
