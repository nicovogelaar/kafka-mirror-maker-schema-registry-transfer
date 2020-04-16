# kafka-mirror-maker-schema-registry-transfer

This is a Kafka MirrorMaker message handler to copy Avro schemas between schema registries. 

## Usage

Arguments that can be passed to the message handler:

| Name        | Description                  | Required | Default          |
| ----------- | ---------------------------- | -------- | -----------------|
| sourceUrl   | Source schema repository URL | Yes      |                  |
| targetUrl   | Target schema repository URL | Yes      |                  |
| includeKeys | Include message key schema   | No       | False            |
| whitelist   | Whitelist topics             | No       |                  |
| subjectNameStrategy| Subject name strategy | No       |topicNameStrategy |

`subjectNameStrategy` possible values:
- `topicNameStrategy`
- `topicRecordNameStrategy`
- `recordNameStrategy`

Run Kafka MirrorMaker maker as follows:

```bash
$ kafka-mirror-maker [...] \
    --message.handler=com.nicovogelaar.kafka.mirrormaker.SchemaRegistryTransfer \
    --message.handler.args=sourceUrl=http://schema-registry-1:8081,targetUrl=http://schema-registry-2:8081,subjectNameStrategy=topicNameStrategy
```

## Example

Run the following commands to build the message handler, run a Kafka cluster with generated data and to start Kafka MirrorMaker to see whether the schema is being copied.

1. Build message handler:
    ```bash
    $ ./gradlew clean jar
    ```
2. Start Kafka clusters:
    ```bash
    $ docker-compose -f ./docker-composition/clusters.yml up
    ```
3. Start Kafka MirrorMaker once the clusters are up and running:
    ```bash
    $ docker-compose -f ./docker-composition/mirror-maker.yml up --build --force-recreate
    ```

## Reference

I used the following project as a reference:

* https://github.com/cricket007/schema-registry-transfer-smt 
