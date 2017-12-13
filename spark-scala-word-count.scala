/*
Exercise 05 - Develop word count program using Scala Core API and Data Frames

Data is available in HDFS /public/randomtextwriter
Get word count for the input data using space as delimiter (for each word, we need to get how many types it is repeated in the entire input data set)
Number of executors should be 10
Executor memory should be 3 GB
Executor cores should be 20 in total (2 per executor)
Number of output files should be 8
Avro dependency details: groupId -> com.databricks, artifactId -> spark-avro_2.10, version -> 2.0.1
Target Directory: /user/<YOUR_USER_ID>/solutions/solution05/wordcount
Target File Format: Avro
Target fields: word, count
Compression: N/A or default

*/

/*
spark-shell --master yarn \
  --conf spark.ui.port=12456 \
  --num-executors 10 \
  --executor-memory 3G \
  --executor-cores 2 \
  --packages com.databricks:spark-avro_2.10:2.0.1
*/

val lines = sc.textFile("/public/randomtextwriter")
val words = lines.flatMap(line => line.split(" "))
val tuples = words.map(word => (word, 1))
val wordCount = tuples.reduceByKey((total, value) => total + value, 8)
val wordCountDF = wordCount.toDF("word", "count")

import com.databricks.spark.avro._
wordCountDF.write.avro("/user/VladG1974/solutions/solution05/wordcount")