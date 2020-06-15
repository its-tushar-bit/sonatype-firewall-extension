# ADR 2. Revised Event Architecture for Pull Request Commenting 

Date: 2020-06-04

## Status

Accepted

## Context

Pull request commenting has two flows reflecting the two ways in which users interact with pull requests in relation
to policy evaluations being run against their work/commits:

1. The polling flow handles the scenario where the user has completed all their work and then creates the pull request.
   In this case their last commit triggered the immediate flow (described next) but there was no PR yet to act on.
    
2. The immediate flow acts in response to policy evaluations which are typically initiated by a CI system in response 
   to user commits.  In this case there may or may not be a PR yet for that commit.  This flow handles the scenarios
   where new commits are made AFTER a PR has been created.

PR polling currently tries to discover 10 open PRs from the SCM system each polling cycle (set to 60 seconds).
So, at maximum, we process 10 pull request comments per minute which seemed like a reasonable amount of work
to put on the system while at the same time gradually eating away at any initial load of commentable PRs that exist
when this capability is first deployed to a customer's system.  Steady state we anticipate fewer than 10 new pull
requests per minute will be created.

However, with the addition of PR line commenting it has become apparent that processing 10 pull requests per minute
could potentially throw more work at the system than it can handle in any given minute (each PR comment results in 
some git client work to discover commentable locations for each resolvable violation AND an SCM API call to create
the line comment).

And because the PR commenting work is currently performed on the async event bus thread that invoked it, as this work
backs up over time we've noticed during load testing that the async event bus thread pool (currently set to 500) could 
be exhausted and the AsyncEventBusDiscardPolicy invoked.

Caveat: The load test throws an inordinate amount of work at the system - perhaps 5 or 10 times what we'd expect from
a large customer during initial onboarding (steady state the load should be much, much less than this).  However, even 
if PR commenting only kept 50 or 100 threads from the pool active at any time that's still far too many and is a problem
that must be solved.

How all SCM pull request activities fit into the context of insight-brain is captured here:
[SCM messaging architecture](https://docs.sonatype.com/display/INT/SCM+messaging+architecture)

Much of the PR work is currently pushed off onto their own worker threads (i.e. location discovery, PR status, proactive
automated PRs).  Of note, the source control task runner is currently used to resolve competition for file/git resources
between PR comment location discovery and proactive PR branch creation.

One suggestion was to add the PR commenting flows to the source control task runner, but:
  - location discovery is already there 
  - the only other contention for resources is with the SCM API itself and that is already solved elsewhere
  - this would result in greater, and arguably unnecessary, refactoring of the PR commenting flows
  
Another suggestion is to move the PR commenting work onto its own thread pool.  But this has the disadvantage of 
accumulating all this work in memory and is thus vulnerable to IQ server restarts.  Having said that, this is part
of the new solution with that key disadvantage mitigated, as described below.

## Decision

Given the above:
 - We will create a durable event queue by means of a new database table for events
 - Instead of the PullRequestPollingService pushing events onto the async event bus it will instead create entries
   in the event table
 - Likewise, instead of the PullRequestCommentingService handling the application evaluation event work on the async
   event bus thread it will also create entries in the event table and quickly return the async bus thread to its pool
 - We will create a scheduled pull request work processor that pulls some quantity of work from the DB event table
   at regular intervals and adds worker tasks to a new thread pool to complete that work with some small amount of 
   parallelization (no more than 10 threads).  The amount of work currently waiting in the pool/queue will drive how
   much work we pull from the event table each time we pull work. 
 - We will retain event entries in the database for some short period after event processing completes to facilitate 
   debugging and troubleshooting.  As such, the events will go thru some sort of state transition, such as 'new',
   'in process', 'error', 'completed', etc.
   
## Consequences

This approach will have the following benefits:
  - It moves work quickly off of the async event bus.
  - It allows us to potentially process more than 10 discovered PRs per minute (not every PR will result in an IQ
    generated PR comment). 
  - The system can process the maximum amount of work possible in any given period without having an excessive
    amount of work pool up in memory.
  - Pull request commenting work becomes more resilient and is immune to IQ server restarts.
  - SCM API errors that are certain to occur from time to time should no longer prevent PR comments from being created.
  - This solution dovetails nicely into the current design of PR commenting.
  - This solution is relatively straightforward and quick to implement.    
___
