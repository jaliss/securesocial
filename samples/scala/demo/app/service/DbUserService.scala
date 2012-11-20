/**
 * Copyright 2012 Giovanni Di Mingo (giovanni at dimingo dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service

import play.api.{Logger, Application}
import securesocial.core.{UserServicePlugin, UserId, SocialUser}
import java.util.UUID
import org.joda.time.DateTime
import securesocial.core.providers.Token
import securesocial.core.AuthenticationMethod
import securesocial.core.PasswordInfo

import play.api.db._
import anorm._

import play.api.Play.current

import java.sql.Timestamp


/**
 * A Database user service in Scala
 *
 */
class DbUserService(application: Application) extends UserServicePlugin(application) {
  
  
  /**
   * find user
   * 
   */
  def find(id: UserId) = {

    if (Logger.isDebugEnabled) {
      Logger.debug("find...")
      Logger.debug("id = %s".format(id.id))
    }
    
    DB.withConnection { implicit c =>

      val sqlQuery = SQL(
		  """
		    select * from USER
		    where id = {id};
		  """).on("id" -> id.id)

      // Transform the resulting Stream[Row] to a List[SocialUser]
      val users = sqlQuery().map(row =>
        SocialUser(
            UserId(row[String]("id"), row[String]("provider")), 
            row[String]("first_name"), 
            row[String]("last_name"), 
            row[String]("first_name") + " " + row[String]("last_name"), 
            row[Option[String]]("email"),
            None, 
            AuthenticationMethod("userPassword"),
            None,
            None,
            Some(PasswordInfo(row[String]("password"), None))
        )).toList
        
      val socialUser = if (users.size == 1) Some(users(0)) else None
      
      if (Logger.isDebugEnabled) {
        Logger.debug("socialUser = %s".format(socialUser))
      }
      
      socialUser

    } // end DB
    
  } // end find

  
  /**
   * findByEmailAndProvider user
   * 
   */
  def findByEmailAndProvider(email: String, providerId: String): Option[SocialUser] = {
    
    if (Logger.isDebugEnabled) {
      Logger.debug("findByEmailAndProvider...")
      Logger.debug("email = %s".format(email))
      Logger.debug("providerId = %s".format(providerId))
    }
    
    DB.withConnection { implicit c =>

      val sqlQuery = SQL(
		  """
		    select * from USER
		    where email = {email}
              and provider = {provider}
		  """).on(
		      'email -> email,
		      'provider -> providerId
		      )

      // Transform the resulting Stream[Row] to a List[SocialUser]
      val users = sqlQuery().map(row =>
        SocialUser(
            UserId(row[String]("id"), row[String]("provider")),
            row[String]("first_name"), 
            row[String]("last_name"), 
            row[String]("first_name") + " " + row[String]("last_name"), 
            row[Option[String]]("email"),
            None, 
            AuthenticationMethod("userPassword"),
            None,
            None,
            Some(PasswordInfo(row[String]("password"), None))
        )).toList
        
      val socialUser = if (users.size == 1) Some(users(0)) else None
      
      if (Logger.isDebugEnabled) {
        Logger.debug("socialUser = %s".format(socialUser))
      }
      
      socialUser

    } // end DB
    
  } // end findByEmailAndProvider
  

