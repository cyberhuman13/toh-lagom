# toh-lagom

This is a simple example of how an event-sourced backend can be developed for deployment in AWS, following the CQRS principles
(Command Query Responsibility Segregation). This proof of concept uses the well-known Tour of Heroes tutorial of Angular for the frontend,
but adding a backend instead. Granted, the event-sourced solution presented here will be an overkill for a toy example—like shooting birds
with a cannon—but our purpose is to illustrate an approach that is easy to generalize for other, more complex applications.

This solution is implemented in Scala. A similar solution is implemented in Java in a different repository (https://github.com/cyberhuman13/toh-lagom-java).
