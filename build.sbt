name := "player-recommender"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

organization := "com.briefscala"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

val sparkVersion = "1.6.0"

val sparkDependencyScope = "provided"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % sparkDependencyScope,
  "org.apache.spark" %% "spark-mllib" % sparkVersion % sparkDependencyScope,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-Xlint",
  "-deprecation",
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-Ywarn-dead-code",
  "-language:_",
  "-feature"
)

assemblyJarName in assembly := "player-recommender.jar"


