/*
spark-shell --master yarn \
  --conf spark.ui.port=12345 \
  --num-executors 6 \
  --executor-cores 2 \
  --executor-memory 2G
*/

// Solution using Core API
val crimeData = sc.textFile("/public/crime/csv")
val header = crimeData.first
val crimeDataWithoutHeader = crimeData.filter(criminalRecord => criminalRecord != header)

/*
// Logic to convert a record into tuple
val rec = crimeDataWithoutHeader.first

// Extract date eg: 12/31/2007
// We need only year and month in YYYYMM format, 12/31/2007 -> 200712
// Finally create tuple ((crime_month, crime_type), 1)
val t = {
  val r = rec.split(",")
  val d = r(2).split(" ")(0) // 12/31/2007
  val m = d.split("/")(2) + d.split("/")(0) //200712
  ((m.toInt, r(5)), 1) //tuple
}
*/

val criminalRecordsWithMonthAndType = crimeDataWithoutHeader.
  map(rec => {
    val r = rec.split(",")
    val d = r(2).split(" ")(0) // 12/31/2007
    val m = d.split("/")(2) + d.split("/")(0) //200712
    ((m.toInt, r(5)), 1)  
  })
val crimeCountPerMonthPerType = criminalRecordsWithMonthAndType.
  reduceByKey((total, value) => total + value)

//((200707,WEAPONS VIOLATION),count) -> ((200707, count), "200707,count,WEAPONS VIOLATION")
// 200707,count,WEAPONS VIOLATION
val crimeCountPerMonthPerTypeSorted = crimeCountPerMonthPerType.
  map(rec => ((rec._1._1, -rec._2), rec._1._1 + "\t" + rec._2 + "\t" + rec._1._2)).
  sortByKey().
  map(rec => rec._2)

crimeCountPerMonthPerTypeSorted.
  coalesce(1).
  saveAsTextFile("/user/dgadiraju/solutions/solution01/crimes_by_type_by_month", 
    classOf[org.apache.hadoop.io.compress.GzipCodec])