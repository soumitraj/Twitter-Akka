HelloTwitter
============

Description:

 * Akka Actor based client server model to support millions of users and millions of Tweets
 * A full feature twitter simulator
 * An in memory server to process client requests
 * Web frontend with REST APIs implemented in Spray Can
 * Docker compatible
 
Following are the individual feature lists

Simulator :

 * Support customizable user Profiles
 * Create millions of users with varying profiles
 * create users and followers
 * pull timelines for each user at a given interval. We will first implement a pull mechanism to fetch timeline. Each user client actor will ask server for timelines based on the last fetch time. 
 
Server :
 * Register Users
 * Authenticate and Login user
 * Recieve tweets from user
 * Save tweets in the usertimeline of the user, home timeline of the followers and mention timeline of the people mentioned in the tweet.
 *
 
Performance Server :
 * This componenet/Actor/Client will gather performance statistics from the main server and display it on screen at fixed intervals
 
 
