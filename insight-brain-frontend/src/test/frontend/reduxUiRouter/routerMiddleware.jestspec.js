/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import routerMiddleware, { setStateService } from '../../../main/frontend/reduxUiRouter/routerMiddleware';

describe('routerMiddleware', () => {
  let mockStateService, next, middleware;

  beforeEach(() => {
    next = jest.fn().mockReturnValue('nextReturnValue');

    // Mock React ui-router stateService
    mockStateService = {
      go: jest.fn().mockReturnValue(Promise.resolve()),
      reload: jest.fn().mockReturnValue(Promise.resolve()),
      transitionTo: jest.fn().mockReturnValue(Promise.resolve()),
      includes: jest.fn(),
    };

    // Initialize the middleware with the mock state service
    setStateService(mockStateService);
    middleware = routerMiddleware()(next);
  });

  it('calls stateService.go on @@reduxUiRouter/stateGo actions, and passes action to next middleware', async () => {
    const action = {
      type: '@@reduxUiRouter/stateGo',
      payload: {
        to: 'toState',
        params: 'testParams',
        options: 'testOptions',
      },
    };

    mockStateService.go.mockResolvedValue(undefined);

    const promise = middleware(action);

    expect(mockStateService.go).toHaveBeenCalledWith('toState', 'testParams', 'testOptions');

    // Wait for promise to resolve
    await promise;
    expect(next).toHaveBeenCalledWith(action);
  });

  it('calls stateService.reload on @@reduxUiRouter/stateReload actions, and passes action to next middleware', async () => {
    const action = {
      type: '@@reduxUiRouter/stateReload',
      payload: 'state to reload',
    };

    mockStateService.reload.mockResolvedValue(undefined);

    const promise = middleware(action);

    expect(mockStateService.reload).toHaveBeenCalledWith('state to reload');

    // Wait for promise to resolve
    await promise;
    expect(next).toHaveBeenCalledWith(action);
  });

  it('calls stateService.reload with undefined when no state specified', async () => {
    const action = {
      type: '@@reduxUiRouter/stateReload',
      payload: undefined,
    };

    mockStateService.reload.mockResolvedValue(undefined);

    const promise = middleware(action);

    expect(mockStateService.reload).toHaveBeenCalledWith(undefined);

    // Wait for promise to resolve
    await promise;
    expect(next).toHaveBeenCalledWith(action);
  });

  it('calls stateService.transitionTo on @@reduxUiRouter/transitionTo actions, and passes action to next middleware', async () => {
    const action = {
      type: '@@reduxUiRouter/transitionTo',
      payload: {
        to: 'toState',
        params: 'testParams',
        options: 'testOptions',
      },
    };

    mockStateService.transitionTo.mockResolvedValue(undefined);

    const promise = middleware(action);

    expect(mockStateService.transitionTo).toHaveBeenCalledWith('toState', 'testParams', 'testOptions');

    // Wait for promise to resolve
    await promise;
    expect(next).toHaveBeenCalledWith(action);
  });
});
