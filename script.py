# Test script for parallel rendering

import bpy
import socket
import time

scene = bpy.context.scene
filepath = scene.render.filepath # get the current output path

port = 65432
address = '192.168.1.88'

print("START ----------------------------------------------")

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

s.connect((address, port))


loop = True
while(loop):
    # print("     start loop ---------------------")
    # Read from the server
    # print("     waiting for server to talk")
    data = s.recv(1000).decode()
#    print("received from server:")
#    print("     "+data)
    if (data.find("END") >= 0):
        # end detected.
        loop = False
        print("Detecting end")
    else:
    #    data.replace('\n', ' ');
    #    data.replace('\r', ' ');
        
        imageIndex = data.split(" ")[1]
        imageIndex.replace("\n", "")
        
        imageIndex = int(imageIndex)
        
        print("     received from server <"+str(imageIndex)+">")
    #    time.sleep(2)
        
        # Go to the requested frame
        scene.frame_set(int(imageIndex))
        scene.render.filepath = filepath + str(imageIndex)
        
        # Render the frame
        bpy.ops.render.render(write_still = True)
        
        # Send reply to server
        # print("     Sending to server...")
        stringToSend = "            client ONE: rendered "+str(imageIndex)+"\n"
        s.send(stringToSend.encode())
        # print("     Sending to server DONE")

print("program terminated.")