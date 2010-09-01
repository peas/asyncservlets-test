#!/usr/bin/env python
# coding: utf-8

import sys
import simplejson
from twisted.web import client
from twisted.python import log
from twisted.internet import reactor

class CometClient(object):
    def __init__(self, id):
        self.id = id

    def write(self, content):
        log.msg("[%s]: %s" % (self.id, content.strip()))

    def close(self):
        pass

if __name__ == "__main__":
    log.startLogging(sys.stdout)
    for a in range(1, 1000):
        client.downloadPage("http://localhost:8080/subscribe", CometClient(a))
    reactor.run()
