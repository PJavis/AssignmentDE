package org.kafka;

import org.apache.kafka.clients.producer.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class Producer {
    private static final String TOPIC_NAME = "vdt2024";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9094";
    private static final String logPath = "src\\data\\log_action.csv";
    private static final String danhsachPath = "src\\data\\danh_sach_sv_de.csv";

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props);
             BufferedReader reader = new BufferedReader(new FileReader(logPath))) {
            String line;
            int recordCount = 0;

            while ((line = reader.readLine()) != null) {
                // Process the CSV line and create a ProducerRecord
                String[] fields = line.split(",");
                String key = getKey(fields); // Control partitions of records
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, key, line);

                // Send the record and handle the result with callback
                producer.send(record, (metadata, exception) -> {
                    if (exception == null) {
                        System.out.println("Record id " + record.key() + " sent successfully - topic: " + metadata.topic() +
                                ", partition: " + metadata.partition() +
                                ", offset: " + metadata.offset());
                    } else {
                        System.err.println("Error while sending record " + record.key() + ": " + exception.getMessage());
                    }
                });

                recordCount++;
            }

            System.out.println(recordCount + " records sent to Kafka successfully.");
        } catch (IOException e) {
            System.out.println("Source not found or Can't connect to Kafka Broker");
            System.out.println(e.getMessage());
        }
    }

    private static String getKey (String [] fields) {
        if (Integer.parseInt(fields[0]) % 2 == 0)
            return "even";
        else return "odd";
    }

    private static void sendBatch(KafkaProducer<String, String> producer, List<ProducerRecord<String, String>> batchRecords) {
        for (ProducerRecord<String, String> record : batchRecords) {
            producer.send(record);
        }
        producer.flush();
    }
}