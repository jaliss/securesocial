import scala.concurrent.ExecutionContext

package object helpers {
  val sequentialExecutionContext = ExecutionContext.fromExecutor(new CurrentThreadExecutor())
}
