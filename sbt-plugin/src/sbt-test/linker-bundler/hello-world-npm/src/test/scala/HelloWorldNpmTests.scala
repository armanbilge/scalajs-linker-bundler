import org.junit.Assert._
import org.junit.Test
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("hello-world-npm")
object HelloWorldNpm extends js.Object {
  def helloWorld(): String = js.native
}

class HelloWorldNpmTests {

  @Test def testHello(): Unit =
    assertEquals("Hello World", HelloWorldNpm.helloWorld())

}
