/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import { NxInfoAlert } from '@sonatype/react-shared-components';
import InnerSourceProducerAlert from '../../../../../main/frontend/componentDetails/overview/InnerSourceProducerAlert/InnerSourceProducerAlert';

describe('InnerSourceProducerAlert', () => {
  let minimalProps, getShallowComponent, onClickSpy;

  beforeEach(() => {
    onClickSpy = jasmine.createSpy('onClick');

    minimalProps = {
      onClick: onClickSpy,
      isInnerSource: true,
      innerSourceProducerData: {
        loading: false,
        loadError: null,
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(InnerSourceProducerAlert, minimalProps);
  });

  it('should not render if the component isn"t innersource', () => {
    const wrapper = getShallowComponent({
      isInnerSource: false,
    });

    expect(wrapper.isEmptyRender()).toBe(true);
  });

  it('should not render if the producer data is still loading', () => {
    const wrapper = getShallowComponent({
      innerSourceProducerData: {
        loading: true,
        loadError: null,
      },
    });

    expect(wrapper.isEmptyRender()).toBe(true);
  });

  it('should not render if loading error exist', () => {
    const wrapper = getShallowComponent({
      innerSourceProducerData: {
        loading: false,
        loadError: 'some error',
      },
    });

    expect(wrapper.isEmptyRender()).toBe(true);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders a single NxInfoAlert', () => {
    const wrapper = getShallowComponent();
    const alert = wrapper.find(NxInfoAlert);

    expect(alert).toExist();
  });

  it('renders a single a tag with text "View the latest report"', () => {
    const wrapper = getShallowComponent();
    const aTag = wrapper.find('a');

    expect(aTag).toExist();
    expect(aTag).toHaveText('View the latest report');
  });

  it('calls onClick when clicking on the a tag', () => {
    const wrapper = getShallowComponent();
    const aTag = wrapper.find('a');

    aTag.simulate('click');

    expect(onClickSpy).toHaveBeenCalledTimes(1);
  });
});
