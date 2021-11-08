/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxSelectableTag } from '@sonatype/react-shared-components';

import * as enzymeUtils from 'TestRoot/enzymeUtils';
import TransferListHalf from 'MainRoot/componentDetails/TransferList/TransferListHalf';

describe('TransferListHalf', () => {
  let minimalProps, getMounted, getShallow, onChangeMock;

  beforeEach(function () {
    onChangeMock = jasmine.createSpy('onItemChange');

    minimalProps = {
      title: 'Test',
      isInAvailableItems: true,
      items: [],
      onItemChange: onChangeMock,
    };

    getShallow = enzymeUtils.getShallowComponent(TransferListHalf, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(TransferListHalf, minimalProps);
  });

  it('renders two NxSelectableTag with correct props', () => {
    const component = getMounted({
      items: [
        { id: 1, color: 'dark-blue', label: 'test' },
        { id: 2, color: 'light-blue', label: 'test 2' },
      ],
    });
    const labels = component.find('.iq-transfer-list__item-list');
    const first = labels.find(NxSelectableTag).at(0);
    const second = labels.find(NxSelectableTag).at(1);

    expect(first).toExist();
    expect(first).toHaveText('test');
    expect(first).toHaveProp('color', 'blue');
    expect(first).toHaveProp('selected', false);

    expect(second).toExist();
    expect(second).toHaveText('test 2');
    expect(second).toHaveProp('color', 'light-blue');
    expect(second).toHaveProp('selected', false);
  });

  it('onSelect triggers add label handler', () => {
    const component = getShallow({
      items: [{ id: 1, color: 'dark-blue', label: 'test' }],
    });

    const labels = component.find('.iq-transfer-list__item-list');
    const first = labels.find(NxSelectableTag).at(0);

    first.invoke('onSelect')();
    expect(onChangeMock).toHaveBeenCalledTimes(1);
  });
});
