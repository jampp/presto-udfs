# Jampp Presto User Defined Functions

This repository contains the **JSON_SUM** aggregation function.
It combines all the JSONs in a column by adding the shared keys and appending the rest.
It's used as an example for our [How to write custom Presto functions](https://geeks.jampp.com/data-infrastructure/technology/writing-custom-presto-functions/) blogpost.

## Build

Inside the [presto-udfs](./presto-udfs) folder, run:

```bash
mvn clean package
```

## Docker testing

To setup a Docker container with the UDFs, run:

```bash
docker run --name presto -p 8080:8080 -v $PWD/presto-udfs/target/presto-jampp-udfs-0.306/:/usr/lib/presto/plugin/udfs prestosql/presto:346
```

This will add the UDFs to the `/usr/lib/presto/plugin/` folder inside the container.

You can then connect to the Presto server through the [Presto CLI](https://prestosql.io/docs/current/installation/cli.html).
If you run the `SHOW FUNCTIONS;` command, you should see the `json_sum` on the list (it will appear once per input type).
