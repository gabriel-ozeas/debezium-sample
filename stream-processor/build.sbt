name := "stream-processor"
version := "0.1"
scalaVersion := "2.12.8"

libraryDependencies += "org.apache.kafka" % "kafka-streams" % "2.2.0"
libraryDependencies += "org.apache.kafka" %% "kafka-streams-scala" % "2.2.0"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.25"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.8"
