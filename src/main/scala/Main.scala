/**
  * Created by glcohen on 12/29/15.
  */

import twitter4j._

object Main {
  private final val rateLimit = 450 // # search queries per 15 min interval

  def main(args: Array[String]) {
    println("Running....")
    val twitter = runAuth/*
    println("Auth successful")
    val (avg, max, min, tot) = getAvgMaxMinTot(twitter, rateLimit)
    println("Data fetch and calculations successful")
    postTweet(twitter, avg, max, min, tot)
    println("Tweet posted successfully!")*/
    DataIngest.checkRateLimitStatus(twitter)
  }

  // Run authorization protocol, return Twitter object
  def runAuth(): Twitter = {
    var twitter: Twitter = null
    try {
      twitter = AuthUtil.applicationAuthSetUp()
    } catch {
      case te: TwitterException => te.printStackTrace()
    }
    twitter
  }

  // Pull data from Twitter, return frequency map of Date -> Frequency for WOTD
  def runIngest(twitter: Twitter, numQueries: Int): Iterator[(String, Int)] = {
    println("Starting ingest")
    val queryText = IngestUtil.getWOTD + " " + IngestUtil.getXMonthsAgo(1)
    val query: Query = new Query(queryText)
    query.setCount(100) // get up to 100 tweets per query (max)
    val result = DataIngest.runMultipleQueries(twitter, query, numQueries)
    val freqMapIter = DataIngest.dateMapReduce(result.toList).toIterator
    freqMapIter
  }

  // (Average, Max, Min, Total) days and # tweets for WOTD
  def getAvgMaxMinTot(twitter: Twitter, numQueries: Int): (String, String, String, String) = {
    val freqMapIter = runIngest(twitter, numQueries)
    println("Ingest done")
    var mapCounter = 1 // # elements in map
    var avg = 1
    var (maxDay, maxCount) = ("", Int.MinValue)
    var (minDay, minCount) = ("", Int.MaxValue)

    while (freqMapIter.hasNext) {
      val (key, value) = freqMapIter.next

      // Calculating avg
      avg += value
      mapCounter += 1

      // Calculating max
      if (value > maxCount) {
        maxCount = value
        maxDay = key
      }

      // Calculating min
      if (value < minCount) {
        minCount = value
        minDay = key
      }
    }
    ((avg / mapCounter).toString, minDay + ": " + minCount.toString, maxDay + ": " + maxCount.toString, mapCounter.toString)
  }

  // Form and post tweet to Twitter
  def postTweet(twitter: Twitter, avg: String, max: String, min: String, to: String) {
    val tweet = IngestUtil.getWOTDString + "\n" + "Avg: " + avg + "\n" + "Max: " + max + "\n" + "Min: " + min
    println(tweet)
   /* if (tweet.length <= 140) {
      try {
        twitter.updateStatus(tweet)
      } catch {
        case te: TwitterException => te.printStackTrace()
      }
    }*/
  }
}
