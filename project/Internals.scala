package scala.scalanative

// Proxy to package private methods
package nir {
  object Proxy {
    def nativeBinaryVersion(version: String) = Versions.binaryVersion(version)
  }
}
