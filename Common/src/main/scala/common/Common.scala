package common
import scala.collection.mutable.MutableList

case object Start // local sends to local to start
case object BindRequest // local worker sends to master
case object BindOK
case object RegistrationOK
case object LoginOK
case object TweetProcessedOK
case object FollowingAcceptedOK


case class Tweet(tweetId : String,userId:String,time:Long,tweet:String) extends Serializable
case class TweetFromUser(tweet : String,senderId:String,tweetTimeInMillisec:Long)
case class Register(userFullName:String,userId:String,password:String)
case class Login(userId:String,password:String)
                           
case class Timeline(userId:String,tweetList:List[Tweet]) extends Serializable
case class UserTimeline(timeline:Timeline) extends Serializable
case class HomeTimeline(timeline:Timeline) extends Serializable
case class SearchTimeline(tweetList:MutableList[String],pageNo:Int) extends Serializable
case class FollowerList(userId:String,followers:List[String])

case class Follow(sourceId:String, targetId: String)
case class UnFollow(sourceId:String, targetId: String)
case class GetFollowerList(userid:String)

case class FetchUserToFollow(sourceId : String,rand: Int)

case class Message(msg: String)

case class ShutdownMaster(message: String)

case class FollowTarget(followerId:String,targetUID :String)

case class UpdateUserTimeline(userId: String)
case class UpdateHomeTimeline(userId: String)
case class UpdateMentionTimeline(userId: String)
case class UpdateSearchTimeline(userId: String,searchToken:String)
case class SentMessages(sourceId:String, tagretId: String, message:String)

case class GetTweetById(tweetId:String)
case class DeleteTweetById(id:String)
