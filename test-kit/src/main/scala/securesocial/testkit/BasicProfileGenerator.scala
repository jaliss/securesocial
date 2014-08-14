package securesocial.testkit

import org.scalacheck.Gen
import securesocial.core.{AuthenticationMethod, BasicProfile}

object BasicProfileGenerator {
  val nameGen = for {
    head <- Gen.alphaUpperChar
    size<- Gen.choose(1,10)
    tail <- Gen.listOfN(size,Gen.alphaLowerChar)
  } yield (head +: tail).mkString("")

  def authMethodGen = Gen.oneOf(AuthenticationMethod.OAuth1,AuthenticationMethod.OAuth2,AuthenticationMethod.OpenId,AuthenticationMethod.UserPassword)

  def authMethod = authMethodGen.sample.get
  def userId = nameGen.sample.get
  def providerId= nameGen.sample.get

  def basicProfileGen(userId:String, providerId:String,authMethod: AuthenticationMethod=authMethod)= for{
    firstName <- nameGen
    lastName <- nameGen
    email = s"${firstName.head}.$lastName@example.com"
  }yield BasicProfile(userId,providerId, Some(firstName), Some(lastName), Some(s"$firstName $lastName"),Some(email),None, authMethod,None, None, None)

  def basicProfile(userId:String=userId, providerId:String=providerId,authMethod: AuthenticationMethod=authMethod)=basicProfileGen(userId,providerId, authMethod).sample.get
}
