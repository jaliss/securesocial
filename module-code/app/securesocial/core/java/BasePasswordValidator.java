/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core.java;

import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import securesocial.core.providers.utils.PasswordValidator;

import java.util.List;

/**
 * The base class for all Java password validators.
 * Subclasses need to implement:
 *
 *             public boolean isValid(String password)
 *             public Tuple2<String, Seq<Object>>  errorMessage()
 *
 * Use the helper method toScalaTuple to create the result for errorMessage()
 */
public abstract class BasePasswordValidator implements PasswordValidator {
   /**
     * A helper method to create the tuple expected by PasswordValidator from a Java String and List objects.
     *
     * @param message
     * @param params
     * @return
     */
    protected Tuple2<String, Seq<Object>> toScalaTuple(String message, List<Object> params) {
        return new Tuple2<String, Seq<Object>>(message, JavaConverters.collectionAsScalaIterableConverter(params).asScala().toSeq());
    }
}
