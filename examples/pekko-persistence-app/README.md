## pekko-persistence-app example project
`pekko-persistence-app` is an example project to show real usage of the Dump Persistence Schema Compiler Plugin.
This project uses logic from the [pekko-microservices-tutorial](https://github.com/apache/incubator-pekko-platform-guide/blob/main/docs-source/docs/modules/microservices-tutorial/pages/index.adoc)
(the `shopping-cart-service`), with Pekko Serialization Helper added.

## Dump Persistence Schema Compiler Plugin usage
You can test Dump Persistence Schema Compiler Plugin on this example project. There are two possibilities to do it:
- `sbt compile` - compiles the project with the `dump-persistence-schema` phase enabled. `dump-persistence-schema` phase dumps detected persistence schema (types for Events and States) into `.json` files under the `target/dump-psersistence-schema-cache` directory of this project. These `.json` files are cache files used later by the `ashDumpPersistenceSchema` sbt task.
- `sbt ashDumpPersistenceSchema` &mdash; sbt task that creates final output of the dump using mentioned `.json` files. Output is saved into the `target/pekko-persistence-app-dump-persistence-schema-0.1.0-SNAPSHOT.yaml` file.
You can just invoke `sbt ashDumpPersistenceSchema` without `sbt compile` &mdash; `ashDumpPersistenceSchema` task will invoke `sbt compile` if needed.

## Running the sample code

1. Prepare docker containers with:

    ```
    docker-compose up -d
    ````

2. Create database tables with:

    ```
    docker exec -i pekko-persistence-app_postgres-db_1 psql -U shopping-cart -t < ddl-scripts/create_tables.sql`
    ```

3. Start a first node:

    ```
    sbt "run local1.conf"
    ```

4. (Optional) Start another node with different ports:

    ```
    sbt "run local2.conf"
    ```

5. (Optional) Check for service readiness:

    ```
    curl http://localhost:9101/ready
    ```

6. Interact with the service using [grpcurl](https://github.com/fullstorydev/grpcurl) - example request (put items in the cart):

    ```
    grpcurl -d '{"cartId":"cart1", "itemId":"T-shirt", "quantity":3}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.AddItem
    ```

7. Dump persistence schema with Pekko Serialization Helper (after shutting down the application):

    ```
    sbt ashDumpPersistenceSchema
    ```
Output saved into the `target/pekko-persistence-app-dump-persistence-schema-0.1.0-SNAPSHOT.yaml` file.
