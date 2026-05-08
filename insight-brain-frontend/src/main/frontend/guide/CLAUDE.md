# Guide SPA

This is the self-hosted Guide SPA — a standalone React app, fully independent from the legacy IQ Server UI.

**Before starting any Guide work**, read the **"Guide SPA"** section in [`insight-brain-frontend/CLAUDE.md`](../../../../CLAUDE.md) — it covers architecture, routing rationale, directory conventions, and testing patterns.

## Coding standards

### Components and hooks — use `@guide/ui-core` first
Before writing a custom component or hook, check `@guide/ui-core`:
- Layout: `FilteredPageLayout`, `Button`, `CVSSBadge`, `VulnerabilityResultCard`
- Navigation: `useNavigate`, `useAdapterPathname`, `useAdapterSearchParams`, `useLink`
- Forms: `useForm`

When in doubt whether a component or pattern exists, check the seaworthy equivalent at https://github.com/sonatype/seaworthy — the self-hosted SPA shares `@guide/ui-core` with Seaworthy and should match its UX behaviour.

### Types and interfaces — use `@guide/ui-core` first
Import types from `@guide/ui-core/types` unless there is no obvious interface there for the development task:
- Domain types: `Component`, `Vulnerability`, `License`, `Aggregations`
- Request/response types: `ComponentSearchResponse`, `ComponentsFilters`, `ComponentsSearchOptions`, etc.
- Utility types: `NavigationAdapter`, `LinkProps`, `FormProps`

This ensures type consistency between self-hosted and SaaS Guide and prevents duplicate type definitions.

### Spacing, sizing, and color — always use tokens
Never hardcode Radix prop values. Import `tokens` from `@guide/ui-core/utils`:

```tsx
import { tokens } from '@guide/ui-core/utils';

// spacing
py={tokens.space.section}   // "4" = 16px
gap={tokens.space.item}     // "3" = 12px
px={tokens.space.inline}    // "2" = 8px

// component sizes
size={tokens.sizes.caption} // "2"
size={tokens.sizes.body.sm} // "2"
size={tokens.sizes.body.xs} // "1"
```

Available space tokens: `tight(1) inline(2) item(3) section(4) header(5) page(6) hero(7) billboard(8) jumbo(9)`

### CSS
- **No hardcoded colors** — use CSS variables from `globals.css` (`var(--color-panel)`, `var(--gray-6)`, `var(--overlay-bg)`, etc.)
- **No `vh` units** — use `dvh` (`height: '100dvh'`)
- **CSS Modules**: class names in kebab-case (`.my-class`), accessed in TS as camelCase (`styles.myClass`) — `localsConvention: 'camelCaseOnly'` is configured in esbuild
- **No redundant prefixes** in CSS module class names — the module scope already isolates them

## Testing and type-checking

```bash
# TypeScript (Guide only)
npx tsc --noEmit --project tsconfig.guide.json

# All guide tests
yarn jest -- guide

# Single test file
yarn jest -- guide/layout/AppShell
```

Test files live at `src/test/frontend/guide/` mirroring source structure, named `*.jestspec.tsx`.
Use `render` and `screen` from `../test-utils` (not directly from `@testing-library/react`) — it wraps components with `MemoryRouter`, `Theme`, `AuthProvider`, and `NavigationProvider`.

### Do not cross-import with the legacy IQ UI
Guide code must never import from `MainRoot/*`, Redux, UI Router, or `@sonatype/react-shared-components`.
