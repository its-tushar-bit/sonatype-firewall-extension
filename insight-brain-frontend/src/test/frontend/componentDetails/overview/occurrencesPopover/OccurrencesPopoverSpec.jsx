/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxList, NxButton } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../../enzymeUtils';
import OccurrencesPopover from '../../../../../main/frontend/componentDetails/overview/occurrencesPopover/OccurrencesPopover';
import IqPopover from '../../../../../main/frontend/react/IqPopover';
import * as componentDetailsUtils from '../../../../../main/frontend/componentDetails/componentDetailsUtils';

describe('OccurrencesPopover', function () {
  let minimalProps, getShallowComponent, onCloseSpy;

  beforeEach(function () {
    spyOn(componentDetailsUtils, 'parseOccurrencePathname').and.callThrough();
    onCloseSpy = jasmine.createSpy('onClose');
    minimalProps = {
      occurrences: [
        'Container.zip/Inner/double_inner/component1.jar',
        'Container.zip/Inner/double_inner/component2.jar',
      ],
      onClose: onCloseSpy,
      showOccurrencesPopover: true,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(OccurrencesPopover, minimalProps);
  });

  it('renders an IqPopover if `showOccurrencesPopover` is true', () => {
    let component = getShallowComponent({ showOccurrencesPopover: false });
    expect(component).toBeEmptyRender();

    component = getShallowComponent();
    expect(component).toMatchSelector(IqPopover);
    expect(component.find('.iq-popover-header__title-text')).toHaveText('Occurrences');
  });

  it('renders a Popover with close button', () => {
    const component = getShallowComponent();
    expect(component).toMatchSelector(IqPopover);
    const closeBtn = component.find(NxButton);
    expect(closeBtn).toHaveProp('onClick', minimalProps.onClose);

    closeBtn.simulate('click');
    expect(onCloseSpy).toHaveBeenCalled();
  });

  it('renders an NxList with Occurrence elements', () => {
    const list = getShallowComponent().find(NxList);
    expect(list).toExist();
    expect(list.childAt(0)).toHaveProp(
      'occurrence',
      componentDetailsUtils.parseOccurrencePathname(minimalProps.occurrences[0])
    );
    expect(list.childAt(1)).toHaveProp(
      'occurrence',
      componentDetailsUtils.parseOccurrencePathname(minimalProps.occurrences[1])
    );
  });
});
