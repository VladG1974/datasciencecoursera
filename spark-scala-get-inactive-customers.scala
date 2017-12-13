/*
Exercise 02 - Get details of inactive customers in Scala using Core API

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
val orders = sc.parallelize(ordersRaw)

val customersRaw = Source.fromFile("/data/retail_db/customers/part-00000").getLines.toList
val customers = sc.parallelize(customersRaw)

val ordersMap = orders.
  map(order => (order.split(",")(2).toInt, 1))
val customersMap = customers.
  map(c => (c.split(",")(0).toInt, (c.split(",")(2), c.split(",")(1))))
val customersLeftOuterJoinOrders = customersMap.leftOuterJoin(ordersMap)
val inactiveCustomersSorted = customersLeftOuterJoinOrders.
  filter(t => t._2._2 == None).
  map(rec => rec._2).
  sortByKey()
inactiveCustomersSorted.
  map(rec => rec._1._1 + ", " + rec._1._2).
  saveAsTextFile("/user/VladG1974/solutions/solutions02/inactive_customers")