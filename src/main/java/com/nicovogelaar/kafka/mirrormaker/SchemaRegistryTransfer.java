package com.nicovogelaar.kafka.mirrormaker;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.subject.TopicNameStrategy;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;
import kafka.consumer.BaseConsumerRecord;
import kafka.tools.MirrorMaker;
import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.cache.Cache;
import org.apache.kafka.common.cache.LRUCache;
import org.apache.kafka.common.cache.SynchronizedCache;
import org.apache.kafka.common.record.RecordBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class SchemaRegistryTransfer implements MirrorMaker.MirrorMakerMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryTransfer.class);

    private final CachedSchemaRegistryClient sourceSchemaRegistryClient;
    private final CachedSchemaRegistryClient targetSchemaRegistryClient;

    private final Cache<Integer, Integer> schemaCache;

    private static final Integer schemaCapacity = 1000;

    private static final byte MAGIC_BYTE = 0x0;

    private final SubjectNameStrategy<Schema> subjectNameStrategy;

    private final boolean includeKeys;
    private final List<String> whitelist;

    private static final String BASIC_AUTH_CREDENTIALS_SOURCE_URL = "URL";

    public SchemaRegistryTransfer(String args) {
        Arguments arguments = Arguments.parseArgs(args);

        Map<String, Object> configs = new HashMap<>();
        configs.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, BASIC_AUTH_CREDENTIALS_SOURCE_URL);

        sourceSchemaRegistryClient = new CachedSchemaRegistryClient(arguments.sourceUrl, schemaCapacity, configs);
        targetSchemaRegistryClient = new CachedSchemaRegistryClient(arguments.targetUrl, schemaCapacity, configs);

        schemaCache = new SynchronizedCache<>(new LRUCache<>(schemaCapacity));

        subjectNameStrategy = new TopicNameStrategy();

        includeKeys = arguments.includeKeys;
        whitelist = arguments.whitelist;
    }

    @Override
    public List<ProducerRecord<byte[], byte[]>> handle(BaseConsumerRecord record) {
        log.trace("Received event on topic {} with offset {}", record.topic(), record.offset());

        java.lang.Long timestamp = record.timestamp() == RecordBatch.NO_TIMESTAMP ? null : record.timestamp();

        if (!whitelist.isEmpty() && !whitelist.contains(record.topic())) {
            log.trace("Skipping event. Topic {} is not in the whitelist.", record.topic());
            return Collections.singletonList(
                    new ProducerRecord<>(record.topic(),
                                         record.partition(),
                                         timestamp,
                                         record.key(),
                                         record.value(),
                                         record.headers()));
        }

        byte[] key = record.key();

        if (includeKeys && key != null) {
            final ByteBuffer keyBuffer = ByteBuffer.wrap(record.key());
            copySchema(keyBuffer, record.topic(), true)
                    .ifPresent(targetSchemaId -> keyBuffer.putInt(1, targetSchemaId));
            key = keyBuffer.array();
        }

        byte[] value = record.value();

        if (value != null) {
            final ByteBuffer valueBuffer = ByteBuffer.wrap(value);
            copySchema(valueBuffer, record.topic(), false)
                    .ifPresent(targetSchemaId -> valueBuffer.putInt(1, targetSchemaId));
            value = valueBuffer.array();
        }

        return Collections.singletonList(
                new ProducerRecord<>(record.topic(),
                                     record.partition(),
                                     timestamp,
                                     key,
                                     value,
                                     record.headers()));
    }

    private Optional<Integer> copySchema(ByteBuffer buffer, String topic, boolean isKey) {
        if (!buffer.hasRemaining()) {
            log.trace("Buffer is empty");
            return Optional.empty();
        }

        if (buffer.get() != MAGIC_BYTE) {
            log.trace("Unknown magic byte!");
            return Optional.empty();
        }

        final int sourceSchemaId = buffer.getInt();

        Integer targetSchemaId = schemaCache.get(sourceSchemaId);
        if (targetSchemaId != null) {
            log.trace("Schema id {} has been seen before. Not registering with destination registry again.", sourceSchemaId);
        } else {
            log.trace("Schema id {} has not been seen before", sourceSchemaId);
            Schema schema;

            try {
                log.trace("Looking up schema id {} in source registry", sourceSchemaId);
                schema = sourceSchemaRegistryClient.getById(sourceSchemaId);
            } catch (IOException | RestClientException e) {
                String msg = String.format("Unable to fetch source schema for id %d.", sourceSchemaId);
                log.error(msg, e);
                return Optional.empty();
            }

            if (schema == null) {
                String msg = "Error getting schema from source registry. Not registering null schema with destination registry.";
                log.error(msg);
                return Optional.empty();
            }

            try {
                log.trace("Registering schema {} to destination registry", schema);
                String subjectName = subjectNameStrategy.subjectName(topic, isKey, schema);
                targetSchemaId = targetSchemaRegistryClient.register(subjectName, schema);
                schemaCache.put(sourceSchemaId, targetSchemaId);
            } catch (IOException | RestClientException e) {
                log.error(String.format("Unable to register source schema id %d to destination registry.", sourceSchemaId), e);
                return Optional.empty();
            }
        }

        return Optional.of(targetSchemaId);
    }
}
