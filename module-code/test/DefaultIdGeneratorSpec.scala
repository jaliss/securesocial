/**
 * Copyright 2013 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
import org.specs2.mutable.Specification
import play.api.test._
import securesocial.core.DefaultIdGenerator

class DefaultIdGeneratorSpec extends Specification {
  val generator = new DefaultIdGenerator(new FakeApplication())

  "DefaultIdGenerator" should {
    "generate an id with the correct size" in {
      val id = generator.generate
      id.length == generator.IdSizeInBytes * 2
    }
  }

}
