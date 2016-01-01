/**
  * Created by glcohen on 12/27/15.
  */

import java.io.{FileWriter}
import java.text.SimpleDateFormat
import java.util.{Calendar}

import scala.xml.XML

object IngestUtil {

  // return single word
  def getWOTD(): String = {
    val wotdStr = getWOTDString
    wotdStr.substring(0, wotdStr.indexOf(":")).capitalize
  }

  // return word and definition
  def getWOTDString(): String = {
    val xml = XML.load("http://dictionary.reference.com/wordoftheday/wotd.rss")
    val firstTitle = (xml \\ "description").map(_.text).mkString("\n") // '\\' searches whole doc, '\' just searches children
    val wotd = firstTitle.substring(firstTitle.lastIndexOf("\n") + 1, firstTitle.length - 1) // get last \n, wotd is after
    wotd
  }

  // Write daily info to file
  def toFile(text: String, fileName: String) {
    val fw = new FileWriter(fileName, true)
    fw.write("\n\n%s\n%s".format(getDate, text))
    fw.close()
    println("Wrote " + text.length + " characters to " + fileName)
  }

  // Return current timestamp
  def getDate(): String = {
    val format = new SimpleDateFormat("y-M-d")
    val cal = Calendar.getInstance()
    format.format(cal.getTime)
  }

  // Check if the word of day for today the date matches today's date
  def isNewWOTD(): Boolean = {
    // current date
    val format = new SimpleDateFormat("M-d")
    val cal = Calendar.getInstance()
    val currDate = format.format(cal.getTime())

    // get last updated date from wotd.rss
    val xml = XML.load("http://dictionary.reference.com/wordoftheday/wotd.rss")
    val lastBuildDate = (xml \\ "lastBuildDate").map(_.text).mkString(" ")
    val date = lastBuildDate.substring(lastBuildDate.indexOf(",") + 2, lastBuildDate.indexOf(":") - 7) // get date of format "30 Dec"
    var d = date.substring(0, date.indexOf(" "))
    if (d.charAt(0) == '0' ) { d = d.charAt(1).toString } // make sure date is correctly formatted
    val formattedDate =
      date.substring(date.indexOf(" ") + 1, date.length - 1) match {
        case "Jan" => "1-" + d
        case "Feb" => "2-" + d
        case "Mar" => "3-" + d
        case "Apr" => "4-" + d
        case "May" => "5-" + d
        case "Jun" => "6-" + d
        case "Jul" => "7-" + d
        case "Aug" => "8-" + d
        case "Sep" => "9-" + d
        case "Oct" => "10-" + d
        case "Nov" => "11-" + d
        case "Dec" => "12-" + d
      }
    if (formattedDate == currDate) true else false
  }

  /*********************************
   Below methods no longer used
   **********************************/

  // Return date from x year(s) ago for search query of format y-M-d
  // ex: 2 years ago from 2015-12-27 = 2014-12-27
  def getXYearsAgo(years: Int) = {
    val format = new SimpleDateFormat("y-M-d")
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, years * -365) // subtract x years (assume year = 365 days)
    format.format(cal.getTime())
  }

  // Return date from x month(s) ago for search query of format y-M-d
  // ex: 2 months ago from 2015-12-27 = 2015-10-27
  def getXMonthsAgo(months: Int) = {
    val format = new SimpleDateFormat("y-M-d")
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -months) // subtract x months
    format.format(cal.getTime())
  }
}