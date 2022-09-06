#!/bin/bash
gnome-terminal --tab -- bash -c " cd .. ; cd server; mvn exec:java; exec bash"
gnome-terminal --tab -- bash -c " cd .. ; cd server; mvn exec:java; exec bash"
gnome-terminal --tab -- bash -c " cd .. ; cd server; mvn exec:java; exec bash"
gnome-terminal --tab -- bash -c " cd .. ; cd server; mvn exec:java; exec bash"

gnome-terminal --tab -- bash -c " cd .. ; cd client; mvn exec:java -Dexec.args="localhost <server_pub_key>"; exec bash"
gnome-terminal --tab -- bash -c " cd .. ; cd client; mvn exec:java; exec bash"

