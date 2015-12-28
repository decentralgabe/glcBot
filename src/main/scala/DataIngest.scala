/**
  * Created by glcohen on 12/26/15.
  */

import java.io.{BufferedWriter, File, FileWriter}
import java.text.SimpleDateFormat
import java.net.URLEncoder._
import java.util.{ArrayList, Calendar}
import twitter4j.{Query, QueryResult, Status, Twitter, TwitterException}

object DataIngest {
  def main(args: Array[String]): Unit = {
    println("SHIIEET HERE WE GO")

    val (works, twitter) = AuthUtil.setUp()
    val queryText = encode(("ucsdkoala since:" + IngestUtil.getXMonthsAgo(1)), "UTF-8")
    val query: Query = new Query(queryText)
    if (works) {
      //val tweets = runAndSaveQuery(twitter, query)
      //toFile(tweets, "twitterOutput.txt")
      val qList = runQueryPullData(twitter, query, 1)
      println("List size: " + qList.size)
      qList.toArray.foreach(println)
    } else {
      println("Pull failed")
    }
  }

  def runQueryPullData(twitter: Twitter, query: Query, totalTweets: Int): java.util.ArrayList[java.util.Date] = {
    var lastID: Long = Long.MaxValue
    val dateList = new java.util.ArrayList[java.util.Date]()
    var tweetsPulled: Int = 0 // counter for # tweets pulled so far
    var result: QueryResult = null
    var nextTweet: Status = null

    while (tweetsPulled < totalTweets) {
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