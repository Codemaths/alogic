package com.alogic.remote.httpclient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultClientConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import com.alogic.remote.AbstractClient;
import com.alogic.remote.Request;
import com.anysoft.util.Properties;
import com.anysoft.util.PropertiesConstants;

/**
 * 基于apache http client实现的Client
 * 
 * @author yyduan
 * @since 1.6.8.12
 * 
 * @version 1.6.8.14 <br>
 * - 优化http远程调用的超时机制 <br>
 * 
 * @version 1.6.10.3 [20171009 duanyy] <br>
 * - 增加PUT,GET,DELETE,HEAD,OPTIONS,TRACE,PATCH等http方法;
 * 
 */
public class HttpClient extends AbstractClient{
	/**
	 * http client
	 */
	protected CloseableHttpClient httpClient = null;
	
	/**
	 * request configuration
	 */
	protected RequestConfig requestConfig = null;
	
	protected String encoding = "utf-8";
	
	protected int autoRetryCnt = 2;
	
	/**
	 * Http method
	 * @author yyduan
	 *
	 */
	public enum Method {
		GET(HttpGet.class),
		POST(HttpPost.class),
		DELETE(HttpDelete.class),
		HEAD(HttpHead.class),
		OPTIONS(HttpOptions.class),
		TRACE(HttpTrace.class),
		PATCH(HttpPatch.class),
		PUT(HttpPut.class);
		
		protected Class<? extends HttpRequestBase> clazz;
		
		Method(Class<? extends HttpRequestBase> clazz){
			this.clazz = clazz;
		}
		
		public HttpRequestBase createRequest(){
			try {
				return this.clazz.newInstance();
			} catch (Exception ex){
				return null;
			}
		}
	};
	
	@Override
	public void configure(Properties p) {
		super.configure(p);
		
		encoding = PropertiesConstants.getString(p,"http.encoding",encoding);
		autoRetryCnt = PropertiesConstants.getInt(p, "rpc.ketty.autoRetryTimes", autoRetryCnt);
		
		final long keepAliveTime = PropertiesConstants.getInt(p,"rpc.http.keepAlive.ttl",60000);
		ConnectionKeepAliveStrategy kaStrategy = new DefaultConnectionKeepAliveStrategy() {
		    @Override
		    public long getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
		    	long keepAlive = super.getKeepAliveDuration(response, context);
				if (keepAlive == -1) {
					keepAlive = keepAliveTime;
				}
				return keepAlive;
		    }
		};
		
		List<Header> headers = new ArrayList<Header>();
		boolean keepAliveEnable = PropertiesConstants.getBoolean(p,"rpc.http.keepAlive.enable", true);
		if (keepAliveEnable){
			headers.add(new BasicHeader("Connection",HTTP.CONN_KEEP_ALIVE));
		}else{
			headers.add(new BasicHeader("Connection",HTTP.CONN_CLOSE));
		}
		
		int maxConnPerRoute = PropertiesConstants.getInt(p, "rpc.http.maxConnPerHost", 200);
		int maxConn = PropertiesConstants.getInt(p,"rpc.http.maxConn",2000);
		int ttlOfConn = PropertiesConstants.getInt(p,"rpc.http.keepalive.ttl",60000);
		
		httpClient = HttpClients.custom().
				useSystemProperties().
				setMaxConnPerRoute(maxConnPerRoute).
				setConnectionReuseStrategy(DefaultClientConnectionReuseStrategy.INSTANCE).
				setMaxConnTotal(maxConn).
				setDefaultHeaders(headers).
				setConnectionTimeToLive(ttlOfConn,TimeUnit.MILLISECONDS).
				setKeepAliveStrategy(kaStrategy).build();
		
		int timeOut = PropertiesConstants.getInt(p, "rpc.http.timeout", 10000);
		requestConfig = RequestConfig.custom().
				setConnectionRequestTimeout(PropertiesConstants.getInt(p, "rpc.http.timeout.request", timeOut))
                .setConnectTimeout(PropertiesConstants.getInt(p, "rpc.http.timeout.conn", timeOut))
                .setSocketTimeout(PropertiesConstants.getInt(p, "rpc.http.timeout.socket", timeOut)).build();
	}
	
	@Override
	public Request build(String method) {
		HttpRequestBase request = getRequestByMethod(method);
		if (request == null){
			request = new HttpPost();
		}
		request.setConfig(requestConfig);
		return new HttpClientRequest(httpClient,request,this,encoding,autoRetryCnt);
	}
	
	public HttpRequestBase getRequestByMethod(final String method){
		Method m = Method.valueOf(method.toUpperCase());
		return m.createRequest();
	}
}
