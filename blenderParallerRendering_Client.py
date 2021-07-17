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
address = '192.168.1.39'

print("START ----------------------------------------------")

localhostName = socket.gethostname()
print("hostname: "+localhostName)
localIP = socket.gethostbyname(localhostName)
print("local ip: "+localIP)

# Identify the host
if(localIP == address):
    print("THIS IS THE SERVER")


macAddress = hex(uuid.getnode())
print("Mac Address: " + macAddress)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((address, port))


# TODO: run the Java process that watches the rendered images directory.



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

        # Read from the server
        print("ready to read from server")
        data = s.recv(1000).decode()
        
        if (data.find("END") >= 0):
            # end detected.
            loop = False
            print("Detecting end")

        else:
            print("Receiving image id")
            print("String received from server: \"" + data + "\"")
            imageIndex = data.split(" ")[1]
            imageIndex.replace("\n", "")
            
            imageIndex = int(imageIndex)
            
            print("     received from server <"+str(imageIndex)+">")
            
            # Go to the requested frame
            scene.frame_set(int(imageIndex))
            scene.render.filepath = initFilepath + str(imageIndex)
            
            # Render the frame
            bpy.ops.render.render(write_still = True)
            
            # Send reply to server
            stringToSend = "client " + macAddress + " , " + localIP + " rendered " + str(imageIndex) + "\n"
            encodedString = stringToSend.encode()
            print("Init string: <" + stringToSend + ">, encoded: <" + str(encodedString) + ">")
            s.send(encodedString)

            # Send the image stored ad scene.render.filepath
            filesize = os.path.getsize(filepath)
            s.send("sending image" + str(imageIndex) + " size " + filesize)

            # Images shall remain on the clients until manually deleted.

        print("end of read loop")


except ConnectionResetError as e:
    print("Connection reset by server.")

# Reset the output filepath
scene.render.filepath = initFilepath

print("program terminated.")


