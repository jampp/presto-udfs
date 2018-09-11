# Jampp Presto User Defined Functions

This repository contains the **JSON_SUM** aggregation function.
It combines all the JSONs in a column by adding the shared keys and appending the rest.
It's used as an example for our [How to write custom Presto functions](TODO) blogpost.

## Testing

The docker-compose and Makefile located in [docker-presto-cluster/](docker-presto-cluster/) are slightly modified versions of the ones found in this [repo](https://github.com/Lewuathe/docker-presto-cluster).

### Build image

```bash
make
```

### Launch presto

Presto cluster can be launched by using docker-compose.

```bash
make run
```

or

```bash
make run-with-logs
```

### Shut presto down

```bash
make down
```
