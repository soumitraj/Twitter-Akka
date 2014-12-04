
import akka.actor._
import spray.routing.SimpleRoutingApp

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write,writePretty}
import spray.http._
import spray.http.MediaTypes._
import spray.routing.Route
import spray.routing.directives._

object MyServer extends App with SimpleRoutingApp{
 implicit val actorSystem = ActorSystem()
  
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
  
  lazy val helloRoute = get{
  	path("hello"){
  	complete{
  					"Hello from Server"	
  	    	}
 //	ctx => ctx.complete("HellO from Server")
  	}
  	}
  
  startServer(interface = "localhost", port = 8080){
  		helloRoute ~
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

