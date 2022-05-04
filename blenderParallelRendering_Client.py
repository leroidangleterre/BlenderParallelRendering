# Python script for parallel rendering

import bpy
import socket
import time
from time import sleep
import io
import os
import uuid

scene = bpy.context.scene


port = 65432
address = '192.168.56.1'

print("START ----------------------------------------------")

localhostName = socket.gethostname()
print("hostname: "+localhostName)
localIP = socket.gethostbyname(localhostName)
print("local ip: "+localIP)

# Identify the host
if(localIP == address):
    print("This is a client on the same machine as the server")
    isServer = True
else:
    print("This is a client")
    isServer = False

macAddress = hex(uuid.getnode())
print("Mac Address: " + macAddress)


# Size of the image fragments that we send to the server
BUFFER_SIZE = 4096


def endsWithDigit(text):
    lastChar = text[len(text)-1]
    if('0' <= lastChar and lastChar <= '9'):
        return True
    else:
        return False



# remember the current output path, especially if the client is relaunched.
while(endsWithDigit(scene.render.filepath)):
    length = len(scene.render.filepath)
    initFilepath = scene.render.filepath[0:length-1]
    print("trimmed filepath: " + initFilepath)
    scene.render.filepath = initFilepath


initFilepath = scene.render.filepath


try:
    
    loop = True
    while(loop):

        # Connect to the server in order to create one single image, then send it to the server.
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((address, port))
        
        
        # Read from the server        
        data = s.recv(1000).decode()
                
        if (data.find("END") >= 0 or data.find("-1") >= 0):
            # end detected.
            loop = False
            print("Detecting end")
            
        else:
                            
            imageIndex = data.split(" ")[1].replace("\n", "").replace("\r", "")
            
            print("Now rendering image <"+str(imageIndex)+">")
            
            # Go to the requested frame
            scene.frame_set(int(imageIndex))
            scene.render.filepath = initFilepath + str(imageIndex)
            
            # Render the frame
            bpy.ops.render.render(write_still = True)
            print(" Image rendered")
            
            filesize = str(os.path.getsize(scene.render.filepath+".png"))
            
            # Send reply to server, with info about the image (stored at scene.render.filepath)

            if isServer:
                stringToSend = "server " + macAddress + " , " + localIP + " rendered " + imageIndex + " size " + filesize + "\n"
                stringToSend = stringToSend.replace('\n', '').replace('\r', '') + '\n'
                encodedString = stringToSend.encode()
                s.send(encodedString)

            else:
                stringToSend = "client " + macAddress + " , " + localIP + " rendered " + imageIndex + " size " + filesize + "\n"
                stringToSend = stringToSend.replace('\n', '').replace('\r', '') + '\n'
                encodedString = stringToSend.encode()

                s.send(encodedString)

                # Wait for the server to be ready for the image
                data = s.recv(BUFFER_SIZE).decode()

                # Clients must send the image via a stream
                with open(scene.render.filepath + ".png", "rb") as f:
                    isSendingFile = True # File is transmitted as several packets
                    while isSendingFile:
                        bytes_read = f.read()
                        if(len(str(bytes_read)) > 10):
                            s.send(bytes_read)
                        else:
                            isSendingFile = False
                print(" Transmission done")
            
                s.shutdown(socket.SHUT_WR)

except ConnectionResetError as e:
    print("Connection reset by server.")

# Reset the output filepath
scene.render.filepath = initFilepath

print("program terminated.")


