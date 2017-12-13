/*
Exercise 01B - Get monthly crime count by type using data frame
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

val crimeDataWithDateAndTypeDF = crimeDataWithoutHeader.
  map(rec => (rec.split(",")(2), rec.split(",")(5))).
  toDF("crime_date", "crime_type")

crimeDataWithDateAndTypeDF.registerTempTable("crime_data")

val crimeCountPerMonthPerTypeDF = sqlContext.
  sql("select cast(concat(substr(crime_date, 7, 4), substr(crime_date, 0, 2)) as int) crime_month, " +
  "count(1) crime_count_per_month_per_type, " +
  "crime_type " +
  "from crime_data " +
  "group by cast(concat(substr(crime_date, 7, 4), substr(crime_date, 0, 2)) as int), crime_type " +
  "order by crime_month, crime_count_per_month_per_type desc")

crimeCountPerMonthPerTypeDF.rdd.
  map(rec => rec.mkString("\t")).
  coalesce(1).
  saveAsTextFile("/user/VladG1974/solutions/solution01/crimes_by_type_by_month",
    classOf[org.apache.hadoop.io.compress.GzipCodec])