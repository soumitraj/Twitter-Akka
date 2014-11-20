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
case class Profile(numberoftweetsperday: Int, percentageusers: Double, followercount: Int, followingcount: Int, 
	               userTimelineRefreshrate: Int, homeTimelineRefreshrate: Int, mentionTimelineRefreshrate: Int)      // refresh rate in seconds
case class FollowerTarget(sourceId: String,targetId: String)
case class addFollowing(targetId: String)
case class updateUserTimeline(userId: String)
case class updateHomeTimeline(userId: String)
case class updateMentionTimeline(userId: String)


object Local {

	def main(args: Array[String]){
	
		//	println(" Scala Version : "+util.Properties.versionString)
		// println("Argument 0 :"+ args(0))
		val serverIP = args(0)
		val serverPort = "5150"
		val profileobj1 = new Profile(300, 0.6, 100, 10, 30, 30, 30)
		val profileobj2 = new Profile(100, 0.2, 60, 20, 20, 20, 20)
		val profileobj3 = new Profile(10, 0.2, 10, 1, 10, 10, 10)
		val profiles = List(profileobj1, profileobj2, profileobj3)

		val profileCount: Int = profiles.length
		val totalusers: Int = 100
		var i: Int = 0
		var j: Int = 0
		var prev: Int = 0
		var profileusers: Int = 0
		var percentusers: Double = 0

  		implicit val system = ActorSystem("LocalSystem")

  		for(i <- 0 to profileCount-1) {
  			prev = profileusers + 1
  			percentusers = profiles(i).percentageusers
			profileusers = profileusers + (percentusers * totalusers).toInt  
    		for(j <- prev to profileusers)	{
				val username = "user"+j
 				val userActor = system.actorOf(Props(new UserActor(serverIP,serverPort,profiles(i),
 					      totalusers,"user"+j,j)), 
 				          name = username)  // the user actor
 		 		userActor ! Start                       // start the action
				userActor ! Register(username, username, "password")
			}
		}
	}

}

/*
class Worker extends Actor {
	
	def receive = {
			
	}
}
 
class LocalMaster(nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int,compString:String, listener: ActorRef)
extends Actor {
 
	val workerRouter = context.actorOf(
	Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")
 
	def receive = {		
		} 
	}
}
*/
	
class UserActor(masterIP: String , masterPort: String, profileobj: Profile, totalusers: Int,
	userId: String, j: Int) extends Actor {

// create the remote actor
val remoteActorString = "akka.tcp://BtcMasterSystem@"+masterIP+":"+masterPort+"/user/MasterActor"

//println(remoteActorString)
val remote = context.actorFor(remoteActorString)
var counter = 0

var tweet: String = Random.nextString(140)
var senderId: String = userId
val time: Long = System.currentTimeMillis

var sourceId: String = userId
var targetId: String = _ 
//var followercount: Int = 5 

var followingcount: Int = profileobj.followingcount
var numberoftweetsperday: Int = profileobj.numberoftweetsperday
var userTimelineRefreshrate: Int = profileobj.userTimelineRefreshrate
var homeTimelineRefreshrate: Int = profileobj.userTimelineRefreshrate
var mentionTimelineRefreshrate: Int = profileobj.userTimelineRefreshrate

var tempcount: Int = _
var tempcount2: Int = 0

//var Idmap = scala.collection.mutable.HashMap[String, Int]()
//var x: Int = 0
var Id: Int = 0


var m: Int = _
var k: Int = 0

var userFullName: String = _
var password: String = _


implicit val system = ActorSystem("LocalSystem")
var tweetschedulor:akka.actor.Cancellable = _
var userTimelineschedulor:akka.actor.Cancellable = _
var homeTimelineschedulor:akka.actor.Cancellable = _
var mentionTimelineschedulor:akka.actor.Cancellable = _
var userFollowingschedulor:akka.actor.Cancellable = _

def receive = {
  	case RemoteDetail(remoteActorString) =>
	println("Details recieved : "+remoteActorString)
	val remote2 = context.actorFor(remoteActorString)
	println("sending bind request to remote")
	remote ! BindRequest
   	case Start =>
        remote ! Message("Hello from the LocalActor")
//        remote ! BindRequest 
    case BindOK =>
      //  sender ! RequestWork

    // register users
	case Register(userFullName,userId,password) =>
	{
		remote ! Register(userFullName, userId, password)
		//self ! RegistrationOK
	}
	
	case RegistrationOK =>
	{
		remote ! Login(userId, password)
		//self ! LoginOK
	}


	// login users
	case LoginOK =>
	{	
    		userFollowingschedulor = context.system.scheduler.schedule(0 millis, 50 millis, self, "message")
 
	}  
    
  /*  case Profile(numberoftweetsperday,percentageusers, followercount, followingcount, userTimelineRefreshrate, homeTimelineRefreshrate, mentionTimelineRefreshrate) =>
    {
    	

    	// send followers info
    	for( m <- 1 to followercount) {
			if(k < 9800){
				k = j + m
			} else {
				k = j - m
			}
			sourceId = "user" + k
			remote ! FollowerTarget(sourceId, targetId)

		/*	if(!userfollowingcount.contains(userId)){
				userfollowingcount += (sourceId -> 0)
			} 	
			userfollowingcount(sourceId) += 1 */
		}

		// schedulor.cancel()

    }	*/

    case "message" =>
    {
    	Id = Random.nextInt(totalusers + 1)
    	targetId = "user" + Id
    	remote ! Follow(sourceId, targetId)
    /*	followingcount -= 1

    	if(followingcount == 0) {   	
  			userFollowingschedulor.cancel()
  		} */
    }

    case FollowingAcceptedOK =>
	{
		tempcount += 1
		if(tempcount ==  followingcount)
		{
			userFollowingschedulor.cancel()
			
		}
	} 


    var tweetpermillisecond = numberoftweetsperday/24*60*60*1000
    var timepertweet = 1/tweetpermillisecond          // in milliseconds
    tweetschedulor = context.system.scheduler.schedule(0 millis, timepertweet millis, self, "tickTweet")
    tweetschedulor.cancel()

    case "tickTweet" => 
	{
    	self!TweetFromUser(tweet,senderId,time)
    } 	

    // send the generated random tweets to the server	
    case TweetFromUser(tweet,senderId,time) => 
    {
    // send tweet message	
    	remote ! TweetFromUser(tweet, senderId, time)
	}

	var userTimelinerate = userTimelineRefreshrate * 1000   // convert to milliseconds
	userTimelineschedulor = context.system.scheduler.schedule(0 millis, userTimelinerate millis, self, "updateUserTimeline")

	case "updateUserTimeline" =>
	{
		remote ! updateUserTimeline(userId)
	}

	var homeTimelinerate = homeTimelineRefreshrate * 1000
	homeTimelineschedulor = context.system.scheduler.schedule(0 millis, homeTimelinerate millis, self, "updateHomeTimeline")

	case "updateHomeTimeline" =>
	{
		remote ! updateUserTimeline(userId)
	}

	var mentionTimelinerate = mentionTimelineRefreshrate * 1000
	mentionTimelineschedulor = context.system.scheduler.schedule(0 millis, mentionTimelinerate millis, self, "updateMentionTimeline")

	case "updateMentionTimeline" =>
	{
		remote ! updateUserTimeline(userId)
	}


	 
    case Message(msg) => 
        println(s"LocalActor received message: '$msg'")
        if (counter < 5) {
            sender ! Message("Hello back to you")
            counter += 1
        }
    case _ =>
        println("LocalActor got something unexpected.")
  }

}

