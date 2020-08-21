# Test script for parallel rendering

import bpy
import socket
import time

scene = bpy.context.scene
initFilepath = scene.render.filepath # remember the current output path

port = 65432
address = '192.168.1.88'

print("START ----------------------------------------------")

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((address, port))
hostname = socket.gethostname()
clientNumber = -1

try:
    loop = True
    while(loop):

        # Read from the server
        data = s.recv(1000).decode()
        if (data.find("END") >= 0):
            # end detected.
            loop = False
            print("Detecting end")

        elif (clientNumber==-1 and data.find("Node") >= 0):
            clientNumber = data.split(" ")[1]
            clientNumber.replace("\n", "")

        else:
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
            stringToSend = "client " + clientNumber + " rendered " + str(imageIndex) + "\n"
            s.send(stringToSend.encode())

except ConnectionResetError as e:
    print("Connection reset by server.")

# Reset the output filepath
scene.render.filepath = initFilepath

print("program terminated.")