/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { act } from '@testing-library/react';
import { render, screen, waitFor, within } from '../../../test-utils';
import { OwnerAdapterProvider } from 'GuideRoot/components/navigation/context-picker/OwnerAdapterProvider';
import { PolicyContextProvider } from 'GuideRoot/components/navigation/context-picker/PolicyContext';
import { PolicyContextPicker } from 'GuideRoot/components/navigation/context-picker/PolicyContextPicker';
import {
  MOCK_MISSING_OWNER_ID,
  MockOwnerAdapter,
  type MockOwnerData,
} from 'GuideRoot/api/context-picker/MockOwnerAdapter';
import type { Owner, OrgAppsResult } from 'GuideRoot/components/navigation/context-picker/types';

/**
 * MockOwnerAdapter whose getAppsForOrg stays pending until {@link settle} is called, so a test can
 * interleave two drills and resolve them out of order. Honors the AbortSignal (rejects with
 * AbortError on abort) exactly as apiFetch would, exercising the picker's per-drill cancellation.
 */
class DeferredAppsAdapter extends MockOwnerAdapter {
  private readonly resolvers = new Map<string, (r: OrgAppsResult) => void>();

  getAppsForOrg(orgId: string, limit: number, signal?: AbortSignal): Promise<OrgAppsResult> {
    return new Promise<OrgAppsResult>((resolve, reject) => {
      signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
      this.resolvers.set(orgId, resolve);
    });
  }

  /** Resolve a previously-requested org's pending fetch with the real fixture data. */
  async settle(orgId: string): Promise<void> {
    const resolve = this.resolvers.get(orgId);
    if (!resolve) {
      return;
    }
    resolve(await super.getAppsForOrg(orgId, 500));
  }
}

/**
 * Component tests for the policy-context picker, driven through the trigger + modal against the
 * in-memory {@link MockOwnerAdapter}. Covers: modal-open → top-orgs fetch, drill → apps fetch,
 * 0-app orgs selecting directly, debounced global search, the empty-permission state, keyboard
 * navigation (ArrowDown into the list, Esc clear-then-close), and localStorage rehydrate.
 */

function renderPicker(adapter: MockOwnerAdapter = new MockOwnerAdapter()) {
  const result = render(
    <OwnerAdapterProvider adapter={adapter}>
      <PolicyContextProvider>
        <PolicyContextPicker />
      </PolicyContextProvider>
    </OwnerAdapterProvider>
  );
  return { adapter, ...result };
}

function trigger() {
  return screen.getByRole('button', { name: /Policy context — open picker/ });
}

