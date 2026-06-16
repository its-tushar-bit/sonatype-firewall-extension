/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Provider } from 'react-redux';
import { renderHook, waitFor } from '@testing-library/react';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import { configureStore } from 'TestRoot/SpecUtil';
import reducers from 'MainRoot/reduxConfig/reducers';
import { useWaiversList } from 'MainRoot/nosc/waivers/useWaivers';
import {
  selectNoscWaiversListHasEntry,
  waiversListKey,
} from 'MainRoot/nosc/waivers/noscWaiversSlice';

/**
 * CLM-40901 (Anastasia round-2): the keyed waivers cache must not be torn down
 * by a single consumer's unmount. Two consumers sharing a listKey rely on the
 * entry surviving so the survivor never flashes a spurious loading state.
 */
describe('useWaiversList — shared keyed cache', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(axios);
    mock.onPost(/.*/).reply(200, { dashboardResults: [], hasNextPage: false });
  });

  afterEach(() => mock.restore());

  function makeStore() {
    return configureStore({ reducer: reducers });
  }

  it('does not delete the shared cache entry when one co-mounted consumer unmounts', async () => {
    const store = makeStore();
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <Provider store={store}>{children}</Provider>
    );
    const options = { applicationInternalId: 'app-1', includeAutoWaivers: true };
    const key = waiversListKey(options);

    const first = renderHook(() => useWaiversList(options), { wrapper });
    const second = renderHook(() => useWaiversList(options), { wrapper });

    await waitFor(() =>
      expect(selectNoscWaiversListHasEntry(store.getState(), key)).toBe(true),
    );

    // One consumer unmounts — the survivor still needs the entry.
    first.unmount();

    expect(selectNoscWaiversListHasEntry(store.getState(), key)).toBe(true);
    expect(second.result.current.loading).toBe(false);

    second.unmount();
  });
});
