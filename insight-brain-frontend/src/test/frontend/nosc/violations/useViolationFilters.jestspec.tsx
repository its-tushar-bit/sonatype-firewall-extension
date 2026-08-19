/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { renderHook, act } from '@testing-library/react';
import { useViolationFilters } from 'MainRoot/nosc/violations/useViolationFilters';

describe('useViolationFilters', () => {
  it('initializes with default filter state', () => {
    const { result } = renderHook(() => useViolationFilters());
    expect(result.current.filters.states.size).toBe(0);
    expect(result.current.filters.threatCategories.size).toBe(0);
    expect(result.current.filters.stageIds.size).toBe(0);
    expect(result.current.filters.organizationIds.size).toBe(0);
    expect(result.current.filters.applicationIds.size).toBe(0);
    expect(result.current.filters.threatRange).toEqual([0, 10]);
    expect(result.current.filters.waiverType).toBe('ANY');
  });

  describe('toggle', () => {
    it('toggles a state filter', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.toggle('states', 'OPEN');
      });

      expect(result.current.filters.states.has('OPEN')).toBe(true);

      act(() => {
        result.current.toggle('states', 'OPEN');
      });

      expect(result.current.filters.states.has('OPEN')).toBe(false);
    });

    it('toggles a threat category filter', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.toggle('threatCategories', 'security');
      });

      expect(result.current.filters.threatCategories.has('security')).toBe(true);

      act(() => {
        result.current.toggle('threatCategories', 'security');
      });

      expect(result.current.filters.threatCategories.has('security')).toBe(false);
    });

    it('toggles a stage id filter', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.toggle('stageIds', 'build');
      });

      expect(result.current.filters.stageIds.has('build')).toBe(true);
    });

    it('toggles an organization id filter', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.toggle('organizationIds', 'org-123');
      });

      expect(result.current.filters.organizationIds.has('org-123')).toBe(true);
    });

    it('toggles an application id filter', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.toggle('applicationIds', 'app-456');
      });

      expect(result.current.filters.applicationIds.has('app-456')).toBe(true);
    });

    it('supports multiple values in the same group', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.toggle('states', 'OPEN');
        result.current.toggle('states', 'WAIVED');
      });

      expect(result.current.filters.states.size).toBe(2);
      expect(result.current.filters.states.has('OPEN')).toBe(true);
      expect(result.current.filters.states.has('WAIVED')).toBe(true);
    });

    it('does not affect other filter groups', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.toggle('states', 'OPEN');
        result.current.toggle('threatCategories', 'security');
      });

      expect(result.current.filters.states.has('OPEN')).toBe(true);
      expect(result.current.filters.threatCategories.has('security')).toBe(true);
    });
  });

  describe('setThreatRange', () => {
    it('sets the threat range', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.setThreatRange([5, 10]);
      });

      expect(result.current.filters.threatRange).toEqual([5, 10]);
    });

    it('overwrites the previous range', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.setThreatRange([5, 10]);
      });
      expect(result.current.filters.threatRange).toEqual([5, 10]);

      act(() => {
        result.current.setThreatRange([0, 7]);
      });
      expect(result.current.filters.threatRange).toEqual([0, 7]);
    });
  });

  describe('setWaiverType', () => {
    it('sets waiver type to AUTO', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.setWaiverType('AUTO');
      });

      expect(result.current.filters.waiverType).toBe('AUTO');
    });

    it('sets waiver type to MANUAL', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.setWaiverType('MANUAL');
      });

      expect(result.current.filters.waiverType).toBe('MANUAL');
    });

    it('sets waiver type to ANY', () => {
      const { result } = renderHook(() => useViolationFilters());

      act(() => {
        result.current.setWaiverType('AUTO');
      });
      expect(result.current.filters.waiverType).toBe('AUTO');

      act(() => {
        result.current.setWaiverType('ANY');
      });
      expect(result.current.filters.waiverType).toBe('ANY');
    });
  });

  describe('reset', () => {
    it('resets all filters to default', () => {
      const { result } = renderHook(() => useViolationFilters());

      // Set up some filters
      act(() => {
        result.current.toggle('states', 'OPEN');
        result.current.toggle('threatCategories', 'security');
        result.current.toggle('stageIds', 'build');
        result.current.toggle('organizationIds', 'org-1');
        result.current.toggle('applicationIds', 'app-1');
        result.current.setThreatRange([7, 10]);
        result.current.setWaiverType('AUTO');
      });

      // Verify filters are set
      expect(result.current.filters.states.size).toBe(1);
      expect(result.current.filters.threatCategories.size).toBe(1);
      expect(result.current.filters.threatRange).toEqual([7, 10]);
      expect(result.current.filters.waiverType).toBe('AUTO');

      // Reset
      act(() => {
        result.current.reset();
      });

      // Verify all defaults
      expect(result.current.filters.states.size).toBe(0);
      expect(result.current.filters.threatCategories.size).toBe(0);
      expect(result.current.filters.stageIds.size).toBe(0);
      expect(result.current.filters.organizationIds.size).toBe(0);
      expect(result.current.filters.applicationIds.size).toBe(0);
      expect(result.current.filters.threatRange).toEqual([0, 10]);
      expect(result.current.filters.waiverType).toBe('ANY');
    });
  });

  describe('immutable state', () => {
    it('returns a new filter object on each change', () => {
      const { result } = renderHook(() => useViolationFilters());
      const firstFilters = result.current.filters;

      act(() => {
        result.current.toggle('states', 'OPEN');
      });

      // Object reference should change (for React memoization)
      expect(result.current.filters).not.toBe(firstFilters);
    });
  });
});
