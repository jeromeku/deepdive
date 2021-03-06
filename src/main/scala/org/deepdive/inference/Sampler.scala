package org.deepdive.inference

import akka.actor._
import scala.sys.process._
import scala.util.{Success, Failure}

/* Companion object for the Sampler actor. Use the props method to create a new Sampler */
object Sampler {
  def props = Props[Sampler]

  // Tells the Sampler to run inference
  case class Run(samplerJavaArgs: String, samplerOptions: String, weightsFile: String, variablesFile: String,
    factorsFile: String, edgesFile: String, variableOutPath: String)
}

/* Runs inferece on a dumped factor graph. */
class Sampler extends Actor with ActorLogging {

  override def preStart() = { log.info("starting") }

  def receive = {
    case Sampler.Run(samplerJavaArgs, samplerOptions, weightsFile, variablesFile,
      factorsFile, edgesFile, variableOutPath) =>
      // Build the command
      val samplerCmd = buildSamplerCmd(samplerJavaArgs, samplerOptions, weightsFile, variablesFile,
      factorsFile, edgesFile, variableOutPath)
      log.info(s"Executing: ${samplerCmd.mkString(" ")}")
      // We run the process, get its exit value, and print its output to the log file
      val exitValue = samplerCmd!(ProcessLogger(
        out => log.info(out),
        err => System.err.println(err)
      ))
      // Depending on the exit value we return success or throw an exception
      exitValue match {
        case 0 => sender ! Success()
        case _ => throw new RuntimeException("sampling failed (see error log for more details)")
      }
  }

  // Build the command to run the sampler
  def buildSamplerCmd(samplerJavaArgs: String, samplerOptions: String, weightsFile: String, 
    variablesFile: String, factorsFile: String, edgesFile: String, variableOutPath: String) = {
    Seq("java", samplerJavaArgs, 
      "-jar", "util/sampler-assembly-0.1.jar", 
      "--input-weights", weightsFile,
      "--input-variables", variablesFile,
      "--input-factors", factorsFile,
      "--input-edges", edgesFile,
      "--outputFile", variableOutPath) ++ samplerOptions.split(" ")
  }

}

