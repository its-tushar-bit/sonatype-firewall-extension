/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxButton, NxDateInput, NxDropdown, useToggle } from '@sonatype/react-shared-components';
import moment from 'moment';

import { actions } from './usageSlice';
import { selectPeriodPreset, selectPeriodRange } from './usageSelectors';
import { PERIOD_PRESETS, formatRangeLabel } from './periodPresets';

// Matches backend DateRangeValidator inclusive-day-count cap (ConsumptionResource passes 366).
const MAX_RANGE_DAYS = 366;

const inclusiveDayCount = (startIso, endIso) =>
  moment(endIso, 'YYYY-MM-DD').diff(moment(startIso, 'YYYY-MM-DD'), 'days') + 1;

export default function UsagePeriodFilter() {
  const dispatch = useDispatch();
  const preset = useSelector(selectPeriodPreset);
  const range = useSelector(selectPeriodRange);

  // useToggle's third return is the explicit setter. We need it because
  // RSC's AbstractDropdown registers a document-level click handler that
  // calls onToggleCollapse on EVERY bubbled click — including clicks on
  // inner menu items. If close() used toggleOpen() and the click bubbled,
  // we'd flip isOpen to false (our intended close) and then RSC's handler
  // would flip it back to true (re-opening), leaving the dropdown stuck open
  // after a preset selection. Explicit setIsOpen(false) is idempotent against
  // that race.
  const [isOpen, toggleOpen, setIsOpen] = useToggle(false);

  // 'menu' = preset list; 'custom' = date-input form
  const [mode, setMode] = useState('menu');
  const [draft, setDraft] = useState({ startDate: '', endDate: '' });
  // Inline error message for the custom-range form. null = no error. The
  // backend rejects start > end with a 400, but without a client-side guard the
  // user just sees the dropdown collapse and a page-level error banner appears
  // far from the input that caused it. Surfacing the validation here keeps the
  // failure colocated with the offending fields.
  const [customError, setCustomError] = useState(null);

  const triggerLabel = formatRangeLabel(range);

  const close = () => {
    setIsOpen(false);
    setMode('menu');
    setCustomError(null);
  };

  const updateDraft = (next) => {
    setDraft(next);
    // Re-validate eagerly on every edit. Without this, the error persists until
    // the next Apply click — clears even when the user corrects to ANOTHER
    // invalid state, leaving the UI showing no error against still-broken
    // input. Recompute the start <= end check on the in-progress draft so the
    // error reflects the current values: clear it on valid, set it on invalid.
    if (next.startDate && next.endDate && next.startDate > next.endDate) {
      setCustomError('Start date must be on or before end date');
    } else if (next.startDate && next.endDate && inclusiveDayCount(next.startDate, next.endDate) > MAX_RANGE_DAYS) {
      setCustomError(`Date range cannot exceed ${MAX_RANGE_DAYS} days`);
    } else if (customError) {
      setCustomError(null);
    }
  };

  const selectPreset = (key, e) => {
    if (key === 'custom') {
      // Stop the click from bubbling to RSC's document-level close handler so
      // the dropdown stays open while the user fills in the date inputs.
      if (e) e.stopPropagation();
      setMode('custom');
      setDraft({ startDate: range.startDate ?? '', endDate: range.endDate ?? '' });
      setCustomError(null);
      return;
    }
    dispatch(actions.setPeriodPreset(key));
    dispatch(actions.loadSummaryForPeriod());
    close();
  };

  const apply = () => {
    if (!draft.startDate || !draft.endDate) return;
    // ISO YYYY-MM-DD strings are lexicographically comparable as date order,
    // so a plain `>` check is sufficient — no Date parse needed.
    if (draft.startDate > draft.endDate) {
      setCustomError('Start date must be on or before end date');
      return;
    }
    if (inclusiveDayCount(draft.startDate, draft.endDate) > MAX_RANGE_DAYS) {
      setCustomError(`Date range cannot exceed ${MAX_RANGE_DAYS} days`);
      return;
    }
    dispatch(actions.setPeriodRange({ startDate: draft.startDate, endDate: draft.endDate }));
    dispatch(actions.loadSummaryForPeriod());
    close();
  };

  const cancel = () => {
    close();
  };

  // RSC calls onToggleCollapse on (a) trigger button click, (b) Esc key, and
  // (c) document-level outside click. Route the closing transition through
  // close() so mode resets to 'menu' — otherwise pressing Esc while the user
  // is in the custom-range form would leave mode='custom', and the NEXT open
  // would jump straight into the date inputs instead of the preset list.
  const handleToggleCollapse = () => {
    if (isOpen) close();
    else toggleOpen();
  };

  return (
    <span className="iq-usage-period-filter__wrapper">
      <span className="iq-usage-period-filter__label">Period:</span>
      <NxDropdown
        label={triggerLabel}
        isOpen={isOpen}
        onToggleCollapse={handleToggleCollapse}
        className="iq-usage-period-filter"
      >
        {mode === 'menu' &&
          PERIOD_PRESETS.map((p) => (
            <button
              key={p.key}
              type="button"
              className={`nx-dropdown-button iq-usage-period-filter__option${
                preset === p.key ? ' iq-usage-period-filter__option--active' : ''
              }`}
              onClick={(e) => selectPreset(p.key, e)}
            >
              {p.label}
            </button>
          ))}
        {mode === 'custom' && (
          <div
            className="iq-usage-period-filter__custom"
            role="group"
            aria-label="Custom date range"
            onClick={(e) => e.stopPropagation()}
            onKeyDown={(e) => {
              // Pressing Escape inside the custom-form should cancel — same as
              // clicking Cancel — rather than collapsing the dropdown (which
              // RSC's outer Esc handler would also try to do; stop the bubble
              // and route through cancel() so mode also resets cleanly).
              if (e.key === 'Escape') {
                e.stopPropagation();
                cancel();
              }
            }}
          >
            <label className="iq-usage-period-filter__field">
              <span className="iq-usage-period-filter__field-label">Start date</span>
              <NxDateInput
                value={draft.startDate}
                isPristine={!draft.startDate}
                onChange={(newVal) => updateDraft({ ...draft, startDate: newVal })}
                aria-describedby={customError ? 'iq-usage-period-filter__error' : undefined}
              />
            </label>
            <label className="iq-usage-period-filter__field">
              <span className="iq-usage-period-filter__field-label">End date</span>
              <NxDateInput
                value={draft.endDate}
                isPristine={!draft.endDate}
                onChange={(newVal) => updateDraft({ ...draft, endDate: newVal })}
                aria-describedby={customError ? 'iq-usage-period-filter__error' : undefined}
              />
            </label>
            {customError && (
              <div id="iq-usage-period-filter__error" className="iq-usage-period-filter__error" role="alert">
                {customError}
              </div>
            )}
            <div className="iq-usage-period-filter__custom-actions">
              <NxButton
                variant="primary"
                className="iq-usage-period-filter__action-btn"
                onClick={apply}
                disabled={!draft.startDate || !draft.endDate}
              >
                Apply
              </NxButton>
              <NxButton variant="tertiary" className="iq-usage-period-filter__action-btn" onClick={cancel}>
                Cancel
              </NxButton>
            </div>
          </div>
        )}
      </NxDropdown>
    </span>
  );
}
