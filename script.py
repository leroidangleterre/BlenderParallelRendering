# Python script for parallel rendering

import bpy
import socket
import time
from time import sleep

scene = bpy.context.scene
initFilepath = scene.render.filepath # remember the current output path

port = 65432
address = '192.168.1.42'

print("START ----------------------------------------------")

localhostName = socket.gethostname()
print("hostname: "+localhostName)
localIP = socket.gethostbyname(localhostName)
print("local ip: "+localIP)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((address, port))
clientNumber = -1

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

        elif (clientNumber==-1 and data.find("Node") >= 0):
            print("Receiving clientNumber from server")
            clientNumber = data.split(" ")[1]
            clientNumber.replace("\n", "")

        else:
            print("Receiving image id")
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
            stringToSend = "client " + clientNumber + ", " + localIP + " rendered " + str(imageIndex) + "\n"
            s.send(stringToSend.encode())
        print("end of read loop")


except ConnectionResetError as e:
    print("Connection reset by server.")

# Reset the output filepath
scene.render.filepath = initFilepath

print("program terminated.")