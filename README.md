
Twitter-Akka
============

Description:

 * Akka Actor based client server model to support millions of users and millions of Tweets
 * A full feature twitter simulator
 * An in memory server to process client requests
 * Web frontend with REST APIs implemented in Spray Can
 * Docker compatible
 
Following are the individual feature lists

Client(Simulator) :

 * Support customizable user Profiles.
 * The user profile has parameters like : 
    * Number of tweets per day, 
    * total user count contribution ( number of users of a given profile/total user count)
    * portion of this user profile that retweet
    * average follower count
    * average following count
    * average user timeline refresh rate 
    * average home timeline refresh rate
    * average mention timeline refresh rate
 * Create millions of users with varying profiles
 * create users and followers
 * pull timelines for each user at a given interval. We will first implement a pull mechanism to fetch timeline. Each user client actor will ask server for timelines based on the last fetch time. 
 * Record client statistics
 
Server :
 * Register Users
 * Authenticate and Login user
 * Recieve tweets from user
 * Process incoming tweets :
    * save tweets in the usertimeline of the user, 
    * save tweets in the home timeline of the followers
    * save tweets in the mention timeline of the people mentioned in the tweet.
    * save tweets in the hashtag timeline
 * server respective timelines to user based on the last refresh time.
    * The client will request server for various timelines and server will send only the new tweets which are not yet delivered to client. 
 * Record server statistics, performance counters. 
 
Performance Server :
 * this componenet/Actor/Client will gather performance statistics from the main server 
 * display the gathered performance counter on screen at fixed intervals
 
Also, this project should be tested with Docker as well. 


Instructions to run the program:
============

Unzip the file :
*  $ unzip Twitter-Akka.zip
*  Go to home directory of Twitter-Akka 
    *   $ cd Twitter-Akka

For distributed client-server EXECUTION, 
   *  First step, Start the server machine,
    *   $ cd TwitterServer
    *   $ vi src/main/resources/application.conf
  In this file application.conf, replace <IP_ADDRESS_OF_SERVER_MACHINE>” with ip address of the host and run the following command:
    *   $ sbt "run <No. of Computational Clusters>"
    * Here, you can mention the number of computatonal clusters, each of which consists of a server and 10 workers
    *  for example:- 
    * $ sbt “run 6”
          
  Second step, Start the client machine,
     *    $ cd TwitterClient
     *    $ vi src/main/resources/application.conf
  In this file application.conf, replace <IP_ADDRESS_OF_CLIENT_MACHINE>” with ip address of the client and run the following command:
   *      $ sbt “run <IP_ADDRESS_OF_SERVER_MACHINE>” 
   *      for example:-
   *      $ sbt "run 128.227.170.242"

  To stop 
 * To stop server, press CTRL+C
 
TO RUN ON THE LINUX MACHINES PROVIDED:
* Use putty and type pcluster.cise.ufl.edu and port number 2222 for machine p2, the ip for this is 192.168.1.2
* Enter your gatorlink as username and gatorid as password
* Use 'git clone' to clone the Twitter repository into the machine
* You can access this folder through WinSCP to create and add common jar files to the folders
* Change the application.conf files of Server, Client and RestAPI servers to include ip address 192.168.1.2
* Change the MyServer.scala file in RestAPI folder, to replace 'localhost' with 192.168.1.2 and port number 8080 with 8081(optional)
* Start the Server and RestAPIServer on p2
* sbt "run 5" on server and sbt "run 192.168.1.2" on RestAPIServer
* In the same way, start another machine p3 from putty, using 2322 as port number, the ip for this is 192.168.1.3
* From this machine p3, we can run the curl commands to access p2 machine such as:
* curl "http://192.168.1.2/8081/serach?searchString=Hello"



       
