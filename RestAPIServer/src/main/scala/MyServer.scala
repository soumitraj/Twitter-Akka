
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
  
  var users = Server.users
  
  def getJson(route: Route): Route ={
  	get{
		respondWithMediaType(MediaTypes.`application/json`)
		{
  				route
  		}
  	}
  }
  
  val remote = actorSystem.actorFor(remoteActorString) 
  lazy val helloActor = actorSystem.actorOf(Props(new HelloActor()))
  lazy val TestActor = actorSystem.actorOf(Props(new TestActor()))
  
  println(remote)
  lazy val helloRoute2 = get{
  	path("hello2"){
  		ctx => helloActor ! ctx
  	}
  
  }
  
  lazy val helloRoute = get{
  	path("hello"){
  	complete{
  			"Hello from Server"	
  	    	}
 //	ctx => ctx.complete("HellO from Server")
  	 }
  	}
  	
  	
  	
  	lazy val burnRoute = get{
  		path("burn"/ "remaining"){
  			complete{
  				(TestActor ? GetTestAttribute)
  					.mapTo[Int]
  					.map(s => "the remaining burh time is :"+s)
  			}
  			
  		}
  	
  	}
  	
  	lazy val sendTweetRoute = get{
  	path("sendMesg"){
//  		 (remote !  TweetFromUser("HelloTwitter","uid1"+Random.nextInt(500),System.currentTimeMillis) )
  		// println(TweetFromUser("HelloTwitter","uid1"+Random.nextInt(500),System.currentTimeMillis))
  		 complete{
  		 		(remote !  TweetFromUser("HelloTwitter","uid1"+Random.nextInt(500),System.currentTimeMillis) )
        //(remote ! SentMessages("uid1","uid2", "message"))
  					"OK"
  				}
  	}
  
  }
  
  lazy val showTweetRoute = get {
  			path("statuses"/"show"){
  				parameters("id"){id =>
  					complete{
 						//	(remote ? GetTweetById(id)).mapTo[Tweet].map(tweet => "Tweet is :"+tweet)
 						
 						//(   Await.result(remote ? GetTweetById(id), timeout.duration).asInstanceOf[String])
 							//"Path Ok"
 							//(remote ? GetTweetById(id)).mapTo[Tweet].map(tweet => "Tweet is :"+tweet)
 							
 						val future = remote ? GetTweetById(id)
						val userTweet = Await.result(future, timeout.duration).asInstanceOf[Tweet]	
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
           //(remote ! SentMessages("uid1","uid2", "message"))
           "Message sent between " +sourceId+ " and "+targetId
      }
    }
  }
}

lazy val sendTweetRoute2 = get{
    path("sendMesg2"){
//       (remote !  TweetFromUser("HelloTwitter","uid1"+Random.nextInt(500),System.currentTimeMillis) )
      // println(TweetFromUser("HelloTwitter","uid1"+Random.nextInt(500),System.currentTimeMillis))
       complete{
        //  (remote !  TweetFromUser("HelloTwitter","uid1"+Random.nextInt(500),System.currentTimeMillis) )
        (remote ! SentMessages("uid1","uid2", "message"))
            "OK"
          }
    }
  
  }

  startServer(interface = "localhost", port = 8080){
  		sendTweetRoute ~
  		showTweetRoute ~
      sendTweetRoute2 ~
  		helloRoute ~
  		helloRoute2 ~
  		burnRoute ~
      getFriendship~
      destroyFriendship~
      sendMessage~
  		getJson {
  			path("list" / "all"){
	  				complete {
  						Server.toJson(users)
  					}
  			}
  		
  		} ~
  		get {
  			path("user"/IntNumber/"details"){ index => 
  				complete {
  						Server.toJson(users(index))
  				}
  			}
  		} ~
  		post {
  			path("user"/"add"){ name =>
  				val newUser = User("NewUSer")
  				users = newUser :: users
  				
  				complete{
  					"OK"
  				}
  			}
  		}
   	}


}

case class User(userName:String)

object Server{
	var users = List[User]()
		users ::= User("Stuti1")
		users ::= User("Stuti2")
		users ::= User("Stuti3")
		
	private implicit val formats = Serialization.formats(NoTypeHints)
	def toJson(users: List[User]) : String = writePretty(users)
	def toJson(user:User) : String = writePretty(user)
	
	
}

class HelloActor extends Actor {
	override def receive = {
		case ctx: RequestContext => {
			ctx.complete("Hello from Hello Actor")
		}
	
	}

}

class TestActor extends Actor {
	val testattribute = 10
	override def receive = {
		case GetTestAttribute => sender ! testattribute
	}

}

case object GetTestAttribute

object JsonUtil{
	
	private implicit val formats = Serialization.formats(NoTypeHints)
	def toJson(tweet:Tweet) : String = writePretty(tweet)
	
	
}



