import java.util.concurrent.{Executor, Executors}
import scala.concurrent.ExecutionContext

package object helpers {
  class CurrentThreadExecutor extends Executor {
    def execute(r:Runnable) {
      r.run()
    }
  }
  val sequentialExecutionContext = ExecutionContext.fromExecutor(new CurrentThreadExecutor())
}
