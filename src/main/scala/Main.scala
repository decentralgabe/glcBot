/**
  * Created by glcohen on 12/29/15.
  */

import twitter4j._
import scalax.chart.api._

object Main {
  def main(args: Array[String]) {
    val t0 = System.nanoTime()
    println("Running....")
    var twitter = runAuth("app") // application auth for raising query limit to 450 tweets/15 min
    println("App auth successful")
    val (freqMapIter, tweetCount) = runIngest(twitter)
    val freqMapIterDup = freqMapIter
    val imgPath = generateChart(freqMapIterDup)
    println("Ingestion successful")
    val (avg, max, min, tot, days) = getAvgMaxMinTot(freqMapIter, tweetCount)
    println("Data fetch and calculations successful")
    twitter = runAuth("normal") // update authorization to enable posting to Twitter
    println("Normal auth successful")
    postTweet(twitter, avg, max, min, tot, days)
    println("Tweet posted successfully!")
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
  }

  // Run authorization protocol, return Twitter object
  def runAuth(opt: String): Twitter = {
    var twitter: Twitter = null
    opt match {
      case "app" => try {
        twitter = AuthUtil.authApp()
      } catch {
        case te: TwitterException => te.printStackTrace()
      }
      case "normal" => try {
        // normal setup
        twitter = AuthUtil.authNormal()
      } catch {
        case te: TwitterException => te.printStackTrace()
      }
    }
    twitter
  }

  // Pull data from Twitter, return frequency map of Date -> Frequency for WOTD and tweet count
  def runIngest(twitter: Twitter): (Iterator[(String, Int)], Int) = {
    val queryText = IngestUtil.getWOTD + " since:" + IngestUtil.getXMonthsAgo(1) // careful with this
    val query: Query = new Query(queryText)
    query.setCount(100) // get up to 100 tweets per query (max)
    val (result, tweetCount) = DataIngest.runMultipleQueries(twitter, query)
    val freqMapIter = DataIngest.dateMapReduce(result.toList).toIterator
    (freqMapIter, tweetCount)
  }

  // Generate chart with data gathered on wotd
  def generateChart(freqMapIter: Iterator[(String, Int)]): String = {
    /*val data =
      for (i <- 1 to 5) yield (i, i) */

    var freqMap: Map[Float, Int] = Map()
    while(freqMapIter.hasNext) { // some magic to convert a date "12/30" to float "12.30"
      val (k, v) = freqMapIter.next
      val newK = (k.substring(0, k.indexOf("/")) + "." + k.substring(k.indexOf("/") + 1, k.length)).toFloat
      freqMap += (newK -> v)
    }
    implicit val theme = org.jfree.chart.StandardChartTheme.createDarknessTheme
    val data = freqMap.toIndexedSeq
    val chart = XYLineChart(data, title = IngestUtil.getWOTD() + " Twitter Usage in Last Month", legend = false)

    val chartString = "/Users/Gabe/Documents/IdeaProjects/glcBot-s/docs/wotd-" + IngestUtil.getDate() + ".jpg"
    chart.saveAsJPEG(chartString)
    chartString
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
  def postTweet(twitter: Twitter, avg: String, max: String, min: String, tot: String, days: String) {
    val tweet = IngestUtil.getWOTDString + "\n" + "Avg # tweets/day: " + avg + "\n" + "Max on " + max +
      "\n" + "Min on " + min + "\n" + "# tweets: " + tot + " over " + days + " days"
    println(tweet)
    IngestUtil.toFile(tweet, "/Users/Gabe/Documents/IdeaProjects/glcBot-s/docs/twitterOutPut.txt") // write data to file

    /*if (tweet.length <= 140) {
      try {
        twitter.updateStatus(tweet)
      } catch {
        case te: TwitterException => te.printStackTrace()
      }
    }*/
  }
}
