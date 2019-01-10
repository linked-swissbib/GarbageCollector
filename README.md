# GarbageCollector

Checks if obsolete `person` and `organisation` documents in
the Elasticsearch index are present and removes found documents.

`person` and `organisation` entities are linked by _bibliographic
resources_ via the respective identifiers. Since the these 
relations are of the `n:m` kind, we can't be sure if a `person` or
an `organisation` is obsolete (i.e. is not linked by a _bibliographic
resource_ anymore) unless a comparison between all respective links (`person` or
`organisation` identifiers) in `bibliographicResource` documents  and all present
`person` and `organisation` documents is performed. For that matter all links as well
as all `person` and `organisation` identifiers are fetched. If a
`person` or a `organisation` identifier can't be found in the links list,
the respective document will finally be removed from the index.

## Build

```bash
./gradlew clean shadowJar
```

The fat jar can be found in the `build/libs` directory.

## Usage

```bash
java -jar garbageCollector.jar --eshost host:port --esname cluster-name --esindex index-name
```
