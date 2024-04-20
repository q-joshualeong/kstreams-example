package com.its.example.streams.models

import Types.{ApplicationId, IntermediateJoinedApplications}
import com.its.example.streams.serdes.Implicits.serde
import io.circe.generic.auto._
import org.apache.kafka.common.serialization.Serde

case class Application(
    applicationId: ApplicationId,
    name: String,
    age: String,
    nationality: String
)

object Application {
  implicit val applicationsSerde: Serde[Application] = serde[Application]

  implicit val intermediateJoinedApplicationsSerde: Serde[IntermediateJoinedApplications] =
    serde[IntermediateJoinedApplications]
}
