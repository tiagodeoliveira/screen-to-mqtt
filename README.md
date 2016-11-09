share.groovy - Sends a screenshot over MQTT as a PNG byte array

read.groovy - reads the MQTT byte array and renders it on a frame

Sending a small screen portion capture via MQTT (local broker), receiving the byte array back and rendering on a window frame.
![The result](https://dl.dropboxusercontent.com/u/6575781/gifs/screen-to-mqtt.gif)

A good enhancement would be send only the difference between images, that would reduce a lot the content size.
