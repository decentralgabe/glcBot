/**
  * Created by glcohen on 12/29/15.
  */

import java.io.File
import twitter4j._
import twitter4j.conf.{Configuration}

object Main {
  def main(args: Array[String]) {
    val t0 = System.nanoTime()
    println("Running....")

    if (IngestUtil.isNewWOTD()) { // check if there's a new word of the day
      val twitterApp = AuthUtil.authApp() // application auth for raising query limit to 450 tweets/15 min
      println("App auth done")

      val (freqMap, tweetCount) = runIngest(twitterApp)
      println("Ingestion done")

      val imgPath = ChartUtil.generateChart(freqMap.toIterator)
      println("Chart generation done")

      val (config, twitterPost) = AuthUtil.authNormal() // updte authorization to enable posting to Twitter
      println("Normal auth done")

      val (avg, max, min, tot, days) = getAvgMaxMinTot(freqMap.toIterator, tweetCount)
      println("Data fetch and calculations done")

      postTweet(twitterPost, avg, max, min, tot, days, imgPath, config)
      println("Tweet posted!")
    }

    else {
      println("Program will not run. Word of the day not current.")
    }

    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
  }

  // Pull data from Twitter, return frequency map of Date -> Frequency for WOTD and tweet count
  def runIngest(twitter: Twitter): (Map[String, Int], Int) = {
    val queryText = IngestUtil.getWOTD + " since:" + IngestUtil.getXMonthsAgo(1) // careful with this
    val query: Query = new Query(queryText)
    query.setCount(100) // get up to 100 tweets per query (max)
    val (result, tweetCount) = DataIngest.runMultipleQueries(twitter, query)
    val freqMap = DataIngest.dateMapReduce(result.toList)
    (freqMap, tweetCount)
  }

  // (Average, Max, Min, Total) days and # tweets for WOTD
  def getAvgMaxMinTot(freqMapIter: Iterator[(String, Int)], tweetCount: Int): (String, String, String, String, String) = {
    var mapCounter = 0 // # elements in map
    var avg = 0
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
    if (avg != 0 && mapCounter != 0) {
      ((avg / mapCounter).toString, maxDay + ": " + maxCount.toString, minDay + ": " +
        minCount.toString, tweetCount.toString, mapCounter.toString)
    } else {
      // no queries! probably impossible...
      println("ERROR: No tweets found...something's wrong...")
      ("", "", "", "", "")
    }
  }

  // Form and post tweet to Twitter
  def postTweet(twitter: Twitter, avg: String, max: String, min: String, tot: String, days: String, imgPath: String, config: Configuration) {
    val tweet = IngestUtil.getWOTDString + "\n" + "Avg: " + avg + "\n" + "Max: " + max +
      "\n" + "Min: " + min + "\n" + "Tweets: " + tot + " / " + days + " days"
    IngestUtil.toFile(tweet, "/Users/Gabe/Documents/IdeaProjects/glcBot-s/docs/twitterOutPut.txt") // write data to file
    val upload = AuthUtil.authUpload(config)
    val url = upload.upload(new File(imgPath), tweet)
    println("Tweet posted: " + url)
  }
}
