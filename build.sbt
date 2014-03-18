name := "scala-sync"

version := "1.0"

scalaVersion := "2.10.3"

EclipseKeys.withSource := true

libraryDependencies ++= Seq(
  "com.jcraft"                    %  "jsch"                         % "0.1.50",
  "org.scalatest"                 %  "scalatest_2.10"               % "2.0"      % "test",
  "org.mockito"                   %  "mockito-core"                 % "1.9.5"    % "test"
)

