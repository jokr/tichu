This is the most awesome P2P Tichu platform - because the world clearly needed a P2P Tichu platform.

What is Tichu? [Oh dear, you shouldn't have asked](http://en.wikipedia.org/wiki/Tichu).

[![Build Status](https://api.shippable.com/projects/5510c8db5ab6cc1352a8c99b/badge?branchName=master)](https://app.shippable.com/projects/5510c8db5ab6cc1352a8c99b/builds/latest)

# Build

Use sbt to build the project. You can run the sbt client in the root directory. The project consists
of four sub projects:

* messages, contains the shared message object required for the protocol
* ordinarynode, the client node
* supernode, the code for a super node

# Run

## Super Node

A supernode can be run via sbt:

	project supernode
	run-main tichu.supernode.SuperNode