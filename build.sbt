name := "glcBot"

organization := "glcohen"

version := "1.0"

scalaVersion := "2.11.7"

enablePlugins(JavaAppPackaging)

libraryDependencies += "org.twitter4j" % "twitter4j-core" % "4.0.4"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

libraryDependencies += "com.github.wookietreiber" %% "scala-chart" % "latest.integration"