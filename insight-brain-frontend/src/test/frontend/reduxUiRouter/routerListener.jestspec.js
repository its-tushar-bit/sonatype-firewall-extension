/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { initializeRouterListener } from '../../../main/frontend/reduxUiRouter/routerListener';
import store from '../../../main/frontend/reduxConfig/store';

describe('routerListener', () => {
  let mockTransitions, mockTransition, dispatchSpy;

  beforeEach(() => {
    dispatchSpy = jest.spyOn(store, 'dispatch');

    // Mock ui-router transitionService
    mockTransitions = {
      onFinish: jest.fn((query, callback) => {
        // Store callback for manual triggering
        mockTransitions._callback = callback;
      }),
    };

    mockTransition = {
      to: jest.fn().mockReturnValue({ name: 'to-state' }),
      from: jest.fn().mockReturnValue({ name: 'from-state' }),
      params: jest.fn((key) => {
        const params = {
          to: 'to-params',
          from: 'from-params',
        };
        return params[key];
      }),
    };
  });

  afterEach(() => {
    dispatchSpy.mockRestore();
  });

  it('listens to onFinish transition event and dispatches UI_ROUTER_ON_FINISH action', () => {
    initializeRouterListener(mockTransitions);

    expect(mockTransitions.onFinish).toHaveBeenCalledWith({}, expect.any(Function));
    expect(dispatchSpy).not.toHaveBeenCalled();

    // Trigger onFinish transition event by calling the registered callback
    const callback = mockTransitions.onFinish.mock.calls[0][1];
    callback(mockTransition);

    expect(dispatchSpy).toHaveBeenCalledTimes(1);
    expect(dispatchSpy).toHaveBeenCalledWith({
      type: '@@reduxUiRouter/onFinish',
      payload: {
        toState: { name: 'to-state' },
        toParams: 'to-params',
        fromState: { name: 'from-state' },
        fromParams: 'from-params',
      },
    });
  });
});
