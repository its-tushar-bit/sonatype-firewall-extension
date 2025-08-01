<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Priorities Page Developer Understanding Guide

## Overview

The **Priorities Page** is a core feature of the Nexus IQ Server Developer Dashboard that provides developers with a prioritized, actionable list of security and policy violations across their application components. It transforms raw scan results into a focused workflow that helps developers address the most critical issues first.

## Table of Contents

- [Purpose & Value Proposition](#purpose--value-proposition)
- [Key Features](#key-features)
- [Architecture Overview](#architecture-overview)
- [Prioritization Algorithm](#prioritization-algorithm)
- [Frontend Implementation](#frontend-implementation)
- [Backend Implementation](#backend-implementation)
- [Reachability Analysis](#reachability-analysis)
- [Integration Points](#integration-points)
- [Data Flow](#data-flow)
- [Testing Strategy](#testing-strategy)
- [Performance Considerations](#performance-considerations)

## Purpose & Value Proposition

### Problem It Solves
- **Information Overload**: Raw scan reports can contain hundreds of violations
- **Lack of Context**: Not all violations are equally actionable or risky
- **Developer Workflow**: Need to quickly identify what to fix first
- **Remediation Guidance**: Developers need specific version recommendations

### Value Delivered
- **Intelligent Prioritization**: Uses policy actions, reachability, and threat levels to rank issues
- **Actionability Focus**: Shows only items that can be acted upon with clear next steps
- **Remediation Recommendations**: Provides specific version upgrade suggestions
- **Integration Support**: Works with various development workflows (CLI, CI/CD, manual)

## Key Features

### 1. **Intelligent Priority Scoring**
```java
// Core scoring algorithm
private int getScore(final UnprioritizedComponent unprioritizedComponent) {
    return getActionNumber(unprioritizedComponent.action) * 100000 +
           getRecommendationNumber(unprioritizedComponent) * 100 +
           unprioritizedComponent.highestReachableThreat;
}
```

### 2. **Reachability Analysis**
- **Reachable**: Vulnerable code paths can be executed
- **Non-Reachable**: Vulnerable code is present but not in execution path
- **Unknown**: Reachability cannot be determined

### 3. **Remediation Recommendations**
- Version upgrades that resolve violations
- Smart suggestions based on semantic versioning
- Inner source component recommendations
- Breaking vs non-breaking change analysis

### 4. **Pull Request Integration**
- Automated PR creation for remediation
- Integration with SCM systems
- Support for bulk recommendations

### 5. **Filtering & Navigation**
- Component name filtering
- Policy action filtering (Fail/Warn only)
- Integration with dependency tree
- Deep linking to component details

## Architecture Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend       │    │   Data Layer    │
│                 │    │                  │    │                 │
│ PrioritiesPage  │───▶│ DevelopmentPrio- │───▶│ PolicyThreats   │
│ PrioritiesTable │    │ ritiesService    │    │ Component       │
│ PrioritiesRow   │    │                  │    │ Reachability    │
│                 │    │ REST APIs        │    │ Remediation     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Technology Stack
- **Frontend**: React + Redux, TypeScript/JavaScript
- **Backend**: Java, JAX-RS REST services
- **Data**: JPA/Hibernate, JSON report storage
- **Testing**: Jest (frontend), JUnit (backend), Selenium (functional)

## Prioritization Algorithm

The priorities page uses a sophisticated scoring system to rank components:

### Scoring Components (in order of importance):

1. **Policy Action Level** (×100,000 multiplier)
   - `FAIL` actions get highest priority
   - `WARN` actions get medium priority
   - No violations get lowest priority

2. **Remediation Availability** (×100 multiplier)
   - Components with available fixes ranked higher
   - Considers version compatibility and breaking changes

3. **Reachable Threat Level** (×1 multiplier)
   - Based on actual reachability analysis
   - Higher threat scores for reachable vulnerabilities

### Secondary Sorting
- Components with same score are sorted by highest overall threat level
- Results are sorted in descending order (highest priority first)

### Filtering Logic
```java
// Applied before prioritization
.filter(prioritizedComponent -> 
    StringUtils.isEmpty(componentNameFilter) || 
    matchesFilter(prioritizedComponent.getDisplayName(), componentNameFilter))
.filter(prioritizedComponent ->
    !filterOnPolicyActions || 
    Action.ID_FAIL.equals(prioritizedComponent.getAction()) ||
    Action.ID_WARN.equals(prioritizedComponent.getAction()))
```

## Frontend Implementation

### Component Hierarchy
```
PrioritiesPage
├── PrioritiesPageHeader    (breadcrumbs, metadata, actions)
├── PrioritiesPageTable     (filtering, table structure)
│   └── PrioritiesPageRow   (individual component row)
│       ├── BuildAction     (policy action display)
│       ├── Reachability    (reachability status)
│       ├── Recommendation  (version suggestions)
│       └── NextStep        (PR creation, etc.)
└── CreatePRModal          (pull request creation)
```

### State Management (Redux)
```javascript
// State shape
{
  priorities: PrioritizedComponent[],
  loadingTableData: boolean,
  loadErrorTableData: Error,
  page: number,
  pageSize: number,
  componentNameFilter: string,
  filterOnPolicyActions: boolean,
  recommendations: Map<string, Recommendation>,
  // ... other state
}
```

### Key Selectors
- `selectPrioritiesPageSlice`: Get entire priorities state
- `selectApplicationReportMetaData`: Get scan metadata
- `selectIsDeveloperDashboardEnabled`: Feature flag check

## Backend Implementation

### REST API Endpoints

#### Primary API
```java
@Path("rest/developer/priorities/{applicationId}/{scanId}")
@GET
public DevelopmentPrioritizationResults getPriorities(
    @PathParam("applicationId") String applicationId,
    @PathParam("scanId") String scanId,
    @QueryParam("page") int page,
    @QueryParam("pageSize") int pageSize,
    @QueryParam("componentNameFilter") String componentNameFilter,
    @QueryParam("filterOnPolicyActions") boolean filterOnPolicyActions
)
```

#### Public API (v2)
```java
@Path("api/v2/developer/priorities/{applicationId}/{scanId}")
@GET
public DevelopmentPrioritizationResults getPriorities(...)

@Path("api/v2/developer/priorities/{applicationId}/{scanId}/export")
@GET
@Produces("text/csv")
public Response getPrioritiesExport(...)
```

### Service Architecture

#### DevelopmentPrioritiesService
```java
// Main service methods
public DevelopmentPrioritizationResults getPrioritizedFindings(...)
public List<PrioritizedComponent> getAllPrioritizedFindings(...)

// Core algorithm
private int getScore(UnprioritizedComponent component)
private List<PrioritizedComponent> addPrioritiesToSortedList(...)
```

#### Key Dependencies
- `DevelopmentPrioritiesReportService`: Report data access
- `PolicyViolationReachabilityService`: Reachability analysis
- `ApplicationDAO`: Application metadata
- `PolicyEvaluationDAO`: Policy evaluation results

## Reachability Analysis

### Concept
Reachability analysis determines whether vulnerable code in a dependency can actually be executed from the application's entry points.

### Implementation
```java
public enum ReachabilityStatus {
    REACHABLE,      // Vulnerable code can be reached
    NON_REACHABLE,  // Vulnerable code cannot be reached
    UNKNOWN         // Reachability cannot be determined
}
```

### Analysis Process
1. **Code Path Analysis**: Trace execution paths from entry points
2. **Vulnerability Mapping**: Map vulnerabilities to specific code locations
3. **Status Determination**: Combine analysis results using precedence rules
4. **Policy Integration**: Factor reachability into priority scoring

### UI Representation
- **Reachable**: Red indicator with high priority
- **Non-Reachable**: Green indicator with lower priority
- **Unknown**: Gray indicator with medium priority

## Integration Points

### 1. **Navigation Entry Points**
- Developer Dashboard → Priorities button
- Reports Page → Priorities link (for external users)
- Integration workflows → Direct deep links

### 2. **Routing Configuration**
```javascript
// Multiple entry routes
'prioritiesPageFromDashboard'     // Internal developer flow
'prioritiesPageFromReports'       // External/management flow  
'prioritiesPageFromIntegrations'  // CI/CD integration flow
```

### 3. **Component Details Integration**
- Deep linking to component details from priorities table
- Breadcrumb navigation back to priorities
- Context preservation across navigation

### 4. **Pull Request Integration**
- Automated PR creation with remediation changes
- Integration with SCM systems (GitHub, GitLab, etc.)
- Branch and commit linking

## Data Flow

### 1. **Data Sources**
```
Scan Results → Policy Evaluation → Reachability Analysis
     ↓              ↓                    ↓
Application    Policy Threats    Vulnerability
Report         Components        Mappings
     ↓              ↓                    ↓
         Priorities Calculation
                    ↓
            Prioritized Results
```

### 2. **Frontend Flow**
```
Page Load → Load Report Metadata → Load Priorities Data
    ↓             ↓                      ↓
Route Params → API Request → Redux State Update
    ↓             ↓                      ↓
Filtering → Table Rendering → User Interaction
```

### 3. **Backend Processing**
```
API Request → Authorization Check → Data Retrieval
     ↓              ↓                    ↓
Policy Threats → Reachability → Component Analysis
     ↓              ↓                    ↓
Prioritization → Pagination → Response
```

## Testing Strategy

### Frontend Testing
```javascript
// Component testing
describe('PrioritiesPage', () => {
  it('renders priorities table with correct data');
  it('handles filtering correctly');
  it('navigates to component details');
});

// Integration testing  
describe('Priorities Integration', () => {
  it('loads data on route change');
  it('preserves filters in URL');
});
```

### Backend Testing
```java
// Unit testing
@Test
public void testPrioritizationScoring() {
    // Test scoring algorithm
}

// Integration testing
@Test  
public void testPrioritiesAPI() {
    // Test REST endpoints
}
```

### Functional Testing
```java
@Test
public void testPrioritiesPageWorkflow() {
    // End-to-end user workflow testing
    prioritiesPage.open(applicationId, scanId);
    prioritiesPage.verifyComponentPriorities();
    prioritiesPage.filterByComponent("slf4j");
    prioritiesPage.createPullRequest(0);
}
```

## Performance Considerations

### 1. **Backend Optimizations**
- **Pagination**: Limit data processing to current page
- **Lazy Loading**: Load remediation data only when needed
- **Caching**: Cache expensive reachability calculations
- **Database Indexing**: Optimize queries for large datasets

### 2. **Frontend Optimizations**
- **Virtual Scrolling**: Handle large result sets efficiently
- **Debounced Filtering**: Prevent excessive API calls
- **State Normalization**: Efficiently manage complex state
- **Component Memoization**: Prevent unnecessary re-renders

### 3. **API Design**
```java
// Efficient pagination
public DevelopmentPrioritizationResults getPrioritizedFindings(
    String applicationId,
    String scanId,
    int page,
    int pageSize,
    String componentNameFilter,
    boolean includeRemediation,  // Load expensive data only when needed
    boolean filterOnPolicyActions
)
```

### 4. **Memory Management**
- **Streaming**: Process large datasets without loading into memory
- **Connection Pooling**: Efficient database connections
- **Resource Cleanup**: Proper cleanup of temporary resources

## Key Files Reference

### Frontend
- `insight-brain-frontend/src/main/frontend/development/prioritiesPage/`
  - `PrioritiesPage.jsx` - Main page component
  - `PrioritiesPageTable.jsx` - Table and filtering
  - `PrioritiesPageRow.jsx` - Individual row rendering
  - `PrioritiesPageHeader.jsx` - Page header and navigation
  - `slices/prioritiesPageSlice.js` - Redux state management
  - `priorities.page.module.js` - Routing configuration

### Backend
- `insight-brain-service/src/main/java/com/sonatype/insight/brain/development/prioritization/`
  - `DevelopmentPrioritiesService.java` - Core business logic
  - `DevelopmentPrioritiesRestResource.java` - Internal REST API
  - `DevelopmentPrioritiesReportService.java` - Report data access
- `insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/`
  - `ApiDeveloperPrioritiesResourceV2.java` - Public API

### Data Models
- `insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/dto/`
  - `PrioritizedComponent.java` - Main data model
  - `DevelopmentPrioritizationResults.java` - API response model

## Conclusion

The Priorities Page represents a sophisticated approach to making security and policy data actionable for developers. By combining intelligent prioritization algorithms with reachability analysis and remediation recommendations, it transforms overwhelming scan results into a focused, actionable workflow.

Key success factors:
- **Developer-Centric Design**: Optimized for developer workflow and productivity
- **Intelligent Automation**: Reduces manual analysis through smart prioritization
- **Integration Focus**: Works seamlessly with existing development tools
- **Performance**: Handles large datasets efficiently
- **Extensibility**: Architecture supports new features and integrations

Understanding this implementation provides insight into how complex data analysis can be made accessible and actionable through thoughtful UX and system design.