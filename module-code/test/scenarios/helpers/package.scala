package scenarios

import play.api.test.{ PlayRunners, RouteInvokers, Writeables }

package object helpers {
  type ApiExecutor = PlayRunners with RouteInvokers with Writeables
}
