name := "StreetEvents_SentimentAnalysis"

version := "1.0"

organization := "org.thomsonreuters"

scalaVersion := "2.10.3"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies  ++= Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % "3.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.4" classifier "models",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.6" % "test"
)

resolvers ++= Seq(
  // other resolvers here
  // if you want to use snapshot builds (currently 0.8-SNAPSHOT), use this.
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")