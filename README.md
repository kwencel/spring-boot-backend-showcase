# Backend showcase

## How to run
In order to build the project (and also run the tests), you need to have Docker installed.

It is because I am using TestContainers for tests, and Docker Compose for running the app itself.
I have mixed feelings about using a different DB for testing and actual application runtime, so I have made
sure to use the same database for both. PostgreSQL was my weapon of choice.

In order to run the tests, execute `./gradlew tests`

In order to build and run the application, execute `./gradlew assemble`.

Next, preferably in a separate console, run `docker-compose up`, to start PostgreSQL.
Finally, execute the application by running:

```
java -XX:MaxDirectMemorySize=10M -Dmovie-detail-provider.omdb.api-key=<your-omdb-api-key> -jar build/libs/backend-showcase-0.0.1-SNAPSHOT.jar
```
You can also pass the external properties by other means as described [here](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)

If you don't provide your OMDb API key, the application will still work, just the movie detail fetching feature won't be available.
The application has been coded with such scenario in mind.

The `-XX:MaxDirectMemorySize` parameter sets the maximum size of Direct Memory Buffers which can grow to enormous sizes if not limited.
It is especially relevant when dealing with data streaming from external API (which I have used for OMDb handling).
Overall, it is a good practice to limit various parts of JVM memory to have a consistent and predictable performance
and resource usage.

# Things that can be improved
Not all things are perfect, mostly due to limited amount of time I could dedicate to this project.

**Spring Security**. It currently uses in-memory storage, with the following predefined credentials:
`user1:user1`, `user2:user2`, `admin:admin` using Basic Auth. A better way to do it would be to use a database-backed user
credential storage and to use more sophisticated authentication mechanism, like JWTs.

**Movie ratings could be utilized better**. Currently, a user can rate movies and see the rates he's given.
Users don't see ratings of each other. I think it would be nice if the backend exposed an average rating of any movie,
visible to anyone. However, that would need to be done cleverly because computing the averages on every request
is an obvious waste of resources. On the other hand, we can't just update the average and completely disregard the
individual ratings, because users could then rate the same movies multiple times, rendering the feature useless.

**The API is not completely RESTful**. For instance, it does not embrace [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS)
like [Spring Data Rest](https://spring.io/projects/spring-data-rest) project does. Initially I wanted to base this
application on Spring Data Rest, but it turned out it does not support WebFlux and requires Servlet environment to work.
For this reason, I decided to implement the API myself, as I wanted to make use of the newer, much lighter
and more performant stack.

**_PUT_ and _PATCH_ could be utilized more**. Currently, there is no way to partially update or to
completely replace the resource (except for the movie rating).

# Design choices
**Consistent database environment**. I have personally encountered many problems when dealing with different databases
in production and in the test environment. The more close your tests are to the real thing, the better.
This is also why I have refrained from mocking data, and tried to operate on real DB and Spring Context instead.

These kinds of tests run longer (however I have made sure to not trigger Spring Context refreshes, so after Spring boots up,
everything runs fast). However, it is much closer to production use. Mocking repositories or using
[@DataJpaTest](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/autoconfigure/orm/jpa/DataJpaTest.html)
(which wraps every test method in a transaction) can influence the behaviour of the code, by running it in the
transactional context during tests, but not on production, for instance. That is why my tests are mostly E2E instead of
being unit tests with a lot of mocking.

**Proxying the response from OMDb**. I was wondering whether to stick to some subset of the fields that would most likely
always be available, or just proxy the whole response to the client. I decided to do the latter - because OMDb does not
provide a schema, and I wanted to show how WebFlux can be used to stream the response from some external service to the client.
In this example it doesn't give much benefit, but when dealing with large payloads or streaming some BLOBs, it is a really
memory-efficient way of solving the problem.

**Global error handler**. It is important for the backend not to expose internal information about the error that has occurred.
To make sure it doesn't happen, I have made a global exception handler where I enforce a certain body structure, which
also enabled me to throw custom exceptions from controllers and make error handling elegant.

**Hibernate performance optimizations**. I'm a big advocate of paying huge attention to the Entities and their design.
Most of the time, the backend-database communication overhead is the bottleneck, so it's crucial to have
a good understanding of Java Persistence API and how to make most out of it.

In this project, I have made use of techniques such as:
- Inserts and updates batching
- SEQUENCE generation type instead of IDENTITY (required for batching)
- Prepared statements caching
- Server-side performance statements
- EntityGraphs for eager fetching when it makes sense

Here are some links for further reading:
- https://dzone.com/articles/50-best-performance-practices-for-hibernate-5-amp
- https://vladmihalcea.com/postgresql-serial-column-hibernate-identity
- https://vladmihalcea.com/how-to-batch-insert-and-update-statements-with-hibernate
- https://vladmihalcea.com/postgresql-multi-row-insert-rewritebatchedinserts-property
- https://vladmihalcea.com/jpa-hibernate-synchronize-bidirectional-entity-associations
- https://vladmihalcea.com/the-best-way-to-map-a-naturalid-business-key-with-jpa-and-hibernate
- https://www.baeldung.com/spring-open-session-in-view
- https://vladmihalcea.com/hibernate-facts-equals-and-hashcode
- https://stackoverflow.com/questions/40194614/spring-data-jpa-projection-selected-fields-from-the-db
