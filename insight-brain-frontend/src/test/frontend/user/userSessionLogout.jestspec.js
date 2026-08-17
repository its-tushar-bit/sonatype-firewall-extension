/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getSessionLogoutUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/user/userSessionSlice';
import defaultStore from 'MainRoot/reduxConfig/store';
import { logoutRedirection } from 'MainRoot/util/urlUtil';

// Nexus One never calls setUrlService, so pendoService stays null for the page lifetime.
jest.mock('MainRoot/pendo/mainBundlePendoService', () => ({
  __esModule: true,
  default: null,
}));

jest.mock('MainRoot/util/urlUtil', () => {
  const actual = jest.requireActual('MainRoot/util/urlUtil');
  return {
    __esModule: true,
    ...actual,
    logoutRedirection: jest.fn(),
  };
});

describe('userSession logout (pendo uninitialized — Nexus One bundle)', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
    logoutRedirection.mockClear();
    defaultStore.dispatch(actions.resetUserSession());
  });

  it('still DELETEs the session and redirects when pendoService is null', async () => {
    axiosMock.onDelete(getSessionLogoutUrl()).reply(204, null, {});

    await expect(defaultStore.dispatch(actions.logout())).resolves.toMatchObject({
      type: 'userSession/logout/fulfilled',
    });

    expect(axiosMock.history.delete).toHaveLength(1);
    expect(axiosMock.history.delete[0].url).toBe(getSessionLogoutUrl());
    expect(logoutRedirection).toHaveBeenCalledTimes(1);
    expect(logoutRedirection).toHaveBeenCalledWith(undefined);
  });

  it('follows the Location header for SSO/SAML logout', async () => {
    const idpUrl = 'https://idp.example/v2/logout?returnTo=http://localhost:8070/';
    axiosMock.onDelete(getSessionLogoutUrl()).reply(204, null, { location: idpUrl });

    await defaultStore.dispatch(actions.logout());

    expect(logoutRedirection).toHaveBeenCalledWith(idpUrl);
  });
});
