/*
Exercise 01A - Get monthly crime count by type using Core API
Data set:
https://data.cityofchicago.org/Public-Safety/Crimes-2001-to-present/ijzp-q8t2
Choose language of your choice Python or Scala
Data is available in HDFS file system under /public/crime/csv
You can check properties of files using hadoop fs -ls -h /public/crime/csv
Structure of data (ID,Case Number,Date,Block,IUCR,Primary Type,Description,Location Description,Arrest,Domestic,Beat,District,Ward,Community Area,FBI Code,X Coordinate,Y Coordinate,Year,Updated On,Latitude,Longitude,Location)
File format - text file
Delimiter - “,”
Get monthly count of primary crime type, sorted by month in ascending and number of crimes per type in descending order
Store the result in HDFS path /user/<YOUR_USER_ID>/solutions/solution01/crimes_by_type_by_month
Output File Format: TEXT
Output Columns: Month in YYYYMM format, crime count, crime type
Output Delimiter: \t (tab delimited)
Output Compression: gzip
*/

/*
spark-shell --master yarn \
  --conf spark.ui.port=12345 \
  --num-executors 6 \
  --executor-cores 2 \
  --executor-memory 2G
*/

val crimeData = sc.textFile("/public/crime/csv")
val header = crimeData.first
val crimeDataWithoutHeader = crimeData.filter(criminalRecord => criminalRecord != header)

val criminalRecordsWithMonthAndType = crimeDataWithoutHeader.
  map(rec => {
    val r = rec.split(",")
    val d = r(2).split(" ")(0) // 12/31/2007
    val m = d.split("/")(2) + d.split("/")(0) //200712
    ((m.toInt, r(5)), 1)  
  })
val crimeCountPerMonthPerType = criminalRecordsWithMonthAndType.
  reduceByKey((total, value) => total + value)

val crimeCountPerMonthPerTypeSorted = crimeCountPerMonthPerType.
  map(rec => ((rec._1._1, -rec._2), rec._1._1 + "\t" + rec._2 + "\t" + rec._1._2)).
  sortByKey().
  map(rec => rec._2)

crimeCountPerMonthPerTypeSorted.
  coalesce(1).
  saveAsTextFile("/user/vlad1974/solutions/solution01/crimes_by_type_by_month",
    classOf[org.apache.hadoop.io.compress.GzipCodec])