/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import PolicyViolationDetailsPopover from '../../../../main/frontend/componentDetails/violations/PolicyViolationDetailsPopover';
import ViolationPageContainer from '../../../../main/frontend/violation/ViolationPageContainer';

describe('PolicyViolationDetailsPopover', () => {
  let minimalProps, getShallow, onCloseSpy;

  beforeEach(function () {
    onCloseSpy = jasmine.createSpy('onClose');
    minimalProps = {
      violationsDetailProps: {},
      onClose: onCloseSpy,
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolationDetailsPopover, minimalProps);
  });

  describe('clicks the close button and calls the appropiate onClose', () => {
    it('clicks on a row outside of the button and calls the setShowViolationsDetail action', () => {
      const closeBtn = getShallow().find('#policy-violation-close-btn');
      closeBtn.simulate('click');
      expect(onCloseSpy).toHaveBeenCalledTimes(1);
    });
  });

  it('renders the ViolationPage component', () => {
    const component = getShallow();
    const violationPageComp = component.find(ViolationPageContainer);
    expect(violationPageComp).toExist();
  });
});
