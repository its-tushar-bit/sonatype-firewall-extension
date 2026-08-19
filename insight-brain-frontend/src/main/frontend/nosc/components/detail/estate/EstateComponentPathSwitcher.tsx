/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactElement, ReactNode } from 'react';
import axios from 'axios';
import { Box, Button, DropdownMenu, Flex, Link as RadixLink, Text, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { applicationUsageReportHref, formatLastSeen } from './estateComponentDetailUtils';
import {
  COMPONENT_USAGE_PAGE_SIZE,
  fetchComponentUsageApplications,
  fetchComponentUsageOrganizations,
  fetchComponentUsageReports,
} from './estateComponentUsageApi';
import type {
  ComponentUsageApplicationRow,
  ComponentUsageOrganizationRow,
  ComponentUsageReportRow,
} from './estateComponentUsageApi';
import type { EstateComponentPathSelection } from './estateComponentDetailContext';

const NAME_SEARCH_DEBOUNCE_MS = 300;

type LoadStatus = 'loading' | 'ready' | 'error';

export type EstateComponentPathSwitcherOrganizationsUsage = {
  readonly organizations: ReadonlyArray<ComponentUsageOrganizationRow>;
  readonly total: number;
  readonly status: LoadStatus;
};

export type EstateComponentPathSwitcherApplicationsUsage = {
  readonly applications: ReadonlyArray<ComponentUsageApplicationRow>;
  readonly total: number;
  readonly status: LoadStatus;
};

type EstateComponentPathSwitcherProps = {
  readonly componentHash: string;
  readonly organizationsUsage: EstateComponentPathSwitcherOrganizationsUsage;
  readonly applicationsUsage: EstateComponentPathSwitcherApplicationsUsage;
  readonly pathSelection: EstateComponentPathSelection;
  readonly onPathChange: (next: EstateComponentPathSelection) => void;
};

type PathSearchSelectItem = {
  readonly id: string;
  readonly label: string;
  readonly secondary?: string;
};

function organizationName(row: ComponentUsageOrganizationRow): string {
  return row.organizationName || row.organizationId || 'Organization';
}

function organizationApplicationCountLabel(row: ComponentUsageOrganizationRow): string | undefined {
  if (typeof row.applicationCount !== 'number') {
    return undefined;
  }
  const noun = row.applicationCount === 1 ? 'application' : 'applications';
  return `${row.applicationCount} ${noun}`;
}

function applicationLabel(row: ComponentUsageApplicationRow): string {
  const primary = row.applicationName || row.applicationPublicId || row.applicationId || 'Application';
  return row.organizationName ? `${primary} - ${row.organizationName}` : primary;
}

function reportLabel(row: ComponentUsageReportRow): string {
  const stage = row.stageTypeId || 'Report';
  const seen = formatLastSeen(row.evaluationTime);
  return seen === '—' ? stage : `${stage} - ${seen}`;
}

function hasOrganizationId(row: ComponentUsageOrganizationRow): row is ComponentUsageOrganizationRow & {
  readonly organizationId: string;
} {
  return Boolean(row.organizationId?.trim());
}

function hasApplicationId(row: ComponentUsageApplicationRow): row is ComponentUsageApplicationRow & {
  readonly applicationId: string;
} {
  return Boolean(row.applicationId?.trim());
}

function hasReportId(row: ComponentUsageReportRow): row is ComponentUsageReportRow & { readonly reportId: string } {
  return Boolean(row.reportId?.trim());
}

function truncationCopy(pageLength: number, total: number, noun: string): string | null {
  if (total <= pageLength) {
    return null;
  }
  return `Showing the first ${pageLength} of ${total} ${noun}.`;
}

type PathSearchSelectProps = {
  readonly label: string;
  readonly testId: string;
  readonly disabled?: boolean;
  readonly status: LoadStatus;
  readonly selectedId: string;
  readonly selectedLabel: string;
  readonly placeholder: string;
  readonly items: ReadonlyArray<PathSearchSelectItem>;
  readonly total: number;
  readonly truncationNoun: string;
  readonly showSearch?: boolean;
  readonly onSelect: (id: string) => void;
  readonly onSearch: (query: string) => void;
};

function PathSearchSelect({
  label,
  testId,
  disabled = false,
  status,
  selectedId,
  selectedLabel,
  placeholder,
  items,
  total,
  truncationNoun,
  showSearch = true,
  onSelect,
  onSearch,
}: PathSearchSelectProps): ReactElement {
  const [query, setQuery] = useState('');
  const searchInitializedRef = useRef(false);

  useEffect(() => {
    if (!showSearch) {
      return undefined;
    }
    // Skip the initial empty-query debounce so parent-seeded lists are not refetched on mount.
    if (!searchInitializedRef.current) {
      searchInitializedRef.current = true;
      return undefined;
    }
    const timer = window.setTimeout(() => onSearch(query.trim()), NAME_SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [onSearch, query, showSearch]);

  const triggerLabel =
    status === 'loading' && !selectedId
      ? `Loading ${truncationNoun}...`
      : selectedLabel || placeholder;

  return (
    <Flex direction="column" gap="1">
      <Text size="1" color="gray" as="label">
        {label}
      </Text>
      <PathDropdownRoot>
        <DropdownMenu.Trigger>
          <Button
            variant="soft"
            color="gray"
            disabled={disabled || (status !== 'ready' && items.length === 0 && !selectedId)}
            aria-label={label}
            data-testid={testId}
            style={{ justifyContent: 'space-between', minWidth: 220 }}
          >
            <Text size="2" truncate style={{ maxWidth: 200 }}>
              {triggerLabel}
            </Text>
          </Button>
        </DropdownMenu.Trigger>
        <DropdownMenu.Content
          align="start"
          style={{ width: 320 }}
          data-testid={`${testId}-content`}
          onCloseAutoFocus={(event) => event.preventDefault()}
        >
          {showSearch && (
            <Box px="2" pt="2" onPointerDown={(event) => event.stopPropagation()}>
              <TextField.Root
                size="2"
                placeholder={`Search ${truncationNoun}...`}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                onKeyDown={(event) => event.stopPropagation()}
                data-testid={`${testId}-search`}
              >
                <TextField.Slot>
                  <ActionIcons.Search size={14} aria-hidden="true" />
                </TextField.Slot>
              </TextField.Root>
            </Box>
          )}
          <Flex direction="column" gap="1" p="1" style={{ maxHeight: 240, overflow: 'auto' }}>
            {status === 'loading' && (
              <Text size="2" color="gray">
                Loading...
              </Text>
            )}
            {status === 'error' && (
              <Text size="2" color="red">
                Could not load {truncationNoun}.
              </Text>
            )}
            {status === 'ready' && items.length === 0 && (
              <Text size="2" color="gray">
                No matches.
              </Text>
            )}
            {status === 'ready' &&
              items.map((item) => {
                const isSelected = item.id === selectedId;
                return (
                  <DropdownMenu.Item
                    key={item.id}
                    onSelect={() => onSelect(item.id)}
                    data-testid={`${testId}-option-${item.id}`}
                  >
                    <Flex align="center" justify="between" gap="3" width="100%">
                      <Flex direction="column" gap="0" style={{ minWidth: 0, flex: 1 }}>
                        <Text size="2" truncate>
                          {item.label}
                        </Text>
                        {item.secondary && (
                          <Text size="1" color="gray" truncate>
                            {item.secondary}
                          </Text>
                        )}
                      </Flex>
                      {isSelected && (
                        <ActionIcons.Save size={14} color="var(--accent-9)" aria-hidden="true" />
                      )}
                    </Flex>
                  </DropdownMenu.Item>
                );
              })}
          </Flex>
          {status === 'ready' && truncationCopy(items.length, total, truncationNoun) && (
            <DropdownMenu.Label>
              <Text size="1" color="gray">
                {truncationCopy(items.length, total, truncationNoun)}
              </Text>
            </DropdownMenu.Label>
          )}
        </DropdownMenu.Content>
      </PathDropdownRoot>
    </Flex>
  );
}

function PathDropdownRoot({ children }: { readonly children: ReactNode }): ReactElement {
  const [open, setOpen] = useState(false);
  return (
    <DropdownMenu.Root open={open} onOpenChange={setOpen} modal={false}>
      {children}
    </DropdownMenu.Root>
  );
}

export function EstateComponentPathSwitcher({
  componentHash,
  organizationsUsage,
  applicationsUsage,
  pathSelection,
  onPathChange,
}: EstateComponentPathSwitcherProps): ReactElement {
  const [organizations, setOrganizations] = useState(organizationsUsage.organizations);
  const [organizationsTotal, setOrganizationsTotal] = useState(organizationsUsage.total);
  const [organizationsStatus, setOrganizationsStatus] = useState<LoadStatus>(organizationsUsage.status);

  const [applications, setApplications] = useState(applicationsUsage.applications);
  const [applicationsTotal, setApplicationsTotal] = useState(applicationsUsage.total);
  const [applicationsStatus, setApplicationsStatus] = useState<LoadStatus>(applicationsUsage.status);

  const [reports, setReports] = useState<ReadonlyArray<ComponentUsageReportRow>>([]);
  const [reportsTotal, setReportsTotal] = useState(0);
  const [reportsStatus, setReportsStatus] = useState<LoadStatus>('ready');

  const [selectedOrganizationId, setSelectedOrganizationId] = useState(pathSelection.organizationId ?? '');
  const [selectedApplicationId, setSelectedApplicationId] = useState(pathSelection.applicationId ?? '');
  const [selectedReportId, setSelectedReportId] = useState(pathSelection.reportId ?? '');

  const orgSearchRef = useRef('');
  const appSearchRef = useRef('');
  const selectedApplicationIdRef = useRef(selectedApplicationId);
  const orgFetchAbortRef = useRef<AbortController | null>(null);
  const appFetchAbortRef = useRef<AbortController | null>(null);
  const autoSelectRef = useRef({ org: false, app: false, report: false });

  selectedApplicationIdRef.current = selectedApplicationId;

  useEffect(() => {
    setOrganizations(organizationsUsage.organizations);
    setOrganizationsTotal(organizationsUsage.total);
    setOrganizationsStatus(organizationsUsage.status);
  }, [organizationsUsage]);

  useEffect(() => {
    setApplications(applicationsUsage.applications);
    setApplicationsTotal(applicationsUsage.total);
    setApplicationsStatus(applicationsUsage.status);
  }, [applicationsUsage]);

  useEffect(() => {
    setSelectedOrganizationId(pathSelection.organizationId ?? '');
    setSelectedApplicationId(pathSelection.applicationId ?? '');
    setSelectedReportId(pathSelection.reportId ?? '');
    autoSelectRef.current = { org: false, app: false, report: false };
    orgSearchRef.current = '';
    appSearchRef.current = '';
    setReports([]);
    setReportsTotal(0);
    setReportsStatus('ready');
  }, [componentHash]);

  const loadOrganizations = useCallback(
    (nameSearch: string, includeIds: ReadonlyArray<string> | undefined, signal: AbortSignal) => {
      setOrganizationsStatus('loading');
      void fetchComponentUsageOrganizations(
        componentHash,
        0,
        COMPONENT_USAGE_PAGE_SIZE,
        signal,
        { nameSearch: nameSearch || undefined, includeIds }
      )
        .then((response) => {
          if (signal.aborted) return;
          setOrganizations(response.organizations);
          setOrganizationsTotal(response.total);
          setOrganizationsStatus('ready');
        })
        .catch((err) => {
          if (axios.isCancel(err) || signal.aborted) return;
          setOrganizationsStatus('error');
        });
    },
    [componentHash]
  );

  const loadApplications = useCallback(
    (
      organizationId: string,
      nameSearch: string,
      includeIds: ReadonlyArray<string> | undefined,
      signal: AbortSignal
    ) => {
      setApplicationsStatus('loading');
      void fetchComponentUsageApplications(
        componentHash,
        0,
        COMPONENT_USAGE_PAGE_SIZE,
        signal,
        {
          organizationId: organizationId || undefined,
          nameSearch: nameSearch || undefined,
          includeIds,
        }
      )
        .then((response) => {
          if (signal.aborted) return;
          setApplications(response.applications);
          setApplicationsTotal(response.total);
          setApplicationsStatus('ready');
        })
        .catch((err) => {
          if (axios.isCancel(err) || signal.aborted) return;
          setApplicationsStatus('error');
        });
    },
    [componentHash]
  );

  useEffect(() => {
    if (!selectedOrganizationId) {
      setApplications([]);
      setApplicationsTotal(0);
      setApplicationsStatus('ready');
      return undefined;
    }
    const controller = new AbortController();
    // Pin the current selection when the org list reloads; do not re-fetch on every app select.
    const pinApplicationId = selectedApplicationIdRef.current;
    loadApplications(
      selectedOrganizationId,
      appSearchRef.current,
      pinApplicationId ? [pinApplicationId] : undefined,
      controller.signal
    );
    return () => controller.abort();
  }, [componentHash, loadApplications, selectedOrganizationId]);

  useEffect(() => {
    if (!selectedApplicationId) {
      setReports([]);
      setReportsTotal(0);
      setSelectedReportId('');
      setReportsStatus('ready');
      return undefined;
    }

    const controller = new AbortController();
    setReportsStatus('loading');
    setReports([]);

    void fetchComponentUsageReports(
      componentHash,
      selectedApplicationId,
      0,
      COMPONENT_USAGE_PAGE_SIZE,
      controller.signal
    )
      .then((response) => {
        if (controller.signal.aborted) return;
        setReports(response.reports);
        setReportsTotal(response.total);
        setReportsStatus('ready');
      })
      .catch((err) => {
        if (axios.isCancel(err) || controller.signal.aborted) return;
        setReportsStatus('error');
      });

    return () => controller.abort();
  }, [componentHash, selectedApplicationId]);

  const selectableOrganizations = useMemo(() => organizations.filter(hasOrganizationId), [organizations]);
  const selectableApplications = useMemo(() => applications.filter(hasApplicationId), [applications]);
  const selectableReports = useMemo(() => reports.filter(hasReportId), [reports]);

  const publishPathChange = useCallback(
    (next: Partial<EstateComponentPathSelection>) => {
      const organizationId = next.organizationId ?? selectedOrganizationId;
      const applicationId = next.applicationId ?? selectedApplicationId;
      const reportId = next.reportId ?? selectedReportId;
      const selectedReport = selectableReports.find((report) => report.reportId === reportId);
      onPathChange({
        organizationId: organizationId || undefined,
        applicationId: applicationId || undefined,
        reportId: reportId || undefined,
        stageTypeId: selectedReport?.stageTypeId,
        reportLabel: selectedReport ? reportLabel(selectedReport) : undefined,
      });
    },
    [onPathChange, selectableReports, selectedApplicationId, selectedOrganizationId, selectedReportId]
  );

  useEffect(() => {
    if (organizationsStatus !== 'ready' || selectableOrganizations.length === 0 || autoSelectRef.current.org) {
      return;
    }
    autoSelectRef.current.org = true;
    const urlMatch = pathSelection.organizationId
      ? selectableOrganizations.find((org) => org.organizationId === pathSelection.organizationId)
      : undefined;
    const nextOrgId = urlMatch?.organizationId ?? selectableOrganizations[0].organizationId;
    if (!nextOrgId) {
      return;
    }
    setSelectedOrganizationId(nextOrgId);
    if (nextOrgId !== pathSelection.organizationId) {
      setSelectedApplicationId('');
      setSelectedReportId('');
      publishPathChange({ organizationId: nextOrgId, applicationId: undefined, reportId: undefined });
    }
  }, [organizationsStatus, pathSelection.organizationId, publishPathChange, selectableOrganizations]);

  useEffect(() => {
    if (
      !selectedOrganizationId ||
      applicationsStatus !== 'ready' ||
      selectableApplications.length === 0 ||
      autoSelectRef.current.app
    ) {
      return;
    }
    autoSelectRef.current.app = true;
    const urlMatch = pathSelection.applicationId
      ? selectableApplications.find((app) => app.applicationId === pathSelection.applicationId)
      : undefined;
    const nextAppId = urlMatch?.applicationId ?? selectableApplications[0].applicationId;
    if (!nextAppId) {
      return;
    }
    setSelectedApplicationId(nextAppId);
    if (nextAppId !== pathSelection.applicationId) {
      setSelectedReportId('');
      publishPathChange({ applicationId: nextAppId, reportId: undefined });
    }
  }, [
    applicationsStatus,
    pathSelection.applicationId,
    publishPathChange,
    selectableApplications,
    selectedOrganizationId,
  ]);

  useEffect(() => {
    if (
      !selectedApplicationId ||
      reportsStatus !== 'ready' ||
      selectableReports.length === 0 ||
      autoSelectRef.current.report
    ) {
      return;
    }
    autoSelectRef.current.report = true;
    const urlMatch = pathSelection.reportId
      ? selectableReports.find((report) => report.reportId === pathSelection.reportId)
      : undefined;
    const nextReportId = urlMatch?.reportId ?? selectableReports[0].reportId;
    if (!nextReportId) {
      return;
    }
    setSelectedReportId(nextReportId);
    if (nextReportId !== pathSelection.reportId) {
      publishPathChange({ reportId: nextReportId });
    }
  }, [pathSelection.reportId, publishPathChange, reportsStatus, selectableReports, selectedApplicationId]);

  const selectedOrganization = selectableOrganizations.find((org) => org.organizationId === selectedOrganizationId);
  const selectedApplication = selectableApplications.find((app) => app.applicationId === selectedApplicationId);
  const selectedReport = selectableReports.find((report) => report.reportId === selectedReportId);
  const reportHref = applicationUsageReportHref(
    selectedApplication?.applicationPublicId,
    selectedReport?.reportId
  );

  const handleOrganizationSearch = useCallback(
    (query: string) => {
      orgSearchRef.current = query;
      orgFetchAbortRef.current?.abort();
      const controller = new AbortController();
      orgFetchAbortRef.current = controller;
      loadOrganizations(query, selectedOrganizationId ? [selectedOrganizationId] : undefined, controller.signal);
    },
    [loadOrganizations, selectedOrganizationId]
  );

  const handleApplicationSearch = useCallback(
    (query: string) => {
      if (!selectedOrganizationId) return;
      appSearchRef.current = query;
      appFetchAbortRef.current?.abort();
      const controller = new AbortController();
      appFetchAbortRef.current = controller;
      loadApplications(
        selectedOrganizationId,
        query,
        selectedApplicationId ? [selectedApplicationId] : undefined,
        controller.signal
      );
    },
    [loadApplications, selectedApplicationId, selectedOrganizationId]
  );

  return (
    <Flex direction="column" gap="2" data-testid="nosc-estate-component-path-switcher">
      <Text size="2" weight="medium">
        Path
      </Text>
      <Flex gap="3" wrap="wrap" align="end">
        <PathSearchSelect
          label="Organization"
          testId="nosc-estate-component-path-switcher-organization"
          status={organizationsStatus}
          selectedId={selectedOrganizationId}
          selectedLabel={selectedOrganization ? organizationName(selectedOrganization) : ''}
          placeholder="Select organization"
          items={selectableOrganizations.map((org) => ({
            id: org.organizationId,
            label: organizationName(org),
            secondary: organizationApplicationCountLabel(org),
          }))}
          total={organizationsTotal}
          truncationNoun="organizations"
          onSearch={handleOrganizationSearch}
          onSelect={(organizationId) => {
            setSelectedOrganizationId(organizationId);
            setSelectedApplicationId('');
            setSelectedReportId('');
            autoSelectRef.current.app = false;
            autoSelectRef.current.report = false;
            publishPathChange({ organizationId, applicationId: undefined, reportId: undefined });
          }}
        />

        <PathSearchSelect
          label="Application"
          testId="nosc-estate-component-path-switcher-application"
          disabled={!selectedOrganizationId}
          status={applicationsStatus}
          selectedId={selectedApplicationId}
          selectedLabel={selectedApplication ? applicationLabel(selectedApplication) : ''}
          placeholder="Select application"
          items={selectableApplications.map((app) => ({
            id: app.applicationId,
            label: applicationLabel(app),
          }))}
          total={applicationsTotal}
          truncationNoun="applications"
          onSearch={handleApplicationSearch}
          onSelect={(applicationId) => {
            setSelectedApplicationId(applicationId);
            setSelectedReportId('');
            autoSelectRef.current.report = false;
            publishPathChange({ applicationId, reportId: undefined });
          }}
        />

        <PathSearchSelect
          label="Report"
          testId="nosc-estate-component-path-switcher-report"
          disabled={!selectedApplicationId}
          status={reportsStatus}
          selectedId={selectedReportId}
          selectedLabel={selectedReport ? reportLabel(selectedReport) : ''}
          placeholder={reportsStatus === 'loading' ? 'Loading reports...' : 'Select report'}
          items={selectableReports.map((report) => ({
            id: report.reportId,
            label: reportLabel(report),
          }))}
          total={reportsTotal}
          truncationNoun="reports"
          showSearch={false}
          onSearch={() => undefined}
          onSelect={(reportId) => {
            setSelectedReportId(reportId);
            publishPathChange({ reportId });
          }}
        />

        {reportHref && (
          <Button size="2" variant="soft" asChild>
            <RadixLink href={reportHref} data-testid="nosc-estate-component-path-switcher-report-link">
              Open report
            </RadixLink>
          </Button>
        )}
      </Flex>

      {organizationsStatus === 'error' && (
        <Text size="1" color="red">
          Organizations could not be loaded.
        </Text>
      )}
      {applicationsStatus === 'error' && (
        <Text size="1" color="red">
          Applications could not be loaded for this organization.
        </Text>
      )}
      {reportsStatus === 'error' && (
        <Text size="1" color="red">
          Reports could not be loaded for this application.
        </Text>
      )}
    </Flex>
  );
}
