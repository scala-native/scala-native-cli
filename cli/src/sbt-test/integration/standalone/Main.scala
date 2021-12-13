object Main {
  def main(args: Array[String]): Unit = {
    println("Hello world")
  }
}

class Foo{
  def foo = Foo.foo
}

object Foo{
  val foo = "bar"
}
