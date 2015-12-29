/**
  * Created by glcohen on 12/26/15.
  */

import java.util.{Calendar, ArrayList, Date}
import twitter4j._
import scala.collection.mutable.ListBuffer

object DataIngest {
  private final val rateLimit = 450 // # search queries per 15 min interval

  def main(args: Array[String]): Unit = {
    println("Starting ingest")

    val twitter = AuthUtil.applicationAuthSetUp()
    val queryText = "persnickety since:" + IngestUtil.getXMonthsAgo(12)
    val query: Query = new Query(queryText)
    query.setCount(100) // get up to 100 tweets per query (max)
    val result = runMultipleQueries(twitter, query, rateLimit - 50)
    val freqMapIter = dateMapReduce(result.toList).toIterator
    var mapCounter = 0
    while (freqMapIter.hasNext) {
      val (key, value) = freqMapIter.next
      println(mapCounter + ": " + key + " " + value)
      mapCounter += 1
    }
  }

  // takes list of dates, returns Map of form [frequency, day of month]
  def dateMapReduce(dateList: List[Date]): Map[String, Int] = {
    // map
    val calList = dateList.map((d: Date) => dateToCalendar(d)) // list of dates -> list of cals
    val freqList: List[(String, Int)] = calList.map((c: Calendar) =>
        ((c.get(Calendar.MONTH) + 1).toString + "-" + (c.get(Calendar.DAY_OF_MONTH).toString) -> 1)

    // reduce
    var freqMap: Map[String, Int] = Map() // map to hold frequency -> day of Month
    for ((k, v) <- freqList) {
      if (freqMap.contains(k)) {
        val vs = freqMap(k)
        freqMap += (k -> (v + vs))
      } else {
        freqMap += (k -> v)
      }
    }

    freqMap
  }

  // Run query one time, return ListBuffer of Dates
  def runSingleQuery(twitter: Twitter, query: Query): ListBuffer[Date] = {
    val dateList = ListBuffer[Date]()
    val result = twitter.search(query)
    val tweets = result.getTweets
    val tweetsIter = tweets.listIterator()
    println("Got " + result.getCount + " tweets.")
    println("limit status: " + checkRateLimitStatus(twitter))
    while (tweetsIter.hasNext) {
      val nextTweet = tweetsIter.next()
      dateList += nextTweet.getCreatedAt // add date to dateList
    }

    dateList // return dateList
  }

  // Run query multiple times, return ListBuffer of Dates
  def runMultipleQueries(twitter: Twitter, query: Query, numQueries: Int): ListBuffer[Date] = {
    val dateList = ListBuffer[Date]() // list to hold all dates
    var lastID: Long = Long.MaxValue // used to make sure no duplicate tweets pulled
    var result: QueryResult = null
    do {
      result = twitter.search(query)
      val tweets = result.getTweets
      val tweetsIter = tweets.listIterator()

      //println("Got " + result.getCount + " tweets.")
      //println("limit status: " + checkRateLimitStatus(twitter) + "\n")

      while (tweetsIter.hasNext) {
        val nextTweet = tweetsIter.next()
        dateList += nextTweet.getCreatedAt // add date to dateList
      }

      for (i <- 0 until tweets.size) { // get lowest ID of tweets this batch
        if (tweets.get(i).getId() < lastID) {
          lastID = tweets.get(i).getId()
        }
      }

      query.setMaxId(lastID - 1) // set max ID to make sure no duplicate tweets pulled
    } while (result.getCount > 0 && checkRateLimitStatus(twitter) > numQueries)
      // run while more tweets to pull and not near limit

    dateList // return dateList
  }

  // Get the remaining number of queries allowed in current 15-minute interval
  def checkRateLimitStatus(twitter: Twitter): Int = {
    val rateLimitStatus = twitter.getRateLimitStatus("search")
    rateLimitStatus.get("/search/tweets").getRemaining
  }

  // Turn a date object to a calendar object since data is mostly deprecated
  def dateToCalendar(date: Date): Calendar = { // NOTE: Months are stupidly 0-based
    val cal = Calendar.getInstance()
    cal.setTime(date)
    cal
  }
}