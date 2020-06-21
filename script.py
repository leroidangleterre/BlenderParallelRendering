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


while(True):
    # print("     start loop ---------------------")
    # Read from the server
    # print("     waiting for server to talk")
    data = s.recv(1000).decode()
#    print("received from server:")
#    print("     "+data)

    data.replace('\n', ' ');
    data.replace('\r', ' ');
    
    imageIndex = data.split(" ")[1]
    
    print("     received from server <"+imageIndex+">")
    time.sleep(1)
    
    # Send reply to server
    # print("     Sending to server...")
    stringToSend = "NODE ONE rendered "+imageIndex+"\n"
    s.send(stringToSend.encode())
    # print("     Sending to server DONE")
