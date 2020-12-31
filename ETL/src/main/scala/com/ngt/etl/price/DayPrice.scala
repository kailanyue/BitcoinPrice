package com.ngt.etl.price

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.scala._

import java.io.File
import java.text.SimpleDateFormat

/**
 * @author ngt
 * @create 2020-12-20 21:00
 */

case class DayPrice_Bitcoin(timestamp: Long, weightedPrice: Double)


object DayPrice {
  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val inputStream = env.readTextFile("data/bitcoin.csv")

    // 表示整点时间
    var theHour: Long = 0L
    val dataStream: DataStream[DayPrice_Bitcoin] = inputStream.
      map(data => {
        val arr: Array[String] = data.split(",")
        theHour = ((arr(0).toLong * 1000L) / (1000 * 60 * 60 * 24)) * (1000 * 60 * 60 * 24)
        DayPrice_Bitcoin(theHour, arr(7).toDouble)
      }).assignAscendingTimestamps(_.timestamp)

    val priceDataStream: DataStream[outPrice] = dataStream
      .map(r => (r.timestamp, r.weightedPrice))
      .keyBy(_._1)
      .timeWindow(Time.hours(24))
      .process(new PriceFunc)

    val result: DataStream[(String, Double, Double, Double)] = priceDataStream
      .map(data => {
        (new SimpleDateFormat("yyyy-MM-dd").format(data.timestamp), data.avgPrice, data.minPrice, data.maxPrice)
      })

    val filePath = "data_time/DayPrice.csv"
    val file = new File(filePath)
    if (file.exists()) {
      file.delete()
    }
    result.writeAsCsv(filePath)
    env.execute("DayPrice")
  }
}



