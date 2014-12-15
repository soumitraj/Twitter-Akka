package local

import akka.actor._
import common._
import akka.util._
import scala.concurrent.duration._


import scala.util.Random

import akka.routing.RoundRobinRouter
import java.net.InetAddress

import scala.concurrent.ExecutionContext.Implicits.global


case class RemoteDetail(remoteActorString : String)
case class Profile(numberoftweetsperhour: Double, percentageusers: Double, followercount: Int, followingcountrate: Double, 
	               userTimelineRefreshrate: Double, homeTimelineRefreshrate: Double, mentionTimelineRefreshrate: Double)      // refresh rate in seconds
case class FollowerTarget(sourceId: String,targetId: String)


object Local {

	def main(args: Array[String]){
	
		//	println(" Scala Version : "+util.Properties.versionString)
		// println("Argument 0 :"+ args(0))
		val serverIP = args(0)
		val serverPort = "5150"
		val profileobj1 = new Profile(400, 0.4, 100, 1, 30, 30, 30)
		val profileobj2 = new Profile(300, 0.3, 60, 1, 20, 20, 20)
		val profileobj3 = new Profile(200, 0.2, 10, 0.5, 10, 10, 10)
		val profileobj4 = new Profile(100, 0.1, 10, 0.5, 10, 10, 10) 
		val profiles = List(profileobj1, profileobj2, profileobj3, profileobj4)

		val profileCount: Int = profiles.length
		val totalusers: Int = 100

		var i: Int = 0
		var j: Int = 0
		var prev: Int = 0
		var profileusers: Int = 0
		var percentusers: Double = 0



  		implicit val system = ActorSystem("LocalSystem")

  		val remoteActorString = "akka.tcp://BtcMasterSystem@"+serverIP+":"+serverPort+"/user/MasterActor"
		val remote = system.actorFor(remoteActorString)

  		for(i <- 0 to profileCount-1) {
  			prev = profileusers + 1
  			percentusers = profiles(i).percentageusers
			profileusers = profileusers + (percentusers * totalusers).toInt  
    		for(j <- prev to profileusers)	{
    				val clientId = "Client"+Random.nextInt(50000)
				val username = clientId+j
 				val userActor = system.actorOf(Props(new UserActor(remote,profiles(i),
 					      totalusers,username ,j,clientId)), name = username)  // the user actor
				userActor ! Register(username, username, "password")
			}
		}
	}

}


class UserActor(remote: ActorRef, profileobj: Profile, totalusers: Int,
	userId: String, j: Int,clientId:String) extends Actor {



	//var tweet: String = Random.nextString(140)
	val tweet:String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Utenimad minim."
	var senderId: String = userId
	val time: Long = System.currentTimeMillis

	var sourceId: String = userId
	var targetId: String = _ 
	var userTimelineRefreshrate: Double = profileobj.userTimelineRefreshrate
	var homeTimelineRefreshrate: Double = profileobj.userTimelineRefreshrate
	var mentionTimelineRefreshrate: Double = profileobj.userTimelineRefreshrate


	var followingcountrate: Double = profileobj.followingcountrate
	var numberoftweetsperhour: Double = profileobj.numberoftweetsperhour

	var Id: Int = 0
	
	

	var userFullName: String = _
	var password: String = _
	
	
	var tweetschedulor:akka.actor.Cancellable = _
	var userTimelineschedulor:akka.actor.Cancellable = _
	var homeTimelineschedulor:akka.actor.Cancellable = _
	var mentionTimelineschedulor:akka.actor.Cancellable = _
	var userFollowingschedulor:akka.actor.Cancellable = _

	//start schedulers to send requests to clients based on the request rates specified in the profile.
    
     var timepertweet = 60*60/numberoftweetsperhour          // in seconds
 
     tweetschedulor = context.system.scheduler.schedule(10000 millis, timepertweet seconds, self, "tickTweet")
	userTimelineschedulor = context.system.scheduler.schedule(10000 millis, userTimelineRefreshrate seconds, self, "updateUserTimeline")
	homeTimelineschedulor = context.system.scheduler.schedule(10000 millis, homeTimelineRefreshrate seconds, self, "updateHomeTimeline")
	mentionTimelineschedulor = context.system.scheduler.schedule(10000 millis, mentionTimelineRefreshrate millis, self, "updateMentionTimeline")
	userFollowingschedulor = context.system.scheduler.schedule(5000 millis, 30000 millis, self, "followmessage")


	def receive = {
  		
	    // register users
		case Register(userFullName,userId,password) =>
		{
			remote ! Register(userFullName, userId, password)
		}
		
		case RegistrationOK =>
		{
			remote ! Login(userId, password)
		}
	
	
		// login users
		case LoginOK =>
		{	
			var followingcountrate = userTimelineRefreshrate * 1000    // convert to millis
    			
 		//	userFollowingschedulor.cancel()
		}  
	
    		case "followmessage" =>
    		{
		    	Id = Random.nextInt(totalusers + 1)
	
			// if the random generated USerID is 0 then generate again
		    	while(Id == 0){
    				Id = Random.nextInt(totalusers + 1)
		    	}
   
		    	targetId = clientId + Id
		    	remote ! Follow(sourceId, targetId)
		   
	    }
    
	    case Follow(sourceId,targetUserId) => {
    			 remote ! Follow(sourceId,targetUserId)
	    }
	
	    case "tickTweet" => 
		{
    			remote ! TweetFromUser(tweet,senderId,time)
	    } 	

		case TweetProcessedOK =>
		{

		}	


		case "updateUserTimeline" =>
		{
			remote ! UpdateUserTimeline(userId)
		}
	

		case "updateHomeTimeline" =>
		{
			remote ! UpdateHomeTimeline(userId)
		}

		case "updateMentionTimeline" =>
		{
			remote ! UpdateUserTimeline(userId)
		}
	
		case UpdateSearchTimeline(userId, searchToken) =>
		{
			remote ! UpdateSearchTimeline(userId, searchToken)
		}
	
	
		case UserTimeline(userTimeline) =>
		{
			//println("User Timeline Tweets recieved :"+userTimeline.tweetList.size)
		}
	
		case HomeTimeline(homeTimeline) =>
		{
			//println("Home Timeline Tweets recieved :"+homeTimeline.tweetList.size)
		}
	 
        
	    case _ =>
     	   println("LocalActor got something unexpected.")
	  }

}
