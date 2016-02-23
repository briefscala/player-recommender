### Player Recommender App with Spark

#### Start Spark locally

```
$ [SPARK_HOME]/sbin/start-master.sh
$ [SPARK_HOME]/bin/spark-class org.apache.spark.deploy.worker.Worker spark://127.0.0.1:7077 -m 6g
```

#### Getting the data

The audioscrobbler data was published by last.fm in 2005 and it is publicly available at
http://www-etud.iro.umontreal.ca/~bergstrj/audioscrobbler_data.html

#### Submit the Spark job

```
$ sbt clean assembly
$  [SPARK_HOME]/bin/spark-submit --class com.briefscala.player.Player --master spark://127.0.0.1:7077 --driver-memory 3g target/scala-2.11/player-recommender.jar
```