/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useCallback, useEffect, useRef } from 'react';
import { isCompleteSyntax } from 'MainRoot/nosc/search/searchFilterTree';

/**
 * Quote an enum value for insertion when it contains whitespace. The backend
 * query parser reads a single whitespace-delimited token after `:`, so a bare
 * multi-word value (e.g. `vulnerabilityStatus:Not Applicable`) parses as an
 * exact match on `Not` plus a stray `Applicable` freetext term and silently
 * matches nothing. Wrapping in double quotes produces a `PhraseValue` the parser
 * matches whole against the keyword field. Single-token values are inserted
 * unquoted (unchanged).
 */
export function quoteEnumValue(value: string): string {
  return /\s/.test(value) ? `"${value}"` : value;
}

/**
 * Pure, framework-free logic for inserting a filter leaf's syntax into a search
 * query string. Shared by the omnibar FilterBar and (later) the results-page
 * inline filter bar so the two surfaces can never disagree on spacing, caret
 * placement, or complete-vs-incomplete handling.
 *
 * The rendering layer resolves enum leaves (e.g. appends `:security`) before
 * calling this, so `syntax` here is always the literal string to append.
 */

/** Result of computing an insertion: the new query text and where to place the caret. */
export interface FilterInsertResult {
  /** The new query string with the syntax appended (leading space added if needed). */
  readonly value: string;
  /**
   * Caret offset into `value`. Lands inside a trailing `""` when present, else at
   * the end. Callers apply this via `input.setSelectionRange(caretAt, caretAt)`.
   */
  readonly caretAt: number;
  /**
   * True when the inserted leaf is a full predicate that carries a value already
   * (does not end with `:` or `:""`). The results page may commit/re-run
   * immediately on a complete leaf; the omnibar always defers to Enter.
   */
  readonly complete: boolean;
}

/**
 * Append `syntax` to `current`, inserting a single leading space when the query
 * is non-empty and does not already end in whitespace so chained filters stay
 * separated. Caret lands inside a trailing `""` if present, else at the end.
 *
 * This function does NOT decide whether to re-run the search — it only reports
 * `complete` so the caller can choose. The omnibar ignores `complete` (defers to
 * Enter); the results page commits immediately on a complete leaf.
 */
export function computeFilterInsert(current: string, syntax: string): FilterInsertResult {
  const needsSpace = current.length > 0 && !/\s$/.test(current);
  const value = (needsSpace ? current + ' ' : current) + syntax;
  const caretAt = syntax.endsWith('""') ? value.length - 1 : value.length;
  return { value, caretAt, complete: isCompleteSyntax(syntax) };
}

/**
 * Focus the input and place the caret at `caretAt`, retrying across animation
 * frames until the input wins focus. Radix's DropdownMenu FocusScope shuffles
 * focus during its close cycle (submenu → main menu → previously-focused
 * trigger); a SubContent's own focus restoration can win the race against a
 * single rAF, leaving focus on the filter trigger instead of the input. Retrying
 * (capped so it never spins) lets the input reliably reclaim focus so its keydown
 * handlers (arrow nav, Enter-to-activate) work after a leaf is inserted.
 *
 * Returns a cancel function so a caller can abort a pending retry (e.g. on
 * unmount) — call sites that don't need it can ignore the return value.
 */
export function focusInputWithCaret(
  input: HTMLInputElement | null,
  caretAt: number,
  maxAttempts = 10,
): () => void {
  let attempts = 0;
  let rafId = 0;
  let cancelled = false;

  function tryFocus(): void {
    if (cancelled) return;
    const el = input;
    if (!el) return;
    if (document.activeElement === el) {
      el.setSelectionRange(caretAt, caretAt);
      return;
    }
    el.focus();
    el.setSelectionRange(caretAt, caretAt);
    if (document.activeElement !== el && attempts < maxAttempts) {
      attempts++;
      rafId = requestAnimationFrame(tryFocus);
    }
  }

  rafId = requestAnimationFrame(tryFocus);
  return () => {
    cancelled = true;
    if (rafId) cancelAnimationFrame(rafId);
  };
}

/**
 * React binding for {@link focusInputWithCaret} that owns the returned cancel
 * handle: a pending retry is cancelled when a new focus request supersedes it and
 * on unmount, so the retry loop never touches a detached input.
 */
export function useFocusInputWithCaret(): (input: HTMLInputElement | null, caretAt: number) => void {
  const cancelRef = useRef<(() => void) | null>(null);

  useEffect(
    () => () => {
      cancelRef.current?.();
      cancelRef.current = null;
    },
    [],
  );

  return useCallback((input: HTMLInputElement | null, caretAt: number): void => {
    cancelRef.current?.();
    cancelRef.current = focusInputWithCaret(input, caretAt);
  }, []);
}
