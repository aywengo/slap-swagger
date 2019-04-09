package co.melnyk.slap.Slack.models

case class Payload(
    text: Option[String] = None,
    channel: Option[String] = None,
    username: Option[String] = None,
    icon_url: Option[String] = None,
    icon_emoji: Option[String] = None,
    attachments: Option[Seq[Attachment]] = None
)
