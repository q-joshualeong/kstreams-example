package com.its.example.streams.models

object Types {
  type ApplicationId = String
  type IntermediateJoinedApplications = (ApplicationDetails, Option[Application])
}
