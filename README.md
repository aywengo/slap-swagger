# slap-swagger

Keep track on public api-docs changes.

!['Slap'](https://i.imgflip.com/2y10i3.jpg)

## Quick run

```bash
export SLAP_URL="link to api-doc as json"
export SLAP_INTERVAL=30
export SLAP_SLACK_URL="link to Slack incomming webhook"

sbt run
```

## Environment variables

SLAP_URL - url to the api-doc. Has to be in JSON.

SLAP_INTERVAL - in minutes.

SLAP_SLACK_URL - incomming webhook in Slack.

## Docker image

> sbt docker:publishLocal

## TODO

* switch to [quarz scheduler](https://github.com/enragedginger/akka-quartz-scheduler)
* ~Integration with Slack~
* docker compose
