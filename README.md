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

