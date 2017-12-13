/*
Exercise 02 - Get details of inactive customers in Scala using Data Frames and SQL

Data is available in local file system /data/retail_db
Source directories: /data/retail_db/orders and /data/retail_db/customers
Source delimiter: comma (“,”)
Source Columns - orders - order_id, order_date, order_customer_id, order_status
Source Columns - customers - customer_id, customer_fname, customer_lname and many more
Get the customers who have not placed any orders, sorted by customer_lname and then customer_fname
Target Columns: customer_lname, customer_fname
Number of files - 1
Target Directory: /user/<YOUR_USER_ID>/solutions/solutions02/inactive_customers
Target File Format: TEXT
Target Delimiter: comma (“, ”)
Compression: N/A
*/



/*
spark-shell --master yarn \
  --conf spark.ui.port=12345 \
  --num-executors 1 \
  --executor-cores 1 \
  --executor-memory 2G
*/

import scala.io.Source

val ordersRaw = Source.fromFile("/data/retail_db/orders/part-00000").getLines.toList
val ordersRDD = sc.parallelize(ordersRaw)

val customersRaw = Source.fromFile("/data/retail_db/customers/part-00000").getLines.toList
val customersRDD = sc.parallelize(customersRaw)

val ordersDF = ordersRDD.
  map(o => o.split(",")(2).toInt).
  toDF("order_customer_id")
val customersDF = customersRDD.
  map(c => (c.split(",")(0).toInt, c.split(",")(1), c.split(",")(2))).
  toDF("customer_id", "customer_fname", "customer_lname")

ordersDF.registerTempTable("orders_dg")
customersDF.registerTempTable("customers_dg")

sqlContext.setConf("spark.sql.shuffle.partitions", "1")

sqlContext.
  sql("select customer_lname, customer_fname " + 
      "from customers_dg left outer join orders_dg " +
      "on customer_id = order_customer_id " +
      "where order_customer_id is null " +
      "order by customer_lname, customer_fname").
  rdd.
  map(rec => rec.mkString(", ")).
  saveAsTextFile("/user/VladG1974/solutions/solutions02/inactive_customers")