package com.its.example

package object streams {

  object Topics {
    final val ApplicationsTopic = "kstreams-applications"
    final val ApplicationDetailsTopic = "kstreams-application-details"
    final val IntermediateJoinsTopic = "kstreams-intermediate-applications-and-details-topic"
    final val ImportedApplicationTopic = "kstreams-imported-application"
  }

  object StateStores {
    final val retryStore = "retry-store"
    final val consolidatedStateStoreName = "consolidated-state-store"
    final val applicationStateStoreName = "application-state-store"
  }

  object Processors {
    final val dataProcessor = "data-processor"
  }

}
