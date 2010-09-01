# coding: utf-8
from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest, HTTPPluginControl
from HTTPClient import NVPair
from java.util import Random
import sys


# Parameters
URL = "http://localhost:8080"
RESOURCE = "subscribe"

# Logging
log = grinder.logger.output
out = grinder.logger.TERMINAL

test1 = Test(1, "subscribe")
request = test1.wrap(HTTPRequest())

# Test Interface
class TestRunner:
	def __call__(self):
		log("Connecting")

		result = request.GET("%s/%s" % (URL, RESOURCE))
        #log("result: %s" % result.getText())
