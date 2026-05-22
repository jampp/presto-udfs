# Jampp Trino User Defined Functions

This repository contains the **JSON_SUM** aggregation function.
It combines all the JSONs in a column by adding the shared keys and appending the rest.
It's used as an example for our [How to write custom Presto functions](https://geeks.jampp.com/data-infrastructure/technology/writing-custom-presto-functions/) blogpost.

Targets **Trino 470**. Build requires JDK 23.

## Build

Inside the [presto-udfs](./presto-udfs) folder, run:

```bash
mvn clean package
```

## Docker testing

To setup a Docker container with the UDFs, run:

```bash
docker run --name trino -p 8080:8080 -v $PWD/presto-udfs/target/trino-jampp-udfs-470/:/usr/lib/trino/plugin/udfs trinodb/trino:470
```

This will add the UDFs to the `/usr/lib/trino/plugin/` folder inside the container.

You can then connect to the Trino server through the [Trino CLI](https://trino.io/docs/current/installation/cli.html).
If you run the `SHOW FUNCTIONS;` command, you should see the `json_sum` on the list (it will appear once per input type).
