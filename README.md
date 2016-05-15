#jungle

Submission for Quartet core platform coding challenge.

-----

## API

**Notes:**

- Parameters are query-string parameters
- HTTP verb is not important (not a RESTful API)
- Top-level HTTP status codes are used (x00), but not particularly informative
- Responses are JSON
- Error responses have an `error` key, normal responses have a `value` key (the two are mutually exclusive)
- The value of the `error` key is a string name/title for the error. You can think of this as the error 'type'.
- Error responses optionally have a `message` key that offers more context-specific, dynamic information.

### /record/metric

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

-----

## Approach and Design

I started working on this project with an eye towards performance and succinctness. It seemed like a fairly straightforward task and I wanted the code to be as brief and to-the-point as possible. Once I had a working system that met the requirements of the project, I realized the larger task of the challenge that I had ignored was the design of the API. 

Lately, I've been spending most of my time working with Clojure/Clojurescript systems that communicate arbitrary edn data to each other, bidirectionally and asynchronously over a websocket. The way messages are conveyed and how APIs/routers are defined in these systems is quite a bit different from the way things are done with traditional APIs. 

When designing the interface between a client and server that are both under your control, The onus of input validation and error-handling moves to the front-end, leaving the client-server interface relatively pure/simple. With websockets in particular, handling server-side errors is much less of a problem--pass the websocket around the system and just send an error message to the client when one is encountered. Your primary concern becomes the sanity of your client-side dispatching/routing logic.

For an API used by clients that are out of your control, one of your main concerns is the design of a response format that simultaneously conveys data, metadata about that data, feedback about requests, messages about server health, etc. Another main concern is creating a system that constructs these overloaded messages without using gotos or being overly repetitive. Having specific, meaningful error messages is critical to the usability of a public API, and creating a system that can convey these messages without relying on try/throw/catch is critical to its maintainability. 

These were the goals that I had in mind while working on this project. There are still some gaps in the design of the system and quite a few validations that would need to be dropped in before it would be ready-for-production, but I wanted to show my interest in coming up with patterns for writing maintainable code rather than building something watertight.
