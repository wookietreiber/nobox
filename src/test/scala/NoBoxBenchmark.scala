package nobox
package benchmark

import org.scalameter.api._

trait NoBoxBenchmark extends PerformanceTest {
  lazy val executor = LocalExecutor(new Executor.Warmer.Default, Aggregator.min, new Measurer.Default)
  lazy val reporter = new LoggingReporter
//  lazy val reporter = ChartReporter(ChartFactory.XYLine())
  lazy val persistor = Persistor.None

  val sizes = Gen.range("size")(300000, 1500000, 300000)

  val arInts = for (size ← sizes) yield util.Random.shuffle(1 to size).toArray
  val ofInts = for (array ← arInts) yield new ofInt(array)

  val arLongs = for (size ← sizes) yield util.Random.shuffle(1 to size).map(_.toLong).toArray
  val ofLongs = for (array ← arLongs) yield new ofLong(array)

  def simpleIntComparison(name: String)(fa: Array[Int] ⇒ Unit, fn: ofInt ⇒ Unit) = {
    performance of "scala.Array[Int]" in {
      measure method name in {
        using(arInts) curve(name) in fa
      }
    }

    performance of "nobox.ofInt" in {
      measure method name in {
        using(ofInts) curve(name) in fn
      }
    }
  }

  def simpleLongComparison(name: String)(fa: Array[Long] ⇒ Unit, fn: ofLong ⇒ Unit) = {
    performance of "scala.Array[Long]" in {
      measure method name in {
        using(arLongs) curve(name) in fa
      }
    }

    performance of "nobox.ofLong" in {
      measure method name in {
        using(ofLongs) curve(name) in fn
      }
    }
  }
}
