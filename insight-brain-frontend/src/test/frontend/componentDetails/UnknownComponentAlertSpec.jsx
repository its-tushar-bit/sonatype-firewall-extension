/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import { NxButton } from '@sonatype/react-shared-components';

import UnkownComponentAlert from 'MainRoot/componentDetails/UnknownComponentAlert';

describe('UnkownComponentAlert', function () {
  let minimalProps, getShallowComponent, clickHandlerSpy;

  beforeEach(function () {
    clickHandlerSpy = jasmine.createSpy('clickHandler');
    minimalProps = { onClaimClick: clickHandlerSpy };

    getShallowComponent = enzymeUtils.getShallowComponent(UnkownComponentAlert, minimalProps);
  });

  it('renders an NxWarningAlert with two action buttons', () => {
    const alertEl = getShallowComponent();
    const claimButton = alertEl.find(NxButton).at(0);
    const addButton = alertEl.find(NxButton).at(1);

    expect(alertEl).toExist();
    expect(alertEl.children().first().text()).toEqual('The component is unknown.');

    expect(claimButton).toExist();
    expect(claimButton).toHaveProp('title', 'Claim Component');
    expect(claimButton.text()).toEqual('Claim Component');

    expect(addButton).toExist();
    expect(addButton).toHaveProp('title', 'Add Proprietary Component Matchers');
    expect(addButton.text()).toEqual('Add Proprietary Component Matchers');
  });

  describe('Claim button', () => {
    it('calls onClaimClick when clicked', () => {
      const alertEl = getShallowComponent();
      const claimButton = alertEl.find('#iq-component-details-unknown-component-claim');
      claimButton.simulate('click');

      expect(clickHandlerSpy).toHaveBeenCalled();
    });
  });
});
