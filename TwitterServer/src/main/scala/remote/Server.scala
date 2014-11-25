package remote

import akka.routing.ConsistentHashingRouter
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.actor._
import scala.concurrent._
import akka.pattern.ask
import akka.util.Timeout
import common._
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.security.MessageDigest
import akka.routing.RoundRobinRouter

//   import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._


//Added by Stuti
//case class ServerStatistics(userId : String, keyValue: HashMap[Tweet, Int] )
case class PrintStat(actorName:String, count: Int)

case class ProcessTweet(tweet:String,uid:String,time:Long)
case class UserDetails(userId:String, userName:String, homeTimelineLastFetchIndex:Int)

case class PutTweetHomeTimline(userid:String,tweet:Tweet)
case class PutTweetUserTimline(userid:String,tweet:Tweet)
case class PutFollowerToUser(targetId:String,followerid:String)
case class PutTweet(tweetid:String,tweet:Tweet)

case class GetHomeTimeline(userid:String)
case class GetUserTimeline(userid:String)
case class GetFollowerList(userid:String)
case class Entry(key: String, value: String)

case object PrintStatistics


object TwitterServer {

def main(args : Array[String]){

//implicit val timeout = akka.util.Timeout(500)
//println("Scala version :: "+scala.util.Properties.versionString)
  

val nrOfWorkers = java.lang.Integer.parseInt(args(0))
val system = ActorSystem("BtcMasterSystem")
val listener = system.actorOf(Props[Listener], name = "listener")


def hashMapping: ConsistentHashMapping = {
case Entry(key, _) => key
case s: String => s
case PutTweetHomeTimline(userid,tweet) => userid
case PutTweetUserTimline(userid,tweet) => userid
case PutFollowerToUser(targetId,followerid) => targetId
case PutTweet(tweetid,tweet) => tweetid
case GetHomeTimeline(userid) => userid
case GetUserTimeline(userid) => userid
case GetFollowerList(userid) => userid
}


//val cache = system.actorOf(Props[Cache].withRouter(ConsistentHashingRouter(10, hashMapping = hashMapping)),name = "cache")
val cache = system.actorOf(Props(new Cache(listener)).withRouter(ConsistentHashingRouter(2, hashMapping = hashMapping)),name = "cache")


//val masterActor = system.actorOf(Props(new Master(nrOfWorkers, listener,cache)),name = "MasterActor")

val masterActor = system.actorOf(Props(new Master(nrOfWorkers, listener,cache)).withRouter(RoundRobinRouter(nrOfWorkers)), name = "MasterActor")

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


class Worker(cacheRouter: ActorRef) extends Actor {

implicit val timeout = akka.util.Timeout(5000)

	def receive = {
		case ProcessTweet(tweet,senderId,time) ⇒
			{
				val tweetId = time+"_"+senderId+"_"+Random.nextInt(5000)
				val objTweet = Tweet(tweetId,senderId,time,tweet)
				cacheRouter ! PutTweet(tweetId,objTweet)
				cacheRouter ! PutTweetUserTimline(senderId,objTweet)
								
				val future = cacheRouter ? GetFollowerList(senderId)
				val followerList = Await.result(future, timeout.duration).asInstanceOf[List[String]]
				
				followerList.map { followerId =>
				
				cacheRouter ! PutTweetHomeTimline(followerId,objTweet)
				}
				
			}
			
		case Follow(sourceUserId,targetUserId) =>
			{
				PutFollowerToUser(targetUserId,sourceUserId)
			
			}
			
		case PrintStatistics =>
			{
//				printTimelines()
			}
	
	
	}

}

class Master(nrOfWorkers: Int, listener: ActorRef,cacheRouter: ActorRef)
	extends Actor {
	implicit val timeout = akka.util.Timeout(5000)
		var nrOfResults: Int = _
		var nrOfClients: Int = _
		val start: Long = System.currentTimeMillis
		var register = new scala.collection.mutable.HashMap[String, String]()
		var tweetlist = new java.util.ArrayList[String]()
 		var totalTweet:Int = 0;
		val workerRouter = context.actorOf(
		Props(new Worker(cacheRouter)).withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")
 
		def receive = {
		
		
			case Register(userFullName,userId,password) ⇒
				{
					//the client will send the register message, when the server receives the message it will save the details of the user in a map (userId as key and other details as values) that will be used to autheticate the login request.
					
					register += (userId -> password)
					
					//println(s"$userId registered")
					sender ! RegistrationOK
					
				}
				
				
			case Login(userId,password) ⇒ 
				{
					println("User login successful for "  + userId) 
						sender ! LoginOK
				
				} 
				
			case TweetFromUser(tweet,senderId,time) =>
				{
					totalTweet += 1
					//print(totalTweet +"\t")
					println("Recieved "+tweet +"at :"+System.currentTimeMillis)
					workerRouter ! ProcessTweet(tweet,senderId,time)
					sender ! TweetProcessedOK

				}	
				
				
			case Follow(sourceUserId,targetUserId) =>
				{
					workerRouter ! Follow(sourceUserId,targetUserId)

				}		

			case UpdateUserTimeline(userId) => {
				val future = cacheRouter ? GetUserTimeline(userId)
				val userTweetList = Await.result(future, timeout.duration).asInstanceOf[List[Tweet]]
				sender ! UserTimeline(Timeline(userId,userTweetList))
			
			}
			case UpdateHomeTimeline(userId) => {
				val future = cacheRouter ? GetHomeTimeline(userId)
				val homeTweetList = Await.result(future, timeout.duration).asInstanceOf[List[Tweet]]
				sender ! HomeTimeline(Timeline(userId,homeTweetList))

			}
			case UpdateMentionTimeline(userId) => {

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
	
	class Cache (listener: ActorRef) extends Actor {

        //println("Cache actor "+self+" Created")
        
     var tweetsMap = new scala.collection.mutable.HashMap[String, Tweet]()
	var homeTimelineMap = new scala.collection.mutable.HashMap[String,List[Tweet]]() // userId, tweetlist
	var userTimelineMap = new scala.collection.mutable.HashMap[String,List[Tweet]]()
	var userFollowerMap = new scala.collection.mutable.HashMap[String,List[String]]()
	val tweetcount=0;
	var userDetailsMap = new scala.collection.mutable.HashMap[String,UserDetails]

	var cache = Map.empty[String, String]
	var iTweetsCount=0;
	//Added by Stuti
	var getTweetStat:akka.actor.Cancellable = _

	/*val system = ActorSystem("BtcMasterSystem")
	val listener = system.actorOf(Props[Listener], name = "listener")*/
	getTweetStat = context.system.scheduler.schedule(1000 milliseconds, 10000 milliseconds, self, "printTweetStat")
	

	def receive = {
		//implicit val system = ActorSystem("LocalSystem")
		


		case "printTweetStat" => 
		{
			listener ! PrintStat(self.path.name, tweetsMap.size)
			//iTweetsCount = 0
		}

		case Entry(key, value) => { cache += (key -> value)
        		//println("Key recieved at "+self)
		}
		
		case PutTweet(tweetId,tweet) => {
			println("Cache :"+tweet)
			println("Total tweets in cache :" + tweetsMap.size)
			tweetsMap += (tweetId -> tweet)
		
		}
		
		
		case GetHomeTimeline(userid) => {
		
			val homeTimeline = homeTimelineMap.get(userid).get
			sender ! homeTimeline
			
		//val userDetails = userDetailsMap.get(userID).get
	//	var (read,unread) = splitAt userDetails.homeTimelineLastFetchIndex
		
		
		//return unread
		
		}
		
		case GetUserTimeline(userid) => {
			val userTimeline = userTimelineMap.get(userid).get
			sender ! userTimeline
		
		}
		
		case GetFollowerList(userid) => {
			val userFollowerList = userFollowerMap.get(userid) match {
					case Some(list) => list
					case None => List[String]()
			
			}
			sender ! userFollowerList
		
		}
		
		case PutTweetHomeTimline(userid,tweet) => {
		
			val userHomeTimeline = homeTimelineMap.get(userid) match {
						case Some(timeline) => timeline
						case None => List[Tweet]()
			}
			
			val newHomeTimeline = tweet +: userHomeTimeline
			homeTimelineMap += (userid -> newHomeTimeline)
		
		}
		
		case PutTweetUserTimline(userid,tweet) => {
		
				val userTimeline = userTimelineMap.get(userid) match {
						case Some(timeline) => timeline
						case None => List[Tweet]()
			}
			
			val newUserTimeline = tweet +: userTimeline
			userTimelineMap += (userid -> newUserTimeline)
		
		
		}
		case PutFollowerToUser(targetId,followerid) => {
		
			val followerList : List[String] = userFollowerMap.get(targetId) match {
							case Some(list) => list
							case None => List[String]()
						}
			val newfollowerList = targetId +: followerList

			userFollowerMap += (targetId -> newfollowerList)
		
		}
		case key: String => sender ! cache.get(key).get
	}
}


class Listener extends Actor {
		def receive = {
	
/*		case ShutdownMaster(message) ⇒ {
					println("\n\tShutdown MEssage \t%s".format(message))
			context.system.shutdown() 
		}*/

		

		case PrintStat(actorName, count) => {
			println("TweetCount is " + count + " " + actorName)
		}
	}
}