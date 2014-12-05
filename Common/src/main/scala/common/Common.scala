package common


case object Start // local sends to local to start
case object BindRequest // local worker sends to master
case object BindOK
case object RegistrationOK
case object LoginOK
case object TweetProcessedOK
case object FollowingAcceptedOK


case class Tweet(tweetId : String,userId:String,time:Long,tweet:String)
case class TweetFromUser(tweet : String,senderId:String,tweetTimeInMillisec:Long)
case class Register(userFullName:String,userId:String,password:String)
case class Login(userId:String,password:String)
                           
case class Timeline(userId:String,tweetList:List[Tweet])
case class UserTimeline(timeline:Timeline)
case class HomeTimeline(timeline:Timeline)
case class SearchTimeline(timeline:Timeline,pageNo:Int)

case class Follow(sourceId:String, targetId: String)

case class FetchUserToFollow(sourceId : String,rand: Int)

case class Message(msg: String)

case class ShutdownMaster(message: String)

case class FollowTarget(followerId:String,targetUID :String)

case class UpdateUserTimeline(userId: String)
case class UpdateHomeTimeline(userId: String)
case class UpdateMentionTimeline(userId: String)
case class UpdateSearchTimeline(userId: String,searchToken:String)
