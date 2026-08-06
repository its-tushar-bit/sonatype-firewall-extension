/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FilterBar, FilterInsertRequest } from 'MainRoot/nosc/search/FilterBar';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

beforeAll(() => installRadixJsdomShims());

/**
 * CLM-42453 PR #4: the filter builder's own behavior — the category row it
 * presents, and the syntax it hands back when a leaf (plain, quoted, or
 * enum-valued) is chosen. The insertion maths lives in searchFilterInsert, the
 * vocabulary in searchFilterTree, and each has its own spec.
 */
function renderBar(onInsert: (r: FilterInsertRequest) => void): void {
  render(
    <Theme>
      <FilterBar onInsert={onInsert} />
    </Theme>,
  );
}

/**
 * Walk the open category menu to the named enum leaf with ArrowDown and open its
 * value flyout with ArrowRight — the keyboard path a user takes, and the only one
 * Radix's submenus respond to under jsdom (hover needs real pointer geometry).
 */
async function openFlyout(user: ReturnType<typeof userEvent.setup>, name: RegExp): Promise<void> {
  const trigger = await screen.findByRole('menuitem', { name });
  // One ArrowDown per item, so the walk cannot run out of steps before it
  // reaches the trigger however long the menu grows.
  const steps = screen.getAllByRole('menuitem').length;
  for (let i = 0; i < steps && document.activeElement !== trigger; i++) {
    await user.keyboard('{ArrowDown}');
  }
  expect(trigger).toHaveFocus();
  await user.keyboard('{ArrowRight}');
}

const CATEGORIES = [
  'Type',
  'Application',
  'Component',
  'License',
  'Organization',
  'Policy',
  'Violation',
  'Vulnerability',
];

describe('FilterBar', () => {
  it('presents the eight categories as buttons in a labelled toolbar', () => {
    renderBar(jest.fn());
    const toolbar = screen.getByRole('toolbar', { name: /search filters/i });
    expect(
      within(toolbar)
        .getAllByRole('button')
        .map((b) => b.textContent),
    ).toEqual(CATEGORIES);
  });

  it('opens a category menu on click and lists its leaves', async () => {
    const user = userEvent.setup();
    renderBar(jest.fn());
    await user.click(screen.getByRole('button', { name: /^Type$/ }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByRole('menuitem', { name: /Application/ })).toBeInTheDocument();
  });

  it('reports the chosen leaf syntax for a complete leaf', async () => {
    const user = userEvent.setup();
    const onInsert = jest.fn();
    renderBar(onInsert);
    await user.click(screen.getByRole('button', { name: /^Type$/ }));
    await user.click(await screen.findByRole('menuitem', { name: /Application/ }));
    expect(onInsert).toHaveBeenCalledWith({ syntax: 'itemType:APPLICATION', label: 'Application' });
  });

  it('reports a quoted leaf so the caller can place the caret inside the quotes', async () => {
    const user = userEvent.setup();
    const onInsert = jest.fn();
    renderBar(onInsert);
    await user.click(screen.getByRole('button', { name: /^Application$/ }));
    await user.click(await screen.findByRole('menuitem', { name: /^Name/ }));
    expect(onInsert).toHaveBeenCalledWith({ syntax: 'applicationName:""', label: 'Name' });
  });

  it('resolves an enum leaf through its value flyout and reports field:value', async () => {
    const user = userEvent.setup();
    const onInsert = jest.fn();
    renderBar(onInsert);
    await user.click(screen.getByRole('button', { name: /^Vulnerability$/ }));
    // Radix drives submenus off the keyboard in jsdom: arrow to the value
    // flyout's trigger, open it with ArrowRight, then activate a value.
    await openFlyout(user, /Evaluation Stage/);
    await user.click(await screen.findByRole('menuitem', { name: /^build/ }));
    expect(onInsert).toHaveBeenCalledWith({
      syntax: 'policyEvaluationStage:build',
      label: 'Evaluation Stage = build',
    });
  });

  it('double-quotes a multi-word enum value so it inserts as one fielded phrase', async () => {
    const user = userEvent.setup();
    const onInsert = jest.fn();
    renderBar(onInsert);
    await user.click(screen.getByRole('button', { name: /^Vulnerability$/ }));
    await openFlyout(user, /^Status/);
    await user.click(await screen.findByRole('menuitem', { name: /^Not Applicable/ }));
    expect(onInsert).toHaveBeenCalledWith({
      syntax: 'vulnerabilityStatus:"Not Applicable"',
      label: 'Status = Not Applicable',
    });
  });

  it('notifies the caller when a menu is dismissed without a choice so focus can be returned', async () => {
    const user = userEvent.setup();
    const onMenuClose = jest.fn();
    render(
      <Theme>
        <FilterBar onInsert={jest.fn()} onMenuClose={onMenuClose} />
      </Theme>,
    );
    await user.click(screen.getByRole('button', { name: /^Type$/ }));
    await screen.findByRole('menu');
    await user.keyboard('{Escape}');
    expect(onMenuClose).toHaveBeenCalled();
  });

  it('does not notify onMenuClose when the menu closes because a leaf was chosen', async () => {
    const user = userEvent.setup();
    const onMenuClose = jest.fn();
    render(
      <Theme>
        <FilterBar onInsert={jest.fn()} onMenuClose={onMenuClose} />
      </Theme>,
    );
    await user.click(screen.getByRole('button', { name: /^Type$/ }));
    await user.click(await screen.findByRole('menuitem', { name: /Application/ }));
    expect(onMenuClose).not.toHaveBeenCalled();
  });
});
