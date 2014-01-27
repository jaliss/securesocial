package securesocial.testkit

import securesocial.core.{SocialUser, AuthenticationMethod, IdentityId}
import org.scalacheck.Gen

object SocialUserGenerator {
  val nameGen = for {
    head <- Gen.alphaUpperChar
    size<- Gen.choose(1,10)
    tail <- Gen.listOfN(size,Gen.alphaLowerChar)
  } yield (head +: tail).mkString("")

  def identityIdGen = for{
    pid <- nameGen
    uid <- nameGen
  } yield IdentityId(uid, pid)

  def identityId = identityIdGen.sample.get

  def authMethodGen = Gen.oneOf(AuthenticationMethod.OAuth1,AuthenticationMethod.OAuth2,AuthenticationMethod.OpenId,AuthenticationMethod.UserPassword)

  def authMethod = authMethodGen.sample.get

  def socialUserGen(id:IdentityId=identityId,authMethod: AuthenticationMethod=authMethod)= for{
    firstName <- nameGen
    lastName <- nameGen
    email = s"${firstName.head}.$lastName@example.com"
  }yield SocialUser(id, firstName, lastName, s"$firstName $lastName",Some(email),None, authMethod,None, None, None)

  def socialUser(id:IdentityId=identityId,authMethod: AuthenticationMethod=authMethod)=socialUserGen(id, authMethod).sample.get
}