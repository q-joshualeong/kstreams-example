package com.its.example.streams.models

import Types.ApplicationId
import com.its.example.streams.serdes.Implicits.serde
import io.circe.generic.auto._
import org.apache.kafka.common.serialization.Serde

case class ImportedApplication(
    applicationId: ApplicationId,
    name: String,
    age: String,
    nationality: String,
    details: Seq[ApplicationDetails]
)

object ImportedApplication {
  implicit val importedApplicationsSerde: Serde[ImportedApplication] = serde[ImportedApplication]
}