  /**
   * save user
   * (actually save or update)
   * 
   */
  def save(user: SocialUser) {

    if (Logger.isDebugEnabled) {
      Logger.debug("save...")
      Logger.debug("user = %s".format(user))
    }

    DB.withConnection { implicit c =>
      
      val sqlSelectQuery = SQL(
		  """
		    select * from USER
		    where id = {id};
		  """).on("id" -> user.id.id)

      val users = sqlSelectQuery().map(row =>
        SocialUser(
            UserId(row[String]("id"), row[String]("provider")),
            row[String]("first_name"), 
            row[String]("last_name"), 
            row[String]("first_name") + " " + row[String]("last_name"), 
            row[Option[String]]("email"),
            None, 
            AuthenticationMethod("userPassword"),
            None,
            None,
            Some(PasswordInfo(row[String]("password"), None))
        )).toList
        
      val socialUser = if (users.size == 1) Some(users(0)) else None
      
      if (Logger.isDebugEnabled) {
        Logger.debug("socialUser = %s".format(socialUser))
      }
      
      if (socialUser == None) { // user not exists

        if (Logger.isDebugEnabled) {
          Logger.debug("INSERT")
        }

        // create a new user
        val sqlQuery = SQL(
          """
		    insert into USER 
    		  (id, provider, first_name, last_name, email, password)
    		values
    		  ({id}, {provider}, {first_name}, {last_name}, {email}, {password})
		  """).on(
            'id -> user.id.id,
            'provider -> user.id.providerId,
            'first_name -> user.firstName,
            'last_name -> user.lastName,
            'email -> user.email,
            'password -> user.passwordInfo.get.password)

        val result: Int = sqlQuery.executeUpdate()

        if (Logger.isDebugEnabled) {
          Logger.debug("result = %s".format(result))
        }

      } else { // user exists
        
        if (Logger.isDebugEnabled) {
          Logger.debug("UPDATE")
        }
        
        // update the user
        val sqlQuery = SQL(
          """
		    update USER 
    		  set id = {id}, 
                  provider = {provider},
        		  first_name = {first_name}, 
        		  last_name = {last_name}, 
                  email = {email}, 
                  password = {password}
              where id = {id}
		  """).on(
            'id -> user.id.id,
            'provider -> user.id.providerId,
            'first_name -> user.firstName,
            'last_name -> user.lastName,
            'email -> user.email,
            'password -> user.passwordInfo.get.password)

        val result: Int = sqlQuery.executeUpdate()

        if (Logger.isDebugEnabled) {
          Logger.debug("result = %s".format(result))
        }

      } // end else

    } // end DB
    
  } // end save
  

  /**
   * save token
   * 
   */
  def save(token: Token) {
    
    if (Logger.isDebugEnabled) {
      Logger.debug("save...")
      Logger.debug("token = %s".format(token))
    }

    DB.withConnection { implicit c =>
      
		if (Logger.isDebugEnabled) {
		  Logger.debug("INSERT")
		}
		
		// create a new token
		val sqlQuery = SQL(
		  """
		    insert into TOKEN 
			  (uuid, email, creation_time, expiration_time, is_sign_up)
			values
			  ({uuid}, {email}, {creation_time}, {expiration_time}, {is_sign_up})
		  """).on(
		    'uuid -> token.uuid,
		    'email -> token.email,
		    'creation_time -> token.creationTime.toString("yyyy-MM-dd HH:mm:ss"),
		    'expiration_time -> token.expirationTime.toString("yyyy-MM-dd HH:mm:ss"),
		    'is_sign_up -> token.isSignUp
		    )
		
		val result: Int = sqlQuery.executeUpdate()
		
		if (Logger.isDebugEnabled) {
		  Logger.debug("result = %s".format(result))
		}

    } // end DB
    
  } // end save
  

  /**
   * findToken
   * 
   */
  def findToken(token: String): Option[Token] = {
    
    if (Logger.isDebugEnabled) {
      Logger.debug("findToken...")
      Logger.debug("token = %s".format(token))
    }

    DB.withConnection { implicit c =>
      
      val sqlSelectQuery = SQL(
		  """
		    select * from TOKEN
		    where uuid = {uuid};
		  """).on("uuid" -> token)

      val tokens = sqlSelectQuery().map(row => {
        val creationTime = row[java.util.Date]("creation_time")
   		val expirationTime = row[java.util.Date]("expiration_time")
        if (Logger.isDebugEnabled) {
          Logger.debug("creationTime = %s".format(creationTime))
          Logger.debug("expirationTime = %s".format(expirationTime))
        }
        Token(
            row[String]("uuid"), 
            row[String]("email"), 
            new DateTime(creationTime),
            new DateTime(expirationTime), 
            row[Boolean]("is_sign_up")
        )
      }).toList
        
      val foundToken = if (tokens.size == 1) Some(tokens(0)) else None
      
      if (Logger.isDebugEnabled) {
        Logger.debug("foundToken = %s".format(foundToken))
      }
      
      foundToken

    } // end DB
    
  } // end findToken
  

  /**
   * deleteToken
   * 
   */
  def deleteToken(uuid: String) {
    
    if (Logger.isDebugEnabled) {
      Logger.debug("deleteToken...")
      Logger.debug("uuid = %s".format(uuid))
    }

    DB.withConnection { implicit c =>
      
      // delete token
      val sqlQuery = SQL(
		"""
		  delete from TOKEN 
		  where uuid = {uuid};
		""").on("uuid" -> uuid)
		
	  val result: Int = sqlQuery.executeUpdate()
		
	  if (Logger.isDebugEnabled) {
	    Logger.debug("result = %s".format(result))
	  }
      
    } // end DB
    
  } // end deleteToken
  

  /**
   * deleteTokens
   * 
   */
  def deleteTokens() {
    
    if (Logger.isDebugEnabled) {
      Logger.debug("deleteTokens...")
    }

    DB.withConnection { implicit c =>
      
      // delete all tokens
      val sqlQuery = SQL(
		"""
		  delete from TOKEN;
		""")
		
	  val result: Int = sqlQuery.executeUpdate()
		
	  if (Logger.isDebugEnabled) {
	    Logger.debug("result = %s".format(result))
	  }
      
    } // end DB
    
  } // end deleteTokens
  

  /**
   * deleteExpiredTokens
   * 
   */
  def deleteExpiredTokens() {
    
    if (Logger.isDebugEnabled) {
      Logger.debug("deleteExpiredTokens...")
    }

    DB.withConnection { implicit c =>
      
      // delete expired tokens
      val sqlQuery = SQL(
		"""
		  delete from TOKEN 
		  where EXPIRATION_TIME < NOW();
		""")
		
	  val result: Int = sqlQuery.executeUpdate()
		
	  if (Logger.isDebugEnabled) {
	    Logger.debug("result = %s".format(result))
	  }
      
    } // end DB
    
  } // end deleteExpiredTokens
  
  
} // end DbUserService
