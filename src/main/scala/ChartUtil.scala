/**
  * Created by glcohen on 12/30/15.
  */

import java.io.File
import java.util.Calendar
import twitter4j.{UploadedMedia, Twitter, TwitterException}
import scalax.chart.api._
import org.jfree.data.time._

object ChartUtil {
  // Scala Chart using JFreeChart
  // Generate chart with data gathered on wotd
  def generateChart(freqMapIter: Iterator[(String, Int)]): String = {
    val timeTable:  TimeTableXYDataset = new TimeTableXYDataset()
    while (freqMapIter.hasNext) {
      val (k, v) = freqMapIter.next
      // Parse string arg for month, day, and year
      timeTable.add(new Day(k.substring(k.indexOf("/") + 1, k.lastIndexOf("/")).toInt,
        k.substring(0, k.indexOf("/")).toInt,
        k.substring(k.lastIndexOf("/") + 1, k.length).toInt),
        v,
        "Tweets")
    }
    implicit val theme = org.jfree.chart.StandardChartTheme.createLegacyTheme
    val chart = XYLineChart(timeTable, title = IngestUtil.getWOTD() + " Usage on Twitter in Last Week", legend = false)

    val chartString = "/Users/Gabe/Documents/IdeaProjects/glcBot-s/docs/wotd-" + IngestUtil.getDate() + ".jpg"
    chart.saveAsJPEG(chartString)
    chartString
  }

  // Upload chart, return URL
  // Probably unnecessary, but can be easily modified to upload multiple files if needed
  def uploadChart(twitter: Twitter, chartString: String): Long = {
    var mediaID: Long = 0
    try {
      val media: UploadedMedia = twitter.uploadMedia(new File(chartString)) // upload file
      mediaID = media.getMediaId
      println("Successfully uploaded image to twitter with ID: " + mediaID)
    } catch {
      case te: TwitterException =>
        te.printStackTrace()
        println("Failed to upload the image: " + te.getMessage())
    }
    mediaID
  }
}
