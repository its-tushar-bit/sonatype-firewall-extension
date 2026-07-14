/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  AlertTriangle,
  AreaChart,
  BarChart3,
  Boxes,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  Database,
  FileBadge,
  FileText,
  GitBranch,
  Globe,
  History,
  Home,
  Key,
  Layers,
  LayoutDashboard,
  MessageSquareWarning,
  Microscope,
  Minus,
  Package,
  PieChart,
  Scale,
  Search,
  Server,
  Shield,
  Sparkles,
  TrendingDown,
  TrendingUp,
  User,
  Users,
} from 'lucide-react';

/**
 * Semantic IQ-domain icon mappings.
 *
 * Whereas `nav-icons`, `action-icons`, and `status-icons` cover UI affordances,
 * this catalog covers IQ business-domain concepts (Applications, Legal,
 * Components, …). Each new domain glyph used by a Preview surface lands here
 * with its semantic name so consumers never know which Lucide glyph backs it.
 *
 * @example
 * import { DomainIcons } from 'MainRoot/nosc/icons';
 * <DomainIcons.Applications size={32} />
 */
export const DomainIcons = {
  /** Applications / scanned apps. */
  Applications: Boxes,
  /** Legal obligations, license threats. */
  Legal: Scale,
  /** Reports (scan reports, policy reports, SBOM reports). */
  Reports: FileText,
  /** Policies (security/license/quality rule configuration). */
  Policies: Shield,
  /** Organizations (hierarchical tenancy unit above Applications). */
  Organizations: Building2,
  /** Pipeline/lifecycle stage (build, stage-release, release, …). */
  Stage: Layers,
  /** Repositories (upstream package sources scanned by Firewall/Lifecycle). */
  Repositories: Database,
  /** Users & Groups (identity management). */
  Users: Users,
  /** Source Control integrations (GitHub, GitLab, Bitbucket, Azure DevOps). */
  SourceControl: GitBranch,
  /** Roles & Permissions (RBAC configuration). */
  Roles: Key,
  /** Waiver Requests (developer-submitted policy exception requests). */
  Waivers: MessageSquareWarning,
  /** Audit Log (immutable history of policy/config changes). */
  AuditLog: History,
  /** System Configuration (server-wide admin: proxy, base URL, auth, mail). */
  SystemConfig: Server,
  /**
   * P1-F13: Vulnerability / CVE entity icon for global search results
   * (red triangle alert glyph; tinted with var(--red-9) at use-site).
   */
  Vulnerability: AlertTriangle,
  /**
   * P1-F13: Component / package entity icon (blue Package glyph;
   * tinted with var(--blue-9) at use-site). Distinct from
   * `Applications` which is a multi-cube glyph (collection).
   */
  Component: Package,
  /**
   * P1-F13: SBOM document entity icon for global search results.
   */
  SbomMetadata: FileBadge,
  /** P1-F7c: Application detail — Developer Trust Score "Up" trend. */
  TrendUp: TrendingUp,
  /** P1-F7c: Application detail — DTS "Down" trend. */
  TrendDown: TrendingDown,
  /** P1-F7c: Application detail — DTS "Flat" trend. */
  TrendFlat: Minus,
  /** P1-F7c: Application detail — clean/healthy state (e.g. no malicious
   *  components). Distinct from `StatusIcons.Success` which is used for
   *  feedback states like "save succeeded". */
  Healthy: CheckCircle2,
  /** P1-F7c: Application detail — date / created timestamps. */
  Calendar,
  /** P1-F7c: Application detail — last-scan / scan-frequency metadata. */
  Clock,
  /** P1-F7c: Application detail — "Created by" user metadata. */
  Person: User,
  /**
   * Classic LeftNav parity (CLM-39640): one icon per Classic
   * IqSidebarNav module. Names mirror the FontAwesome glyphs used in
   * react/iqSidebarNav/IqSidebarNav.jsx so a side-by-side review is
   * easy. Lucide equivalents picked to be visually-equivalent and
   * monochrome at 18px.
   */
  /** Home / Dashboard (faHouse). */
  Home,
  /** Reports list (faChartColumn) — distinct from FileText "Reports". */
  ReportsBar: BarChart3,
  /** Success Metrics (faChartArea). */
  SuccessMetrics: AreaChart,
  /** Vulnerability Lookup (faMicroscope). */
  VulnerabilityLookup: Microscope,
  /** Advanced Search (faMagnifyingGlass). */
  AdvancedSearch: Search,
  /** Hosted Repos (faDatabase) — same as Repositories visually. */
  HostedRepos: Database,
  /** Enterprise Reporting (faChartPie). */
  EnterpriseReporting: PieChart,
  /** Operational Reporting (faChartArea). */
  OperationalReporting: AreaChart,
  /** API page (faStars). */
  Api: Sparkles,
  /** Generic Dashboard (matches Lucide's grid-style dashboard glyph
   *  used by the Nexus One Preview Dashboard tile shell). Distinct
   *  from Home: Classic uses the same "House" glyph for the
   *  Dashboard nav entry, but for Nexus One we keep them
   *  separate to match the prototype. */
  Dashboard: LayoutDashboard,
  /** Generic Globe — used for Legal/Obligations or external-resource
   *  contexts. (Nice-to-have; not currently consumed.) */
  Globe,
} as const;

export type DomainIconName = keyof typeof DomainIcons;
