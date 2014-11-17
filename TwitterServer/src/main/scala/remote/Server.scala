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

object HelloRemote  {
 
def main(args : Array[String]){
 println("Scala version :: "+util.Properties.versionString)
  

val nrOfWorkers = 2
val system = ActorSystem("BtcMasterSystem")
val listener = system.actorOf(Props[Listener], name = "listener")
val masterActor = system.actorOf(Props(new Master(nrOfWorkers, listener)),
				name = "MasterActor")

	  masterActor ! Start 
	  masterActor ! Message("The Master is alive and started")
	  masterActor ! Register("abc","uid","pswd")
       masterActor ! Login("uid","pswd")
       masterActor !  TweetFromUser("HelloTwitter","dev001",System.currentTimeMillis) 
}
}

class Worker extends Actor {
	var tweetsMap = new scala.collection.mutable.HashMap[String, Tweet]()
	var homeTimelineMap = new scala.collection.mutable.HashMap[String,List[Tweet]]() // userId, tweetlist
	var userTimelineMap = new scala.collection.mutable.HashMap[String,List[Tweet]]()
	var userFollowerMap = new scala.collection.mutable.HashMap[String,List[String]]()
	val tweetcount=0;
	var userDetailsMap = new scala.collection.mutable.HashMap[String,UserDetails]
	
	def addFollower(sourceUID:String,targetUID:String) = {
		
		val followerList : List[String] = userFollowerMap.get(targetUID).get
		followerList.add(sourceUID)
		userFollowerMap += (targetUID -> followerList)

	}

	def getHomeTimeline(userID : String) : List[Tweet] = {
		val homeTimeline = homeTimelineMap.get(userID).get
		val userDetails = userDetailsMap.get(userID).get
		var (read,unread) = splitAt userDetails.homeTimelineLastFetchIndex
		
		
		return unread

	}
	 
	def populateHomeTimeline(senderId:String,tweetId:String) = {

		val followerList : List[String] = userFollowerMap.get(senderId).get
			val followerCount: Int = followerList.length

			//for(val followerId:String <- followerList){
			followerList.map { followerId =>
				//println(followerId)
				//for each follower add the new tweet to its timeline
				var homeTimeline = homeTimelineMap.get(followerId).get 

					homeTimeline.add(tweetsMap.get(tweetId).get)				
					homeTimelineMap += (followerId -> homeTimeline)
			}

	}
	 
	 //  The user timeline contains tweets that the user sent
	 def populateUserTimeline(senderId:String,tweetId:String)  = {
			var userTimeline = userTimelineMap.get(senderId).get
			userTimeline.add(tweetsMap.get(tweetId).get)
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
				tweetsMap += (tweetId ->Tweet(tweetId,senderId,time))
			//	populateUserTimeline(senderId,tweetId) // fanout will associate the tweets with the follower's timeline
				populateHomeTimeline(senderId,tweetId)
				println("Fanout Completed")
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

				}
				
				
			case Login(userId,password) ⇒ 
				{
				// check the login credentials from the client against the registered ones 
					if( register.contains(userId)){
           				
           				if(register(userId) == password) {
           					println("User login successful for "  + userId) 
           				} else {
           					println("UserId and password doesn't match")
           				}
      				} else {
      					println("User is not registered")
      				}
				
				} 
				
			case TweetFromUser(tweet,senderId,time) =>
				{
					/// send the recieved tweet to the Worker for furhter processing
					println("Tweet recieved from client :"+tweet)
					println("Tweet recieved from client :"+senderId)
					println("Tweet recieved from client :"+time)
					workerRouter ! ProcessTweet(tweet,senderId,time)
					// store the tweet from the user in a ArrayList
					
					//tweetlist.add(tweet)
				}	
				
				
			case Follow(sourceUserId,targetUserId) ⇒
				{
				   // form the following relationship between source and target.Once source follows the target the source will recieve all the tweets from the targetUSer

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


