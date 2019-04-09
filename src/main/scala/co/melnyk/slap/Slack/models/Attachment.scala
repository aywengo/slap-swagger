package co.melnyk.slap.Slack.models

case class Attachment(
    title: Option[String] = None,
    text: String,
    fallback: Option[String] = None,
    image_url: Option[String] = None,
    thumb_url: Option[String] = None,
    title_link: Option[String] = None,
    color: Option[String] = None,
    pretext: Option[String] = None,
    author_name: Option[String] = None,
    author_link: Option[String] = None,
    author_icon: Option[String] = None
)
