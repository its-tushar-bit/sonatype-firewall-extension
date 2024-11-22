/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { isCopyrightDetailsState } from '../../../../main/frontend/legal/copyright/copyrightDetailsUtils';

describe('CopyrightDetailsUtils', function () {
  describe('isCopyrightDetailsState', function () {
    it('should return true if state name belongs to copyright details', function () {
      let copyrightState = isCopyrightDetailsState('anyStateNameWith.copyrightDetails');
      expect(copyrightState).toBeTruthy();
    });

    it('should return false if state name does not belong to copyright details', function () {
      let copyrightState = isCopyrightDetailsState('anyStateNameWith.anythingElse');
      expect(copyrightState).toBeFalsy();
    });
  });
});
