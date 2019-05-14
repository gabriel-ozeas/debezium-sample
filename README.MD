# Debezium Sample

Create a data directory to keep the mysql data persisted

```
$ mkdir data/
```

Here we will use a test database dum with ~4 million rows that will be streamed to Kafka. Clone the git repo: https://github.com/datacharmer/test_db 

```
$ git clone git@github.com:datacharmer/test_db.git
```

In the same DIR create a docker compose file with the bellow configuration. It starts the zookeeper needed by kafka, the kafka itself, the mysql database and the kafka connect with the debezium plugin inside it.

```
docker-composer up -d mysql zookeeper kafka 
```

Import data inside MySQL:

```
docker exec -it {mysql_container_name} bash -c "cd /test_db && mysql < employees.sql"
```


If all the three compoenents started with success, initialize the kafka-connect

```
docker-compose up -d connect
```

Configure a new connector for MySql

```
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d @mysql-source.json
```

You can check if the connector was registered with success

```
curl -H "Accept:application/json" localhost:8083/connectors/["mysql-source"]
```




