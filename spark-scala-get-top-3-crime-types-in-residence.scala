/*
Exercise 03 - Get top 3 crime types based on number of incidents in RESIDENCE in Scala using Core API

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

val crimeCountForResidence = sc.parallelize(crimeDataWithoutHeader.
  filter(rec => rec.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)(7) == "RESIDENCE").
  map(rec => (rec.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)(5), 1)).
  reduceByKey((total, value) => total + value).
  map(rec => (rec._2, rec._1)).
  sortByKey(false).
  take(3))

crimeCountForResidence.
  map(rec => (rec._2, rec._1)).
  toDF("crime_type", "crime_count").
  write.json("user/VladG1974/solutions/solution03/RESIDENCE_AREA_CRIMINAL_TYPE_DATA")
