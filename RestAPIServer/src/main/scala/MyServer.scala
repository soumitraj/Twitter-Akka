
import akka.actor._
import scala.concurrent._
import akka.pattern.ask
import spray.routing.SimpleRoutingApp

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write,writePretty}
import spray.http._
import spray.http.MediaTypes._
import spray.routing.{Route, RequestContext}
import spray.routing.directives._
import scala.concurrent.duration._
import common._
import scala.util.Random


object MyServer extends App with SimpleRoutingApp{
 implicit val actorSystem = ActorSystem("RESTServerSystem")
 import actorSystem.dispatcher
 
 implicit val timeout = akka.util.Timeout(500000)
 
 val serverIP = args(0)
 val serverPort = "5150"
 val remoteActorString = "akka.tcp://BtcMasterSystem@"+serverIP+":"+serverPort+"/user/MasterActor"
 
  
  // type Route = RequestContext => Unit
  
  //directives
  
  
  def getJson(route: Route): Route ={
  	get{
		respondWithMediaType(MediaTypes.`application/json`)
		{
  				route
  		}
  	}
  }
  
  val remote = actorSystem.actorFor(remoteActorString) 
  
  println(remote)
  
  lazy val helloRoute = get{
  	path("hello"){
  	complete{
  			"Hello from Server"	
  	    	}
 //	ctx => ctx.complete("HellO from Server")
  	 }
  	}
  	
  	
  	
  	lazy val sendTestMesgRoute = get{
  	respondWithMediaType(MediaTypes.`application/json`)
  	path("sendMesg"){
  	val newTweet = TweetFromUser("HelloTwitter","uid1"+Random.nextInt(500),System.currentTimeMillis)
  		 complete{
  		 		(remote !  newTweet )
  					JsonUtil.toJson(newTweet)
  				}
  	}
  
  }
  
   	lazy val sendTweetRoute = get{
   	respondWithMediaType(MediaTypes.`application/json`)
  	path("status"/"add"){
  		parameters("tweetText"?,"senderId"){(text,senderId) =>
  		val newTweet =TweetFromUser(text.get,senderId,System.currentTimeMillis)
  		 complete{
	 			(remote !  newTweet )
  					JsonUtil.toJson(newTweet)
  				}
  		}
  	}
  
  }
  
  lazy val showTweetRoute = get {
  respondWithMediaType(MediaTypes.`application/json`)
  			path("statuses"/"show"){
  				parameters("id"){id =>
  					complete{
 						val future = remote ? GetTweetById(id)
						val userTweet = Await.result(future, timeout.duration).asInstanceOf[Tweet]	
							JsonUtil.toJson(userTweet)
 						}
  				}
  				
  			}
  		}
  		
  lazy val fetchUserTimelineRoute = get {
  respondWithMediaType(MediaTypes.`application/json`)
  			path("statuses"/"user_timeline"){
  				parameters("id"){id =>
  					complete{
 						var future = remote ? UpdateUserTimeline(id)
						var userTweet = Await.result(future, timeout.duration).asInstanceOf[UserTimeline]	
							JsonUtil.toJson(userTweet.timeline)
 						}
  				}
  				
  			}
  		}
  		
    lazy val fetchHomeTimelineRoute = get {
    respondWithMediaType(MediaTypes.`application/json`)
  			path("statuses"/"home_timeline"){
  				parameters("id"){id =>
  					complete{

 						var future = remote ? UpdateHomeTimeline(id)
						var userTweet = Await.result(future, timeout.duration).asInstanceOf[HomeTimeline]	
							JsonUtil.toJson(userTweet.timeline)
 						}
  				}
  				
  			}
  		}				
  		
  		
   lazy val distroyTweetRoute = get {
   respondWithMediaType(MediaTypes.`application/json`)
  			path("statuses"/"distroy"){
  				parameters("id"){id =>
  				val future = remote ? GetTweetById(id)
				val userTweet = Await.result(future, timeout.duration).asInstanceOf[Tweet]	
							
  					complete{
 							(remote ? DeleteTweetById(id))
 						    JsonUtil.toJson(userTweet)
 						}
  				}
  				
  			}
  		}		
  
lazy val getFriendship = get{
  path("friendship"/"add"){
    parameters("sourceId"?, "targetId"){
    (sourceId, targetId)=>
      complete{
           remote ! Follow(sourceId.get, targetId)
           "Friendship added between" +sourceId+ "and "+targetId
      }
    }
  }
}

lazy val getFollowerListRoute = get{
  path("followers"/"ids"){
    parameters("userid"){
    (userid)=>
         val future = remote ? GetFollowerList(userid)
				val followers = Await.result(future, timeout.duration).asInstanceOf[FollowerList]	
      complete{
           JsonUtil.toJson(followers)
      }
    }
  }
}


lazy val destroyFriendship = get{
  path("friendship"/"destroy"){
    parameters("sourceId"?, "targetId"){
    (sourceId, targetId)=>
      complete{
           (remote ! UnFollow(sourceId.get, targetId))
           "Friendship destroyed between" +sourceId+ "and "+targetId
      }
    }
  }
}

lazy val sendMessage = get{
  path("sendMessage"/"add"){
    parameters("sourceId"?, "targetId"?, "message"){
    (sourceId, targetId, message)=>
      complete{
           (remote ! SentMessages(sourceId.get, targetId.get, message))
           "Message sent between " +sourceId+ " and "+targetId
      }
    }
  }
}

lazy val sendTweetRoute2 = get{
    path("sendMesg2"){
       complete{
        (remote ! SentMessages("uid1","uid2", "message"))
            "OK"
          }
    }
  
  }

  startServer(interface = "localhost", port = 8080){
  		sendTweetRoute ~
  		distroyTweetRoute ~
  		fetchUserTimelineRoute ~
  		fetchHomeTimelineRoute ~
  		showTweetRoute ~
      sendTweetRoute2 ~
  		helloRoute ~
      getFriendship~
      getFollowerListRoute ~
      destroyFriendship~
      sendTestMesgRoute ~
      sendMessage
//  		post {
//  			path("user"/"add"){ name =>
//  				val newUser = User("NewUSer")
//  				users = newUser :: users
//  				
//  				complete{
//  					"OK"
//  				}
//  			}
//  		}
   	}


}

object JsonUtil{
	
	//private implicit val formats = Serialization.formats(NoTypeHints)
	implicit val formats = native.Serialization.formats(ShortTypeHints(List(classOf[Tweet])))
	def toJson(tweet:Tweet) : String = writePretty(tweet)
	def toJson(timeline:Timeline) : String = writePretty(timeline)
	def toJson(tweet:TweetFromUser) : String = writePretty(tweet)
	def toJson(stringList:List[String]) : String = writePretty(stringList)
	def toJson(followerList:FollowerList) : String = writePretty(FollowerList)
	
	
	
}



