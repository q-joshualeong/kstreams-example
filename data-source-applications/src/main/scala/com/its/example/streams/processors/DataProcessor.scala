package com.its.example.streams.processors

import com.its.example.streams.models.{Application, ApplicationDetails, ImportedApplication}
import com.its.example.streams.StateStores.{applicationStateStoreName, consolidatedStateStoreName, retryStore}
import com.its.example.streams.Topics.{ImportedApplicationTopic, IntermediateJoinsTopic}
import com.its.example.streams.models.Types.{ApplicationId, IntermediateJoinedApplications}
import com.its.example.streams.models._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.processor.api.{Processor, ProcessorContext, Record}
import org.apache.kafka.streams.processor.{PunctuationType, Punctuator}
import org.apache.kafka.streams.state.{KeyValueStore, ValueAndTimestamp}
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.asScalaIteratorConverter

class DataProcessor
    extends Processor[ApplicationId, IntermediateJoinedApplications, ApplicationId, ImportedApplication] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val intermediateJoinedApplicationsSerde: Serde[IntermediateJoinedApplications] =
    Application.intermediateJoinedApplicationsSerde
  private val importedApplicationsSerde: Serde[ImportedApplication] = ImportedApplication.importedApplicationsSerde
  private var retryStateStore: KeyValueStore[ApplicationId, ApplicationDetails] = _
  private var consolidatedStateStore: KeyValueStore[ApplicationId, ImportedApplication] = _
  private var applicationStateStore: KeyValueStore[ApplicationId, Application] = _
  private var context: ProcessorContext[ApplicationId, ImportedApplication] = _

  override def init(context: ProcessorContext[ApplicationId, ImportedApplication]): Unit = {
    this.context = context
    this.retryStateStore =
      context.getStateStore(retryStore).asInstanceOf[KeyValueStore[ApplicationId, ApplicationDetails]]
    this.consolidatedStateStore =
      context.getStateStore(consolidatedStateStoreName).asInstanceOf[KeyValueStore[ApplicationId, ImportedApplication]]
    this.applicationStateStore =
      context.getStateStore(applicationStateStoreName).asInstanceOf[KeyValueStore[ApplicationId, Application]]

    // Schedule the RetryJoinPunctuator for periodic execution
    val retryInterval = 1.minutes // Adjust the retry interval as needed
    context.schedule(
      Duration.ofMinutes(retryInterval.toMinutes),
      PunctuationType.WALL_CLOCK_TIME,
      new Punctuator {
        override def punctuate(timestamp: Long): Unit = {
          logger.info("Starting punctuator")
          val keysToRetry = retryStateStore.all().asScala.map(_.key).toSeq

          keysToRetry.foreach {
            key =>
              logger.info(s"Processing retry key: $key")
              retryStateStore.get(key) match {
                case null =>
                  logger.error(s"Failed to retrieve applicationDetails for key: $key")

                case applicationDetails =>
                  // Check if the applicationDetails are available
                  // If available, attempt to perform the join with the KTable of applications
                  logger.info(s"$applicationDetails")
                  val application = applicationStateStore.get(key).asInstanceOf[ValueAndTimestamp[Application]]
                  application.value() match {
                    case null => logger.info(s"Still no application found for key: $key")
                    case application => {
                      val intermediateJoinedApplication = (applicationDetails, Some(application))
                      val serialisedRecord = Application.intermediateJoinedApplicationsSerde
                        .serializer()
                        .serialize(IntermediateJoinsTopic, intermediateJoinedApplication)
                      val outputRecord: Record[ApplicationId, IntermediateJoinedApplications] =
                        new Record(key, serialisedRecord, System.currentTimeMillis())
                          .asInstanceOf[Record[ApplicationId, IntermediateJoinedApplications]]
                      logger.info(s"Retried join for key: $key")
                      process(outputRecord)
                      logger.info(s"Retry successful, deleting key from retrystatestore.")
                      retryStateStore.delete(key)
                    }
                  }
              }
          }
        }
      }
    )
  }

  override def process(record: Record[ApplicationId, IntermediateJoinedApplications]): Unit = {
    val payload: IntermediateJoinedApplications = intermediateJoinedApplicationsSerde
      .deserializer()
      .deserialize("kstreams-intermediate-applications-and-details-topic", record.value().asInstanceOf[Array[Byte]])
    payload match {
      case (applicationDetail, None) => {
        logger.info(s"Failed to find a key for: ${record.key()}. Updating in retry state store")
        retryStateStore.put(record.key(), applicationDetail)
      }
      case (applicationDetails, Some(application)) => {
        logger.info(
          s"Found a key for :${record.key()}. Application is: ${applicationDetails.toString} and Detail is: ${application.toString}"
        )
        val finalApplication = processJoin(application, applicationDetails)
        val existingApplication = consolidatedStateStore.get(record.key())
        val updatedApplication = if (existingApplication != null) {
          // Combine the details of the existing application with the new one
          logger.info(s"Key: ${record.key()} exists in consolidated store. Combining with existing application.")
          existingApplication.copy(details = existingApplication.details ++ finalApplication.details)
        } else {
          logger.info(s"Key: ${record.key()} not exist in consolidated store. Adding new record.")
          finalApplication
        }

        // Forward the finalApplication to the output topic
        val serialisedRecord =
          importedApplicationsSerde.serializer().serialize(ImportedApplicationTopic, updatedApplication)
        val outputRecord: Record[ApplicationId, ImportedApplication] =
          new Record(record.key(), serialisedRecord, System.currentTimeMillis(), record.headers())
            .asInstanceOf[Record[ApplicationId, ImportedApplication]]
        context.forward(outputRecord, ImportedApplicationTopic)
        consolidatedStateStore.put(record.key(), updatedApplication)
      }
    }
  }

  def processJoin(application: Application, detail: ApplicationDetails): ImportedApplication = {
    ImportedApplication(
      applicationId = application.applicationId,
      name = application.name,
      age = application.age,
      nationality = application.nationality,
      details = Seq(detail)
    )
  }

  override def close(): Unit = {
    // Close any resources if necessary
  }
}
