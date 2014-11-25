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
case class Profile(numberoftweetsperday: Double, percentageusers: Double, followercount: Int, followingcountrate: Double, 
	               userTimelineRefreshrate: Int, homeTimelineRefreshrate: Int, mentionTimelineRefreshrate: Int)      // refresh rate in seconds
case class FollowerTarget(sourceId: String,targetId: String)


object Local {

	def main(args: Array[String]){
	
		//	println(" Scala Version : "+util.Properties.versionString)
		// println("Argument 0 :"+ args(0))
		val serverIP = args(0)
		val serverPort = "5150"
		val profileobj1 = new Profile(300, 0.4, 100, 1, 30, 30, 30)
		val profileobj2 = new Profile(100, 0.3, 60, 1, 20, 20, 20)
		val profileobj3 = new Profile(50, 0.2, 10, 0.5, 10, 10, 10)
		val profileobj4 = new Profile(10, 0.1, 10, 0.5, 10, 10, 10) 
		val profiles = List(profileobj1, profileobj2, profileobj3, profileobj4)

		val profileCount: Int = profiles.length
		val totalusers: Int = 1000

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
				val username = "user"+j
 				val userActor = system.actorOf(Props(new UserActor(remote,profiles(i),
 					      totalusers,"user"+j,j)), 
 				          name = username)  // the user actor
 		 		//userActor ! Start                       // start the action
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
	
class UserActor(remote: ActorRef, profileobj: Profile, totalusers: Int,
	userId: String, j: Int) extends Actor {

// create the remote actor

var counter = 0

var tweet: String = Random.nextString(140)
var senderId: String = userId
val time: Long = System.currentTimeMillis

var sourceId: String = userId
var targetId: String = _ 
//var followercount: Int = 5 

var followingcountrate: Double = profileobj.followingcountrate
var numberoftweetsperday: Double = profileobj.numberoftweetsperday
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


    var tweetpermillisecond = numberoftweetsperday/24*60*60*1000
    var timepertweet = 1/tweetpermillisecond          // in milliseconds
    tweetschedulor = context.system.scheduler.schedule(10000 millis, 5 seconds, self, "tickTweet")
 //   tweetschedulor.cancel()
	
	var userTimelinerate = userTimelineRefreshrate * 1000   // convert to milliseconds
//	userTimelineschedulor = context.system.scheduler.schedule(10000 millis, userTimelinerate millis, self, "updateUserTimeline")

	var homeTimelinerate = homeTimelineRefreshrate * 1000
//	homeTimelineschedulor = context.system.scheduler.schedule(10000 millis, homeTimelinerate millis, self, "updateHomeTimeline")

	var mentionTimelinerate = mentionTimelineRefreshrate * 1000
//	mentionTimelineschedulor = context.system.scheduler.schedule(10000 millis, mentionTimelinerate millis, self, "updateMentionTimeline")


def receive = {
  	case RemoteDetail(remoteActorString) =>
		println("Details recieved : "+remoteActorString)
		val remote2 = context.actorFor(remoteActorString)
		println("sending bind request to remote")
		remote ! BindRequest
   	case Start =>
     //   remote ! Message("Hello from the LocalActor")
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
		var followingcountrate = userTimelineRefreshrate * 1000    // convert to millis
    	userFollowingschedulor = context.system.scheduler.schedule(5000 millis, followingcountrate millis, self, "followmessage")
 	//	userFollowingschedulor.cancel()
	}  

    case "followmessage" =>
    {
    	Id = Random.nextInt(totalusers + 1)
    	while(Id == 0){
    		Id = Random.nextInt(totalusers + 1)
    	}
    	targetId = "user" + Id
    	remote ! Follow(sourceId, targetId)
   
    }

/*    case FollowingAcceptedOK =>
	{
	/*	tempcount += 1
    	if(tempcount == followingcount)
		{
			
			userFollowingschedulor.cancel()
		}  */
	} 
*/



    case "tickTweet" => 
	{
    	remote ! TweetFromUser(tweet,senderId,time)
    } 	

    // send the generated random tweets to the server	
 /*   case TweetFromUser(tweet,senderId,time) => 
    {
    // send tweet message	
    	remote ! TweetFromUser(tweet, senderId, time)
	}
*/

	case TweetProcessedOK =>
	{

	}


	case "updateUserTimeline" =>
	{
		remote ! UpdateUserTimeline(userId)
	}

<
	case "updateHomeTimeline" =>
	{
		remote ! UpdateUserTimeline(userId)
	}

	case "updateMentionTimeline" =>
	{
		remote ! UpdateUserTimeline(userId)
	}


	case UserTimeline(Timeline(userId,userTweetList)) =>
	{

	}

	case HomeTimeline(Timeline(userId,homeTweetList)) =>
	{

	}
	 
    case Message(msg) => 
       // println(s"LocalActor received message: '$msg'")
        if (counter < 5) {
            sender ! Message("Hello back to you")
            counter += 1
        }
    case _ =>
        println("LocalActor got something unexpected.")
  }

}
