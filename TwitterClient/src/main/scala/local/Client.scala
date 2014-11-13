package local

import akka.actor._
import common._
import scala.util.Random
import java.security.MessageDigest
import akka.routing.RoundRobinRouter
import java.net.InetAddress
import java.security.MessageDigest

case class RemoteDetail(remoteActorString : String)

object Local {

	def main(args: Array[String]){
	
//	println(" Scala Version : "+util.Properties.versionString)
// println("Argument 0 :"+ args(0))
val serverIP = args(0)
val serverPort = "5150"

  implicit val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props(new LocalActor(serverIP,serverPort)), name = "LocalActor")  // the local actor
  localActor ! Start                                                       // start the action

	}

}

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

	
class LocalActor(masterIP: String , masterPort: String) extends Actor {

// create the remote actor
val remoteActorString = "akka.tcp://BtcMasterSystem@"+masterIP+":"+masterPort+"/user/MasterActor"

//println(remoteActorString)
val remote = context.actorFor(remoteActorString)
var counter = 0

var tweet: String = Random.nextString(140)

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
	for(i <- 1 to 10000)
		remote ! Register("abc"+i,"user"+i,"pswd"+i)

	// login users
	for(i <- 1 to 10000)
		remote ! Login("user"+i,"pswd"+i)	

	// send the generated random tweets to the server
	for(i <- 1 to 10000)
		remote ! TweetFromUser(tweet, "user"+i, System.currentTimeMillis)	

	
     
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