describe('PolicyContextPicker', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('shows Root Organization when nothing is selected', () => {
    renderPicker();
    expect(trigger()).toHaveTextContent('Root Organization');
  });

  it('fetches top orgs on open and renders orgs with the "+ N more" hint', async () => {
    const user = userEvent.setup();
    const { adapter } = renderPicker();
    const topOrgsSpy = jest.spyOn(adapter, 'getTopOrgs');

    await user.click(trigger());

    await waitFor(() => expect(screen.getByRole('option', { name: /^Payments,/ })).toBeInTheDocument());
    expect(topOrgsSpy).toHaveBeenCalledWith(20);
    // 25 fixture orgs, limit 20 → 5 truncated.
    expect(screen.getByText(/\+ 5 more/)).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Root Organization/ })).toBeInTheDocument();
  });

  it('renders the ancestor path as an org-row subtitle', async () => {
    const user = userEvent.setup();
    renderPicker();
    await user.click(trigger());
    // Frontend is nested under Payments → its row is labelled with the breadcrumb.
    await waitFor(() =>
      expect(screen.getByRole('option', { name: /^Frontend, Payments,/ })).toBeInTheDocument()
    );
  });

  it('drills into an org with apps and fetches its applications', async () => {
    const user = userEvent.setup();
    const { adapter } = renderPicker();
    const appsSpy = jest.spyOn(adapter, 'getAppsForOrg');

    await user.click(trigger());
    await waitFor(() => screen.getByRole('option', { name: /^Payments,/ }));
    await user.click(screen.getByRole('option', { name: /^Payments,/ }));

    await waitFor(() => expect(screen.getByText('Select entire organization')).toBeInTheDocument());
    expect(appsSpy).toHaveBeenCalledWith('payments', 500, expect.any(AbortSignal));
    expect(screen.getByText('payment-service')).toBeInTheDocument();
    expect(screen.getByText('billing-gateway')).toBeInTheDocument();
  });

  it('discards a superseded apps fetch so the list matches the drilled org, not a slow earlier one', async () => {
    const user = userEvent.setup();
    const adapter = new DeferredAppsAdapter();
    renderPicker(adapter);
    await user.click(trigger());

    // Drill Payments — fetch A starts and stays pending (still "Loading applications…").
    await user.click(await screen.findByRole('option', { name: /^Payments,/ }));
    await waitFor(() => expect(screen.getByText(/Loading applications/)).toBeInTheDocument());

    // Back to the org list, then drill Frontend — starting fetch B aborts the pending fetch A.
    await user.click(screen.getByRole('button', { name: 'Back to all organizations' }));
    await user.click(await screen.findByRole('option', { name: /^Frontend, Payments,/ }));

    // Resolve the superseded Payments fetch first (out of order), then the current Frontend fetch.
    await adapter.settle('payments');
    await adapter.settle('frontend');

    // Frontend's apps render; Payments' apps must never leak in under the Frontend header.
    await waitFor(() => expect(screen.getByText('checkout-app')).toBeInTheDocument());
    expect(screen.queryByText('payment-service')).not.toBeInTheDocument();
    expect(screen.queryByText('billing-gateway')).not.toBeInTheDocument();
  });

  it('selects a 0-app org directly without drilling', async () => {
    const user = userEvent.setup();
    const { adapter } = renderPicker();
    const appsSpy = jest.spyOn(adapter, 'getAppsForOrg');

    await user.click(trigger());
    await waitFor(() => screen.getByRole('option', { name: /^Engineering,/ }));
    await user.click(screen.getByRole('option', { name: /^Engineering,/ }));

    // Modal closed and the trigger now reflects the selection.
    await waitFor(() => expect(trigger()).toHaveTextContent('Engineering'));
    expect(within(trigger()).getByText('org')).toBeInTheDocument();
    expect(appsSpy).not.toHaveBeenCalled();
  });

  it('debounces the global search into a single request and flags truncation', async () => {
    // Fake timers so the assertion is driven by the debounce window elapsing, not real wall-clock
    // time via waitFor polling (avoids the real-timer flakiness-under-load pattern flagged elsewhere).
    jest.useFakeTimers();
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    try {
      const { adapter } = renderPicker();
      const searchSpy = jest.spyOn(adapter, 'searchOwners');

      await user.click(trigger());
      await act(async () => {}); // flush the synchronous getTopOrgs load
      expect(screen.getByRole('option', { name: /^Payments,/ })).toBeInTheDocument();

      await user.type(screen.getByRole('textbox', { name: 'Search policy context' }), 'platform');
      // No request fires until the debounce window elapses.
      expect(searchSpy).not.toHaveBeenCalled();

      await act(async () => {
        jest.advanceTimersByTime(275);
      });

      // A single request for the settled query; 11 orgs match "platform" (> limit 10) → truncated.
      expect(searchSpy).toHaveBeenCalledTimes(1);
      expect(searchSpy).toHaveBeenCalledWith('platform', 'all', 10);
      expect(screen.getByText(/More results available/)).toBeInTheDocument();
    } finally {
      jest.useRealTimers();
    }
  });

  it('cancels the in-flight search when the search view is torn down', async () => {
    const user = userEvent.setup();
    const { adapter } = renderPicker();
    const cancelSpy = jest.spyOn(adapter, 'cancelSearch');

    await user.click(trigger());
    await waitFor(() => screen.getByRole('option', { name: /^Payments,/ }));
    await user.type(screen.getByRole('textbox', { name: 'Search policy context' }), 'platform');
    await waitFor(() => expect(screen.getByText(/More results available/)).toBeInTheDocument());

    cancelSpy.mockClear();
    // Escape clears the query → leaves the search view → the search effect cleanup aborts the request.
    await user.keyboard('{Escape}');
    await waitFor(() => expect(cancelSpy).toHaveBeenCalled());
  });

  it('shows the empty-permission state when no orgs are returned', async () => {
    const user = userEvent.setup();
    const emptyData: MockOwnerData = { orgs: [], apps: [] };
    renderPicker(new MockOwnerAdapter(emptyData));

    await user.click(trigger());

    await waitFor(() =>
      expect(screen.getByText(/don't have policy-evaluation access on any organizations/i)).toBeInTheDocument()
    );
  });

  it('ArrowDown from the search field moves focus into the list', async () => {
    const user = userEvent.setup();
    renderPicker();
    await user.click(trigger());
    await waitFor(() => screen.getByRole('option', { name: /Root Organization/ }));

    const searchField = screen.getByRole('textbox', { name: 'Search policy context' });
    searchField.focus();
    await user.keyboard('{ArrowDown}');

    expect(document.activeElement).toHaveAttribute('role', 'option');
  });

  it('uses roving tabindex: one option is tabbable and the cursor follows arrow navigation', async () => {
    const user = userEvent.setup();
    renderPicker();
    await user.click(trigger());
    await waitFor(() => screen.getByRole('option', { name: /Root Organization/ }));

    const tabbable = () => screen.getAllByRole('option').filter((o) => o.getAttribute('tabindex') === '0');

    // Exactly one row is in the Tab order on open, and it is the first (Root Organization).
    expect(tabbable()).toHaveLength(1);
    expect(tabbable()[0]).toHaveTextContent('Root Organization');

    // Arrow into the list (focuses Root), then down once — the tabbable cursor tracks focus.
    screen.getByRole('textbox', { name: 'Search policy context' }).focus();
    await user.keyboard('{ArrowDown}');
    await user.keyboard('{ArrowDown}');

    expect(tabbable()).toHaveLength(1);
    expect(document.activeElement).toHaveAttribute('role', 'option');
    expect(document.activeElement).toHaveAttribute('tabindex', '0');
    expect(document.activeElement).not.toHaveTextContent('Root Organization');
  });

  it('Escape clears an active search first, then closes the modal', async () => {
    const user = userEvent.setup();
    renderPicker();
    await user.click(trigger());
    await waitFor(() => screen.getByRole('option', { name: /^Payments,/ }));

    const searchField = screen.getByRole('textbox', { name: 'Search policy context' });
    await user.type(searchField, 'team');
    await waitFor(() => expect(searchField).toHaveValue('team'));

    await user.keyboard('{Escape}');
    expect(searchField).toHaveValue('');
    expect(screen.getByRole('textbox', { name: 'Search policy context' })).toBeInTheDocument();

    await user.keyboard('{Escape}');
    await waitFor(() =>
      expect(screen.queryByRole('textbox', { name: 'Search policy context' })).not.toBeInTheDocument()
    );
  });

  it('rehydrates a persisted selection and shows its breadcrumb on modal open', async () => {
    const user = userEvent.setup();
    localStorage.setItem('guide.policyOwner', JSON.stringify('app-checkout'));
    renderPicker();

    // Trigger label reflects the restored app once resolveOwner resolves.
    await waitFor(() => expect(trigger()).toHaveTextContent('checkout-app'));

    await user.click(trigger());
    const banner = await screen.findByLabelText('Current policy context');
    expect(banner).toHaveTextContent('Payments / Frontend / checkout-app');
  });

  it('persists the selected owner id to localStorage', async () => {
    const user = userEvent.setup();
    renderPicker();
    await user.click(trigger());
    await waitFor(() => screen.getByRole('option', { name: /^Engineering,/ }));
    await user.click(screen.getByRole('option', { name: /^Engineering,/ }));

    await waitFor(() => expect(trigger()).toHaveTextContent('Engineering'));
    expect(localStorage.getItem('guide.policyOwner')).toBe(JSON.stringify('engineering'));
  });

  it('does not let a late rehydrate clobber a selection the user made first', async () => {
    const user = userEvent.setup();
    localStorage.setItem('guide.policyOwner', JSON.stringify('payments'));
    const adapter = new MockOwnerAdapter();
    let resolveStored!: (owner: Owner | null) => void;
    jest
      .spyOn(adapter, 'resolveOwner')
      .mockReturnValue(new Promise<Owner | null>((resolve) => (resolveStored = resolve)));
    renderPicker(adapter);

    // Before the stored 'payments' rehydrate resolves, the user opens the picker and picks Engineering.
    await user.click(trigger());
    await user.click(await screen.findByRole('option', { name: /^Engineering,/ }));
    await waitFor(() => expect(trigger()).toHaveTextContent('Engineering'));

    // The stale rehydrate now resolves late — it must not overwrite the user's fresh selection.
    const payments = await new MockOwnerAdapter().resolveOwner('payments');
    await act(async () => {
      resolveStored(payments);
    });
    expect(trigger()).toHaveTextContent('Engineering');
    expect(localStorage.getItem('guide.policyOwner')).toBe(JSON.stringify('engineering'));
  });

  it('clears a stale persisted selection (resolveOwner → null) and falls back to root', async () => {
    localStorage.setItem('guide.policyOwner', JSON.stringify(MOCK_MISSING_OWNER_ID));
    const adapter = new MockOwnerAdapter();
    const resolveSpy = jest.spyOn(adapter, 'resolveOwner');
    renderPicker(adapter);

    // The missing id resolves to null (not-found / lost permission) → stored value is removed.
    await waitFor(() => expect(resolveSpy).toHaveBeenCalledWith(MOCK_MISSING_OWNER_ID));
    await waitFor(() => expect(localStorage.getItem('guide.policyOwner')).toBeNull());
    expect(trigger()).toHaveTextContent('Root Organization');
  });

  it('keeps the persisted selection on a transient (non-404) resolve failure and stays on root', async () => {
    localStorage.setItem('guide.policyOwner', JSON.stringify('engineering'));
    const adapter = new MockOwnerAdapter();
    const resolveSpy = jest
      .spyOn(adapter, 'resolveOwner')
      .mockRejectedValue(new Error('network unavailable'));
    renderPicker(adapter);

    // A transient failure must NOT discard the stored id — it is kept for a later retry, root shown meanwhile.
    await waitFor(() => expect(resolveSpy).toHaveBeenCalledWith('engineering'));
    expect(trigger()).toHaveTextContent('Root Organization');
    expect(localStorage.getItem('guide.policyOwner')).toBe(JSON.stringify('engineering'));
  });

  it('clears the persisted selection when Root Organization is selected', async () => {
    localStorage.setItem('guide.policyOwner', JSON.stringify('engineering'));
    const user = userEvent.setup();
    renderPicker();
    // Restored to Engineering first, then switch back to root.
    await waitFor(() => expect(trigger()).toHaveTextContent('Engineering'));

    await user.click(trigger());
    await user.click(await screen.findByRole('option', { name: /Root Organization/ }));

    await waitFor(() => expect(trigger()).toHaveTextContent('Root Organization'));
    expect(localStorage.getItem('guide.policyOwner')).toBeNull();
  });
});
