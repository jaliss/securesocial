/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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

/**
 * A class to uniquely identify users. This combines the id the user has on
 * an external service (eg: twitter, facebook) with a provider.
 */
public class UserId {
    /**
     * The user id on the external provider
     */
    public String id;

    /**
     * The provider id
     */
    public String providerId;

    public UserId() {
    }

    public UserId(final String id, final String provider) {
        this.id = id;
        this.providerId = provider;
    }

    public static UserId fromScala(final securesocial.core.UserId scalaValue) {
        if (scalaValue == null) {
            return null;
        }
        return new UserId(scalaValue.id(), scalaValue.providerId());
    }

    public static securesocial.core.UserId toScala(final UserId javaValue) {
        if (javaValue == null) {
            return null;
        }
        return new securesocial.core.UserId(javaValue.id, javaValue.providerId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((providerId == null) ? 0 : providerId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserId other = (UserId) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (providerId == null) {
            if (other.providerId != null) {
                return false;
            }
        } else if (!providerId.equals(other.providerId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UserId [id=" + id + ", providerId=" + providerId + "]";
    }
}
