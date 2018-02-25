package helpers

import java.util.concurrent.Executor

class CurrentThreadExecutor extends Executor {
  def execute(r: Runnable) {
    r.run()
  }
}