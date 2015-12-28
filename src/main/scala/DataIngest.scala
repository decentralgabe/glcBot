/**
  * Created by glcohen on 12/26/15.
  */

import java.net.URLEncoder._
import java.util
import java.util.{Calendar, ArrayList, Date}
import twitter4j._

import scala.collection.mutable.ListBuffer

object DataIngest {
  def main(args: Array[String]): Unit = {
    println("Starting ingest")

    val twitter = AuthUtil.applicationAuthSetUp()
    val queryText = "persnickety"/* since:" + IngestUtil.getXMonthsAgo(12)*/
    val query: Query = new Query(queryText)
    query.setCount(100) // get up to 100 tweets per query (max)
    val result = runSingleQuery(twitter, query)
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
    //calList.foreach(x => println(x.toString))
    val freqList: List[(String, Int)] = calList.map((c: Calendar) => (c.get(Calendar.MONTH).toString + "-" + c.get(Calendar.DAY_OF_MONTH).toString) -> 1)

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

  // Run query one time return ListBuffer of Dates
  def runSingleQuery(twitter: Twitter, query: Query): ListBuffer[Date] = {
    val dateList = ListBuffer[Date]()
    val result = twitter.search(query)
    val tweets = result.getTweets
    val tweetsIter = tweets.listIterator()
    println("Got " + result.getCount + " tweets.")

    while (tweetsIter.hasNext) {
      val nextTweet = tweetsIter.next()
      dateList += nextTweet.getCreatedAt // add date to dateList
    }

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
    //println("!!!!! " + cal.get(Calendar.DATE))
    cal
  }



  // DONT USE BELOW

  def runQueryPullData(twitter: Twitter, query: Query, totalTweets: Int): java.util.ArrayList[java.util.Date] = {
    var lastID: Long = Long.MaxValue
    val dateList = new java.util.ArrayList[java.util.Date]()
    var tweetsPulled: Int = 0 // counter for # tweets pulled so far
    var result: QueryResult = null
    var nextTweet: Status = null

    while (tweetsPulled < totalTweets) {
      println("HERE")
      var tweetsThisQuery: Int = 0
      if (totalTweets - tweetsPulled > 100) {
        query.setCount(100)
      } else {
        query.setCount(totalTweets - tweetsPulled)
      }
      println("queryCount: " + query.getCount)
      try {
        do {
          result = twitter.search(query)
          println("limit status" + twitter.help().getRateLimitStatus())
          val tweets = result.getTweets // get tweets from this pull
          val tweetsIterator = tweets.iterator
          while (tweetsIterator.hasNext) {
            // loop through all tweets pulled
            nextTweet = tweetsIterator.next

            dateList.add(nextTweet.getCreatedAt)
            tweetsPulled += 1 // increment pulled counter
            tweetsThisQuery += 1 // increment pulled counter for just this query
            println("tweetsPulled so far: " + tweetsPulled)
            println("total available to pull: " + totalTweets)
          }
          for (i <- 0 until tweets.size) { // get lowest ID of tweets
            if (tweets.get(i).getId() < lastID) {
              lastID = tweets.get(i).getId()
            }
          }
          println("Sleeping 10 seconds")
          Thread sleep (10000) // sleep 10 secs
          println("Sleep over")
        } while (tweetsPulled < totalTweets && tweetsThisQuery < 100)

      } catch {
        // exception while pulling tweets
        case e: TwitterException => {
          e.printStackTrace()
          println("Failed to search tweets: " + e.getMessage)
          System.exit(1) // exit with error
        }
      }
      query.setMaxId(lastID - 1) // ensure you don't get duplicate tweets
    }
    dateList
  }

  // method to run a given query and save the output to an ArrayList
  def runAndSaveQuery(twitter: Twitter, query: Query): ArrayList[String] = {
    var lastID = Long.MaxValue // keep track of lastID stored
    val numOfTweets = 100 // # of tweets to pull
    val tweetList = new ArrayList[String]()
    var tweetMap: Map[Int, java.util.Date] = Map()
    var result: QueryResult = null
    var nextTweet: Status = null
    while (tweetList.size() < numOfTweets) {
      if (numOfTweets - tweetList.size() > 100) {
        // set query size
        query.setCount(100)
      } else {
        query.setCount(numOfTweets - tweetList.size())
      }
      try {
        do {
          result = twitter.search(query)
          val tweets = result.getTweets
          val tweetsIterator = tweets.iterator()
          while (tweetsIterator.hasNext) {
            nextTweet = tweetsIterator.next()
            val date = nextTweet.getCreatedAt
            tweetMap += (1 -> date)
            //tweetList.add(nextTweet.getText)
          }
          for (i <- 0 until tweets.size) {
            if (tweets.get(i).getId() < lastID) lastID = tweets.get(i).getId()
          }
        } while (result.nextQuery() != null && tweetList.size() < 150)
      } catch {
        case te: TwitterException => {
          te.printStackTrace()
          println("Failed to search tweets: " + te.getMessage)
        }
      }
      query.setMaxId(lastID - 1)
    }
    tweetList // return list of tweets
  }
}