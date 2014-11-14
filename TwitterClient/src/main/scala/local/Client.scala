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
case class Profile(numberoftweetsperday: Int, percentageusers: Double)

object Local {

	def main(args: Array[String]){
	
		//	println(" Scala Version : "+util.Properties.versionString)
		// println("Argument 0 :"+ args(0))
		val serverIP = args(0)
		val serverPort = "5150"
		val profileobj1 = new Profile(300, 0.6)
		val profileobj2 = new Profile(100, 0.2)
		val profileobj3 = new Profile(10, 0.2)
		val profiles = List(profileobj1, profileobj2, profileobj3)

		val profileCount: Int = profiles.length
		val totalusers: Int = 100000
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
 				val userActor = system.actorOf(Props(new UserActor(serverIP,serverPort,profiles,"user"+j)), name = "UserActor")  // the user actor
 		 		userActor ! Start                                                       // start the action
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
	
class UserActor(masterIP: String , masterPort: String, profiles: List[Any], userId : String) extends Actor {

// create the remote actor
val remoteActorString = "akka.tcp://BtcMasterSystem@"+masterIP+":"+masterPort+"/user/MasterActor"

//println(remoteActorString)
val remote = context.actorFor(remoteActorString)
var counter = 0

var tweet: String = Random.nextString(140)
var senderId: String = userId
val time: Long = System.currentTimeMillis

var userFullName: String = _
var password: String = _

implicit val system = ActorSystem("LocalSystem")
var schedulor:akka.actor.Cancellable = _

def receive = {
  	case RemoteDetail(remoteActorString) =>
	println("Details recieved : "+remoteActorString)
	val remote2 = context.actorFor(remoteActorString)
	println("sending bind request to remote")
	remote ! BindRequest
   	case Start =>
        remote ! Message("Hello from the LocalActor")
        remote ! BindRequest 
    case BindOK =>
      //  sender ! RequestWork

    // register users
	case Register(userFullName,userId,password) =>
	{
		remote ! Register(userFullName, userId, password)
	}

	// login users
	case Login(userId, password) =>
	{
		remote ! Login(userId, password)	
	}  
    
    case Profile(numberoftweetsperday,percentageusers) =>
    	var tweetpermillisecond = numberoftweetsperday/24*60*60*1000
    	var timepertweet = 1/tweetpermillisecond          // in milliseconds
    	schedulor = context.system.scheduler.schedule(0 millis, timepertweet millis, self, "tickTweet")
    
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


