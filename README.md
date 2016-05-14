#jungle

Submission for Quartet core platform coding challenge.

-----

## API

### /record-metric

Store a new metric record

- *name (string):* name of the metric
- *timestamp (number):* unix epoch time (ms)
- *value (number):* value of the metric

### /query/aggregate

Sum the values for a metric within a given time range

- *name (string):* name of the metric
- *from (number):* beginning of range in unix epoch time (ms)
- *to (number):* end of range in unix epoch time (ms)

### /query/names

Retrieve the names of all recorded metrics

*(no parameters)*

### /query/at

Last known value for a metric at a point in time

- *name (string):* name of the metric
- *time (number):* unix epoch time (ms)

-----

## Namespaces

### jungle.receiver

HTTP Layer and owner of system state.

### jungle.api

Helper functions and middleware for implementing an API.

### jungle.metric

Handlers and logic for updating and querying metrics.

### jungle.station

Testing utility for simulating the behavior of a server talking to a `receiver` over HTTP.

### jungle.config

Holds routing information common to `receiver` and `station`.

