all: client

client: src/*
	g++ -Wall -g -std=c++11 -I/usr/local/boost/1.57.0/include/boost -o bin/client src/* -lboost_system -pthread
	
clean: 
	rm -rf bin/*