/*
Exercise 03 - Get top 3 crime types based on number of incidents in RESIDENCE in Scala using  Data Frames and SQL

Data is available in HDFS file system under /public/crime/csv
Structure of data (ID,Case Number,Date,Block,IUCR,Primary Type,Description,Location Description,Arrest,Domestic,Beat,District,Ward,Community Area,FBI Code,X Coordinate,Y Coordinate,Year,Updated On,Latitude,Longitude,Location)
File format - text file
Delimiter - “,” (use regex while splitting split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1), as there are some fields with comma and enclosed using double quotes.
Get top 3 crime types based on number of incidents in RESIDENCE area using “Location Description”
Store the result in HDFS path /user/<YOUR_USER_ID>/solutions/solution03/RESIDENCE_AREA_CRIMINAL_TYPE_DATA
Output Fields: Crime Type, Number of Incidents
Output File Format: JSON
Output Delimiter: N/A
Output Compression: No
*/

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

val crimeDataWithoutHeaderDF = crimeDataWithoutHeader.
  map(rec => {
    val r = rec.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)
    (r(7), r(5))
  }).toDF("location_description", "crime_type")

crimeDataWithoutHeaderDF.registerTempTable("crime_data")

sqlContext.setConf("spark.sql.shuffle.partitions", "4")
sqlContext.sql("select * from (" +
                 "select crime_type, count(1) crime_count " +
                 "from crime_data " +
                 "where location_description = 'RESIDENCE' " +
                 "group by crime_type " +
                 "order by crime_count desc) q " +
               "limit 3").
  coalesce(1).
  save("/user/VladG1974/solutions/solution03/RESIDENCE_AREA_CRIMINAL_TYPE_DATA", "json")
