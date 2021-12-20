# scala-native-cli

Command Line Interface for the various components of Scala Native. 

* scala-native-c compiles the Scala code to NIR (Native Intermediate Representation).
* scala-native-ld links and optimises the NIR code to a binary file.
* scala-native-p allows to view the compiled NIR code.

Please note that this api is meant for Scala Native power users. For a more friendly Scala Native building experience please consider using 
the dedicated [sbt plugin](https://scala-native.readthedocs.io/en/stable/user/sbt.html#minimal-sbt-project) or [scala-cli](https://scala-cli.virtuslab.org).

## Usage

`scala-native-ld --help` will bring up possible options and syntax. 
Since options are dependent on the used version of scala-native-cli, please use it for further instructions.

## Logging

For logging purposes, a default Scala Native logger is used. By default it will only show `Error` messages, but its verbosity can be increased with a -v flag.
