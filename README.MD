#### Data Center Command Interface

Currently supporint Packet.net Driver
Set APIKEY environmental variable to API-Token



[![asciicast](https://asciinema.org/a/BIR2bN13eTxW1HDZX9CaTlDDd.svg)](https://asciinema.org/a/BIR2bN13eTxW1HDZX9CaTlDDd?t=12)



### Main Command Interface

``` 
dci-[platform] --help
```
```
Usage: dci [options] [command]

Dell "Bare Metal Cloud" Command Interface

Options:
  -V, --version           output the version number
  -h, --help              output usage information

Commands:
  organization <command>  Organization operations
  project <command>       Project operations
  server <command>        Server operations
  help [cmd]              display help for [cmd]

```


## Project Listing

``` dci-[platform] project list ```

```
|                                  :id |                    :name | :device-count | :member-count |
|--------------------------------------+--------------------------+---------------+---------------|
| 6b411677-07ed-4172-b351-3798dbc7ca39 |                     Demo |             0 |             4 |
| 9f5074d1-08b6-4783-8757-b0ca483f12ad |              GlenSandbox |             0 |             8 |
| e40d191d-4394-477c-b47d-2f08cc26a98a | Service Provider Testing |             3 |             8 |

```


## Server Listing

``` dci-[platform] server list ```


``` 
Using Project: Service Provider Testing
ID: e40d191d-4394-477c-b47d-2f08cc26a98a
```

| :id                                  | :facility      | :operating-system | :hostname | :addresses                                     | :root-password | :state | :tags       |        |
|--------------------------------------+----------------+-------------------+-----------+------------------------------------------------+----------------+--------+-------------+--------|
| 9ed50c86-8840-455c-a3bc-79706acbc655 | Parsippany, NJ | Ubuntu 16.04 LTS  | etcdnode1 | 147.75.72.241,2604:1380:1:a900::5,10.99.232.5  | redacted       | active | []          |        |
| 11482c6b-0443-4855-86d8-b8113b3d9618 | Parsippany, NJ | Ubuntu 16.04 LTS  | etcdnode1 | 147.75.197.155,2604:1380:1:a900::3,10.99.232.3 | redacted       | active | etcd,server |        |
| 5fc8b971-ab9b-49ab-935a-411d148e604e | Parsippany, NJ | Ubuntu 16.04 LTS  | 1234      | 147.75.106.83,2604:1380:1:a900::1,10.99.232.1  |                | active | etcd        |


### Filter by tags and dump in JSON

``` dci-[platform] server list --tag etcd --json ```

``` 
[
 {
  "id": "11482c6b-0443-4855-86d8-b8113b3d9618",
  "facility": "Parsippany, NJ",
  "operating-system": "Ubuntu 16.04 LTS",
  "hostname": "etcdnode1",
  "addresses": [
   "147.75.197.155",
   "2604:1380:1:a900::3",
   "10.99.232.3"
  ],
  "root-password": "redacted",
  "state": "active",
  "tags": [
   "etcd",
   "server"
  ]
 },
 {
  "id": "5fc8b971-ab9b-49ab-935a-411d148e604e",
  "facility": "Parsippany, NJ",
  "operating-system": "Ubuntu 16.04 LTS",
  "hostname": "1234",
  "addresses": [
   "147.75.106.83",
   "2604:1380:1:a900::1",
   "10.99.232.1"
  ],
  "root-password": null,
  "state": "active",
  "tags": [
   "etcd"
  ]
 }
]
```



