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
import java.util.ArrayList

import java.security.MessageDigest
import akka.routing.RoundRobinRouter

//   import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._


//Added by Stuti
//case class ServerStatistics(userId : String, keyValue: HashMap[Tweet, Int] )
case class PrintStat(actorName:String, count: Int)
case class PrintUserStat(actorName:String, count: Int)
case class PrintOutUserTweets(actorName:String, count: Int)
case class PrintOutHomeTweets(actorName:String, count: Int)
case class PrintFollowerCount(actorName:String,count: Int)

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

case class TokenizeTweet(tweet:Tweet)
case class PutTweetAgainstToken(token:String,tweet:Tweet)
case class GetTweetsAgainstToken(token:String)
case class SearchToken(token:String)
case object PrintStatistics
case class SentMessages(sourceId: String, tagretId: String, message: String)
case class PutSentMessages(sourceId:String, tagretId: String, message:String)
case class PutReceivedMessages(targetId:String, sourceId:String, message:String)
case class PrivateMessage(fromUserId:String, toUserId:String, message:String)

case class RemoveFollowerToUser(targetId:String, followerid:String)
//Inbox(userid:String,messageList:List[PrivateMessage])
//Outbox(userid:String,messageList:List[PrivateMessage])



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
case PutSentMessages(sourceId, targetId,message) => sourceId
case PutReceivedMessages(targetId, sourceId, message) => targetId
case RemoveFollowerToUser(targetId, followerid) => targetId
}


//val cache = system.actorOf(Props[Cache].withRouter(ConsistentHashingRouter(10, hashMapping = hashMapping)),name = "cache")
val cache = system.actorOf(Props(new Cache(listener)).withRouter(ConsistentHashingRouter(10*nrOfWorkers, hashMapping = hashMapping)),name = "cache")

val parser = system.actorOf(Props(new TweetParser(listener,cache)).withRouter(RoundRobinRouter(10*nrOfWorkers)),name="parser")
//val masterActor = system.actorOf(Props(new Master(nrOfWorkers, listener,cache)),name = "MasterActor")

val masterActor = system.actorOf(Props(new Master(10, listener,cache)).withRouter(RoundRobinRouter(nrOfWorkers)), name = "MasterActor")

	masterActor ! Start 
	masterActor ! Message("The Master is alive and started")
	masterActor ! Register("abc","uid1","pswd")
    masterActor ! Login("uid1","pswd")
    masterActor ! TweetFromUser("HelloTwitter","uid1",System.currentTimeMillis) 
	masterActor ! PrintStatistics
	masterActor ! Register("user2","uid2","pswd")
	masterActor ! Login("uid2","pswd")
	masterActor ! Follow("uid2","uid1")
    masterActor ! TweetFromUser("HelloTwitter","uid1",System.currentTimeMillis) 
    //masterActor ! SentMessages("uid1","uid2", "message")
    //masterActor ! SentMessages("uid1","uid2", "message")
    //masterActor ! SentMessages("uid1","uid2", "message")
	masterActor ! PrintStatistics
	//masterActor ! UnFollow("uid2","uid1")
}

}
class Worker(cacheRouter: ActorRef) extends Actor {

implicit val timeout = akka.util.Timeout(500000)

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
			
		case SearchToken(token) => {
		
			val future = cacheRouter ? GetTweetsAgainstToken(token)
			val searchTimeline = Await.result(future, timeout.duration).asInstanceOf[SearchTimeline]
				sender ! searchTimeline
		}	
			
		case Follow(sourceUserId,targetUserId) =>
			{
				cacheRouter ! PutFollowerToUser(targetUserId,sourceUserId)
			
			}

		case common.UnFollow(sourceUserId,targetUserId) =>
			{
				//println("Before calling cache router")
				cacheRouter ! RemoveFollowerToUser(targetUserId,sourceUserId)			
			}

		case common.SentMessages(sourceId,targetId, message) =>
			{
				//println("message recieved in workerRouter")
				cacheRouter ! PutSentMessages(sourceId,targetId, message)
				cacheRouter ! PutReceivedMessages(targetId, sourceId, message)				
			}
		case FetchUserToFollow(sourceId,randNum) => {
			//cacheRouter ! FetchUserToFollow(sourceId+randNum,randNum)
		}
			
		case PrintStatistics =>
			{
//				printTimelines()
			}
	}
}

