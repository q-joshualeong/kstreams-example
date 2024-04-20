package com.its.example.streams.models

import Types.ApplicationId
import com.its.example.streams.serdes.Implicits.serde
import io.circe.generic.auto._
import org.apache.kafka.common.serialization.Serde

case class ApplicationDetails(
    applicationId: ApplicationId,
    createdDate: String,
    version: String,
    description: String
)

object ApplicationDetails {
  implicit val applicationDetailsSerde: Serde[ApplicationDetails] = serde[ApplicationDetails]
}
