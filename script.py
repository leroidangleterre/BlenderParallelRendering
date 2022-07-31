import socket
import time
print("This is the Python client.")

serverIP = "192.168.1.39"
serverPort = 65432

hostname = socket.gethostname()
clientIpAddress = socket.gethostbyname(hostname)

# Simulate a rendering process
renderingTime = 3

# Time allowed before a jobless client asks for a new job
unemployedDuration = 5


def connectToServer():
	with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:

		print("Connecting to server")

		s.connect((serverIP, serverPort))
		loop = True
		while(loop):
			print("*********************")
			print("*   START           *")
			print("*********************")
			print("Receiving from server")

			# RECEIVING
			data = s.recv(1024)
			serverRequest = str(data)

			words = serverRequest.split(' ')
			
			try:
				# Do the work now.
				filenameTab = words[1].split('\\')
				filename = filenameTab[len(filenameTab)-1]
				print("filename: " + filename)
				
				frame = words[2].split('\\')[0]
				print("frame("+frame+");")
				
				print("rendering...")
				time.sleep(renderingTime)
				print("rendering done.")
				messageToServer = "Client " + str(clientIpAddress) + " rendered frame " + str(frame) + ".\n"
				s.sendall(messageToServer.encode('utf-8'))
			except IndexError:
				# Something went wrong or no job is available
				print("No job available. Waiting " + str(unemployedDuration) + "s.")
				time.sleep(unemployedDuration)
				s.sendall(b"Client done waiting.\n")
			except ConnectionResetError:
				# Server disconnected, must be ready for later
				time.sleep(unemployedDuration)
				print("Waiting on server.")
			except BrokenPipeError:
				# Must retry to connect to the server
				loop = False

			#print("Replying to server")
			# REPLYING
			#s.sendall(b"Client done rendering.\n")
			
			print("client done")

# **********************************************************
# MAIN PROGRAM
# **********************************************************

print("creating socket")
connectToServer()
