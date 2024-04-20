package com.its.example.streams

import com.its.example.streams.models.{Application, ApplicationDetails, ImportedApplication}
import com.its.example.streams.processors.DataProcessor
import com.its.example.streams.Processors.dataProcessor
import com.its.example.streams.StateStores.{applicationStateStoreName, consolidatedStateStoreName, retryStore}
import com.its.example.streams.Topics.{ApplicationDetailsTopic, ApplicationsTopic, ImportedApplicationTopic, IntermediateJoinsTopic}
import com.its.example.streams.models.Types.{ApplicationId, IntermediateJoinedApplications}
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.{KStream, KTable, Materialized}
import org.apache.kafka.streams.scala.serialization.Serdes._
import org.apache.kafka.streams.state.{KeyValueStore, StoreBuilder, Stores}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig, Topology}
import org.slf4j.LoggerFactory

import java.util.Properties

object KafkaStreamsJoinExample {
  val builder = new StreamsBuilder()
  val retryStateStoreBuilder: StoreBuilder[KeyValueStore[ApplicationId, ApplicationDetails]] = {
    Stores.keyValueStoreBuilder(
      Stores.persistentKeyValueStore(retryStore),
      stringSerde,
      ApplicationDetails.applicationDetailsSerde // Replace with your value Serde
    )
  }
  val consolidatedStateStoreBuilder: StoreBuilder[KeyValueStore[ApplicationId, ImportedApplication]] = {
    Stores.keyValueStoreBuilder(
      Stores.persistentKeyValueStore(consolidatedStateStoreName),
      stringSerde,
      ImportedApplication.importedApplicationsSerde // Replace with your value Serde
    )
  }
  val applicationStateStoreBuilder: StoreBuilder[KeyValueStore[ApplicationId, Application]] = {
    Stores.keyValueStoreBuilder(
      Stores.persistentKeyValueStore(applicationStateStoreName),
      stringSerde,
      Application.applicationsSerde // Replace with your value Serde
    )
  }
  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val props = new Properties()
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstreams-poc")
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerde.getClass)
    props.put(
      StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
      classOf[LogAndFailExceptionHandler]
    );

    val topology = applicationsTopology()

    logger.info(topology.describe().toString)

    val application: KafkaStreams = new KafkaStreams(topology, props)

    application.start()
  }

  def applicationsTopology(): Topology = {
    val applicationTable: KTable[ApplicationId, Application] = builder.table[ApplicationId, Application](
      ApplicationsTopic,
      Materialized.as[ApplicationId, Application, ByteArrayKeyValueStore](applicationStateStoreName)
    )
    val detailsStream: KStream[ApplicationId, ApplicationDetails] =
      builder.stream[ApplicationId, ApplicationDetails](ApplicationDetailsTopic)
    val retryProcessorSupplier: ProcessorSupplier[ApplicationId, IntermediateJoinedApplications, ApplicationId, ImportedApplication] =
      () => new DataProcessor

    val joinedStream: KStream[ApplicationId, IntermediateJoinedApplications] =
      detailsStream.leftJoin(applicationTable)((details, application) => {
        if (application == null) {
          (details, None)
        } else (details, Option(application))
      })

    joinedStream
      .to(IntermediateJoinsTopic)

    val topology = builder.build()
    topology.addSource(IntermediateJoinsTopic, IntermediateJoinsTopic)
    topology.addProcessor(dataProcessor, retryProcessorSupplier, IntermediateJoinsTopic)
    topology.addStateStore(retryStateStoreBuilder, dataProcessor)
    topology.addStateStore(consolidatedStateStoreBuilder, dataProcessor)
    topology.addSink(ImportedApplicationTopic, ImportedApplicationTopic, dataProcessor)
    topology.connectProcessorAndStateStores(dataProcessor, applicationStateStoreName)
  }
}
