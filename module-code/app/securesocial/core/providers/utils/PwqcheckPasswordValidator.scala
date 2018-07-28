package securesocial.core.providers.utils

import play.api.{Logger, Plugin, Application}
import play.api.i18n.Messages

/**
 *
 * This password validator calls out to "pwqcheck", part of the Openwall
 * <a href="http://www.openwall.com/passwdqc/">passwdqc</a> package.  pwqcheck
 * is a unix command line utility, so you must install passwdqc first and
 * make sure that it is available.  This is easier on Linux based systems,
 * but you can use a Mac variant by installing
 * <a href="https://github.com/iphoting/passwdqc-mac">passwdqc-mac</a>.  You can
 * change the pwqcheck parameters by setting "securesocial.password.pwqcheck" in application.conf.
 * <p>
 * Note that this validator passes back the straight error messages from pwqcheck, and does
 * not do i18n through the Messages class.
 *
 * @author wsargent
 * @since 3/29/13
 */
class PwqcheckPasswordValidator(application: Application) extends PasswordValidator {

  private val logger = Logger(this.getClass)

  // Collects the errors to return in errorMessage.
  private var errors : Seq[String] = Seq()

  def isValid(password: String): Boolean = {
    errors = externalPasswordCheck(password)
    errors.isEmpty
  }

  /**
   * Returns the error messages collected from pwqcheck.  Note that this does not do any
   * form of i18n, and simply passes back the
   *
   * @return
   */
  def errorMessage = errors.mkString("\n")

  /**
   * Calls out to an external process "pwqcheck" (assumed to be on the path) and reads from any errors.
   * Taken from <a href="http://www.openwall.com/articles/PHP-Users-Passwords#enforcing-password-policy">enforcing password policy</a>.
   *
   * @param plainText The plaintext password to check.
   * @return validation errors returned from the stdout of pwqcheck.
   */
  def externalPasswordCheck(plainText: String): Seq[String] = {
    import java.io.{OutputStreamWriter, PrintWriter}
    import scala.sys.process._
    import collection.mutable.ArrayBuffer

    // only check one password.
    val pwqcheckExec = application.configuration.getString("securesocial.password.pwqcheck").getOrElse("pwqcheck -1")
    val pwqcheck = Process(pwqcheckExec)

    // Really don't like using a mutable data structure here.
    var errorList = ArrayBuffer[String]()
    def readFromStdout(input: java.io.InputStream) {
      try {
        scala.io.Source.fromInputStream(input).getLines().foreach(line => {
          if (line != "OK") {
            errorList += line
          }
          logger.debug(line)
        })
      } finally {
        input.close()
      }
    }

    def writeToStdin(output: java.io.OutputStream) {
      val writer = new PrintWriter(new OutputStreamWriter(output))
      try {
        writer.println(plainText)
      } finally {
        writer.close()
      }
    }

    def readFromStderr(stderr: java.io.InputStream) {
      try {
        scala.io.Source.fromInputStream(stderr).getLines().foreach(line => {
          logger.error(line)
        })
      } finally {
        stderr.close()
      }
    }

    val exitValue = pwqcheck.run(new ProcessIO(writeToStdin, readFromStdout, readFromStderr)).exitValue()
    logger.debug("exitValue = " + exitValue)

    errorList.toSeq
  }

}
