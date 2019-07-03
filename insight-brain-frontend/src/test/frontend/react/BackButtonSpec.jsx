import { NxBackButton } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';
import BackButton from '../../../main/frontend/react/BackButton';

describe('BackButton (React)', function() {
  let mockState,
      stateGetSpy,
      stateHrefSpy,
      minimalProps,
      getShallowComponent;

  beforeEach(function() {
    mockState = {
      data: { title: 'BarBaz' }
    };
    stateGetSpy = jasmine.createSpy('get').and.returnValue(mockState);
    stateHrefSpy = jasmine.createSpy('href').and.returnValue('/foo');
    minimalProps = {
      text: 'Link Text',
      stateName: 'foo',
      $state: {
        get: stateGetSpy,
        href: stateHrefSpy
      }
    };

    getShallowComponent = enzymeUtils.getShallowComponent(BackButton, minimalProps);
  });

  it('renders an NxBackButton with the href and title from the state, and the specified text', function() {
    const component = getShallowComponent();

    expect(component).toMatchSelector(NxBackButton);
    expect(component).toHaveProp('href', '/foo');
    expect(component).toHaveProp('text', 'Link Text');
    expect(component).toHaveProp('targetPageTitle', 'BarBaz');

    expect(stateGetSpy).toHaveBeenCalledWith('foo');
    expect(stateHrefSpy).toHaveBeenCalledWith(mockState);
  });
});
