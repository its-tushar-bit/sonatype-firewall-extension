/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import PolicyViolationDetailPopover from '../../../../main/frontend/componentDetails/violations/PolicyViolationDetailPopover';

describe('PolicyViolationsDetail', () => {
  let minimalProps, getShallow;

  beforeEach(function () {
    minimalProps = {
      onClose: jasmine.createSpy('onClose'),
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolationDetailPopover, minimalProps);
  });

  describe('onClose action', () => {
    it('calls onClose when the close button is clicked', () => {
      const btn = getShallow().find('#policy-violation-close-btn');
      expect(btn).toExist();
      btn.simulate('click');
      expect(minimalProps.onClose).toHaveBeenCalled();
    });
  });
});
