#!/usr/bin/env groovy

/**
* Sends the screen capture to a remote MQTT broker whenever the screen has changed
*/

@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

import javax.imageio.ImageIO
import javax.imageio.stream.ImageOutputStream
import java.awt.*
import java.awt.image.BufferedImage

def QOS = 1

def cli = new CliBuilder(usage: 'share.groovy -a [address] -u [user] -p [pass] -s [size] -t [topic]')
cli.with {
  h(longOpt: 'help', 'Show usage information', required: false)
  a(longOpt: 'address', 'Server address, default "localhost:1883"', args: 1, required: false)
  u(longOpt: 'user', 'Username, default "guest"', args: 1, required: false)
  p(longOpt: 'password', 'Password, default "guest"', args: 1, required: false)
  s(longOpt: 'size', 'Capture size, default "300x300"', args: 1, required: false)
  t(longOpt: 'topic', 'Topic name, default "screen"', args: 1, required: false)
}

def cliOptions = cli.parse(args)
if (cliOptions.h) {
  cli.usage()
  return
}

def mqttAddress = cliOptions.a  ?: 'localhost:1883'
def mqttUser = cliOptions.u ?: 'guest'
def mqttPass = cliOptions.p ?: 'guest'
def topicName = cliOptions.t ?: 'screen'
def size = cliOptions.s ?: '300x300'

def (width, height) = size.split('x')

MqttConnectOptions options = new MqttConnectOptions(userName: mqttUser, password: mqttPass)
MqttClient client = new MqttClient("tcp://${mqttAddress}", 'WriterClient', new MemoryPersistence())
client.connect(options)

Runtime.runtime.addShutdownHook {
  println "Shutting down..."
  client.disconnect()
}

Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize()
Rectangle screenRect = new Rectangle(0, 0, width.toInteger(), height.toInteger())

def threadStopped = false
def thread = Thread.start {
  def oldByteArray

  while(!threadStopped) {
    BufferedImage capture = new Robot().createScreenCapture(screenRect)
    ByteArrayOutputStream bos = new ByteArrayOutputStream(255)
    ImageOutputStream stream =  ImageIO.createImageOutputStream(bos)
    ImageIO.write(capture, "png", stream)
    def byteArray = bos.toByteArray()
    if (!Arrays.equals(byteArray, oldByteArray)) {
      println("New capture, publishing to ${topicName} ${byteArray.size()} bytes");
      client.publish(topicName, byteArray, QOS, false)
      oldByteArray = byteArray
    }
  }
}

System.in.newReader().readLine()
threadStopped = true
thread.join()
Thread.sleep(1000)
client.disconnect()
