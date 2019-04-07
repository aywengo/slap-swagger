package co.melnyk.slap

import com.typesafe.config._

object Config {

  val conf: Config = ConfigFactory.load()

  lazy val url: String = conf.getString("url")

  lazy val interval: Int = conf.getInt("interval")

  lazy val slackUrl: String = conf.getString("slackUrl")
}
