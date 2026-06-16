/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  fetchNoscWaiverDetail,
  fetchNoscWaiversList,
  resetNoscWaiversList,
  selectNoscWaiversListHasEntry,
  selectNoscWaiversListState,
  waiversListKey,
} from 'MainRoot/nosc/waivers/noscWaiversSlice';
import type { PolicyWaiverDTO } from 'MainRoot/nosc/waivers/waiverTypes';

function waiver(id: string): PolicyWaiverDTO {
  return {
    id,
    threatLevel: 5,
    ownerId: 'owner-1',
    ownerType: 'application',
    scope: 'Application: demo',
  };
}

describe('noscWaiversSlice', () => {
  it('stores list results under independent waiversListKey entries', () => {
    const globalKey = waiversListKey({ includeAutoWaivers: true });
    const appKey = waiversListKey({ applicationInternalId: 'app-1', includeAutoWaivers: true });

    let state = reducer(undefined, { type: '@@INIT' });
    state = reducer(
      state,
      fetchNoscWaiversList.fulfilled(
        { waivers: [waiver('w-1')], hasNextPage: false, listKey: globalKey },
        '',
        { includeAutoWaivers: true },
      ),
    );
    state = reducer(
      state,
      fetchNoscWaiversList.fulfilled(
        { waivers: [waiver('w-2')], hasNextPage: true, listKey: appKey },
        '',
        { applicationInternalId: 'app-1', includeAutoWaivers: true },
      ),
    );

    expect(selectNoscWaiversListState({ noscWaivers: state }, globalKey).waivers).toHaveLength(1);
    expect(selectNoscWaiversListState({ noscWaivers: state }, appKey).waivers).toHaveLength(1);
    expect(selectNoscWaiversListHasEntry({ noscWaivers: state }, globalKey)).toBe(true);
    expect(selectNoscWaiversListHasEntry({ noscWaivers: state }, appKey)).toBe(true);
  });

  it('evicts a single list entry when resetNoscWaiversList receives a key', () => {
    const appKey = waiversListKey({ applicationInternalId: 'app-1', includeAutoWaivers: true });
    const globalKey = waiversListKey({ includeAutoWaivers: true });

    let state = reducer(undefined, { type: '@@INIT' });
    state = reducer(
      state,
      fetchNoscWaiversList.fulfilled(
        { waivers: [waiver('w-1')], hasNextPage: false, listKey: appKey },
        '',
        { applicationInternalId: 'app-1', includeAutoWaivers: true },
      ),
    );
    state = reducer(
      state,
      fetchNoscWaiversList.fulfilled(
        { waivers: [waiver('w-2')], hasNextPage: false, listKey: globalKey },
        '',
        { includeAutoWaivers: true },
      ),
    );

    state = reducer(state, resetNoscWaiversList(appKey));

    expect(selectNoscWaiversListHasEntry({ noscWaivers: state }, appKey)).toBe(false);
    expect(selectNoscWaiversListHasEntry({ noscWaivers: state }, globalKey)).toBe(true);
  });

  it('caps cached list entries, evicting oldest non-active keys', () => {
    let state = reducer(undefined, { type: '@@INIT' });
    // Drive 40 distinct request tuples through pending+fulfilled; the cap is 32.
    for (let page = 0; page < 40; page += 1) {
      const request = { applicationInternalId: `app-${page}`, includeAutoWaivers: true };
      const key = waiversListKey(request);
      state = reducer(state, fetchNoscWaiversList.pending('', request));
      state = reducer(
        state,
        fetchNoscWaiversList.fulfilled(
          { waivers: [waiver(`w-${page}`)], hasNextPage: false, listKey: key },
          '',
          request,
        ),
      );
    }

    const remaining = Object.keys(state.listsByKey).length;
    expect(remaining).toBeLessThanOrEqual(32);
    // The most recent request must survive eviction.
    const newestKey = waiversListKey({ applicationInternalId: 'app-39', includeAutoWaivers: true });
    expect(selectNoscWaiversListHasEntry({ noscWaivers: state }, newestKey)).toBe(true);
    // The oldest request must have been evicted.
    const oldestKey = waiversListKey({ applicationInternalId: 'app-0', includeAutoWaivers: true });
    expect(selectNoscWaiversListHasEntry({ noscWaivers: state }, oldestKey)).toBe(false);
  });

  it('returns stable singleton for missing list keys', () => {
    const state = reducer(undefined, { type: '@@INIT' });
    const a = selectNoscWaiversListState({ noscWaivers: state }, 'missing-a');
    const b = selectNoscWaiversListState({ noscWaivers: state }, 'missing-b');
    // Singleton prevents unnecessary re-renders in useSelector.
    expect(a).toBe(b);
  });

  it('ignores stale waiver detail responses when activeKey changed', () => {
    const staleKey = 'application|owner-1|waiver-a';
    const currentKey = 'application|owner-1|waiver-b';
    let state = reducer(undefined, { type: '@@INIT' });
    state = reducer(
      state,
      fetchNoscWaiverDetail.pending('', {
        ownerType: 'application',
        ownerId: 'owner-1',
        waiverId: 'waiver-b',
      }),
    );
    expect(state.detail.activeKey).toBe(currentKey);

    state = reducer(
      state,
      fetchNoscWaiverDetail.fulfilled(
        {
          waiver: {
            id: 'waiver-a',
            threatLevel: 5,
            ownerId: 'owner-1',
            ownerType: 'application',
            scope: 'Application: demo',
          },
          detailKey: staleKey,
        },
        '',
        { ownerType: 'application', ownerId: 'owner-1', waiverId: 'waiver-a' },
      ),
    );

    expect(state.detail.status).toBe('loading');
    expect(state.detail.waiver).toBeNull();
  });
});