class Master(nrOfWorkers: Int, listener: ActorRef,cacheRouter: ActorRef)
	extends Actor {
	implicit val timeout = akka.util.Timeout(500000)
		var nrOfResults: Int = _
		var nrOfClients: Int = _
		val start: Long = System.currentTimeMillis
		var register = new scala.collection.mutable.HashMap[String, String]()
		var tweetlist = new java.util.ArrayList[String]()
 		var totalTweet:Int = 0;
		val workerRouter = context.actorOf(
		Props(new Worker(cacheRouter)).withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")
 		var lastUserId:String = ""
		def receive = {
		
			case FetchUserToFollow(sourceId,randNum) => 
				{
					//println("fetch users to folow")
					//val future = cacheRouter ? FetchUserToFollow(sourceId+randNum,randNum)
					//val targetUserId = Await.result(future, timeout.duration).asInstanceOf[String]
					//println(targetUserId)
					//sender ! Follow(sourceId,targetUserId)
					sender ! Follow(sourceId,lastUserId)
					
				}
		
		
			case Register(userFullName,userId,password) ⇒
				{
					//the client will send the register message, when the server receives the message it will save the details of the user in a map (userId as key and other details as values) that will be used to autheticate the login request.
					
					register += (userId -> password)
					
					//println(s"$userId registered")
					sender ! RegistrationOK
					
				}
				
				
			case Login(userId,password) ⇒ 
				{
					//println("User login successful for "  + userId) 
						sender ! LoginOK
				
				} 
				
			case TweetFromUser(tweet,senderId,time) =>
				{	lastUserId = senderId
					totalTweet += 1
					//print(totalTweet +"\t")
					println("Recieved "+tweet +"at :"+System.currentTimeMillis)
					workerRouter ! ProcessTweet(tweet,senderId,time)
					//sender ! TweetProcessedOK

				}	
				
				
			case Follow(sourceUserId,targetUserId) =>
				{	if(!sourceUserId.equalsIgnoreCase(targetUserId))
						workerRouter ! Follow(sourceUserId,targetUserId)

				}		

			case common.UnFollow(sourceUserId,targetUserId) =>
				{	
					if(!sourceUserId.equalsIgnoreCase(targetUserId)){
						//println("Before calling workerRouter")
						workerRouter ! common.UnFollow(sourceUserId,targetUserId)
					}
				}		

			case common.SentMessages(sourceUserId, targetUserId, message) =>
				{
					//println("Message received in master")
					if(!sourceUserId.equalsIgnoreCase(targetUserId))
						workerRouter ! common.SentMessages(sourceUserId,targetUserId, message)

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
			
			case UpdateSearchTimeline(userId,searchToken) => {
		
				val future = cacheRouter ? GetTweetsAgainstToken(searchToken)
				val searchTimeline = Await.result(future, timeout.duration).asInstanceOf[SearchTimeline]
				sender ! searchTimeline
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
	
	// a parser class to parse the tweets and do any string prossing operation inthe incoming tweets
	class TweetParser(listener:ActorRef,cacheRouter: ActorRef) extends Actor{
		def receive = {
			case TokenizeTweet(tweet) => {
				///[ToDO] tokenize the tweet and save the tweetid/tweet against each token in cache
				var token = ""
				cacheRouter ! PutTweetAgainstToken(token,tweet)
			
			}
		}
	}
	
	class Cache (listener: ActorRef) extends Actor{

        //println("Cache actor "+self+" Created")
        
    var tweetsMap = new scala.collection.mutable.HashMap[String, Tweet]()
	var homeTimelineMap = new scala.collection.mutable.HashMap[String,List[Tweet]]() // userId, tweetlist
	var userTimelineMap = new scala.collection.mutable.HashMap[String,List[Tweet]]()
	var userFollowerMap = new scala.collection.mutable.HashMap[String,List[String]]()
	//var userSentMessageMap = new scala.collection.mutable.HashMap[String,List[String]]()
	//var userSentMessageMap = new scala.collection.mutable.HashMap[String,String]()
	var userSentMessageMap = new scala.collection.mutable.HashMap[String,List[PrivateMessage]]()
	var userReceivedMessageMap = new scala.collection.mutable.HashMap[String,List[PrivateMessage]]()

	val tweetcount=0;
	var userDetailsMap = new scala.collection.mutable.HashMap[String,UserDetails]
	var userArrayList = new java.util.ArrayList[String]()

	var cache = Map.empty[String, String]
	var iTweetsCount=0;
	var outUserTweetCount =0
	var outHomeTweetCount =0
	
	
	//Added by Stuti
	var getTweetStat:akka.actor.Cancellable = _

	/*val system = ActorSystem("BtcMasterSystem")
	val listener = system.actorOf(Props[Listener], name = "listener")*/
//	getTweetStat = context.system.scheduler.schedule(1000 milliseconds, 10000 milliseconds, self, "sendTweetStats")
	
/*	PrivateMessage(fromUserId:String,toUserId:String, message:Message,time:long)
	Inbox(userid:String,messageList:List[PrivateMessage])
	Outbox(userid:String,messageList:List[PrivateMessage])
*/
	def receive = {
		//implicit val system = ActorSystem("LocalSystem")
		


		case "sendTweetStats" => 
		{	val actorName = self.path.name
			listener ! PrintStat(actorName, tweetsMap.size)
			listener ! PrintUserStat(actorName, userTimelineMap.size)
			listener ! PrintOutUserTweets(actorName,outUserTweetCount)
			listener ! PrintOutHomeTweets(actorName,outHomeTweetCount)
			listener ! PrintFollowerCount(actorName,userFollowerMap.foldLeft(0)(_+_._2.size))
			//iTweetsCount = 0
		}
		
		case PutTweetAgainstToken(token,tweet) => {
			// ToDo save the tweet against the token page wise (token -> (pageNo -> TweetList))
		}
		
		case GetTweetsAgainstToken(token) => {
			// ToDo save the tweet against the token page wise (token -> (pageNo -> TweetList))
		}

		case Entry(key, value) => { cache += (key -> value)
        		//println("Key recieved at "+self)
		}
		
		
		case PutTweet(tweetId,tweet) => {
			//println("Cache :"+tweet)
			//println("Total tweets in cache :" + tweetsMap.size)
			tweetsMap += (tweetId -> tweet)
			for ((key, value) <- tweetsMap) {	
				if(value.tweet contains "ipsum" ) {
					println (key +"-->"+ value.tweet) 
				}
			}	
		}
		
		case GetHomeTimeline(userid) => {
		
			val homeTimeline = homeTimelineMap.get(userid) match {
				case Some(list) => list
				case None => List[Tweet]()
			}
		//	sender ! homeTimeline
			
		//val userDetails = userDetailsMap.get(userID).get
		//var (read,unread) = splitAt userDetails.homeTimelineLastFetchIndex
		var (newTweets,oldTweets) = homeTimeline splitAt 400
		outHomeTweetCount = outHomeTweetCount + newTweets.size
		
		 sender ! newTweets
		
		}
		
		case GetUserTimeline(userid) => {
			val userTimeline = userTimelineMap.get(userid) match {
					case Some(list) => list
					case None => List[Tweet]()
			}
		 	
		 	var (newTweets,oldTweets) = userTimeline splitAt 400
		 	outUserTweetCount = outUserTweetCount + newTweets.size
			sender ! newTweets
		
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
			if(!followerList.contains(followerid)){
				//println("followerAdded")
				val newfollowerList = followerid +: followerList
				userFollowerMap += (targetId -> newfollowerList)
			}
			println(userFollowerMap + " "+ self.path.name)
		}


		case RemoveFollowerToUser(targetId,followerid) => {
			//println("Inside cache router")		
			val followerList : List[String] = userFollowerMap.get(targetId) match {
							case Some(list) => list
							case None => List[String]()
						}
			if(followerList.contains(followerid)){
				//println("followerAdded")
				//val newfollowerList =  followerList - followerid
				//userFollowerMap += (targetId -> newfollowerList)
				userFollowerMap -= followerid
			}
			println(userFollowerMap + " "+ self.path.name)
		}
		
		case PutSentMessages(sourceId,targetId, message) => {
			var privateMessageObj = PrivateMessage(sourceId, targetId, message)
			val receiverList : List[PrivateMessage] = userSentMessageMap.get(sourceId) match {
				case Some(list) => list
				case None => List[PrivateMessage]()
			}
				val newReceiverList = privateMessageObj +: receiverList
				userSentMessageMap += (sourceId -> newReceiverList)
				/*println("message recieved in cache")
			userSentMessageMap += (sourceId -> targetId)		
			println("Message added to the sent mesaage List" + userSentMessageMap)
			*/
			println(userSentMessageMap+ "  " + self.path.name)

		}

		case PutReceivedMessages(targetId, sourceId, message) => {
			var privateMessageObj = PrivateMessage(targetId, sourceId, message)
			val senderList : List[PrivateMessage] = userReceivedMessageMap.get(targetId) match {
				case Some(list) => list
				case None => List[PrivateMessage]()
			}
				val newSenderList = privateMessageObj +: senderList
				userReceivedMessageMap += (targetId -> newSenderList)
				/*println("message recieved in cache")
			userSentMessageMap += (sourceId -> targetId)		
			println("Message added to the sent mesaage List" + userSentMessageMap)
			*/
			println(userReceivedMessageMap + "  " + self.path.name)

		}

		case FetchUserToFollow(randKeyString,radnId) => {
			sender ! userTimelineMap.keySet.head
		
		}
		case key: String => sender ! cache.get(key).get
	}
}


class Listener extends Actor {
		var statTweetCount = new scala.collection.mutable.HashMap[String, Int]()
		var prevStatTweetCountMap = new scala.collection.mutable.HashMap[String, Int]()	
		var statUserCount = new scala.collection.mutable.HashMap[String, Int]()
		var prevUserTweetCountMap = new scala.collection.mutable.HashMap[String, Int]()
		
		var outUserTweetCountMap = new scala.collection.mutable.HashMap[String, Int]()
		var outHomeTweetCountMap = new scala.collection.mutable.HashMap[String, Int]()
		var followerCountMap     = new scala.collection.mutable.HashMap[String, Int]()
		
		
		
		var printTweetStat:akka.actor.Cancellable = _
	//	printTweetStat = context.system.scheduler.schedule(1000 milliseconds, 10000 milliseconds, self, "printTweetStatistics")

		var totalTweetCount:Int = 0
		var prevTotalTweetCount:Int = 0
		var prevDelta=0
		var totalUserCount:Int = 0
		var prevTotalUserCount:Int = 0
		var prevDeltaUser=0


		def receive = {
	
/*		case ShutdownMaster(message) ⇒ {
					println("\n\tShutdown MEssage \t%s".format(message))
			context.system.shutdown() 
		}*/

		
		case PrintStat(actorName, count) => {
			//println("TweetCount is " + count + " " + actorName)
			totalTweetCount += count
			statTweetCount += (actorName -> count)
		}

		case "printTweetStatistics" => 
		{
			/*var i: Int = 0
			for(i <- 0 to statTweetCount.size){
				print(actorName+" ---" +count + "\t")
			}*/
			val delta = totalTweetCount - prevTotalTweetCount
			//print("new Tweets recieved :"+totalTweetCount)
			totalTweetCount = 0
			prevTotalTweetCount = totalTweetCount
			
			val totalusers = statUserCount.foldLeft(0)(_+_._2)
			val totalFollowers = followerCountMap.foldLeft(0)(_+_._2)
			var averageFollowerCount:Double  = 0.0
			
				if(totalusers!=0)
				{
					averageFollowerCount = totalFollowers*1.0/totalusers
				}
			print("\nTweets In:"+statTweetCount.foldLeft(0)(_+_._2))
			print(" Users :"+totalusers)
			print(" UserTimelineTweets :"+outUserTweetCountMap.foldLeft(0)(_+_._2))
			print(" HomeTimelineTweets :"+outHomeTweetCountMap.foldLeft(0)(_+_._2))
			print(" FollowerCount :" +totalFollowers)
			print(" AvgFollowerCount :"+averageFollowerCount)
			
			
			/**statTweetCount.foreach {keyVal => print(keyVal._1 + "=" + keyVal._2 +"\t")}
			
			//val delta = totalUserCount - prevTotalUserCount
			//println("new Users :"+totalUserCount)
			totalUserCount = 0
			prevTotalUserCount = totalUserCount
			print(" Users :"+statUserCount.foldLeft(0)(_+_._2))
			//statUserCount.foreach {keyVal => print(keyVal._1 + "=" + keyVal._2 +"\t")}

			//println()
			*/
		}

		case PrintUserStat(actorName, count) => {
			//println("TweetCount is " + count + " " + actorName)
			totalUserCount += count
			statUserCount += (actorName -> count)
		}
		
		case PrintOutUserTweets(actorName,count) => {
			outUserTweetCountMap += (actorName -> count)
		}
		
		case PrintOutHomeTweets(actorName,count) => {
			outHomeTweetCountMap += (actorName -> count)
		}
		
		case PrintFollowerCount(actorName,count) => {
			followerCountMap += (actorName -> count)
		}
	}
}
