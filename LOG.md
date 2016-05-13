# jungle

System for reporting, recording, and analyzing metrics from servers.

## log

**5/10 7PM -** Starting things off. I'm making a few assumptions until I hear back from the platform team. Chief among those assumptions is that servers reported their metrics to the receiving server using a common library.

I'm going to start by creating that library and an example server that uses it. Keeping all of this in the same project for now rather than going full-on simulation mode and splitting it into different repos.

**5/10 8PM -** `jungle.station` is filled out. Got sidetracked thinking about backpressure and logging systems. 

Plan for tomorrow:
- Set up `jungle.receiver`'s metric receipt/persistence mechanism. 
- Start up a fleet of example `jungle.station`s.
- Check load on receiver and make sure requests are being processed in parallel

Stretch goals/Thursday or Friday:
- Set up querying system.
- Check performance characteristics of querying before and after dramatically increasing amount of stored metric data.
- Clean up API/check edge case queries.
- Clean up docs

**5/12 2PM -** Okay, Didn't have time to do anything yesterday, which is good because after receiving a reply from the platform team and reviewing my code from Tuesday, it looks like I am solving a bunch of tertiary problems not related to the coding challenge.

Today, I decided to start by setting up a working system with the existing code. All the goals of the coding challenge are now met, going to remove all the extra stuff now and then perform load testing. The platform team's email specified that it should handle bursts of ~100 inserts/sec for about a minute at a time on a magical computer with unlimited memory :).

**5/12 4PM -** Load test passing for sustained rate of 100 req/sec, cleaned up code.