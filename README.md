HelloTwitter
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
 * The user tweets should be marked as ReTweet Yes/No. If a tweet is marked as ReTweet=Yes then it will be Retweeted by its followers depdending on the userprofile.
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
