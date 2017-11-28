// pocoClient.cpp
//
#include <stdlib.h>
#include <cstring>
#include <thread>
#include "../include/connectionHandler.h"
#include <boost/algorithm/string.hpp>

using std::string;
using std::thread;
using std::getline;
using std::cin;

void receiverProc(void* param) {
	ConnectionHandler* connHandler = (ConnectionHandler*)param;
	string line = "";
	while (connHandler->getLine(line)) {
		std::cout << line;
		line = "";
	}

	std::cout << "Client disconnected" << std::endl;;
}

/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl;
        return -1;
    }

    const string host = argv[1];
    const short port = atoi(argv[2]);

	ConnectionHandler connectionHandler(host, port);
	if (!connectionHandler.connect()) {
		std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
		return 1;
	}
    
	thread t1(receiverProc, (void*)&connectionHandler);

	string line = "";
	do {
		getline(cin, line);
		connectionHandler.sendLine(line);
	} while (boost::iequals(line, "quit") == false);

	// The thread should end as the connection has been terminated..
	t1.join();
	
    return 0;
}
