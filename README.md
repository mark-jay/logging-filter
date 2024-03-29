### Overview

![](https://github.com/mark-jay/logging-filter/workflows/JavaCI/badge.svg)

This is a simple spring module that provides server rest-api logging capabilities and was based on the [following](https://stackoverflow.com/a/42023374) answer.

This module is not supposed to be included as a maven dependency, but rather a copy-pastable single java-source file.(class [DoogiesRequestLogger](src/main/java/org/markjay/loggingfilter/logging/DoogiesRequestLogger.java))

This module is a servlet filter that logs input and output as well as elapsed time in json format.
Authorization based headers are filtered.
If logging level set to trace, then a ready-to-use curl command is generated and logged with non-filtered headers.

### Installation and configuration

Copy and paste [DoogiesRequestLogger](src/main/java/org/markjay/loggingfilter/logging/DoogiesRequestLogger.java) class in your project and configure it there

Set logging level in application.properties if needed 
```
logging.level.org.markjay.loggingfilter.logging.DoogiesRequestLogger=TRACE
```

### Motivation

After reading [this](https://stackoverflow.com/questions/33744875/spring-boot-how-to-log-all-requests-and-responses-with-exceptions-in-single-pl)
question and answers on stackoverflow I couldn't find a working solution that prints request and response in json format for graylog consumption.

 - actuator: didn't like the idea of unintentional exposure of sensitive information and the limit of requests it remembers  
 - CommonsRequestLoggingFilter: that way only requests are logged
 - zalando's logbook: unfortunatelly could not make it work with @RequestParam because of [this](https://github.com/zalando/logbook/issues/94)

### Output examples

```
2019-12-22 02:02:27.718 TRACE 19536 --- [o-auto-1-exec-3] o.m.l.logging.DoogiesRequestLogger       : curl -v -XGET -H 'host: localhost:38231' -H 'connection: keep-alive' -H 'accept: text/plain, application/json, application/*+json, */*' -H 'user-agent: Java/1.8.0_91' 'http://localhost:38231/index'
2019-12-22 02:02:27.720  INFO 19536 --- [o-auto-1-exec-3] o.m.l.logging.DoogiesRequestLogger       : {
  "method" : "GET",
  "url" : "http://localhost:38231/index",
  "headers" : {
    "host" : "localhost:38231",
    "connection" : "keep-alive",
    "accept" : "text/plain, application/json, application/*+json, */*",
    "user-agent" : "Java/1.8.0_91"
  },
  "request" : null,
  "httpStatus" : 200,
  "response" : "Hello World",
  "durationMs" : 3
}

```

```
2019-12-22 02:02:27.547  INFO 19536 --- [o-auto-1-exec-1] o.m.l.logging.DoogiesRequestLogger       : => POST http://localhost:38231/examples/post/by-json-body
2019-12-22 02:02:27.607 TRACE 19536 --- [o-auto-1-exec-1] o.m.l.logging.DoogiesRequestLogger       : curl -v -XPOST -H 'authorization: Bearer secretToken' -H 'headerkey1: headerValue1' -H 'host: localhost:38231' -H 'headerkey2: headerValue2' -H 'content-type: application/json' -H 'connection: keep-alive' -H 'accept: application/json, application/*+json' -H 'user-agent: Java/1.8.0_91' -d'{"message":"someEntityMessage"}' 'http://localhost:38231/examples/post/by-json-body'
2019-12-22 02:02:27.621  INFO 19536 --- [o-auto-1-exec-1] o.m.l.logging.DoogiesRequestLogger       : {
  "method" : "POST",
  "url" : "http://localhost:38231/examples/post/by-json-body",
  "headers" : {
    "authorization" : "XXX",
    "content-length" : "31",
    "headerkey1" : "headerValue1",
    "host" : "localhost:38231",
    "headerkey2" : "headerValue2",
    "content-type" : "application/json",
    "connection" : "keep-alive",
    "accept" : "application/json, application/*+json",
    "user-agent" : "Java/1.8.0_91"
  },
  "request" : "{\"message\":\"someEntityMessage\"}",
  "httpStatus" : 200,
  "response" : "{\"message\":\"body='parameter1={\\\"message\\\":\\\"someEntityMessage\\\"}', headers='{accept=application/json, application/*+json, headerkey1=headerValue1, headerkey2=headerValue2, authorization=Bearer secretToken, content-type=application/json, user-agent=Java/1.8.0_91, host=localhost:38231, connection=keep-alive, content-length=31}'\"}",
  "durationMs" : 58
}
```