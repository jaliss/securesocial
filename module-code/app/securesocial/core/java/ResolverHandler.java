package securesocial.core.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import play.api.i18n.Lang;
import play.api.mvc.Cookies;
import play.api.mvc.Flash;
import play.api.mvc.Headers;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBody;
import play.mvc.Http.Session;
import scala.Function1;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;


public abstract class ResolverHandler {
	public static abstract class Resolver {
	    
	    public abstract String getLoginUrl();
	    public abstract String getLogoutUrl();
	    public abstract String getAuthenticateUrl(final String provider);
	    
	    public String getLoginUrlAbsolute(final Request req) {
	    	return getLoginUrlAbsolute(ResolverHandler.req2req(null,req));
	    }
	    
	    public String getLoginUrlAbsolute(final play.api.mvc.Request<?> req) {
	    	return getLoginUrlAbsolute(ResolverHandler.req2req(req));
	    }
	    
	    public String getLogoutUrlAbsolute(final Request req) {
	    	return getLogoutUrlAbsolute(ResolverHandler.req2req(null,req));
	    }
	    public String getLogoutUrlAbsolute(final play.api.mvc.Request<?> request) {
	    	return getLogoutUrlAbsolute(ResolverHandler.req2req(request));
	    }
	    
	    public String getAuthenticateUrlAbsolute(final String provider, final Request req) {
	    	return getAuthenticateUrlAbsolute(provider, ResolverHandler.req2req(null,req));
	    }
	    
	    public String getAuthenticateUrlAbsolute(final String provider, final play.api.mvc.Request<?> request) {
	    	return getAuthenticateUrlAbsolute(provider, ResolverHandler.req2req(request));
	    }
	}
	private static Resolver resolver;
	
	public static void setResolver(final Resolver r) {
		resolver = r;
	}
	
	public static Resolver getResolver() {
		if(resolver == null) {
			throw new RuntimeException("You must set a resolver for securesocial2 in your GlobalSettings first");
		}
		return resolver;
	}

	public static Request req2req(final play.api.mvc.Request<?> request) {
		return new Request() {
			
			@Override
			public String uri() {
				return request.uri();
			}
			
			// 2.1-SNAPSHOT compatibility
			public String remoteAddress() {
				return null;
			}
			
			@Override
			public java.util.Map<String, String[]> queryString() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String path() {
				return request.path();
			}
			
			@Override
			public String method() {
				return request.method();
			}
			
			@Override
			public String host() {
				return request.host();
			}
			
			@Override
			public java.util.Map<String, String[]> headers() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public play.mvc.Http.Cookies cookies() {
				// TODO Auto-generated method stub
				return null;
			}
			
			// 2.1-SNAPSHOT compatibility
			public boolean accepts(String arg0) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public java.util.List<play.i18n.Lang> acceptLanguages() {
				// TODO Auto-generated method stub
				return null;
			}
			
			// 2.1-SNAPSHOT compatibility
			public java.util.List<String> accept() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public RequestBody body() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public static play.api.mvc.Request<?> req2req(final Session s, final Request request) {
		return new play.api.mvc.Request() {
	
			// 2.1-SNAPSHOT compatibility
			public Seq<String> accept() {
				return null;
			}
	
			@Override
			public Seq<Lang> acceptLanguages() {
				final java.util.List<play.i18n.Lang> langs = request.acceptLanguages();
				final java.util.List<Lang> ret = new ArrayList<Lang>(langs.size());
				for(final play.i18n.Lang lang: langs) {
					ret.add(new Lang(lang.code(), lang.country()));
				}
				return JavaConversions.asScalaBuffer(ret).toSeq();
			}
	
			// 2.1-SNAPSHOT compatibility
			public boolean accepts(String arg0) {
				return false;
			}
	
			@Override
			public Option<String> charset() {
				// TODO Auto-generated method stub
				return null;
			}
	
			@Override
			public Option<String> contentType() {
				// TODO Auto-generated method stub
				return null;
			}
	
			@Override
			public Cookies cookies() {
				// TODO Auto-generated method stub
				return null;
			}
	
			@Override
			public String domain() {
				// TODO not correct I guess
				return request.host();
			}
	
			@Override
			public Flash flash() {
				// TODO Auto-generated method stub
				return null;
			}
	
			// 2.1-SNAPSHOT compatibility
			public Option<String> getQueryString(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}
	
			@Override
			public Headers headers() {
				// TODO Auto-generated method stub
				return null;
			}
	
			@Override
			public String host() {
				return request.host();
			}
	
			@Override
			public String method() {
				return request.method();
			}
	
			@Override
			public String path() {
				return request.path();
			}
	
			@SuppressWarnings("unchecked")
			@Override
			public scala.collection.immutable.Map<String, Seq<String>> queryString() {
				final java.util.Map<String, String[]> qs = request.queryString();
				final Map<String, Seq<String>> ret = new HashMap<String, Seq<String>>();
				for (java.util.Map.Entry<String, String[]> entry : qs.entrySet()) {
					 ret.put(entry.getKey(), JavaConversions.asScalaBuffer(java.util.Arrays.asList(entry.getValue())).toSeq());
				}
				return new scala.collection.immutable.HashMap().$plus$plus(ret);
			}
	
			@Override
			public String rawQueryString() {
				// TODO Auto-generated method stub
				return null;
			}
	
			// 2.1-SNAPSHOT compatibility
			public String remoteAddress() {
				return null;
			}
	
			@Override
			public play.api.mvc.Session session() {
				final Map<String, String> ret = new HashMap<String, String>();
				for(java.util.Map.Entry<String, String> entry: s.entrySet()) {
					ret.put(entry.getKey(), entry.getValue());
				}
				
				
				final scala.collection.immutable.Map<String, String> m = new scala.collection.immutable.HashMap().$plus$plus(JavaConversions.asScalaMap(s));
				final play.api.mvc.Session sess = new play.api.mvc.Session(m);
				return sess;
			}
	
			@Override
			public String uri() {
				// TODO Auto-generated method stub
				return null;
			}
	
			@Override
			public Object body() {
				// TODO Auto-generated method stub
				return null;
			}
	
			@Override
			public play.api.mvc.Request map(Function1 arg0) {
				// TODO Auto-generated method stub
				return null;
			}
	
		};
	}
}