package remote

import akka.actor._
import common._
import scala.util.Random

import java.security.MessageDigest
import akka.routing.RoundRobinRouter

//   import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._


case class ProcessTweet(tweet:String,uid:String,time:Long)

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
}
}

class Worker extends Actor {
	
	 
		def receive = {
			case Tweet(tweet, senderId,time) ⇒
				println("tweet recieved by worker")
		}
	}
 
	class Master(nrOfWorkers: Int, listener: ActorRef)
	extends Actor {
 
//		var hashval: java.util.ArrayList[BitCoin] = _ 
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


