/**
  * Created by glcohen on 12/30/15.
  */

import java.io.File
import twitter4j.{UploadedMedia, Twitter, TwitterException}
import scalax.chart.api._

object ChartUtil {
  // Generate chart with data gathered on wotd
  def generateChart(freqMapIter: Iterator[(String, Int)]): String = {
    var freqMap: Map[Float, Int] = Map()
    while (freqMapIter.hasNext) {
      // some magic to convert a date "12/30" to float "12.30"
      val (k, v) = freqMapIter.next
      val newK = (k.substring(0, k.indexOf("/")) + "." + k.substring(k.indexOf("/") + 1, k.length)).toFloat
      freqMap += (newK -> v)
    }
    implicit val theme = org.jfree.chart.StandardChartTheme.createLegacyTheme
    val data = freqMap.toIndexedSeq
    val chart = XYLineChart(data, title = IngestUtil.getWOTD() + " Usage on Twitter in Last Month", legend = false)

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
