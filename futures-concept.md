The concept of futures is not limited to asynchronous programming in Java.
This [wikipedia article](https://en.wikipedia.org/wiki/Futures_and_promises) has more details about it.

## Producers and consumers

You can think of futures from two sides - producers and consumers.

We usually spend most of the time using futures as consumers, and then we usually seem them as read-only containers
that change state once. We can subscribe to be notified when the state changes and trigger further work.

From a producer side, we should instead use the term Promise. You create a _promise_ that you will produce a value
and some time later when the value is ready, you complete or fulfill the promise. The future associated with the promise
will thus complete and the consumer can act on it.

You can think of it as the following flow:
* The producer creates a Promise
* The produces extracts the Future from the Promise and gives the Future to the consumer.
* The consumer starts registering callbacks and transformations on the future.
* At some point later the producer fulfills the Promise and the callbacks will trigger for the consumer. 

