package com.nicovogelaar.kafka.mirrormaker;

import kafka.consumer.BaseConsumerRecord;
import org.apache.kafka.common.record.TimestampType;
import org.junit.Test;

public class SchemaRegistryTransferTest {

    @Test
    public void testSchemaRegistryTransfer() {
        final String args = "sourceUrl=http://schema-registry-1:8081,targetUrl=http://schema-registry-2:8081,includeKeys=true";
        SchemaRegistryTransfer schemaRegistryTransfer = new SchemaRegistryTransfer(args);
        schemaRegistryTransfer.handle(new BaseConsumerRecord("topic",
                                                             0,
                                                             0,
                                                             0,
                                                             TimestampType.NO_TIMESTAMP_TYPE,
                                                             "".getBytes(),
                                                             "value".getBytes(),
                                                             null));
    }

    @Test
    public void testSchemaRegistryTransfer_WithNullKey() {
        final String args = "sourceUrl=http://schema-registry-1:8081,targetUrl=http://schema-registry-2:8081,includeKeys=true";
        SchemaRegistryTransfer schemaRegistryTransfer = new SchemaRegistryTransfer(args);
        schemaRegistryTransfer.handle(new BaseConsumerRecord("topic",
                                                             0,
                                                             0,
                                                             0,
                                                             TimestampType.NO_TIMESTAMP_TYPE,
                                                             null,
                                                             "value".getBytes(),
                                                             null));
    }
}
