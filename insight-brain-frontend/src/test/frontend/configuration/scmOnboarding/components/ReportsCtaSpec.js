/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { NxButton } from '@sonatype/react-shared-components';
import ReportsCta from '../../../../../main/frontend/configuration/scmOnboarding/components/ReportsCta';

describe('ReportsCta', function () {
  let getShallowComponent, mock$State;

  beforeEach(() => {
    mock$State = jasmine.createSpyObj('$state', ['go']);

    const minimalProps = { $state: mock$State, id: 'id' };

    getShallowComponent = enzymeUtils.getShallowComponent(ReportsCta, minimalProps);
  });

  it('renders button with given id', () => {
    const component = getShallowComponent(),
      button = component.find(NxButton);

    // when button is clicked
    button.invoke('onClick')();

    // then router is called
    expect(mock$State.go).toHaveBeenCalled();
  });

  it('renders button with given id', () => {
    const component = getShallowComponent(),
      button = component.find(NxButton);

    // expect id to match
    expect(button.props().id).toEqual('id');
  });
});
