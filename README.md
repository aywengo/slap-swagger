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

## TODO

* switch to [quarz scheduler](https://github.com/enragedginger/akka-quartz-scheduler)
* Integration with Slack
* docker compose
