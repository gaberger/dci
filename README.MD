#### Data Center Command Interface


[![Demo](https://img.youtube.com/vi/fw8XVNx_ZAg/0.jpg)](https://www.youtube.com/watch?v=fw8XVNx_ZAg)



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
  facility <command>        Facility operations
  project <command>         Project operations
  server <command>          Server operations
  cluster <command>         Cluster operations
  platform <command>        Platform operations
  help [cmd]                display help for [cmd]
```

Cluster Manifest
```
---
organization_id: 60bdbe9f-10c2-4ac9-bc75-6fd635f52daa
project_name: cluster-test-1
bootstrap_node: 147.75.107.215
node_spec:
  - replicas: 0
    plan: baremetal_0
    distribute: false
    facilities:
      - ewr1
      - ams1
      - sjc1
    tags:
      - control-plane
    operating_system: ubuntu_16_04
    userdata: |
     #!/bin/bash
     HOMESERVER=$(curl -s https://metadata.packet.net/metadata | jq -r .phone_home_url | sed -r 's/^(.*)\/[a-zA-Z-]+/\1/')
     curl -X POST -d '{"state":"succeeded","code":1000,"message":"Ready"}' ${HOMESERVER}/events
 ```   

### Features

1. Supports interactive mode and `--force` mode for automated scripting
2. Configuration file stored in $HOME directory called `dci-config.edn`
3. Session state stored in $HOME directory `.dci-state.edn` provide easy interactivity
4. Resource identifiers can be referenced with an unambiguous prefix search
5. Output modifiers to support `JSON` and `EDN` for pipeline consumption
6. Table views of collections such as `Organizations`, `Projects` and `Servers`
7. Idempotent operations based on resource name as key
8. Batch processing provided with an encoded `YAML` file, also idempotent.
9. Support for `kubeone` Cluster automation with `machine-controller` support for `Packet`