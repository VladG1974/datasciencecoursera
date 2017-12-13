/*
Exercise 04 - Convert nyse data to parquet

Data is available in local file system under /data/nyse (ls -ltr /data/nyse)
Fields (stockticker:string, transactiondate:string, openprice:float, highprice:float, lowprice:float, closeprice:float, volume:bigint)
Convert file format to parquet
Save it /user/<YOUR_USER_ID>/nyse_parquet
*/



// hadoop fs -copyFromLocal /data/nyse /user/vlad1974/nyse

/*
spark-shell --master yarn \
  --conf spark.ui.port=12345 \
  --num-executors 4
*/

val nyse = sc.textFile("/user/vlad1974/nyse").
  coalesce(4).
  map(stock => {
    val s = stock.split(",")
    (s(0), s(1), s(2).toFloat, s(3).toFloat, s(4).toFloat, s(5).toFloat, s(6).toInt)
  }).
  toDF("stockticker", "transactiondate", "openprice", "highprice", "lowprice", "closeprice", "volume")
  
sqlContext.setConf("spark.sql.shuffle.partitions", "4")
nyse.save("/user/vlad1974/nyse_parquet", "parquet")
//nyse.write.parquet("spark-scala-")