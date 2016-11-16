#!/usr/bin/env groovy

/**
* Gets a byte array from a remote MQTT broker and renders on a local frame
*/

@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import javax.swing.*
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage

def QOS = 1

def cli = new CliBuilder(usage: 'share.groovy -a [address] -u [user] -p [pass] -t [topic]')
cli.with {
  h(longOpt: 'help', 'Show usage information', required: false)
  a(longOpt: 'address', 'Server address, default "localhost:1883"', args: 1, required: false)
  u(longOpt: 'user', 'Username, default "guest"', args: 1, required: false)
  p(longOpt: 'password', 'Password, default "guest"', args: 1, required: false)
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

MqttConnectOptions options = new MqttConnectOptions(userName: mqttUser, password: mqttPass)
MqttClient client = new MqttClient("tcp://${mqttAddress}", 'ReaderClient', new MemoryPersistence())

ImageScreen imageScreen = new ImageScreen()

client.setCallback(new MqttCallback() {
  void connectionLost(Throwable throwable) {
  }

  void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
    imageScreen.setImage(mqttMessage.payload)
  }

  void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
  }
})

client.connect(options)
client.subscribe(topicName, QOS)

Runtime.runtime.addShutdownHook {
  println "Shutting down..."
  client.disconnect()
}

JFrame window = new JFrame("Images from ${topicName}")
window.add(imageScreen)
window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
window.pack()
window.setVisible(true)
window.setLocation(0, 0)
window.setSize(800, 600)

class ImageScreen extends JPanel {
  BufferedImage image

  public void setImage(byte[] bytes) {
    ByteArrayInputStream bis = new ByteArrayInputStream(bytes)
    ImageInputStream imageInputStream = ImageIO.createImageInputStream(bis)
    image = ImageIO.read(imageInputStream)
    this.revalidate()
    this.repaint()
  }

  @Override
  public void paintComponent(Graphics g) {
    if (image) {
      g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null)
    }
  }
}
