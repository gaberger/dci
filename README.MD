#### Data Center Command Interface

### Introduction

`dci` is a command line interface for exploring and interacting with a Bare Metal Cloud service such as Packet.net. The tool provides both an interactive interface with `prompt` support and a `force` mode for command-line scripting and automation.


### Development

`dci` is written in Clojurescript and is compiled down into an optimized Javascript implementation hosted within a `node.js` runtime executable.


### Extensibility

`dci` is designed for plugability leveraging a multi-method interface to abstract the concrete implementaion deails. Commands can be written in any language and merely have to conform to a naming convention `dci`-command.


### Platform Support

Support for native executables for `Linux X86`, `MacOS` and `Windows`

### Interface

`./dci-<platform> --help`

```
Usage: dci [options] [command]

DataCenter "Bare Metal Cloud" Command Interface

Options:
  -V, --version             output the version number
  -P --provider <provider>  Provider (default: "packet")
  -h, --help                output usage information

Commands:
  organization <command>    Organization operations
  project <command>         Project operations
  server <command>          Server operations
  service <command>         Service operations
  help [cmd]                display help for [cmd]
  ```



### Features

1. Supports interactive mode and `--force` mode for automated scripting
2. Configuration file stored in $HOME directory called `dci-config.edn`
3. Session state stored in $HOME directory `.dci-state.edn` provide easy interactivity
4. Resource identifiers can be referenced with an unambiguous prefix search
5. Output modifiers to support `JSON` and `EDN` for pipeline consumption
6. Table views of collections such as `Organizaitons`, `Projects` and `Servers`
7. Idempotent operations based on resource name as key
8. Batch processing provided with an encoded `YAML` file, also idempotent.