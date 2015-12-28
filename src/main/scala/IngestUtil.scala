/**
  * Created by glcohen on 12/27/15.
  */

import java.io.{FileWriter, BufferedWriter, File}
import java.text.SimpleDateFormat
import java.util.{Calendar, ArrayList}

object IngestUtil {
  def toFile(list: ArrayList[String], fileName: String): Boolean = {
    val file = new File(fileName)
    val bw = new BufferedWriter(new FileWriter(file))
    for (i <- 0 until list.size()) {
      bw.write(list.get(i).toString)
    }
    bw.close()
    true
  }

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