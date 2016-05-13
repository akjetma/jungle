#jungle

Submission for Quartet core platform coding challenge.

-----

## API

### /metric

**Description** - Store a new metric record

**Parameters**

- *name (string):* name of the metric
- *timestamp (number):* unix epoch time (ms)
- *value (number):* value of the metric

### /aggregate

**Description** - Sum the values for a metric within a given time range

**Parameters**

- *name (string):* name of the metric
- *from (number):* beginning of range in unix epoch time (ms)
- *to (number):* end of range in unix epoch time (ms)

### /names

**Description** - Retrieve the names of all recorded metrics

**Parameters**

*(none)*

### /at

**Description** - Last known value for a metric at a point in time

**Parameters**

- *name (string):* name of the metric
- *time (number):* unix epoch time (ms)

-----

## Namespaces

### jungle.receiver

HTTP Layer and owner of system state.

### jungle.metric

Handlers and logic for updating and querying metrics.

### jungle.station

Testing utility for simulating the behavior of a server talking to a `receiver` over HTTP.

### jungle.config

Holds routing information common to `receiver` and `station`.