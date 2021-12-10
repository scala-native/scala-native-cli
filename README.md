# scala-native-cli

Command Line Interface for the tools component of Scala Native. Tools handle the linking, optimisation and code generation phases. 
This means that the initial .scala files cannot be compiled with it - for that look to the nscplugin component. 

Please note that this api is meant for Scala Native power users. For a more friendly Scala Native building experience please use 
the dedicated [sbt plugin](https://scala-native.readthedocs.io/en/stable/user/sbt.html#minimal-sbt-project) or [scala-cli](https://scala-cli.virtuslab.org).

## Usage

scala-native-cli --help will bring up possible options and syntax. 
Since options are dependent on the used version of scala-native-cli, please use it for further instructions.

## Logging 

For logging purposes, a default Scala Native logger is used. By default it will only show `Error` messages, but its verbosity can be increased with a -v flag.
