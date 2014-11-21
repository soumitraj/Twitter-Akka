package remote

import akka.actor._
import common._
import scala.util.Random

import java.security.MessageDigest
import akka.routing.RoundRobinRouter

//   import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._


case class ProcessTweet(tweet:String,uid:String,time:Long)
case class UserDetails(userId:String, userName:String, homeTimelineLastFetchIndex:Int)

case object PrintStatistics

object HelloRemote  {
 
def main(args : Array[String]){
 println("Scala version :: "+util.Properties.versionString)
  

val nrOfWorkers = 1
val system = ActorSystem("BtcMasterSystem")
val listener = system.actorOf(Props[Listener], name = "listener")
val masterActor = system.actorOf(Props(new Master(nrOfWorkers, listener)),
				name = "MasterActor")

	  masterActor ! Start 
	  masterActor ! Message("The Master is alive and started")
	  masterActor ! Register("abc","uid1","pswd")
       masterActor ! Login("uid1","pswd")
       masterActor !  TweetFromUser("HelloTwitter","uid1",System.currentTimeMillis) 
	
	masterActor ! PrintStatistics

	masterActor ! Register("user2","uid2","pswd")
	masterActor ! Login("uid2","pswd")
	masterActor ! Follow("uid2","uid1")
       masterActor !  TweetFromUser("HelloTwitter","uid1",System.currentTimeMillis) 

	masterActor ! PrintStatistics
}
}

class Worker extends Actor {
	var tweetsMap = new scala.collection.mutable.HashMap[String, Tweet]()
	var homeTimelineMap = new scala.collection.mutable.HashMap[String,List[Tweet]]() // userId, tweetlist
	var userTimelineMap = new scala.collection.mutable.HashMap[String,List[Tweet]]()
	var userFollowerMap = new scala.collection.mutable.HashMap[String,List[String]]()
	val tweetcount=0;
	var userDetailsMap = new scala.collection.mutable.HashMap[String,UserDetails]
	
	def printTimelines() = {
		println("tweetsMap: "+tweetsMap)
		println("home Timelines : "+homeTimelineMap)
		println("user TimeLine : "+userTimelineMap)
		println("userFollowerMap : "+userFollowerMap)
		println("userDetails : "+userDetailsMap)
	}
	
	def addFollower(sourceUID:String,targetUID:String) = {
		
		val followerList : List[String] = userFollowerMap.get(targetUID) match {
							case Some(list) => list
							case None => List[String]()
						}
		val newfollowerList = sourceUID +: followerList

		userFollowerMap += (targetUID -> newfollowerList)

	}

//	def getHomeTimeline(userID : String) : List[Tweet] = {
//		val homeTimeline = homeTimelineMap.get(userID).get
//		val userDetails = userDetailsMap.get(userID).get
//		var (read,unread) = splitAt userDetails.homeTimelineLastFetchIndex
//		
//		
//		return unread
//
//	}
	 
	def populateHomeTimeline(senderId:String,tweetId:String) = {

	//	val followerList : List[String] = userFollowerMap.get(senderId).get
		val followerList : List[String] = userFollowerMap.get(senderId) match {
							case Some(list) => list
							case None => List[String]()
						}
			val followerCount: Int = followerList.length
			printTimelines()
			//for(val followerId:String <- followerList){
			followerList.map { followerId =>
				//println(followerId)
				//for each follower add the new tweet to its timeline
				var homeTimeline = homeTimelineMap.get(followerId) match {
								case Some(timeline) => timeline
								case None => List[Tweet]()
							}
					homeTimeline = tweetsMap.get(tweetId).get +: homeTimeline
					//homeTimeline.add(tweetsMap.get(tweetId).get)				
					homeTimelineMap += (followerId -> homeTimeline)
			}

	}
	 
	 //  The user timeline contains tweets that the user sent
	 def populateUserTimeline(senderId:String,tweetId:String)  = {
			var userTimeline = userTimelineMap.get(senderId) match {
						case Some(timeline) => timeline
						case None => List[Tweet]()
			}
			
			userTimeline = tweetsMap.get(tweetId).get +: userTimeline
			homeTimelineMap += (senderId -> userTimeline)
			
	 	}
	 
	 
		def receive = {
			case ProcessTweet(tweet,senderId,time) ⇒
			{
				// store the tweets in database
				// associate the tweetId to the followers of the tweet sender
				//send update to the client
				println("Tweet recieved from serverMaster :"+tweet)
				println("Tweet recieved from serverMaster :"+senderId)
				println("Tweet recieved from serverMaster :"+time)
				val tweetId = time+"_"+senderId
				tweetsMap += (tweetId ->Tweet(tweetId,senderId,time,tweet))
				populateUserTimeline(senderId,tweetId) // fanout will associate the tweets with the follower's timeline
				populateHomeTimeline(senderId,tweetId)
				println("Fanout Completed")
		//		printTimelines()
			}
			
			case Follow(sourceUserId,targetUserId) =>
			{
				addFollower(sourceUserId,targetUserId)
				
			}
			
			case PrintStatistics =>
			{
				printTimelines()
			}
		}
	}
 
	class Master(nrOfWorkers: Int, listener: ActorRef)
	extends Actor {
 
		var nrOfResults: Int = _
		var nrOfClients: Int = _
		val start: Long = System.currentTimeMillis
		var register = new scala.collection.mutable.HashMap[String, String]()
		var tweetlist = new java.util.ArrayList[String]()
 
		val workerRouter = context.actorOf(
		Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")
 
		def receive = {
		
		
			case Register(userFullName,userId,password) ⇒
				{
					//the client will send the register message, when the server receives the message it will save the details of the user in a map (userId as key and other details as values) that will be used to autheticate the login request.
					
					register += (userId -> password)
					
					println(s"$userId registered")
					sender ! RegistrationOK

				}
				
				
			case Login(userId,password) ⇒ 
				{
				// check the login credentials from the client against the registered ones 
			//		if( register.contains(userId)){
           				
           			//	if(register(userId) == password) {
           					println("User login successful for "  + userId) 
						sender ! LoginOK
           			//	} else {
           			//		println("UserId and password doesn't match")
           			//	}
      			//	} else {
      			//		println("User is not registered")
      			//	}
				
				} 
				
			case TweetFromUser(tweet,senderId,time) =>
				{
					/// send the recieved tweet to the Worker for furhter processing
					println("Tweet recieved from client :"+tweet)
					println("Tweet recieved from client :"+senderId)
					println("Tweet recieved from client :"+time)
					workerRouter ! ProcessTweet(tweet,senderId,time)
					sender ! TweetProcessedOK
					// store the tweet from the user in a ArrayList
					
					//tweetlist.add(tweet)
				}	
				
				
			case Follow(sourceUserId,targetUserId) ⇒
				{
				   // form the following relationship between source and target.Once source follows the target the source will recieve all the tweets from the targetUSer
					workerRouter ! Follow(sourceUserId,targetUserId)
					println(s"$sourceUserId is now following $targetUserId")

					sender ! FollowingAcceptedOK

				}		
	
			case PrintStatistics => 
			{
				workerRouter ! PrintStatistics
			}
				
			 case Start =>
    				println("Master starting work")
   			 case BindRequest =>
				println("Bind request recieved ")
       		 sender ! BindOK
				println("Bind ok sent")
                case Message(msg) =>
                  println(s"Master received message "+msg)    	
				
			}
		}
 
	class Listener extends Actor {
		def receive = {
	
		case ShutdownMaster(message) ⇒
			println("\n\tShutdown MEssage \t%s"
			.format(message))
			context.system.shutdown() 
		}
	}


