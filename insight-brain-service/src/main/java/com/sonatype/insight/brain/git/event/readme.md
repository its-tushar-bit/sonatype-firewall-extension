<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Source Control Event Processing

How we manage the interactions with the various source control providers with respect to IQ for SCM has changed considerably over the last couple years of development.

* Initially we were piggy-backing off of the async event bus but that did not scale and the volume of source control events overwhelmed the event bus.
* We then implemented a separate queue for source control specific events via the source_control_event DB table and associated publisher and processing classes.  The key classes driving that implementation were the SourceControlEventProcessingScheduler and the SourceControlEventService.
* That served us well for a while but still did not scale as we'd like and had some inefficiencies.  We noticed we were throwing more work at the event service than it could reasonably process during a given cycle and that work wasn't organized beyond some simplistic prioritization.
* We then invented the SourceControlEventOrchestrator to process and sequence events in a more orderly and thoughtful manner.

Also, a new key piece of the puzzle is the UserEventManager which handles the events for a particular configured SCM user (so we can make better use of parallelization).

## The orchestrator

One of the key changes we made with the orchestrator is to prioritize and sequence events immediately, in memory, with the database being the persistent backup for events.

This allows use to better plan how to execute the work in real time rather than relying solely on the DB query that runs every 30 seconds or so (which is still necessary to ingest events coming from other IQ instances  that might be running - currently, only one IQ instance can be the one that performs the actual interaction with the SCM systems).

### Thoughts on future enhancements for multi-node IQ
On the topic of multi-node IQ, in order to allow each IQ instance to process source control events there would have to be some sort of load balancing mechanism that told each instance which events to process with the restriction that all events for the same SCM user MUST be processed by the same IQ instance as there is downstream coordination in the embedded scm-client that ensures we don't send simultaneous API calls for the same user to the SCM system.
* So, any advanced multi-IQ event processing would have to leverage the username we now capture with the events and make sure only one instance of IQ server is processing events for a given user at any one time.
* A simple lookup table could be used for that but then something more sophisticated that helped monitor and balance the SCM workload for each instance could be put in place.

### The User Event Manager

The orchestrator creates a UserEventManger object for each configured SCM user (lazily) and funnels events for each user to the proper one.

The heavy lifting for the prioritization, sequencing and parallelization of events is accomplished by the UserEventManager.  The user event managers use a set of rule objects to determine which events, how many of them, and in what sequence they should be processed.

Not all events are equal.  For example, commit status events are super simple - just one API call and the information we need is already available.  On the other hand, pull request events are much more complicated and can result in many API calls as well as performing one or more internal source control evaluations to gather the needed information for a PR comment.

There are two categories of rules:
* Selection rules which determine how many and what kind of events can be submitted for parallel processing.  We have to be especially careful with remediation pull request events as GitHub is very sensitive to those and we can only process one or two at a time.
* Processing rules that handle things like performance throttling and error retries.

### Event Status Listener

We capture pretty detailed information on every event we process, especially for errors where we capture the actual stack traces in the DB.  This is invaluable during testing as well as for monitoring our own policy server.
So, for every event we capture the result and here is how the process works:

     database or publisher -> orchestrator -> user event manager -> event processor -> status listener -> database

* The orchestrator receives the events (from the publisher [push] or the database [pull], as appropriate) and sends them to the correct user event manager
* The user event manager prioritizes and sequences the events and sends them, one by one, to the processor per the rules previously mentioned
* The processor handles each event in its own thread and invokes the appropriate class to perform the work specific to each event type
* When the event completes or errors out the processor sends the result to the status listener and it gets recorded in the database (source_control_event table)

The status listener IS the user event manager but to break the cyclic dependency we had to introduce the listener interface (and it's good design anyway).

### Event Processor

The source control event processor is where the work specific to each event actually gets done.  A thread pool services the events and the appropriate handlers are invoked.  

The processor also controls access to each repository (via the application) so that SCM operations for the same repository occur sequentially.  We use the application ID to control this, so it's not perfect as we could be using the repo URL instead, but we're giving each app it's own git workspace, so we don't really have a problem other than perhaps multiple copies of the same git repo being used).
